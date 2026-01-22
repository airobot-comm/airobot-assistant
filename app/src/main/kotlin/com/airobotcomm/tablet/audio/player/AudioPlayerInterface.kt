package com.airobotcomm.tablet.audio.player

import android.content.Context
import kotlinx.coroutines.flow.SharedFlow
import com.airobotcomm.tablet.audio.AudioConfig

/**
 * 音频播放器接口
 */
interface AudioPlayer {
    /**
     * 初始化播放器
     */
    fun initialize(config: AudioConfig): Boolean

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
     * 获取播放状态
     */
    fun isPlaying(): Boolean

    /**
     * 清理资源
     */
    fun cleanup()

    /**
     * 播放状态变化监听器
     */
    val onPlayingStateChanged: SharedFlow<Boolean>
}