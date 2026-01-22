package com.airobotcomm.tablet.audio.recorder

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.SharedFlow
import com.airobotcomm.tablet.audio.AudioConfig
import com.airobotcomm.tablet.audio.AudioEvent

/**
 * 音频录制器接口
 */
interface AudioRecorder {
    /**
     * 初始化录音器
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun initialize(config: AudioConfig): Boolean

    /**
     * 开始录音
     */
    fun startRecording()

    /**
     * 停止录音
     */
    fun stopRecording()

    /**
     * 获取录音状态
     */
    fun isRecording(): Boolean

    /**
     * 清理资源
     */
    fun cleanup()

    /**
     * 音频事件流
     */
    val audioEvents: SharedFlow<AudioEvent>

    /**
     * 录音状态变化监听器
     */
    val onRecordingStateChanged: SharedFlow<Boolean>
}