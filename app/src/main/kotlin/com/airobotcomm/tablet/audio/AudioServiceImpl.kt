package com.airobotcomm.tablet.audio

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import com.airobotcomm.tablet.audio.recorder.AudioRecorder
import com.airobotcomm.tablet.audio.recorder.DefaultAudioRecorder
import com.airobotcomm.tablet.audio.player.AudioPlayer
import com.airobotcomm.tablet.audio.player.DefaultAudioPlayer

/**
 * 音频管理器实现类 - 将录音和播放功能分离为独立模块
 */
@Singleton
class AudioServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioService {
    companion object {
        private const val TAG = "AudioServiceImpl"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 分离的录音和播放模块
    private val audioRecorder: AudioRecorder
    private val audioPlayer: AudioPlayer

    // 合并的音频事件流
    private val _audioEvents = MutableSharedFlow<AudioEvent>()
    override val audioEvents: SharedFlow<AudioEvent> = _audioEvents

    // 状态管理
    private var currentConfig: AudioConfig? = null

    init {
        // 初始化录音和播放模块
        audioRecorder = DefaultAudioRecorder(context)
        audioPlayer = DefaultAudioPlayer(context)
        
        // 监听录音和播放事件
        scope.launch {
            audioRecorder.audioEvents.collect { event ->
                _audioEvents.emit(event)
            }
        }
        
        scope.launch {
            audioPlayer.onPlayingStateChanged.collect { isPlaying ->
                // 播放状态变化可以通过其他方式处理
            }
        }
        
        scope.launch {
            audioRecorder.onRecordingStateChanged.collect { isRecording ->
                // 录音状态变化可以通过其他方式处理
            }
        }
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun initialize(config: AudioConfig): Boolean {
        currentConfig = config
        
        // 初始化录音模块
        val recorderInitialized = audioRecorder.initialize(config)
        if (!recorderInitialized) {
            Log.e(TAG, "录音模块初始化失败")
            return false
        }
        
        // 初始化播放模块
        val playerInitialized = audioPlayer.initialize(config)
        if (!playerInitialized) {
            Log.e(TAG, "播放模块初始化失败")
            return false
        }
        
        Log.d(TAG, "音频系统初始化成功")
        return true
    }

    override fun startRecording() {
        audioRecorder.startRecording()
    }

    override fun stopRecording() {
        audioRecorder.stopRecording()
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
        audioRecorder.cleanup()
        audioPlayer.cleanup()
        scope.cancel()
    }

    override fun isRecording(): Boolean = audioRecorder.isRecording()
    override fun isPlaying(): Boolean = audioPlayer.isPlaying()

    override fun testAudioPlayback() {
        scope.launch {
            try {
                val testPlayer = DefaultAudioPlayer(context)
                val config = currentConfig ?: AudioConfig(playSampleRate = 24000)
                
                testPlayer.initialize(config)
                
                val sampleRate = config.playSampleRate
                val duration = 1.0
                val frequency = 440.0
                val samples = (sampleRate * duration).toInt()
                val pcmData = ByteArray(samples * 2)
                
                for (i in 0 until samples) {
                    val sample = (32767 * kotlin.math.sin(2 * kotlin.math.PI * frequency * i / sampleRate)).toInt().toShort()
                    pcmData[i * 2] = (sample.toInt() and 0xFF).toByte()
                    pcmData[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                }
                
                val opusData = byteArrayOf() // 这里只是测试，实际应该编码PCM数据
                testPlayer.playAudio(opusData)
                
                delay(1500)
                testPlayer.cleanup()
            } catch (e: Exception) {
                Log.e(TAG, "测试音频播放失败", e)
            }
        }
    }
}