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
 * 音频管理器接口，定义音频核心功能
 */
interface AudioService {
    /**
     * 初始化音频系统
     */
    fun initialize(): Boolean

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
