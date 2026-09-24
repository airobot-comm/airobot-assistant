package com.airobot.features.aiserv.event

/**
 * Generic timer category for AI event classification.
 * Feature modules map their internal timer types to this enum
 * when dispatching events through the AI event bus.
 */
enum class AiTimerCategory {
    COUNTDOWN,
    FOCUS
}
