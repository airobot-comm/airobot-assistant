package com.airobot.features.podcast.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the current playback state of the podcast player.
 *
 * @property isPlaying Whether media is currently playing.
 * @property currentPositionMs Current playback position in milliseconds.
 * @property durationMs Total duration of the current media in milliseconds.
 * @property bufferedPercentage Percentage of media buffered (0-100).
 * @property activeEpisodeId The episode ID currently loaded, or null if none.
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPercentage: Int = 0,
    val activeEpisodeId: String? = null
)

/**
 * Service interface for podcast media playback powered by Media3 ExoPlayer.
 *
 * Provides reactive [playbackState] observation and full transport controls.
 * Use [getPlayer] to obtain the underlying [Player] instance for video surface
 * binding in Jetpack Compose.
 */
interface PodcastPlaybackService {

    /** Observable playback state updated in real-time. */
    val playbackState: StateFlow<PlaybackState>

    /**
     * Returns the underlying Media3 [Player] for video surface binding.
     * May return null if the player has not been initialized or has been released.
     */
    fun getPlayer(): Player?

    /**
     * Start playback of an episode.
     *
     * @param episodeId Unique identifier of the episode.
     * @param mediaUri URI string of the media (supports file://, content://, https://).
     * @param startPositionMs Optional start position in milliseconds. Defaults to 0.
     */
    fun play(episodeId: String, mediaUri: String, startPositionMs: Long = 0L)

    /** Pause the current playback. */
    fun pause()

    /** Resume the current playback after a pause. */
    fun resume()

    /**
     * Seek to a specific position.
     *
     * @param positionMs Target position in milliseconds.
     */
    fun seekTo(positionMs: Long)

    /** Stop playback and reset the player to idle. */
    fun stop()

    /** Release all player resources. The service should not be used after this call. */
    fun release()
}

/**
 * Singleton implementation of [PodcastPlaybackService] backed by Media3 [ExoPlayer].
 *
 * The player is lazily initialized on first use via [ensurePlayer]. Audio attributes
 * are configured for music content with automatic audio-focus handling. A coroutine
 * job polls the player position every 500 ms while playing and emits updates to
 * [playbackState].
 */
@Singleton
class PodcastPlaybackServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PodcastPlaybackService {

    companion object {
        private const val TAG = "PodcastPlayback"
        private const val POSITION_UPDATE_INTERVAL_MS = 500L
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var player: ExoPlayer? = null
    private var positionUpdateJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // ---- Player lifecycle -----------------------------------------------------------------------

    /**
     * Lazily initializes the [ExoPlayer] instance with music audio attributes
     * and registers the playback state listener.
     */
    private fun ensurePlayer(): ExoPlayer {
        return player ?: ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .build()
            .also { exo ->
                exo.addListener(playerListener)
                player = exo
                Log.d(TAG, "ExoPlayer initialized")
            }
    }

    override fun getPlayer(): Player? = player

    // ---- Transport controls ---------------------------------------------------------------------

    override fun play(episodeId: String, mediaUri: String, startPositionMs: Long) {
        Log.d(TAG, "play() episodeId=$episodeId uri=$mediaUri startMs=$startPositionMs")

        val exo = ensurePlayer()
        val uri = Uri.parse(mediaUri)
        val mediaItem = MediaItem.fromUri(uri)

        exo.setMediaItem(mediaItem)
        exo.prepare()

        if (startPositionMs > 0L) {
            exo.seekTo(startPositionMs)
        }

        exo.playWhenReady = true

        _playbackState.value = _playbackState.value.copy(
            activeEpisodeId = episodeId
        )

        startPositionUpdates()
    }

    override fun pause() {
        Log.d(TAG, "pause()")
        player?.playWhenReady = false
        stopPositionUpdates()
    }

    override fun resume() {
        Log.d(TAG, "resume()")
        val exo = player ?: return
        if (exo.playbackState == Player.STATE_ENDED) {
            exo.seekTo(0L)
        }
        if (exo.playbackState == Player.STATE_IDLE) {
            exo.prepare()
        }
        exo.playWhenReady = true
        startPositionUpdates()
    }

    override fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    override fun stop() {
        Log.d(TAG, "stop()")
        stopPositionUpdates()
        player?.stop()
        _playbackState.value = PlaybackState()
    }

    override fun release() {
        Log.d(TAG, "release()")
        stopPositionUpdates()
        player?.removeListener(playerListener)
        player?.release()
        player = null
        _playbackState.value = PlaybackState()
    }

    // ---- Position update coroutine --------------------------------------------------------------

    /**
     * Starts a coroutine that polls the player position every [POSITION_UPDATE_INTERVAL_MS]
     * and emits updated [PlaybackState] values while the player is playing.
     */
    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = serviceScope.launch {
            while (isActive) {
                val exo = player ?: break
                if (exo.isPlaying) {
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = exo.currentPosition,
                        durationMs = exo.duration.coerceAtLeast(0L),
                        bufferedPercentage = exo.bufferedPercentage
                    )
                }
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

    /** Cancels the periodic position update job if running. */
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    // ---- Player.Listener ------------------------------------------------------------------------

    /**
     * Listens for ExoPlayer state and playWhenReady changes and maps them to
     * [PlaybackState] updates on the [_playbackState] flow.
     */
    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)

            if (isPlaying) {
                startPositionUpdates()
            } else {
                stopPositionUpdates()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val exo = player ?: return

            when (playbackState) {
                Player.STATE_READY -> {
                    _playbackState.value = _playbackState.value.copy(
                        durationMs = exo.duration.coerceAtLeast(0L)
                    )
                }
                Player.STATE_ENDED -> {
                    stopPositionUpdates()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = exo.duration.coerceAtLeast(0L)
                    )
                    Log.d(TAG, "Playback ended for episode=${_playbackState.value.activeEpisodeId}")
                }
                else -> { /* STATE_IDLE, STATE_BUFFERING — no special handling */ }
            }
        }
    }
}
