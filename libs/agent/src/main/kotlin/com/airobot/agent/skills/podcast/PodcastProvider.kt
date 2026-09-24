package com.airobot.agent.skills.podcast

/**
 * Capability interface for podcast features.
 * Implemented by feature modules and consumed by agent skills.
 */
interface PodcastProvider {
    /**
     * Indicates whether the podcast capability is initialized and ready to play.
     */
    val isReady: Boolean

    /**
     * Indicates whether the podcast is currently playing.
     */
    val isPlaying: Boolean

    /**
     * Returns the titles of all available episodes/songs.
     */
    suspend fun getEpisodeTitles(): List<String>

    /**
     * Plays the episode with the exact title.
     * Returns true if successful, false otherwise.
     */
    suspend fun playEpisode(title: String): Boolean

    /**
     * Plays the recommended episodes, optionally filtered by type.
     */
    suspend fun playRecommended(type: String?): Boolean

    /**
     * Skips to the next episode, optionally filtered by type.
     */
    suspend fun playNext(type: String?): Boolean

    /**
     * Pauses podcast playback.
     */
    suspend fun pause(): Boolean

    /**
     * Resumes podcast playback.
     */
    suspend fun resume(): Boolean

    /**
     * Closes the podcast player overlay and pauses playback.
     */
    suspend fun closePlayer(): Boolean
}
