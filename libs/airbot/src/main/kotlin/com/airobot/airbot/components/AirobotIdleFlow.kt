package com.airobot.airbot.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airobot.airbot.character.RobotCharacter
import com.airobot.airbot.components.interaction.VoiceInputPanel
import com.airobot.airbot.domain.model.CharacterType
import com.airobot.airbot.viewmodel.RobotVisualState

/**
 * AirobotIdleFlow — A high-level idle widget.
 * Combines the pure robot character avatar with the IDLE state voice input panel
 * (which renders the "叫名字对话" prompt and cyan dot waveform).
 */
@Composable
fun AirobotIdleFlow(
    isVisible: Boolean,
    visualState: RobotVisualState,
    characterType: CharacterType = CharacterType.ANDROID_CANVAS,
    roleName: String? = null,
    audioLevel: Float,
    isConnected: Boolean,
    characterSize: Dp,
    scaleRatio: Float = 1.0f,
    onRobotClick: () -> Unit,
    onCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Freeze the state when exiting to prevent the inner Voice Panel from reacting
    // to dialogue states (which would cause it to flash the ActiveStatusPanel during fade-out).
    var displayState by remember { mutableStateOf(visualState) }
    if (!visualState.isDialogueFamily) {
        displayState = visualState
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Pure Avatar
            RobotCharacter(
                state = displayState,
                characterType = characterType,
                roleName = roleName,
                audioLevel = { audioLevel },
                headSize = characterSize,
                onRobotClick = onRobotClick
            )

            Spacer(modifier = Modifier.height(8.dp * scaleRatio))

            // 2. Idle Voice Input Prompt
            VoiceInputPanel(
                robotState = displayState,
                isConnected = isConnected,
                audioLevel = audioLevel,
                scaleRatio = scaleRatio,
                onStopListening = {},
                onInterruptSpeak = {},
                onTimerControl = {},
                onCommandClick = onCommandClick
            )
        }
    }
}
