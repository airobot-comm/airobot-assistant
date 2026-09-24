package com.airobot.features.clock.data.model

import kotlinx.serialization.Serializable

/**
 * Preset item for Timer and Focus modes
 */
@Serializable
data class PresetItem(
    val id: String,
    val label: String,
    val seconds: Int,
    val reminderInterval: Int = 0,
    val bgMusic: String = "",
    val musicEnabled: Boolean = true,
    val mode: TimerMode = TimerMode.COUNTDOWN
)
