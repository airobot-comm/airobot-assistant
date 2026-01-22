package com.airobotcomm.tablet.audio

import kotlinx.coroutines.flow.SharedFlow

/**
 * 音频事件
 */
sealed class AudioEvent {
    data class AudioData(val data: ByteArray) : AudioEvent()
    data class AudioLevel(val level: Float) : AudioEvent()
    data class Error(val message: String) : AudioEvent()
}

/**
 * 音频配置
 */
data class AudioConfig(
    val recordSampleRate: Int = 16000,
    val playSampleRate: Int = 24000,
    val channels: Int = 1,
    val audioFormat: Int = android.media.AudioFormat.ENCODING_PCM_16BIT,
    val frameDurationMs: Int = 60,
    val enableAec: Boolean = true,
    val enableNs: Boolean = true
)

/**
 * 音频管理器接口，定义音频核心功能
 */
interface AudioService {
    /**
     * 初始化音频系统
     */
    fun initialize(config: AudioConfig = AudioConfig()): Boolean

    /**
     * 开始录音
     */
    fun startRecording()

    /**
     * 停止录音
     */
    fun stopRecording()

    /**
     * 播放音频数据（单次播放）
     */
    fun playAudio(audioData: ByteArray)

    /**
     * 停止播放
     */
    fun stopPlaying()

    /**
     * 开始流式播放
     */
    fun startStreamPlayback(opusDataFlow: SharedFlow<ByteArray>)

    /**
     * 停止流式播放
     */
    fun stopStreamPlayback()

    /**
     * 等待播放完成
     */
    suspend fun waitForPlaybackCompletion()

    /**
     * 清理资源
     */
    fun cleanup()

    /**
     * 获取录音状态
     */
    fun isRecording(): Boolean

    /**
     * 获取播放状态
     */
    fun isPlaying(): Boolean

    /**
     * 测试音频播放
     */
    fun testAudioPlayback()

    /**
     * 音频事件流
     */
    val audioEvents: SharedFlow<AudioEvent>
}