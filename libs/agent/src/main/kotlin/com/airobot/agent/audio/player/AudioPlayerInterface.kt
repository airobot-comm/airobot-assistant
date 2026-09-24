package com.airobot.agent.audio.player

import kotlinx.coroutines.flow.SharedFlow
import com.airobot.agent.audio.AudioConfig

/**
 * Audio player interface.
 */
interface AudioPlayer {
    /**
     * Initializes the player with configuration.
     */
    fun initialize(config: AudioConfig): Boolean

    /**
     * Plays single-shot audio data.
     */
    fun playAudio(audioData: ByteArray)

    /**
     * Stops playback.
     */
    fun stopPlaying()

    /**
     * Starts streaming playback.
     */
    fun startStreamPlayback(opusDataFlow: SharedFlow<ByteArray>)

    /**
     * Stops streaming playback.
     */
    fun stopStreamPlayback()

    /**
     * Waits for playback to complete.
     */
    suspend fun waitForPlaybackCompletion()

    /**
     * Gets current playing state.
     */
    fun isPlaying(): Boolean

    /**
     * Cleans up and releases resources.
     */
    fun cleanup()

    /**
     * Flow emitting changes to the playing state.
     */
    val onPlayingStateChanged: SharedFlow<Boolean>
}
