package com.airobot.assistant.audio

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import com.airobot.assistant.audio.player.AudioPlayer
import com.airobot.assistant.audio.player.DefaultAudioPlayer
import com.airobot.assistant.audio.recorder.AudioRecorder
import com.airobot.assistant.audio.recorder.DefaultAudioRecorder

/**
 * 音频服务实现类 - 轻量级编排层
 * 
 * 核心逻辑：
 * 1. 初始化时自动启动录音 Pipeline 并进入 LISTENING 状态。
 * 2. 状态切换通过 setWorkState 改变数据流向，不停止硬件录音。
 * 3. 统一使用单路事件流，减少多层转发损耗。
 */
@Singleton
class AudioServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioService {
    companion object {
        private const val TAG = "AudioServiceImpl"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 核心组件
    private val audioRecorder: AudioRecorder = DefaultAudioRecorder(context)
    private val audioPlayer: AudioPlayer = DefaultAudioPlayer(context)
    
    // 唯一的事件流 - 增加了缓冲区和背压处理
    private val _events = MutableSharedFlow<AudioEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    override val events: SharedFlow<AudioEvent> = _events

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun init(config: AudioConfig): Boolean {
        // 1. 初始化音频播放器
        if (!audioPlayer.initialize(config)) {
             _events.tryEmit(AudioEvent.SystemError("Player Init Failed"))
            return false
        }

        // 2. 初始化录音器，直接注入全局事件流
        if (!audioRecorder.initialize(config, _events)) {
            _events.tryEmit(AudioEvent.SystemError("Recorder Init Failed"))
            return false
        }
        
        // 3. 启动 Pipeline (启动后默认处于 WAITING 状态)
        audioRecorder.startRecording()
        
        Log.d(TAG, "音频系统初始化成功")
        return true
    }

    override fun activate() {
        Log.d(TAG, "Manual activation requested")
        audioRecorder.setWorkState(AudioWorkState.ACTIVE)
    }

    override fun deactivate() {
        Log.d(TAG, "Manual deactivation requested - Forcing WAITING state")
        audioRecorder.setWorkState(AudioWorkState.WAITING)
    }

    override fun play(audioData: ByteArray) {
        audioPlayer.playAudio(audioData)
    }

    override fun stopPlaying() {
        audioPlayer.stopPlaying()
    }

    override fun startStreamPlayback(opusDataFlow: SharedFlow<ByteArray>) {
        audioPlayer.startStreamPlayback(opusDataFlow)
    }

    override fun stopStreamPlayback() {
        audioPlayer.stopStreamPlayback()
    }

    override suspend fun waitForPlaybackCompletion() {
        audioPlayer.waitForPlaybackCompletion()
    }

    override fun release() {
        audioRecorder.stopRecording()
        audioRecorder.cleanup()
        audioPlayer.cleanup()
        scope.cancel()
        Log.d(TAG, "音频系统已彻底释放")
    }
}
