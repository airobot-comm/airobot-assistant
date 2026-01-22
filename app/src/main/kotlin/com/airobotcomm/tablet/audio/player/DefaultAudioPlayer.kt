package com.airobotcomm.tablet.audio.player

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.airobotcomm.tablet.audio.AudioConfig
import com.airobotcomm.tablet.audio.tools.codec.OpusDecoder

/**
 * 默认音频播放器实现
 */
class DefaultAudioPlayer(private val context: Context) : AudioPlayer {
    companion object {
        private const val TAG = "DefaultAudioPlayer"
    }

    private var isPlayingState = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var opusDecoder: OpusDecoder? = null
    private var streamPlayer: AudioStreamPlayer? = null

    // 配置参数
    private lateinit var config: AudioConfig

    // 音频播放流
    private val _audioPlaybackFlow = MutableSharedFlow<ByteArray>()
    private var playbackJob: Job? = null
    private var isPlaybackSetup = false

    private val _onPlayingStateChanged = MutableSharedFlow<Boolean>()
    override val onPlayingStateChanged: SharedFlow<Boolean> = _onPlayingStateChanged

    override fun initialize(config: AudioConfig): Boolean {
        this.config = config
        return try {
            // 初始化Opus解码器
            opusDecoder = OpusDecoder(config.playSampleRate, config.channels, config.frameDurationMs)
            streamPlayer = AudioStreamPlayer(config.playSampleRate, config.channels, config.frameDurationMs, context)

            Log.d(TAG, "音频播放器初始化成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "音频播放器初始化失败", e)
            false
        }
    }

    override fun playAudio(audioData: ByteArray) {
        scope.launch {
            try {
                if (!isPlayingState) {
                    isPlayingState = true
                    _onPlayingStateChanged.emit(true)
                    setupAudioPlayback()
                }
                _audioPlaybackFlow.emit(audioData)
            } catch (e: Exception) {
                Log.e(TAG, "播放音频失败", e)
            }
        }
    }

    override fun stopPlaying() {
        isPlayingState = false
        isPlaybackSetup = false
        streamPlayer?.stop()
        scope.launch {
            _onPlayingStateChanged.emit(false)
        }
    }

    override fun startStreamPlayback(opusDataFlow: SharedFlow<ByteArray>) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            isPlayingState = true
            _onPlayingStateChanged.emit(true)
            opusDataFlow.collect { opusData ->
                _audioPlaybackFlow.emit(opusData)
            }
        }
    }

    override fun stopStreamPlayback() {
        isPlayingState = false
        isPlaybackSetup = false
        playbackJob?.cancel()
        streamPlayer?.stop()
        scope.launch {
            _onPlayingStateChanged.emit(false)
        }
    }

    override suspend fun waitForPlaybackCompletion() {
        streamPlayer?.waitForPlaybackCompletion()
    }

    override fun isPlaying(): Boolean = isPlayingState

    override fun cleanup() {
        stopPlaying()
        playbackJob?.cancel()
        opusDecoder?.release()
        streamPlayer?.release()
        scope.cancel()
    }

    private fun setupAudioPlayback() {
        if (isPlaybackSetup) return

        isPlaybackSetup = true

        val pcmFlow = kotlinx.coroutines.flow.flow {
            _audioPlaybackFlow.collect { opusData ->
                try {
                    opusDecoder?.let { decoder ->
                        val pcmData = decoder.decode(opusData)
                        pcmData?.let { emit(it) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解码音频数据失败", e)
                }
            }
        }

        streamPlayer?.start(pcmFlow)
    }
}