package com.airobot.features.podcast.viewmodel

import android.util.Log
import androidx.media3.common.Player
import com.airobot.features.aiserv.event.AiEvent
import com.airobot.features.aiserv.event.AiEventDispatcher
import com.airobot.features.aiserv.popup.OverlayCoordinator
import com.airobot.features.FeatureCards
import com.airobot.features.R
import com.airobot.features.podcast.data.PodcastRepository
import com.airobot.features.podcast.data.model.PodcastEpisode
import com.airobot.features.podcast.service.PlaybackState
import com.airobot.features.podcast.service.PodcastPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegate responsible for managing podcast playback state, player interactions,
 * progress ticks (both real and simulated), and dispatching playback started/stopped/completed events.
 */
@Singleton
class PodcastPlaybackDelegate @Inject constructor(
    private val repository: PodcastRepository,
    private val playbackService: PodcastPlaybackService,
    private val eventDispatcher: AiEventDispatcher,
    private val dataDelegate: PodcastDataDelegate,
    private val overlayCoordinator: OverlayCoordinator
) {
    companion object {
        private const val TAG = "PodcastPlaybackDelegate"
    }

    private val _activeEpisode = MutableStateFlow<PodcastEpisode?>(null)
    val activeEpisode: StateFlow<PodcastEpisode?> = _activeEpisode.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _playbackError = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val playbackError: SharedFlow<Int> = _playbackError.asSharedFlow()

    val realPlaybackState: StateFlow<PlaybackState> = playbackService.playbackState

    fun getPlayer(): Player? = playbackService.getPlayer()

    private val delegateScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Whether this delegate has been initialized by a ViewModel. */
    val isInitialized: Boolean get() = true

    private var playbackJob: Job? = null
    private var playbackObserverJob: Job? = null

    // Session tracking variables
    private var sessionStartTimestamp = 0L
    private var lastRecordedPlayState = false
    private var wasLastCompleted = false

    init {
        observeRealPlayback()
        delegateScope.launch {
            dataDelegate.episodes.collect { eps ->
                if (_activeEpisode.value == null && eps.isNotEmpty()) {
                    val firstReal = eps.firstOrNull { it.isDiy && !it.mediaUri.isNullOrEmpty() }
                    if (firstReal != null) {
                        _activeEpisode.value = firstReal
                        _progress.value = firstReal.progress
                        Log.d(TAG, "Pre-populated activeEpisode with first real DIY episode: ${firstReal.title}")
                    } else {
                        val firstPreset = eps.firstOrNull()
                        if (firstPreset != null) {
                            _activeEpisode.value = firstPreset
                            _progress.value = firstPreset.progress
                            Log.d(TAG, "Pre-populated activeEpisode with first preset episode: ${firstPreset.title}")
                        }
                    }
                }
            }
        }
    }

    private fun observeRealPlayback() {
        playbackObserverJob = delegateScope.launch {
            playbackService.playbackState.collect { state ->
                val active = _activeEpisode.value ?: return@collect
                if (!active.isDiy || active.mediaUri == null) return@collect

                // Ignore state updates that belong to a different active episode
                if (state.activeEpisodeId != null && state.activeEpisodeId != active.id) return@collect

                // Treat the player as playing if it is either actually playing or preparing/buffering (playWhenReady is true and state is BUFFERING or READY)
                val exo = playbackService.getPlayer()
                val isPreparingOrPlaying = exo != null && exo.playWhenReady &&
                    (exo.playbackState == Player.STATE_BUFFERING || exo.playbackState == Player.STATE_READY)
                val isReallyPlaying =
                    state.isPlaying || (isPreparingOrPlaying && state.activeEpisodeId == active.id)
                val playStateChanged = isReallyPlaying != lastRecordedPlayState
                lastRecordedPlayState = isReallyPlaying
                _isPlaying.value = isReallyPlaying

                val hasEnded = state.durationMs > 0 && state.currentPositionMs >= state.durationMs

                if (state.durationMs > 0) {
                    val prog = (state.currentPositionMs.toFloat() / state.durationMs) * 100f
                    _progress.value = if (hasEnded) 0f else prog.coerceIn(0f, 100f)
                }

                // Update episode progress in memory
                val isPlayed =
                    hasEnded || state.currentPositionMs >= state.durationMs * 0.95f || active.played
                val updated = active.copy(
                    progress = _progress.value,
                    lastPositionMs = if (hasEnded) 0L else state.currentPositionMs,
                    played = isPlayed
                )
                _activeEpisode.value = updated

                val updatedEpisodes = dataDelegate.episodes.value.map { ep ->
                    if (ep.id == active.id) updated else ep
                }
                dataDelegate.updateEpisodesState(updatedEpisodes)

                // Dispatch Started, Stopped, Completed events reactively
                if (playStateChanged) {
                    if (isReallyPlaying) {
                        // Playback started/resumed
                        sessionStartTimestamp = System.currentTimeMillis()
                        wasLastCompleted = false
                        eventDispatcher.dispatch(
                            AiEvent.PodcastPlaybackStarted(
                                episodeId = active.id,
                                title = active.title,
                                type = active.type,
                                channelName = active.channelName,
                                resumePositionMs = state.currentPositionMs,
                                timestamp = sessionStartTimestamp
                            )
                        )
                    } else if (sessionStartTimestamp > 0L) {
                        // Playback paused/stopped
                        val listened = System.currentTimeMillis() - sessionStartTimestamp
                        if (listened > 0) {
                            if (hasEnded || isPlayed) {
                                if (!wasLastCompleted) {
                                    wasLastCompleted = true
                                    eventDispatcher.dispatch(
                                        AiEvent.PodcastPlaybackCompleted(
                                            episodeId = active.id,
                                            title = active.title,
                                            type = active.type,
                                            channelName = active.channelName,
                                            durationMs = state.durationMs,
                                            listenedMs = listened,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                }
                            } else {
                                eventDispatcher.dispatch(
                                    AiEvent.PodcastPlaybackStopped(
                                        episodeId = active.id,
                                        title = active.title,
                                        type = active.type,
                                        channelName = active.channelName,
                                        progressPercent = _progress.value,
                                        listenedMs = listened,
                                        timestamp = System.currentTimeMillis(),
                                        reason = "user_paused"
                                    )
                                )
                            }
                        }
                        sessionStartTimestamp = 0L
                    }
                } else if (hasEnded && !wasLastCompleted && sessionStartTimestamp > 0L) {
                    // Playback completed while in the same play state
                    val listened = System.currentTimeMillis() - sessionStartTimestamp
                    wasLastCompleted = true
                    eventDispatcher.dispatch(
                        AiEvent.PodcastPlaybackCompleted(
                            episodeId = active.id,
                            title = active.title,
                            type = active.type,
                            channelName = active.channelName,
                            durationMs = state.durationMs,
                            listenedMs = listened,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    sessionStartTimestamp = 0L
                }
            }
        }
    }

    fun togglePlay() {
        val currentActive = _activeEpisode.value
        if (currentActive == null) {
            val firstEpisode = dataDelegate.episodes.value.firstOrNull { it.isDiy && !it.mediaUri.isNullOrEmpty() }
            if (firstEpisode != null) {
                playEpisode(firstEpisode)
            } else {
                val fallback = dataDelegate.episodes.value.firstOrNull()
                if (fallback != null) {
                    playEpisode(fallback)
                }
            }
            return
        }

        if (currentActive.isDiy && currentActive.mediaUri != null) {
            if (_isPlaying.value) {
                playbackService.pause()
                Log.d(TAG, "togglePlay: paused real playback for ${currentActive.title}")
                savePlaybackProgress()
            } else {
                playbackService.resume()
                Log.d(TAG, "togglePlay: resumed real playback for ${currentActive.title}")
            }
        } else {
            delegateScope.launch {
                _playbackError.emit(R.string.podcast_error_invalid_demo_data)
            }
        }
    }

    fun playEpisode(episode: PodcastEpisode) {
        Log.d(TAG, "playEpisode: ${episode.title}, isDiy=${episode.isDiy}")

        // Save progress and dispatch stopped for currently playing
        val prevActive = _activeEpisode.value
        if (prevActive != null && _isPlaying.value && sessionStartTimestamp > 0L) {
            val listened = System.currentTimeMillis() - sessionStartTimestamp
            eventDispatcher.dispatch(
                AiEvent.PodcastPlaybackStopped(
                    episodeId = prevActive.id,
                    title = prevActive.title,
                    type = prevActive.type,
                    channelName = prevActive.channelName,
                    progressPercent = _progress.value,
                    listenedMs = listened,
                    timestamp = System.currentTimeMillis(),
                    reason = "switched_episode"
                )
            )
            sessionStartTimestamp = 0L
        }

        savePlaybackProgress()
        stopTicker()
        playbackService.stop()

        val updatedEpisode = episode.copy(playCount = episode.playCount + 1)
        _activeEpisode.value = updatedEpisode
        _progress.value = updatedEpisode.progress

        val isDemo = !episode.isDiy || episode.mediaUri.isNullOrEmpty()
        if (isDemo) {
            _isPlaying.value = false
            val updatedEpisodes = dataDelegate.episodes.value.map { ep ->
                if (ep.id == episode.id) updatedEpisode else ep
            }
            dataDelegate.updateEpisodesState(updatedEpisodes)
            delegateScope.launch { repository.saveEpisodes(updatedEpisodes) }
        } else {
            _isPlaying.value = true
            wasLastCompleted = false

            val updatedEpisodes = dataDelegate.episodes.value.map { ep ->
                if (ep.id == episode.id) updatedEpisode else ep
            }
            dataDelegate.updateEpisodesState(updatedEpisodes)
            delegateScope.launch { repository.saveEpisodes(updatedEpisodes) }

            // Real media playback via Media3
            playbackService.play(
                episodeId = episode.id,
                mediaUri = episode.mediaUri,
                startPositionMs = episode.lastPositionMs
            )
            // sessionStartTimestamp will be set when playbackState transitions to isPlaying=true
            Log.d(
                TAG,
                "Started real playback: uri=${episode.mediaUri}, resume=${episode.lastPositionMs}ms"
            )
        }
    }

    fun seekForward() {
        val current = _activeEpisode.value ?: return
        if (current.isDiy && current.mediaUri != null) {
            val state = playbackService.playbackState.value
            val newPos = (state.currentPositionMs + 10_000L).coerceAtMost(state.durationMs)
            playbackService.seekTo(newPos)
        } else {
            val currentVal = _progress.value
            _progress.value = (currentVal + 5f).coerceAtMost(100f)
            updateActiveEpisodeProgress(_progress.value)
        }
    }

    fun seekBackward() {
        val current = _activeEpisode.value ?: return
        if (current.isDiy && current.mediaUri != null) {
            val state = playbackService.playbackState.value
            val newPos = (state.currentPositionMs - 10_000L).coerceAtLeast(0L)
            playbackService.seekTo(newPos)
        } else {
            val currentVal = _progress.value
            _progress.value = (currentVal - 5f).coerceAtLeast(0f)
            updateActiveEpisodeProgress(_progress.value)
        }
    }

    fun seekTo(prog: Float) {
        val current = _activeEpisode.value ?: return
        if (current.isDiy && current.mediaUri != null) {
            val state = playbackService.playbackState.value
            if (state.durationMs > 0) {
                val posMs = ((prog / 100f) * state.durationMs).toLong()
                playbackService.seekTo(posMs)
            }
        } else {
            _progress.value = prog.coerceIn(0f, 100f)
            updateActiveEpisodeProgress(_progress.value)
        }
    }

    fun savePlaybackProgress() {
        val active = _activeEpisode.value ?: return
        if (!active.isDiy || active.mediaUri == null) return

        val state = playbackService.playbackState.value
        if (state.currentPositionMs > 0) {
            delegateScope.launch {
                val prog = if (state.durationMs > 0) {
                    (state.currentPositionMs.toFloat() / state.durationMs) * 100f
                } else 0f
                val played = prog >= 95f || active.played
                repository.updateEpisodeProgress(
                    id = active.id,
                    lastPositionMs = state.currentPositionMs,
                    progress = prog,
                    played = played
                )
                Log.d(
                    TAG,
                    "Saved playback progress: id=${active.id}, pos=${state.currentPositionMs}ms, prog=$prog%"
                )
            }
        }
    }

    private fun startTicker() {
        playbackJob?.cancel()
        playbackJob = delegateScope.launch {
            while (_isPlaying.value) {
                delay(1000)
                val currentActive = _activeEpisode.value ?: break
                val nextProgress = (currentActive.progress + 0.5f).coerceAtMost(100f)
                _progress.value = nextProgress
                updateActiveEpisodeProgress(nextProgress)

                if (nextProgress >= 100f) {
                    _isPlaying.value = false
                    if (sessionStartTimestamp > 0L) {
                        val listened = System.currentTimeMillis() - sessionStartTimestamp
                        eventDispatcher.dispatch(
                            AiEvent.PodcastPlaybackCompleted(
                                episodeId = currentActive.id,
                                title = currentActive.title,
                                type = currentActive.type,
                                channelName = currentActive.channelName,
                                durationMs = 300_000L, // Simulated 5 min duration
                                listenedMs = listened,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        sessionStartTimestamp = 0L
                    }
                    stopTicker()
                    break
                }
            }
        }
    }

    private fun updateActiveEpisodeProgress(prog: Float) {
        val currentActive = _activeEpisode.value ?: return
        val isPlayed = prog >= 95f || currentActive.played
        val updated = currentActive.copy(progress = prog, played = isPlayed)
        _activeEpisode.value = updated

        val updatedEpisodes = dataDelegate.episodes.value.map { ep ->
            if (ep.id == currentActive.id) updated else ep
        }
        dataDelegate.updateEpisodesState(updatedEpisodes)
    }

    private fun stopTicker() {
        playbackJob?.cancel()
        playbackJob = null
    }

    fun getRecommendedEpisodes(type: String? = null): List<PodcastEpisode> {
        val strategy = DefaultPodcastRecommendationStrategy()
        val allEpisodes = dataDelegate.episodes.value
        val realEpisodes = allEpisodes.filter { it.isDiy && !it.mediaUri.isNullOrEmpty() }
        val recommended = strategy.recommend(realEpisodes, _activeEpisode.value, _isPlaying.value)

        val normalized = when (type?.trim()?.lowercase()) {
            "audio", "音频" -> "audio"
            "video", "视频" -> "video"
            else -> null
        }

        return if (normalized != null) {
            recommended.filter { ep ->
                val epType = ep.type.trim().lowercase()
                if (normalized == "audio") {
                    epType == "audio" || epType == "音频"
                } else {
                    epType == "video" || epType == "视频"
                }
            }
        } else {
            recommended
        }
    }

    fun getNextEpisodeOf(type: String? = null): PodcastEpisode? {
        val allEpisodes = dataDelegate.episodes.value
        val realEpisodes = allEpisodes.filter { it.isDiy && !it.mediaUri.isNullOrEmpty() }
        if (realEpisodes.isEmpty()) return null

        val normalized = when (type?.trim()?.lowercase()) {
            "audio", "音频" -> "audio"
            "video", "视频" -> "video"
            else -> null
        }

        val filteredEpisodes = if (normalized != null) {
            realEpisodes.filter { ep ->
                val epType = ep.type.trim().lowercase()
                if (normalized == "audio") {
                    epType == "audio" || epType == "音频"
                } else {
                    epType == "video" || epType == "视频"
                }
            }
        } else {
            realEpisodes
        }

        if (filteredEpisodes.isEmpty()) return null

        val active = _activeEpisode.value
        val index = if (active != null) filteredEpisodes.indexOfFirst { it.id == active.id } else -1

        val nextIndex = if (index != -1) (index + 1) % filteredEpisodes.size else 0
        return filteredEpisodes[nextIndex]
    }

    fun playRecommended(type: String? = null): Boolean {
        Log.d(TAG, "playRecommended: type=$type")
        val active = _activeEpisode.value

        val normalized = when (type?.trim()?.lowercase()) {
            "audio", "音频" -> "audio"
            "video", "视频" -> "video"
            else -> null
        }

        // If an episode of correct type is active, just make sure overlay is shown and resume
        if (active != null && active.isDiy && !active.mediaUri.isNullOrEmpty()) {
            val activeType = active.type.trim().lowercase()
            val matchesType = normalized == null ||
                if (normalized == "audio") {
                    activeType == "audio" || activeType == "音频"
                } else {
                    activeType == "video" || activeType == "视频"
                }
            if (matchesType) {
                overlayCoordinator.showOverlay(FeatureCards.PODCAST)
                if (!_isPlaying.value) {
                    resume()
                    return true
                } else {
                    val nextEpisode = getNextEpisodeOf(type)
                    if (nextEpisode != null) {
                        playEpisode(nextEpisode)
                        return true
                    }
                }
            }
        }

        // Otherwise find first recommended episode of correct type
        val nextEpisode = getNextEpisodeOf(type)
        if (nextEpisode == null) {
            Log.w(TAG, "playRecommended: recommended list is empty for type=$type")
            return false
        }
        overlayCoordinator.showOverlay(FeatureCards.PODCAST)
        playEpisode(nextEpisode)
        return true
    }

    fun playNext(type: String? = null): Boolean {
        Log.d(TAG, "playNext: type=$type")
        val nextEpisode = getNextEpisodeOf(type)
        if (nextEpisode == null) {
            Log.w(TAG, "playNext: recommended list is empty for type=$type")
            return false
        }
        overlayCoordinator.showOverlay(FeatureCards.PODCAST)
        playEpisode(nextEpisode)
        return true
    }

    fun pause() {
        val currentActive = _activeEpisode.value ?: return
        if (!_isPlaying.value) return // already paused

        if (currentActive.isDiy && currentActive.mediaUri != null) {
            playbackService.pause()
            savePlaybackProgress()
        } else {
            _isPlaying.value = false
            if (sessionStartTimestamp > 0L) {
                val listened = System.currentTimeMillis() - sessionStartTimestamp
                eventDispatcher.dispatch(
                    AiEvent.PodcastPlaybackStopped(
                        episodeId = currentActive.id,
                        title = currentActive.title,
                        type = currentActive.type,
                        channelName = currentActive.channelName,
                        progressPercent = _progress.value,
                        listenedMs = listened,
                        timestamp = System.currentTimeMillis(),
                        reason = "user_paused"
                    )
                )
                sessionStartTimestamp = 0L
            }
            stopTicker()
        }
    }

    fun resume() {
        val currentActive = _activeEpisode.value ?: return
        if (_isPlaying.value) return // already playing

        if (currentActive.isDiy && currentActive.mediaUri != null) {
            val playerState = playbackService.playbackState.value
            val isStopped = playerState.activeEpisodeId == null
            val exo = playbackService.getPlayer()
            val isIdleOrEnded = exo == null || exo.playbackState == Player.STATE_IDLE || exo.playbackState == Player.STATE_ENDED

            if (isStopped || isIdleOrEnded) {
                // If it ended or is stopped, we should play/re-prepare it
                val isEndedState = exo?.playbackState == Player.STATE_ENDED || currentActive.progress >= 95f
                val startPos = if (isEndedState) 0L else currentActive.lastPositionMs
                if (isEndedState) {
                    _progress.value = 0f
                    _activeEpisode.value = currentActive.copy(progress = 0f, lastPositionMs = 0L)
                }
                playbackService.play(
                    episodeId = currentActive.id,
                    mediaUri = currentActive.mediaUri,
                    startPositionMs = startPos
                )
                _isPlaying.value = true
                wasLastCompleted = false
            } else {
                playbackService.resume()
            }
        } else {
            delegateScope.launch {
                _playbackError.emit(R.string.podcast_error_invalid_demo_data)
            }
        }
    }

    fun closePlayer() {
        Log.d(TAG, "closePlayer()")
        val currentActive = _activeEpisode.value
        if (currentActive != null && currentActive.isDiy && currentActive.mediaUri != null) {
            playbackService.stop()
            savePlaybackProgress()
        } else {
            _isPlaying.value = false
            stopTicker()
        }
        overlayCoordinator.hideOverlay()
    }

    fun onDestroy() {
        Log.d(TAG, "onDestroy: cleaning up playback delegate resources")
        val active = _activeEpisode.value
        if (active != null && _isPlaying.value && sessionStartTimestamp > 0L) {
            val listened = System.currentTimeMillis() - sessionStartTimestamp
            eventDispatcher.dispatch(
                AiEvent.PodcastPlaybackStopped(
                    episodeId = active.id,
                    title = active.title,
                    type = active.type,
                    channelName = active.channelName,
                    progressPercent = _progress.value,
                    listenedMs = listened,
                    timestamp = System.currentTimeMillis(),
                    reason = "app_cleared"
                )
            )
        }
        savePlaybackProgress()
        stopTicker()
        playbackService.release()
    }
}
