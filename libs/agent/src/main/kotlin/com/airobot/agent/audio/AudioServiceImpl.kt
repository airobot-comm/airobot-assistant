package com.airobot.agent.audio

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.airobot.agent.audio.player.AudioPlayer
import com.airobot.agent.audio.player.DefaultAudioPlayer
import com.airobot.agent.audio.recorder.AudioRecorder
import com.airobot.agent.audio.recorder.DefaultAudioRecorder
import com.airobot.agent.audio.tools.afe.AfeManager
import com.airobot.agent.audio.tools.kws.KwsManager
import com.airobot.agent.audio.tools.kws.Text2Token
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio Service Implementation - Lightweight Orchestrator Layer
 *
 * Core Logic:
 * 1. Automatically start the recording pipeline upon initialization and enter the WAITING state.
 * 2. State switching changes the data flow via setWorkState, without stopping hardware recording.
 * 3. Unified single-channel event stream to reduce overhead.
 */
@Singleton
class AudioServiceImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val kwsManager: KwsManager,
    private val afeManager: AfeManager
) : AudioService {
    companion object {
        private const val TAG = "AudioServiceImpl"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager

    // Core Components
    private val audioRecorder: AudioRecorder = DefaultAudioRecorder(context, kwsManager, afeManager)
    private val audioPlayer: AudioPlayer = DefaultAudioPlayer(context)

    @Volatile
    private var isCaptureMuted = false

    // Single Event Stream - Buffer and Backpressure Handling
    private val _events = MutableSharedFlow<AudioEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    override val events: SharedFlow<AudioEvent> = _events
        .filter { event ->
            !(isCaptureMuted && event is AudioEvent.SpeechData)
        }
        .shareIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            replay = 0
        )

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun init(config: AudioConfig): Boolean {
        try {
            audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
            Log.d(TAG, "Set AudioManager mode to MODE_IN_COMMUNICATION for hardware AEC")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set AudioManager mode", e)
        }

        // 1. Initialize audio player
        if (!audioPlayer.initialize(config)) {
            _events.tryEmit(AudioEvent.SystemError("Player Init Failed"))
            return false
        }

        // 2. Initialize recorder and inject events
        if (!audioRecorder.initialize(config, _events)) {
            _events.tryEmit(AudioEvent.SystemError("Recorder Init Failed"))
            return false
        }

        // 3. Start recording pipeline (default state: WAITING)
        audioRecorder.startRecording()

        Log.d(TAG, "Audio system initialized successfully")
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

    override fun setSpeakerVolume(volume: Int) {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val mappedVolume = (volume / 100f * maxVolume).toInt().coerceIn(0, maxVolume)
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, mappedVolume, 0)
            Log.d(TAG, "setSpeakerVolume to $volume (mapped: $mappedVolume/$maxVolume)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set speaker volume", e)
        }
    }

    override suspend fun waitForPlaybackCompletion() {
        audioPlayer.waitForPlaybackCompletion()
    }

    override fun convertTextToTokens(text: String): List<String> {
        val validTokens = kwsManager.getValidTokens()
        return Text2Token.convertToTokensVariations(text, validTokens)
    }

    override fun updateKwsKeywords(keywordLines: List<String>) {
        Log.d(TAG, "updateKwsKeywords received ${keywordLines.size} lines")
        kwsManager.updateKeywords(keywordLines)
    }

    override fun setCaptureMuted(muted: Boolean) {
        isCaptureMuted = muted
        Log.d(TAG, "setCaptureMuted: $muted")
    }

    override fun release() {
        audioRecorder.stopRecording()
        audioRecorder.cleanup()
        audioPlayer.cleanup()
        scope.cancel()

        try {
            audioManager.mode = android.media.AudioManager.MODE_NORMAL
            Log.d(TAG, "Reset AudioManager mode to MODE_NORMAL on release")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset AudioManager mode", e)
        }

        Log.d(TAG, "Audio system fully released")
    }
}
