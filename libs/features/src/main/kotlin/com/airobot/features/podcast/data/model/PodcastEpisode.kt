package com.airobot.features.podcast.data.model

import kotlinx.serialization.Serializable

@Serializable
data class QnaItem(
    val question: String,
    val answer: String
)

/**
 * Podcast episode data model.
 *
 * Supports both system-preset text episodes (demo content) and
 * user-imported DIY episodes with real media playback.
 */
@Serializable
data class PodcastEpisode(
    val id: String,
    val title: String,
    val summary: String,
    val type: String,                    // "video" | "audio" | "text"
    val channelName: String,
    val content: String,
    val date: String,
    val bgImage: String? = null,
    val progress: Float = 0f,            // 0f to 100f (UI percentage)
    val played: Boolean = false,
    val favorite: Boolean = false,
    val qnaHistory: List<QnaItem> = emptyList(),
    // --- Real media fields (for DIY episodes) ---
    val mediaUri: String? = null,        // Internal storage URI for ExoPlayer
    val durationMs: Long = 0L,           // Actual media duration in milliseconds
    val lastPositionMs: Long = 0L,       // Resume position for playback history
    val originalFileName: String? = null, // Original file name before import
    val fileSizeBytes: Long = 0L,        // File size in bytes
    val mimeType: String? = null,        // e.g., "audio/mpeg", "video/mp4"
    val isDiy: Boolean = false,          // True for user-imported episodes
    val createdAt: Long = 0L,            // Creation timestamp in epoch milliseconds
    val playCount: Int = 0               // Playback count
)
