package com.airobotcomm.tablet.audio

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 音频服务流水线状态 - 内部各模块统一使用
 */
enum class AudioWorkState {
    IDLE,       // 未初始化
    WAITING,    // 等待唤醒 (KWS 侦听中)
    ACTIVE      // 活跃对话 (录音上报中)
}

/**
 * 音频事件定义
 */
sealed class AudioEvent {
    data class SpeechData(val data: ByteArray) : AudioEvent() // 采集到的 Opus 数据帧
    data class VoiceLevel(val level: Float) : AudioEvent()    // 实时音量分贝/等级
    object Wakeup : AudioEvent()                             // 唤醒词检测触发 (不携带数据)
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
 * 音频服务接口 - 事件驱动设计
 * 
 * 职责：负责音频采集状态（Waiting <-> Active）控制与播放
 */
interface AudioService {
    /**
     * 初始化音频系统，完成后自动进入 WAITING 状态
     */
    fun init(config: AudioConfig = AudioConfig()): Boolean

    /**
     * 手动激活进入 ACTIVE 状态（开始上报语音流）
     */
    fun activate()

    /**
     * 强制回退到 WAITING 状态（停止语音流上报，重新开始关键词检测）
     * 场景：唤醒后发现条件不满足（如网络错误），ViewModel 调用此接口拉回录音状态
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
     * 释放所有音频资源
     */
    fun release()

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
