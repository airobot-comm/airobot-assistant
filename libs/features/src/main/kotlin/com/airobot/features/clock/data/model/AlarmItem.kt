package com.airobot.features.clock.data.model

import kotlinx.serialization.Serializable

/**
 * Alarm item.
 * Key design constraint: The list is fixed to 4 slots (no add/delete allowed).
 * [voiceMode] determines the skeuomorphic behavior: 'hint', 'standard', or 'urgent'.
 */
@Serializable
data class AlarmItem(
    val id: String,
    val time: String,                   // HH:mm format
    val label: String,
    val enabled: Boolean = true,
    val days: List<Int> = emptyList(),  // 0-6 weekday (0 is Sunday, 1 is Monday, etc. matching React)
    val type: String = "everyday",       // workday, everyday, temporary
    val repeatCount: Int = 3,           // 1-5 times
    val interval: Int = 5,              // 1-15 minutes
    val voiceMode: String = "standard", // hint, standard, urgent
    val requireName: Boolean = false,   // legacy field, kept for backward compatibility
    val dismissMode: String = "manual", // manual, auto, voice
    val autoDismissSeconds: Int = 10,   // seconds before auto-dismiss
    val soundId: String = "system_default" // system default or raw resource name
)
