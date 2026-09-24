package com.airobot.features.podcast.viewmodel

import com.airobot.features.podcast.data.model.PodcastEpisode

/**
 * Strategy interface for calculating podcast episode recommendation scores.
 * Enables extensible plug-and-play algorithm tuning.
 */
interface PodcastRecommendationStrategy {
    /**
     * Compute and sort the recommended podcast episodes.
     */
    fun recommend(
        episodes: List<PodcastEpisode>,
        activeEpisode: PodcastEpisode?,
        isPlaying: Boolean
    ): List<PodcastEpisode>
}

/**
 * Default implementation of [PodcastRecommendationStrategy].
 * Prioritizes unplayed new episodes, in-progress episodes, and favorites.
 */
class DefaultPodcastRecommendationStrategy : PodcastRecommendationStrategy {
    override fun recommend(
        episodes: List<PodcastEpisode>,
        activeEpisode: PodcastEpisode?,
        isPlaying: Boolean
    ): List<PodcastEpisode> {
        return episodes
            .map { ep ->
                var score = 0f
                
                // 1. Progress state (unplayed vs uncompleted vs played-favorite)
                if (ep.progress == 0f) {
                    score += 70f // Brand new
                } else if (ep.progress > 0f && ep.progress < 95f) {
                    score += 80f + (ep.progress / 5f) // In-progress, closer to completion gets higher priority
                } else if (ep.played) {
                    score += 30f // Played but favorited (lower base score so unplayed take priority)
                }

                // 2. Favorite boost (representing user preference)
                if (ep.favorite) {
                    score += 25f
                }

                // 3. Play count boost (playback count history)
                val playCountBonus = (ep.playCount * 5f).coerceAtMost(15f)
                score += playCountBonus

                // 4. Current active status
                if (ep.id == activeEpisode?.id) {
                    score += if (isPlaying) 15f else 25f // Paused gets a higher boost to resume immediately
                }

                // 5. Recency micro bonus
                score += (ep.createdAt % 100_000) / 10_000f

                ep to score
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
