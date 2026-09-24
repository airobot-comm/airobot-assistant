package com.airobot.agent.brain

import android.util.Log
import com.airobot.agent.audio.AudioService
import com.airobot.agent.skills.podcast.PodcastProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global session coordinator for managing dialogue flow states, routing intents,
 * and resolving resource conflict states between active voice conversations
 * and background media features.
 */
@Singleton
class AgentSessionCoordinator @Inject constructor(
    private val podcastProvider: PodcastProvider,
    private val audioService: AudioService
) {
    companion object {
        private const val TAG = "AgentSessionCoordinator"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var muteJob: Job? = null

    /**
     * Checks if any background media/feature is currently active/playing.
     * Can be expanded to check other media providers (e.g. music, alarms) in the future.
     */
    val isMediaPlaying: Boolean
        get() = podcastProvider.isPlaying

    /**
     * Pauses all active background media features when a dialogue starts.
     */
    fun pauseMedia() {
        scope.launch {
            podcastProvider.pause()
        }
    }

    /**
     * Temporarily mute audio capture (microphone input) to resolve audio focus conflicts
     * and prevent echo/noise feedback during media playback transitions.
     */
    fun closeEarsTemporarily(durationMs: Long = 2000L) {
        muteJob?.cancel()
        muteJob = scope.launch {
            Log.d(TAG, "Closing ears (muting capture) for ${durationMs}ms")
            audioService.setCaptureMuted(true)
            delay(durationMs)
            audioService.setCaptureMuted(false)
            Log.d(TAG, "Opening ears (unmuting capture)")
        }
    }
}
