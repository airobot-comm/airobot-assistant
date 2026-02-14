package com.airobotcomm.tablet.audio.recorder

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 工具组件
    private val kwsManager = KwsManager(context)
    private val encoder = OpusEncoder(config.recordSampleRate, config.channels, config.frameDurationMs)
    
    // 统一使用 AudioWorkState
    private var currentState = AudioWorkState.WAITING
    
    // 历史帧缓存 (PCM 原始数据帧队列)
    private val historyBuffer = LinkedList<ByteArray>()

    init {
        if (!kwsManager.init()) {
            Log.e(TAG, "KWS Manager init failed in pipeline")
        }
    }

    /**
     * 处理一帧 PCM 数据
     */
    suspend fun processFrame(pcmData: ByteArray) {
        // 1. 计算 Level (异步发送)
        val audioLevel = calculateRmsLevel(pcmData)
        scope.launch { events.emit(AudioEvent.VoiceLevel(audioLevel)) }

        when (currentState) {
            AudioWorkState.WAITING -> {
                // 监听状态：更新历史缓存并进行 KWS
                addToHistory(pcmData)
                
                val keyword = kwsManager.process(pcmData)
                if (keyword != null) {
                    Log.d(TAG, "KWS Detected keyword: $keyword. Initializing internal transition.")
                    // 内部触发状态切换与缓存处理
                    handleWakeupInternal()
                }
            }
            AudioWorkState.ACTIVE -> {
                // 工作状态：编码当前帧并发送 SpeechData
                encodeAndEmit(pcmData)
            }
            else -> {}
        }
    }

    private suspend fun handleWakeupInternal() {
        try {
            // 1. 立即触发 Wakeup 事件
            events.emit(AudioEvent.Wakeup)
            
            // 2. 切换状态，防止后续帧在处理缓存时进入 WAITING
            currentState = AudioWorkState.ACTIVE

            // 3. 逐帧编码并发送历史缓存
            Log.d(TAG, "Sending ${historyBuffer.size} historical frames...")
            while (historyBuffer.isNotEmpty()) {
                val historicalPcm = historyBuffer.removeFirst()
                encodeAndEmit(historicalPcm)
            }
            Log.d(TAG, "Historical frames sent.")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process internal wakeup transition", e)
            // 如果处理失败，根据需要决定是否回退或上报错误
        }
    }

    private suspend fun encodeAndEmit(pcmData: ByteArray) {
        val opusData = encoder.encode(pcmData)
        if (opusData != null) {
            events.emit(AudioEvent.SpeechData(opusData))
        }
    }

    private fun addToHistory(pcmData: ByteArray) {
        if (historyBuffer.size >= MAX_HISTORY_FRAMES) {
            historyBuffer.removeFirst()
        }
        historyBuffer.addLast(pcmData.copyOf())
    }

    fun setWorkState(newState: AudioWorkState) {
        if (currentState == newState) return
        
        currentState = newState
        if (newState == AudioWorkState.WAITING) {
            historyBuffer.clear()
        }
        Log.d(TAG, "Pipeline state set to: $newState")
    }

    fun cleanup() {
        kwsManager.cleanup()
        encoder.release()
        historyBuffer.clear()
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
