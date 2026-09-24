package com.airobot.features.schedule.data.model

import kotlinx.serialization.Serializable

/**
 * Schedule item
 */
@Serializable
data class ScheduleItem(
    val id: String,
    val time: String,                 // HH:mm format
    val task: String,
    val completed: Boolean = false,
    val category: String = "work",    // work, health, personal
    val durationMin: Int = 90,        // duration in minutes
    val dayOfWeek: Int? = null,       // Day of week (0=Sun, 1=Mon, ..., 6=Sat)
    val date: String? = null          // YYYY-MM-DD format
)
