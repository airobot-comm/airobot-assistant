package com.airobot.airbot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airobot.agent.brain.AiBrain
import com.airobot.airbot.api.AirbotCharacterApi
import com.airobot.airbot.api.AirbotEngineApi
import com.airobot.airbot.domain.model.AirbotServiceSubState
import com.airobot.airbot.domain.model.CharacterType
import com.airobot.airbot.domain.model.ConversationSubState
import com.airobot.airbot.domain.model.RobotState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Presentation controller for the [AirobotScreen].
 * Decouples the character configuration and system voice levels of Airbot from the App shell.
 */
@HiltViewModel
class AirobotViewModel @Inject constructor(
    robotStateEngine: AirbotEngineApi,
    characterManager: AirbotCharacterApi,
    aiBrain: AiBrain
) : ViewModel() {

    val uiState: StateFlow<RobotUiState> = combine(
        robotStateEngine.robotState,
        robotStateEngine.idleVisualState,
        characterManager.activeCharacter,
        aiBrain.audioLevel
    ) { engineState, idleVisual, activeRole, voiceLevel ->
        val visualState = when (engineState) {
            is RobotState.Offline -> RobotVisualState.SLEEPING
            is RobotState.Initializing -> RobotVisualState.THINKING
            is RobotState.Connecting -> RobotVisualState.THINKING
            is RobotState.Unauthorized -> RobotVisualState.IDLE
            is RobotState.Ready -> idleVisual
            is RobotState.Conversation -> when (engineState.subState) {
                ConversationSubState.LISTENING -> RobotVisualState.LISTENING
                ConversationSubState.THINKING -> RobotVisualState.THINKING
                ConversationSubState.SPEAKING -> RobotVisualState.SPEAKING
            }

            is RobotState.FunctionService -> when (engineState.subState) {
                AirbotServiceSubState.IDLE -> RobotVisualState.IDLE
                AirbotServiceSubState.RUNNING -> RobotVisualState.WORKING
                AirbotServiceSubState.PAUSED -> RobotVisualState.IDLE
                AirbotServiceSubState.COMPLETED -> RobotVisualState.HAPPY
                AirbotServiceSubState.CANCELLED -> RobotVisualState.IDLE
            }
        }

        RobotUiState(
            visualState = visualState,
            isConnected = engineState !is RobotState.Offline,
            isCharacterLoaded = activeRole != null,
            characterType = CharacterType.fromString(activeRole?.characterType ?: "ANDROID_CANVAS"),
            roleName = activeRole?.roleName ?: "AETHER",
            audioLevel = voiceLevel
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RobotUiState(isCharacterLoaded = false)
    )
}
