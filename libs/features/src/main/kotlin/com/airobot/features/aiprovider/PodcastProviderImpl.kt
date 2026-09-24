package com.airobot.features.aiprovider

import com.airobot.agent.skills.podcast.PodcastProvider
import com.airobot.features.podcast.viewmodel.PodcastDataDelegate
import com.airobot.features.podcast.viewmodel.PodcastPlaybackDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PodcastProvider, bridging Agent system capability calls
 * to actual features/podcast components.
 */
@Singleton
class PodcastProviderImpl @Inject constructor(
    private val dataDelegate: PodcastDataDelegate,
    private val playbackDelegate: PodcastPlaybackDelegate
) : PodcastProvider {

    override val isReady: Boolean
        get() = true

    override val isPlaying: Boolean
        get() = playbackDelegate.isPlaying.value

    override suspend fun getEpisodeTitles(): List<String> = withContext(Dispatchers.Main) {
        dataDelegate.episodes.value.map { it.title }
    }

    override suspend fun playEpisode(title: String): Boolean = withContext(Dispatchers.Main) {
        val episodes = dataDelegate.episodes.value
        val targetEpisode = episodes.find { it.title == title } ?: return@withContext false
        playbackDelegate.playEpisode(targetEpisode)
        true
    }

    override suspend fun playRecommended(type: String?): Boolean = withContext(Dispatchers.Main) {
        playbackDelegate.playRecommended(type)
    }

    override suspend fun playNext(type: String?): Boolean = withContext(Dispatchers.Main) {
        playbackDelegate.playNext(type)
    }

    override suspend fun pause(): Boolean = withContext(Dispatchers.Main) {
        playbackDelegate.pause()
        true
    }

    override suspend fun resume(): Boolean = withContext(Dispatchers.Main) {
        playbackDelegate.resume()
        true
    }

    override suspend fun closePlayer(): Boolean = withContext(Dispatchers.Main) {
        playbackDelegate.closePlayer()
        true
    }
}
