package com.airobotcomm.tablet.audio

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 音频服务状态定义 - 语义化重构
 */
sealed class AudioState {
    object Idle : AudioState()          // 未初始化
    object Waiting : AudioState()       // 等待唤醒 (原 KWS 侦听) - 对应 UI 呼吸/闭眼
    object Active : AudioState()        // 活跃对话 (原 Streaming) - 对应 UI 聆听/波形
    data class Error(val message: String) : AudioState()
}

/**
 * 音频事件定义
 */
sealed class AudioEvent {
    data class SpeechData(val data: ByteArray) : AudioEvent() // 采集到的 PCM/Opus 数据
    data class VoiceLevel(val level: Float) : AudioEvent()    // 实时音量分贝/等级
    data class Wakeup(val data: ByteArray? = null) : AudioEvent() // 唤醒词检测触发
    data class SystemError(val message: String) : AudioEvent()
}

/**
 * 音频配置
 */
data class AudioConfig(
    val recordSampleRate: Int = 16000,
    val playSampleRate: Int = 24000,
    val channels: Int = 1,
    val audioFormat: Int = android.media.AudioFormat.ENCODING_PCM_16BIT,
    val frameDurationMs: Int = 60, // 帧长度，默认为 60ms
    val enableAec: Boolean = true,
    val enableNs: Boolean = true
)

/**
 * 音频服务接口 - 语义化重构
 * 
 * 职责：负责音频采集状态切换 (Waiting <-> Active) 与音频播放
 */
interface AudioService {
    /**
     * 初始化音频系统，完成后自动进入 Waiting 状态
     */
    fun init(config: AudioConfig = AudioConfig()): Boolean

    /**
     * 激活进入 Active 状态（开始上报完整语音流）
     * 手动触发或由唤醒词触发，从 Waiting -> Active
     */
    fun activate()

    /**
     * 回退到 Waiting 状态（停止语音流上报，重新开始关键词检测）
     * 从 Active -> Waiting
     */
    fun deactivate()

    /**
     * 播放音频数据
     */
    fun play(audioData: ByteArray)

    /**
     * 停止当前播放
     */
    fun stopPlaying()

    /**
     * 释放所有音频资源，进入 Idle
     */
    fun release()

    /**
     * 当前服务状态观察流 - 全局统一状态
     */
    val state: StateFlow<AudioState>

    /**
     * 音频事件流（数据、音量、唤醒等）
     */
    val events: SharedFlow<AudioEvent>
    
    /**
     * 启动流式播放 (Opus)
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
}
