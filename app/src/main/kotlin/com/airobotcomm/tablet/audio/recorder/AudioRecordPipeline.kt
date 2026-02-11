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
import com.airobotcomm.tablet.audio.tools.codec.OpusEncoder
import com.airobotcomm.tablet.audio.tools.kws.KwsManager
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 录音处理流水线 - 核心业务逻辑实现
 * 
 * 职责：
 * 1. 计算音频强度 (Level)
 * 2. KWS 关键词检测
 * 3. 音频编码 (Opus)
 * 4. 维护唤醒上下文音频缓存 (PCM) 并输出编码后的唤醒数据
 */
class AudioRecordPipeline(
    private val context: Context,
    private val config: AudioConfig,
    private val events: MutableSharedFlow<AudioEvent>
) {
    companion object {
        private const val TAG = "AudioRecordPipeline"
        private const val WAKEUP_BUFFER_DURATION_SEC = 2
        
        // 内部状态
        const val STATE_LISTENING = 1
        const val STATE_WORKING = 2
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 工具组件
    private val kwsManager = KwsManager(context)
    private val encoder = OpusEncoder(config.recordSampleRate, config.channels, config.frameDurationMs)
    
    // 状态
    private var currentState = STATE_LISTENING
    
    // 唤醒缓存 (PCM 原始数据)
    private val wakeupBufferSize = config.recordSampleRate * WAKEUP_BUFFER_DURATION_SEC * 2 // 16bit = 2 bytes
    private val pcmBuffer = ByteArray(wakeupBufferSize)
    private var bufferHead = 0

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
        scope.launch { events.emit(AudioEvent.AudioLevel(audioLevel)) }

        when (currentState) {
            STATE_LISTENING -> {
                // 监听状态：更新缓存并进行 KWS
                addToBuffer(pcmData)
                
                val keyword = kwsManager.process(pcmData)
                if (keyword != null) {
                    Log.d(TAG, "KWS Detected keyword: $keyword. Switch to WORKING.")
                    currentState = STATE_WORKING
                    
                    // 核心逻辑：编码缓存中的音频并发送 Wakeup 事件
                    processWakeupEvent()
                }
            }
            STATE_WORKING -> {
                // 工作状态：编码当前帧并发送 AudioData
                val opusData = encoder.encode(pcmData)
                if (opusData != null) {
                    scope.launch { events.emit(AudioEvent.AudioData(opusData)) }
                }
            }
        }
    }

    private fun processWakeupEvent() {
        scope.launch {
            try {
                // 获取原始缓存音频
                val rawPcm = getFullBuffer()
                
                // 对缓存音频进行分帧编码
                // 注意：由于是唤醒瞬间，这里需要一次性将过去2秒的音频编码为 Opus
                val opusContext = encodeLargeBuffer(rawPcm)
                
                if (opusContext.isNotEmpty()) {
                    events.emit(AudioEvent.Wakeup(opusContext))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process wakeup audio encoding", e)
            }
        }
    }

    /**
     * 将大的 PCM 缓存按帧大小进行切分编码
     */
    private suspend fun encodeLargeBuffer(pcmData: ByteArray): ByteArray {
        val frameSize = (config.recordSampleRate * config.frameDurationMs) / 1000 * 2
        val outStream = java.io.ByteArrayOutputStream()
        
        var offset = 0
        while (offset + frameSize <= pcmData.size) {
            val frame = pcmData.copyOfRange(offset, offset + frameSize)
            val encoded = encoder.encode(frame)
            if (encoded != null) {
                // 这里按顺序拼接 Opus 帧包（通常上层或服务器能识别这种流式拼接）
                outStream.write(encoded)
            }
            offset += frameSize
        }
        
        return outStream.toByteArray()
    }

    fun setWorking(working: Boolean) {
        currentState = if (working) STATE_WORKING else STATE_LISTENING
        Log.d(TAG, "Pipeline state changed to: ${if (working) "WORKING" else "LISTENING"}")
    }

    fun cleanup() {
        kwsManager.cleanup()
        encoder.release()
        // 不再取消 scope，因为 pipeline 可能伴随整改录音生命周期
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

    private fun addToBuffer(data: ByteArray) {
        val buffer = pcmBuffer
        val bufferSize = wakeupBufferSize
        
        if (data.size >= bufferSize) {
            System.arraycopy(data, data.size - bufferSize, buffer, 0, bufferSize)
            bufferHead = 0
            return
        }
        val spaceToEnd = bufferSize - bufferHead
        if (data.size <= spaceToEnd) {
            System.arraycopy(data, 0, buffer, bufferHead, data.size)
            bufferHead = (bufferHead + data.size) % bufferSize
        } else {
            System.arraycopy(data, 0, buffer, bufferHead, spaceToEnd)
            System.arraycopy(data, spaceToEnd, buffer, 0, data.size - spaceToEnd)
            bufferHead = data.size - spaceToEnd
        }
    }

    private fun getFullBuffer(): ByteArray {
        val result = ByteArray(wakeupBufferSize)
        val spaceToEnd = wakeupBufferSize - bufferHead
        System.arraycopy(pcmBuffer, bufferHead, result, 0, spaceToEnd)
        System.arraycopy(pcmBuffer, 0, result, spaceToEnd, bufferHead)
        return result
    }
}
