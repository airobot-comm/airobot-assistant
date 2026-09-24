package com.airobot.airbot.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airobot.agent.brain.AiBrain
import com.airobot.agent.brain.BrainState
import com.airobot.agent.brain.model.Message
import com.airobot.airbot.api.AirbotEngineApi
import com.airobot.airbot.domain.model.ConversationSubState
import com.airobot.airbot.domain.model.RobotState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI Conversation ViewModel.
 * Delegates dialogue state flows and session control commands to the AiBrain facade.
 * Synchronizes brainState changes with the system RobotStateEngine.
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    application: Application,
    private val robotStateEngine: AirbotEngineApi,
    private val aiBrain: AiBrain
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ConversationViewModel"
    }

    // Delegate State Flows directly to AiBrain
    val messages: StateFlow<List<Message>> = aiBrain.messages
    val currentRoundUserText: StateFlow<String?> = aiBrain.currentRoundUserText
    val currentRoundAiText: StateFlow<String?> = aiBrain.currentRoundAiText
    val audioLevel: StateFlow<Float> = aiBrain.audioLevel

    init {
        // Collect brainState from AiBrain and synchronize with robotStateEngine
        viewModelScope.launch {
            aiBrain.brainState.collect { brainState ->
                val currentRobotState = robotStateEngine.robotState.value
                Log.d(TAG, "Syncing brainState: $brainState, current robotState: $currentRobotState")

                if (currentRobotState is RobotState.Ready ||
                    currentRobotState is RobotState.Conversation ||
                    currentRobotState is RobotState.Connecting
                ) {
                    when (brainState) {
                        BrainState.LISTENING -> {
                            robotStateEngine.updateEngineState(
                                RobotState.Conversation(ConversationSubState.LISTENING)
                            )
                        }
                        BrainState.THINKING -> {
                            robotStateEngine.updateEngineState(
                                RobotState.Conversation(ConversationSubState.THINKING)
                            )
                        }
                        BrainState.SPEAKING -> {
                            robotStateEngine.updateEngineState(
                                RobotState.Conversation(ConversationSubState.SPEAKING)
                            )
                        }
                        BrainState.IDLE -> {
                            if (currentRobotState is RobotState.Conversation) {
                                Log.d(TAG, "Brain returned to IDLE, transitioning robotState to Ready")
                                robotStateEngine.updateEngineState(RobotState.Ready)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Delegation Commands ---

    fun startConversation(contextData: ByteArray? = null) {
        Log.d(TAG, "startConversation called")
        aiBrain.startConversation(contextData)
    }

    fun interrupt() {
        Log.d(TAG, "interrupt called")
        aiBrain.interrupt()
    }

    fun stopAutoConversation() {
        Log.d(TAG, "stopAutoConversation called")
        aiBrain.stopAutoConversation()
    }

    fun interruptSpeak(source: String = "ui") {
        Log.d(TAG, "interruptSpeak called by source: $source")
        aiBrain.interruptSpeak(source)
    }
}
