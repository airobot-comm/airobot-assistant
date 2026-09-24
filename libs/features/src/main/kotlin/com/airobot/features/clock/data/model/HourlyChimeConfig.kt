package com.airobot.features.clock.data.model

import kotlinx.serialization.Serializable

enum class ChimeMode { EVERY_HOUR, ODD_HOUR, EVEN_HOUR }

@Serializable
data class HourlyChimeConfig(
    val enabled: Boolean = true,
    val mode: ChimeMode = ChimeMode.EVERY_HOUR,
    val startHour: Int = 7,       // 0-23
    val endHour: Int = 22,        // 0-23
    val weatherReminder: Boolean = false,  // placeholder, AI integration later
    val scheduleReminder: Boolean = false  // placeholder, AI integration later
)
