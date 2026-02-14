package com.airobotcomm.tablet.audio.recorder

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import com.airobotcomm.tablet.audio.AudioConfig
import com.airobotcomm.tablet.audio.AudioEvent
import com.airobotcomm.tablet.audio.AudioWorkState
import com.airobotcomm.tablet.audio.tools.codec.OpusEncoder
import com.airobotcomm.tablet.audio.tools.kws.KwsManager
import java.util.LinkedList
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 录音处理流水线 - 核心业务逻辑实现
 * Refactored to 3-stage concurrent pipeline (Input -> Compute -> Encode/Send)
 *
 * 职责：
 * 1. 计算音频强度 (Level)
 * 2. KWS 关键词检测
 * 3. 音频编码 (Opus)
 * 4. 维护 32 帧历史音频缓存 (PCM)，唤醒后逐帧发送
 */
class AudioRecordPipeline(
    private val context: Context,
    private val config: AudioConfig,
    private val events: MutableSharedFlow<AudioEvent>
) {
    companion object {
        private const val TAG = "AudioRecordPipeline"
        private const val MAX_HISTORY_FRAMES = 32 // 维护 32 帧缓存 (约2s，太小会丢数据)
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // 核心组件
    private val kwsManager = KwsManager(context)
    private val encoder = OpusEncoder(config.recordSampleRate, config.channels, config.frameDurationMs)
    
    // 状态管理
    @Volatile
    private var currentState = AudioWorkState.WAITING
    
    // Pipeline Channels
    // Stage 1 -> Stage 2: 原始PCM数据通道
    private val inputChannel = Channel<ByteArray>(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    
    // Stage 2 -> Stage 3: 处理结果通道
    private data class ProcessingResult(
        val pcmData: ByteArray,
        val isWakeupFrame: Boolean = false
    )
    private val encodingChannel = Channel<ProcessingResult>(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    // 历史帧缓存 (仅在 WAITING 状态下于 Compute 线程维护)
    private val historyBuffer = LinkedList<ByteArray>()

    init {
        if (!kwsManager.init()) {
            Log.e(TAG, "KWS Manager init failed in pipeline")
        }
        startPipeline()
    }

    /**
     * Stage 1: Produce (External Call)
     * 非阻塞推送数据到 Pipeline
     */
    fun processFrame(pcmData: ByteArray) {
        // 仅负责推送，不再挂起等待，触发后续的2/3步去计算，发送
        inputChannel.trySend(pcmData)
    }

    private fun startPipeline() {
        Log.d(TAG, "Starting Audio Pipeline...")
        // Stage 2: Computation Loop (Level, KWS, Buffer Logic)
        scope.launch {
            Log.d(TAG, "Computation Loop Started")
            for (pcmData in inputChannel) {
                try {
                    processComputationStage(pcmData)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in Computation Loop", e)
                }
            }
        }

        // Stage 3: Encoding & Dispatch Loop (Opus, Emit)
        scope.launch {
            Log.d(TAG, "Encoding Loop Started")
            for (result in encodingChannel) {
                try {
                    processEncodingStage(result)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in Encoding Loop", e)
                }
            }
        }
    }

    /**
     * Stage 2: Compute
     * 运行在 Default 线程，负责计算密集型任务 (RMS, KWS) 和状态逻辑
     */
    private suspend fun processComputationStage(pcmData: ByteArray) {
        // 1. 计算并发送 Level (这是轻量级操作，可以直接发，UI层会优化频率)
        val audioLevel = calculateRmsLevel(pcmData)
        events.emit(AudioEvent.VoiceLevel(audioLevel))

        when (currentState) {
            AudioWorkState.WAITING -> {
                // 维护唤醒前缓存
                addToHistory(pcmData)
                
                // 运行 KWS
                val keyword = kwsManager.process(pcmData)
                if (keyword != null) {
                    Log.d(TAG, "KWS Detected keyword: $keyword. Initializing internal transition.")
                    handleWakeupDetected()
                }
            }
            AudioWorkState.ACTIVE -> {
                // 直接传递给编码层
                encodingChannel.send(ProcessingResult(pcmData))
            }
            else -> {} // IDLE or other states ignore data
        }
    }

    /**
     * Stage 3: Encode & Dispatch
     * 运行在 Default 线程，负责编码和最终发送
     */
    private suspend fun processEncodingStage(result: ProcessingResult) {
        // 唤醒事件优先发送，确保接收端状态切换
        if (result.isWakeupFrame) {
            events.emit(AudioEvent.Wakeup)
            return
        }

        // Opus 编码
        val opusData = encoder.encode(result.pcmData)
        if (opusData != null) {
            events.emit(AudioEvent.SpeechData(opusData))
        }
    }

    private suspend fun handleWakeupDetected() {
        // 1. 立即切换内部状态，停止 KWS
        currentState = AudioWorkState.ACTIVE
        
        // 2. 发送唤醒标记到编码层 (保持时序)
        encodingChannel.send(ProcessingResult(ByteArray(0), isWakeupFrame = true))
        
        // 3. 将历史缓存全部推送到编码层
        Log.d(TAG, "Flushing ${historyBuffer.size} historical frames to encoder...")
        while (historyBuffer.isNotEmpty()) {
            val historicalPcm = historyBuffer.removeFirst()
            encodingChannel.send(ProcessingResult(historicalPcm))
        }
        // historyBuffer 已经空了
        Log.d(TAG, "Historical frames flushed.")
    }

    private fun addToHistory(pcmData: ByteArray) {
        if (historyBuffer.size >= MAX_HISTORY_FRAMES) {
            historyBuffer.removeFirst()
        }
        historyBuffer.addLast(pcmData.copyOf())
    }

    fun setWorkState(newState: AudioWorkState) {
        if (currentState == newState) return
        
        Log.d(TAG, "Pipeline state transition: $currentState -> $newState")
        currentState = newState
        
        if (newState == AudioWorkState.WAITING) {
             // 清理工作，这里主要靠 computationLoop 的状态判断自然切换
             // historyBuffer 的重置由 computationLoop 在 WAITING 状态下自然维护（add/remove）
             // 如果需要显式清空，可以发个信号，但 inputChannel 可能会有滞留数据，
             // 所以最好的方式是让 computationLoop 自己处理。
             // 鉴于设计简化，这里不做额外操作。
             synchronized(historyBuffer) {
                 historyBuffer.clear()
             }
        }
    }

    fun cleanup() {
        inputChannel.close()
        encodingChannel.close()
        kwsManager.cleanup()
        encoder.release()
        historyBuffer.clear()
        scope.cancel()
    }

    // --- 内部辅助方法 ---

    private fun calculateRmsLevel(buffer: ByteArray): Float {
        val shorts = ShortArray(buffer.size / 2)
        for (i in 0 until buffer.size step 2) {
            shorts[i / 2] = ((buffer[i + 1].toInt() and 0xFF) shl 8 or (buffer[i].toInt() and 0xFF)).toShort()
        }
        var sumOfSquares = 0.0
        for (sample in shorts) {
            val normalizedSample = sample / 32768.0
            sumOfSquares += normalizedSample * normalizedSample
        }
        val rms = sqrt(sumOfSquares / shorts.size)
        return min(1.0, rms * 3.0).toFloat()
    }
}
