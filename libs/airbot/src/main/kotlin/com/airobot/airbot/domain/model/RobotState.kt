package com.airobot.airbot.domain.model

/**
 * Conversation sub-state
 */
enum class ConversationSubState {
    LISTENING,
    THINKING,
    SPEAKING
}

/**
 * Generic service sub-state (decoupled from features module)
 */
enum class AirbotServiceSubState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED
}

/**
 * Top-level engine state — the system-level ground truth.
 */
sealed class RobotState {
    object Offline : RobotState()
    object Initializing : RobotState()
    data class Unauthorized(val code: String) : RobotState()
    object Connecting : RobotState()
    object Ready : RobotState()
    data class Conversation(val subState: ConversationSubState) : RobotState()
    data class FunctionService(
        val serviceId: String,
        val subState: AirbotServiceSubState
    ) : RobotState()
}
