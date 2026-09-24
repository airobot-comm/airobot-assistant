package com.airobot.airbot.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airobot.airbot.character.RobotCharacter
import com.airobot.airbot.components.dialogue.DialoguePanel
import com.airobot.airbot.components.interaction.VoiceInputPanel
import com.airobot.airbot.domain.model.CharacterType
import com.airobot.agent.brain.model.Message
import com.airobot.airbot.viewmodel.RobotVisualState

/**
 * AirobotDialogueFlow 鈥?A unified, high-fidelity dialogue flow component.
 * Combines the robot character, message panel, and voice input into a single stable unit.
 *
 * Height constraint: The overall component must NOT exceed the functional card height
 * (typically fillMaxHeight(0.82f) in the center column). We achieve this by limiting
 * the outer Column to fillMaxHeight(0.82f) and using weight(1f) for the dialogue panel.
 */
@Composable
fun AirobotDialogueFlow(
    isVisible: Boolean,
    visualState: RobotVisualState,
    messages: List<Message>,
    characterType: CharacterType = CharacterType.ANDROID_CANVAS,
    roleName: String? = null,
    audioLevel: Float,
    isConnected: Boolean,
    scaleRatio: Float = 1.0f,
    onClose: () -> Unit,
    onStopListening: () -> Unit,
    onInterruptSpeak: () -> Unit,
    onCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Freeze the state when exiting to prevent the inner Voice Panel from reacting
    // to non-dialogue states (which would cause it to flash the IdleStatusPanel during fade-out).
    var displayState by remember { mutableStateOf(visualState) }
    if (visualState.isDialogueFamily) {
        displayState = visualState
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { 40 }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    // Constrain total height to not exceed card height
                    .fillMaxHeight(0.82f)
                    .width(320.dp * scaleRatio),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp * scaleRatio)
            ) {
                // 1. Spacer to push content downwards, leaving elegant whitespace above the dialogue panel
                Spacer(modifier = Modifier.weight(1f))

                // 2. Spacious Message Box with Overlapping Avatar inside header
                Box(
                    modifier = Modifier
                        .height(345.dp * scaleRatio) // Spacious height (expanded 10%) to show multiple messages and let layout breathe
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) {
                    // The Dialogue Message Panel (Header + List)
                    DialoguePanel(
                        isVisible = true,
                        visualState = displayState,
                        messages = messages,
                        scaleRatio = scaleRatio,
                        onClose = onClose,
                        modifier = Modifier.fillMaxSize()
                    )

                    // The Floating Avatar (Nestled inside header, not exceeding card top, slightly overflowing left)
                    RobotCharacter(
                        state = displayState,
                        characterType = characterType,
                        roleName = roleName,
                        audioLevel = { audioLevel },
                        headSize = 80.dp * scaleRatio,  // Generous head size matching Figure 2
                        showAura = false,                // No background glow in mini mode
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-18).dp * scaleRatio, y = 12.dp * scaleRatio)
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                clip = false,
                                spotColor = Color.Black.copy(alpha = 0.12f)
                            )
                    )
                }

                // 3. Integrated Voice Input Panel (Fixed below the message box)
                VoiceInputPanel(
                    robotState = displayState,
                    isConnected = isConnected,
                    audioLevel = audioLevel,
                    scaleRatio = scaleRatio,
                    onStopListening = onStopListening,
                    onInterruptSpeak = onInterruptSpeak,
                    onTimerControl = {},
                    onCommandClick = onCommandClick,
                    modifier = Modifier.wrapContentSize()
                )

                // Bottom spacer to ensure the second row of prompt suggestion tags aligns with left card bottom
                Spacer(modifier = Modifier.height(14.dp * scaleRatio))
            }
        }
    }
}
