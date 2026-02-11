package com.airobotcomm.tablet.audio

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import com.airobotcomm.tablet.audio.player.AudioPlayer
import com.airobotcomm.tablet.audio.player.DefaultAudioPlayer
import com.airobotcomm.tablet.audio.recorder.AudioRecorder
import com.airobotcomm.tablet.audio.recorder.DefaultAudioRecorder

/**
 * 音频服务实现类 - 轻量级编排层
 * 
 * 核心逻辑：
 * 1. 初始化时自动启动录音 Pipeline 并进入 LISTENING 状态。
 * 2. 状态切换仅通过 startWorking/stopWorking 改变数据流向，不停止硬件录音。
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
    private val _audioEvents = MutableSharedFlow<AudioEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    override val audioEvents: SharedFlow<AudioEvent> = _audioEvents

    private var isInWorkingState = false

    init {
        // 观察统一事件流，同步业务层状态
        scope.launch {
            _audioEvents.collect { event ->
                if (event is AudioEvent.Wakeup) {
                    isInWorkingState = true
                    // 确保指令下达到 recorder (虽然 pipeline 内部已自切换)
                    audioRecorder.startWorking()
                    Log.d(TAG, "检测到唤醒事件，同步业务状态为 WORKING")
                }
            }
        }
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun initialize(config: AudioConfig): Boolean {
        // 1. 初始化音频播放器
        if (!audioPlayer.initialize(config)) return false

        // 2. 初始化录音器，直接注入全局事件流
        if (!audioRecorder.initialize(config, _audioEvents)) return false
        
        // 3. 启动 Pipeline (启动后默认处于 LISTENING 状态)
        audioRecorder.startRecording()
        isInWorkingState = false
        
        Log.d(TAG, "音频系统初始化成功，事件流已打通")
        return true
    }

    override fun startWorking() {
        isInWorkingState = true
        audioRecorder.startWorking()
        Log.d(TAG, "手动切入 WORKING 状态")
    }

    override fun stopWorking() {
        isInWorkingState = false
        audioRecorder.stopWorking()
        Log.d(TAG, "手动切回 LISTENING 状态")
    }

    override fun playAudio(audioData: ByteArray) {
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

    override fun cleanup() {
        audioRecorder.stopRecording()
        audioRecorder.cleanup()
        audioPlayer.cleanup()
        scope.cancel()
        Log.d(TAG, "音频系统已彻底清理")
    }

    override fun isWorking(): Boolean = isInWorkingState
    override fun isPlaying(): Boolean = audioPlayer.isPlaying()

    override fun testAudioPlayback() {
        // 保持不变...
    }
}