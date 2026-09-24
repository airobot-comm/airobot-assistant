package com.airobot.agent.audio

import android.media.AudioFormat
import kotlinx.coroutines.flow.SharedFlow

/**
 * Audio Pipeline State - Unified state engine across audio modules
 */
enum class AudioWorkState {
    IDLE,       // Not initialized
    WAITING,    // Waiting for wakeup (KWS listener active)
    ACTIVE      // Active conversation (recording stream uploads)
}

/**
 * Audio Event definitions
 */
sealed class AudioEvent {
    object Wakeup : AudioEvent()                             // Wakeup keyword detected trigger
    data class VoiceLevel(val level: Float) : AudioEvent()   // Real-time voice level
    data class SpeechStart(val timestamp: Long = System.currentTimeMillis()) : AudioEvent()
    data class SpeechEnd(val timestamp: Long = System.currentTimeMillis()) : AudioEvent()
    data class SpeechData(val data: ByteArray) : AudioEvent() // Captured Opus audio data frame
    data class SystemError(val message: String) : AudioEvent()
}

/**
 * Audio Configurations
 */
data class AudioConfig(
    val recordSampleRate: Int = 16000,
    val playSampleRate: Int = 24000,
    val channels: Int = 1,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val frameDurationMs: Int = 60, // Frame duration, default 60ms
    val enableAec: Boolean = true,
    val enableNs: Boolean = true,

    // AI VAD Configurations (Silero ONNX)
    val vadThreshold: Float = 0.5f,
    val vadMinSpeechDuration: Float = 0.15f,
    val vadMinSilenceDuration: Float = 0.5f,
    val vadMaxSpeechDuration: Float = 20.0f
)

/**
 * Audio Service Interface - Event Driven Design
 *
 * Responsibility: Manages recording lifecycle state changes and playback services
 */
interface AudioService {
    /**
     * Initialize audio systems. Enters WAITING state upon successful completion.
     */
    fun init(config: AudioConfig = AudioConfig()): Boolean

    /**
     * Manually activate recording pipeline and transition to ACTIVE state (voice streaming start).
     */
    fun activate()

    /**
     * Force fallback to WAITING state (stop recording stream uploads and resume wake word detection).
     */
    fun deactivate()

    /**
     * Play raw sound buffer.
     */
    fun play(audioData: ByteArray)

    /**
     * Stop current playback activity.
     */
    fun stopPlaying()

    /**
     * Release hardware resources and channels.
     */
    fun release()

    /**
     * Set the speaker volume (0 to 100)
     */
    fun setSpeakerVolume(volume: Int)

    /**
     * Unified event stream (broadcasts recording bytes, volume decibels, and wakes)
     */
    val events: SharedFlow<AudioEvent>

    /**
     * Start Opus stream playback pipeline
     */
    fun startStreamPlayback(opusDataFlow: SharedFlow<ByteArray>)

    /**
     * Stop Opus stream playback pipeline
     */
    fun stopStreamPlayback()

    /**
     * Suspends until the current audio queue finishes rendering.
     */
    suspend fun waitForPlaybackCompletion()

    /**
     * Converts raw text to model-compatible token sequences (phonemes/pinyin) using underlying verification.
     * @param text The input Chinese or English text.
     * @return A list of valid token sequences.
     */
    fun convertTextToTokens(text: String): List<String>

    /**
     * Commits a new flat list of keyword lines directly into the KWS engine and triggers a hot-reload.
     * Format per line should be: `<token sequence> @<keyword name>`
     */
    fun updateKwsKeywords(keywordLines: List<String>)

    /**
     * Mutes or unmutes the audio recording input capture.
     * When muted, captured microphone data (SpeechData events) is ignored/discarded.
     */
    fun setCaptureMuted(muted: Boolean)
}
