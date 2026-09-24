package com.airobot.features.podcast.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airobot.features.aiserv.guidance.RemindEngine
import com.airobot.features.aiserv.guidance.data.RemindCard
import com.airobot.features.FeatureCards
import com.airobot.features.podcast.cards.creator.ScannedFile
import com.airobot.features.podcast.data.model.PodcastEpisode
import com.airobot.features.podcast.data.model.PodcastSubscription
import com.airobot.features.podcast.service.PlaybackState
import androidx.media3.common.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel facade for the AI Podcast feature.
 * Delegates actual business rules, playback controls, DIY imports,
 * and recommendation engine scoring to independent modular delegates.
 */
@HiltViewModel
class PodcastViewModel @Inject constructor(
    private val dataDelegate: PodcastDataDelegate,
    private val playbackDelegate: PodcastPlaybackDelegate,
    private val creationDelegate: PodcastCreationDelegate,
    private val recommendationEngine: PodcastRecommendationEngine,
    private val remindEngine: RemindEngine
) : ViewModel() {

    /** Podcast-specific remind cards filtered by podcast-related overlay tags. */
    val remindCards: Flow<List<RemindCard>> = remindEngine.getRemindCards(
        listOf(FeatureCards.PODCAST, FeatureCards.DIY_PODCAST, FeatureCards.LOGBOOK)
    )

    companion object {
        private const val TAG = "PodcastViewModel"
    }

    // --- State Streams Delegated to Sub-modules ---
    val episodes: StateFlow<List<PodcastEpisode>> = dataDelegate.episodes
    val subscriptions: StateFlow<List<PodcastSubscription>> = dataDelegate.subscriptions
    val activeEpisode: StateFlow<PodcastEpisode?> = playbackDelegate.activeEpisode
    val isPlaying: StateFlow<Boolean> = playbackDelegate.isPlaying
    val progress: StateFlow<Float> = playbackDelegate.progress
    val recommendation: StateFlow<String> = creationDelegate.recommendation
    
    // Engine recommendation flows
    val latestEpisodes: StateFlow<List<PodcastEpisode>> = recommendationEngine.latestEpisodes
    val recommendedEpisodes: StateFlow<List<PodcastEpisode>> = recommendationEngine.recommendedEpisodes

    val realPlaybackState: StateFlow<PlaybackState> = playbackDelegate.realPlaybackState

    val playbackError: Flow<Int> = playbackDelegate.playbackError

    fun getPlayer(): Player? = playbackDelegate.getPlayer()

    init {
        Log.d(TAG, "Initializing PodcastViewModel Facade")
    }

    // ============================================================
    // Delegated Operations & Mutators
    // ============================================================

    fun togglePlay() {
        playbackDelegate.togglePlay()
    }

    fun playEpisode(episode: PodcastEpisode) {
        playbackDelegate.playEpisode(episode)
    }

    fun toggleFavorite(id: String) {
        dataDelegate.toggleFavorite(id)
    }

    fun toggleSubscription(id: String) {
        dataDelegate.toggleSubscription(id)
    }

    fun addQnA(id: String, question: String, answer: String) {
        dataDelegate.addQnA(id, question, answer)
    }

    suspend fun scanMediaFiles(type: String): List<ScannedFile> {
        return creationDelegate.scanMediaFiles(type)
    }

    fun getDefaultScanPath(type: String): String {
        return creationDelegate.getDefaultScanPath(type)
    }

    suspend fun importAndCreateEpisode(file: ScannedFile, title: String, type: String): Boolean {
        return creationDelegate.importAndCreateEpisode(file, title, type)
    }

    fun generateEpisode(type: String, topic: String? = null) {
        creationDelegate.generateEpisode(type, topic)
    }

    fun seekForward() {
        playbackDelegate.seekForward()
    }

    fun seekBackward() {
        playbackDelegate.seekBackward()
    }

    fun seekTo(prog: Float) {
        playbackDelegate.seekTo(prog)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: cleaning up facade resources")
        playbackDelegate.onDestroy()
    }
}
