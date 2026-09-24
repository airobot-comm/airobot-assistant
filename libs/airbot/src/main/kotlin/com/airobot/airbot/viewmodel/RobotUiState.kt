package com.airobot.airbot.viewmodel

/**
 * Robot visual state — drives eyes, ears, body animations.
 * Mapped from RobotState; focused on visual presentation.
 *
 * Idle sub-states (BORED, DAZING, DOZING) are automatically cycled
 * by AirbotEngineApi's idle-loop when the engine is in Ready state.
 */
enum class RobotVisualState {
    IDLE,       // Ready standby — round eyes + breathing animation
    BORED,      // Restless — eyes wander left/right, body sways
    DAZING,     // Zoning out — large drift, dreamy gaze
    DOZING,     // Drowsy — sinking float, half-closed eyes
    SLEEPING,   // Asleep — closed eyes + slow breathing
    LISTENING,  // Listening — audio-reactive height pulse
    THINKING,   // Thinking — pulsing scale + eye drift
    SPEAKING,   // Speaking — scale pulse + mouth animation
    FOCUS,      // Focus mode — flat zen eyes
    HAPPY,      // Happy — curved smiling eyes
    WORKING;    // Working — active task execution, cyan scanning laser

    /**
     * Whether this state belongs to the idle family (non-interactive resting states).
     * These states do not have a voice input panel at the bottom.
     */
    val isIdleFamily: Boolean
        get() = this == IDLE || this == BORED || this == DAZING || this == DOZING || this == SLEEPING

    /**
     * Whether this state belongs to the dialogue family (active conversation).
     * These are the ONLY states that should trigger the voice input panel and dialogue flow at the bottom.
     */
    val isDialogueFamily: Boolean
        get() = this == LISTENING || this == THINKING || this == SPEAKING
}

/**
 * 交互类型
 */
enum class InteractionType {
    CHAT,   // 普通聊天模式
    CARD    // 功能卡片模式
}


/**
 * 机器人 UI 整体展现状态 (唯一的 UI Truth Source)
 */
data class RobotUiState(
    // === UI Visual & System ===
    val visualState: RobotVisualState = RobotVisualState.IDLE,
    val isConnected: Boolean = false,
    val isCharacterLoaded: Boolean = false,

    // === Interaction & Dialogue ===
    val interactionType: InteractionType = InteractionType.CHAT,
    val currentUserMsg: String? = null,
    val currentAiMsg: String? = null,
    val statusTip: String = "有什么可以帮你的？",

    // === Character & Engine ===
    val characterType: com.airobot.airbot.domain.model.CharacterType = com.airobot.airbot.domain.model.CharacterType.ANDROID_CANVAS,
    val roleName: String = "AETHER",
    val audioLevel: Float = 0f
) {
    /**
     * 是否处于交互状态
     */
    val isInteracting: Boolean
        get() = !visualState.isIdleFamily

    /**
     * 是否为卡片模式
     */
    val isCardMode: Boolean
        get() = isInteracting && interactionType == InteractionType.CARD

    /**
     * 动态状态提示
     */
    val dynamicStatusTip: String
        get() = statusTip
}
