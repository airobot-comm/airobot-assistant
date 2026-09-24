package com.airobot.features.podcast.data.model

import kotlinx.serialization.Serializable

/**
 * Podcast subscription (channel) data model.
 *
 * Represents both system-preset channels and user-created DIY channels.
 */
@Serializable
data class PodcastSubscription(
    val id: String,
    val title: String,
    val type: String,              // "video" | "audio" | "text"
    val time: String,              // e.g. "每天 08:00"
    val description: String,
    val isSubscribed: Boolean,
    val isDIY: Boolean = false,
    val sourceDir: String? = null,
    val filesCount: Int = 0,
    val createdAt: Long = 0L       // Creation timestamp in milliseconds
)
