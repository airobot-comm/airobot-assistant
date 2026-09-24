package com.airobot.agent.audio.player

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.airobot.agent.audio.AudioConfig
import com.airobot.agent.audio.tools.codec.OpusDecoder

/**
 * Default audio player implementation.
 */
class DefaultAudioPlayer(private val context: Context) : AudioPlayer {
    companion object {
        private const val TAG = "DefaultAudioPlayer"
    }

    private var isPlayingState = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var opusDecoder: OpusDecoder? = null
    private var streamPlayer: AudioStreamPlayer? = null

    // Configuration parameters
    private lateinit var config: AudioConfig

    // Audio playback stream
    private val _audioPlaybackFlow = Channel<ByteArray>(Channel.UNLIMITED)
    private var playbackJob: Job? = null

    private val _onPlayingStateChanged = MutableSharedFlow<Boolean>()
    override val onPlayingStateChanged: SharedFlow<Boolean> = _onPlayingStateChanged

    override fun initialize(config: AudioConfig): Boolean {
        this.config = config
        return try {
            // Initialize Opus decoder
            opusDecoder =
                OpusDecoder(config.playSampleRate, config.channels, config.frameDurationMs)
            streamPlayer = AudioStreamPlayer(
                config.playSampleRate,
                config.channels,
                config.frameDurationMs,
                context
            )

            startDecodeLoop()

            Log.d(TAG, "Audio player initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio player", e)
            false
        }
    }

    override fun playAudio(audioData: ByteArray) {
        if (!isPlayingState) {
            isPlayingState = true
            scope.launch {
                _onPlayingStateChanged.emit(true)
            }
            streamPlayer?.start()
        }
        _audioPlaybackFlow.trySend(audioData)
    }

    override fun stopPlaying() {
        isPlayingState = false
        streamPlayer?.stop()
        drainPlaybackChannel()
        scope.launch {
            _onPlayingStateChanged.emit(false)
        }
    }

    override fun startStreamPlayback(opusDataFlow: SharedFlow<ByteArray>) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            if (!isPlayingState) {
                isPlayingState = true
                _onPlayingStateChanged.emit(true)
                streamPlayer?.start()
            }
            opusDataFlow.collect { opusData ->
                _audioPlaybackFlow.send(opusData)
            }
        }
    }

    override fun stopStreamPlayback() {
        isPlayingState = false
        playbackJob?.cancel()
        streamPlayer?.stop()
        drainPlaybackChannel()
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
        _audioPlaybackFlow.close()
        opusDecoder?.release()
        streamPlayer?.release()
        scope.cancel()
    }

    private fun startDecodeLoop() {
        scope.launch {
            for (opusData in _audioPlaybackFlow) {
                try {
                    opusDecoder?.let { decoder ->
                        val pcmData = decoder.decode(opusData)
                        if (pcmData != null && pcmData.isNotEmpty()) {
                            streamPlayer?.writeDirectly(pcmData)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode audio data", e)
                }
            }
        }
    }

    private fun drainPlaybackChannel() {
        while (true) {
            val result = _audioPlaybackFlow.tryReceive()
            if (result.isFailure) break
        }
    }
}
