package com.airobot.features.schedule.data.model

import kotlinx.serialization.Serializable

/**
 * Todo task item
 */
@Serializable
data class TodoItem(
    val id: String,
    val task: String,
    val status: String = "open", // "open" or "closed"
    val date: String? = null,    // YYYY-MM-DD
    val time: String? = null,    // HH:mm format (optional)
    val createdAt: Long = System.currentTimeMillis()
)
