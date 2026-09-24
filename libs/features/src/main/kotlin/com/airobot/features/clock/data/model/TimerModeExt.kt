package com.airobot.features.clock.data.model

import com.airobot.features.aiserv.event.AiTimerCategory

/**
 * Extension to map clock-domain TimerMode to AI-domain AiTimerCategory.
 */
fun TimerMode.toAiCategory(): AiTimerCategory = when (this) {
    TimerMode.COUNTDOWN -> AiTimerCategory.COUNTDOWN
    TimerMode.FOCUS -> AiTimerCategory.FOCUS
}
