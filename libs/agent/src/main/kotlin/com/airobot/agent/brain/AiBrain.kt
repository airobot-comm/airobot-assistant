package com.airobot.agent.brain

import com.airobot.agent.brain.model.Message
import kotlinx.coroutines.flow.StateFlow

/**
 * Unified AI brain state enumeration.
 * Drives robot visual state transitions and conversation lifecycle.
 */
enum class BrainState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

/**
 * Core AI brain interface — the single abstraction for all AI backends
 * (cloud proxy, local agent, etc.). Upper layers observe brainState
 * to react to AI lifecycle changes without knowing the backend details.
 */
interface AiBrain {
    val brainState: StateFlow<BrainState>
    val messages: StateFlow<List<Message>>
    val currentRoundUserText: StateFlow<String?>
    val currentRoundAiText: StateFlow<String?>
    val audioLevel: StateFlow<Float>

    fun wakeUp()
    fun sleep()

    // Conversation APIs
    fun startConversation(contextData: ByteArray? = null)
    fun stopAutoConversation()
    fun interrupt()
    fun interruptSpeak(source: String)
    fun isSessionActive(): Boolean
    fun injectSystemMessage(content: String)
}

