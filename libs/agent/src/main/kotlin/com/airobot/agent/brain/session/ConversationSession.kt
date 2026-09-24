package com.airobot.agent.brain.session

import android.util.Log
import com.airobot.agent.brain.BrainState
import com.airobot.agent.brain.model.Message
import com.airobot.agent.brain.model.MessageRole
import com.airobot.agent.brain.history.AgentHistoryRepository
import com.airobot.agent.manager.AgentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reusable dialogue conversation session state machine and timer controller.
 * Tracks message logs, active round texts, brainState, and inactivity/thinking timeouts.
 * Independent of network transport protocols.
 */
class ConversationSession @Inject constructor(
    private val configManager: AgentManager,
    private val historyRepository: AgentHistoryRepository
) {
    companion object {
        private const val TAG = "ConversationSession"
        private const val INACTIVITY_TIMEOUT_MS = 90000L
        private const val THINKING_TIMEOUT_MS = 10000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Dialogue State Flows
    private val _brainState = MutableStateFlow(BrainState.IDLE)
    val brainState = _brainState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _currentRoundUserText = MutableStateFlow<String?>(null)
    val currentRoundUserText = _currentRoundUserText.asStateFlow()

    private val _currentRoundAiText = MutableStateFlow<String?>(null)
    val currentRoundAiText = _currentRoundAiText.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel = _audioLevel.asStateFlow()

    // Session Status Flags
    var isActive: Boolean = false
        private set
    var isAutoMode: Boolean = false
        private set

    private var inactivityJob: Job? = null
    private var thinkingTimeoutJob: Job? = null

    init {
        scope.launch {
            configManager.activeRole.collect { roleName ->
                if (roleName != null) {
                    Log.d(TAG, "Active role changed to $roleName, loading conversation history")
                    val history = historyRepository.getMessagesForRole(roleName)
                    _messages.value = history
                } else {
                    _messages.value = emptyList()
                }
            }
        }
    }

    fun startSession(autoMode: Boolean) {
        isActive = true
        isAutoMode = autoMode
        resetRoundText()
    }

    fun stopSession() {
        isActive = false
        isAutoMode = false
        clean()
    }

    fun setBrainState(state: BrainState) {
        _brainState.value = state
    }

    fun updateAudioLevel(level: Float) {
        _audioLevel.value = level
    }

    fun addMessage(message: Message) {
        _messages.value = _messages.value + message
        val role = configManager.getActiveRole()
        if (role != null) {
            scope.launch {
                historyRepository.saveMessage(role, message)
            }
        }
    }

    fun resetRoundText() {
        _currentRoundUserText.value = null
        _currentRoundAiText.value = null
        resetInactivityTimer()
    }

    fun handleSttResult(text: String) {
        cancelThinkingTimeout()
        if (text.isNotBlank()) {
            resetInactivityTimer()
            _currentRoundUserText.value = text
            addMessage(Message(role = MessageRole.USER, content = text))
        }
    }

    fun handleTtsSentence(text: String) {
        resetInactivityTimer()
        _currentRoundAiText.value = text
        addMessage(Message(role = MessageRole.AGENT, content = text))
    }

    fun resetInactivityTimer(onTimeout: () -> Unit = {}) {
        inactivityJob?.cancel()
        if (isActive) {
            inactivityJob = scope.launch {
                delay(INACTIVITY_TIMEOUT_MS)
                Log.d(TAG, "Inactivity timeout reached")
                onTimeout()
            }
        }
    }

    fun cancelInactivityTimer() {
        inactivityJob?.cancel()
    }

    fun startThinkingTimeout(onTimeout: () -> Unit) {
        thinkingTimeoutJob?.cancel()
        thinkingTimeoutJob = scope.launch {
            delay(THINKING_TIMEOUT_MS)
            if (_brainState.value == BrainState.THINKING) {
                Log.w(TAG, "Thinking timeout reached")
                onTimeout()
            }
        }
    }

    fun cancelThinkingTimeout() {
        thinkingTimeoutJob?.cancel()
    }

    fun startNextRound() {
        cancelThinkingTimeout()
        _currentRoundUserText.value = null
        _currentRoundAiText.value = null
        _brainState.value = BrainState.LISTENING
        resetInactivityTimer()
    }

    fun clean() {
        cancelThinkingTimeout()
        cancelInactivityTimer()
        _currentRoundUserText.value = null
        _currentRoundAiText.value = null
        _brainState.value = BrainState.IDLE
    }
}
