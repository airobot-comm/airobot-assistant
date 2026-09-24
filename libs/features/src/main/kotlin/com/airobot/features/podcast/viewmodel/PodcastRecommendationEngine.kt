package com.airobot.features.podcast.viewmodel

import com.airobot.features.podcast.data.model.PodcastEpisode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine responsible for reactively computing the latest (top-6 time-sorted, unplayed prioritized)
 * and recommended (strategy-based) podcast lists.
 */
@Singleton
class PodcastRecommendationEngine @Inject constructor(
    private val dataDelegate: PodcastDataDelegate,
    private val playbackDelegate: PodcastPlaybackDelegate
) {
    
    private val _latestEpisodes = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    val latestEpisodes: StateFlow<List<PodcastEpisode>> = _latestEpisodes.asStateFlow()

    private val _recommendedEpisodes = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    val recommendedEpisodes: StateFlow<List<PodcastEpisode>> = _recommendedEpisodes.asStateFlow()

    var strategy: PodcastRecommendationStrategy = DefaultPodcastRecommendationStrategy()
    
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        engineScope.launch {
            combine(
                dataDelegate.episodes,
                playbackDelegate.activeEpisode,
                playbackDelegate.isPlaying
            ) { eps, active, playing ->
                Triple(eps, active, playing)
            }.collect { (eps, active, playing) ->
                // 1. Calculate Latest Episodes (Top 6 time-series sorted, unplayed first)
                val top6Recent = eps.sortedByDescending { it.createdAt }.take(6)
                val unplayed = top6Recent.filter { !it.played }.sortedByDescending { it.createdAt }
                val played = top6Recent.filter { it.played }.sortedByDescending { it.createdAt }
                _latestEpisodes.value = unplayed + played

                // 2. Calculate Strategy-based Recommended Episodes
                _recommendedEpisodes.value = strategy.recommend(eps, active, playing)
            }
        }
    }
}
