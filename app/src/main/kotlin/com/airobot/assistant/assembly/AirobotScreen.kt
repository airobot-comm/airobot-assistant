package com.airobot.assistant.assembly

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.airobot.airbot.character.RobotCharacter
import com.airobot.airbot.components.dialogue.BubbleAiDialogue
import com.airobot.airbot.components.dialogue.BubbleUserMessage
import com.airobot.airbot.components.interaction.VoiceInputPanel
import com.airobot.airbot.domain.model.CharacterType
import com.airobot.airbot.viewmodel.RobotVisualState

@Composable
fun AirobotScreen(
    robotHorizontalBias: Float,
    robotVisualState: RobotVisualState,
    characterType: CharacterType,
    roleName: String,
    audioLevel: Float,
    isConnected: Boolean,
    isTimerActive: Boolean,
    isTimerPaused: Boolean,
    currentRoundAiText: String?,
    currentRoundUserText: String?,
    isCardMode: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onInterruptSpeak: () -> Unit,
    onTimerControl: (String) -> Unit,
    onCommandClick: (String) -> Unit,
    onBubbleClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Dynamic scaling of robot character: 15% reduction in card mode (340.dp)
    val headSize by animateDpAsState(
        targetValue = if (isCardMode) 340.dp else 400.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "robotHeadSize"
    )

    ConstraintLayout(modifier = modifier.fillMaxSize()) {
        val (robotRef, voicePanelRef, aiBubbleRef, userBubbleRef) = createRefs()

        // 1. 机器人角色 (Centered vertically in the visible screen space)
        Box(
            modifier = Modifier
                .constrainAs(robotRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom, margin = 64.dp) // Exclude bottom footer
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    horizontalBias = robotHorizontalBias
                    verticalBias = if (isCardMode) 0.33f else 0.45f
                    height = Dimension.wrapContent // Wrap content to precisely reference its bottom edge
                },
            contentAlignment = Alignment.Center
        ) {
            RobotCharacter(
                state = robotVisualState,
                characterType = characterType,
                roleName = roleName,
                audioLevel = { audioLevel },
                headSize = headSize
            )
        }

        // 2. 语音输入面板 (Centered vertically in the gap between robot.bottom and parent.bottom)
        Box(
            modifier = Modifier
                .constrainAs(voicePanelRef) {
                    top.linkTo(robotRef.bottom)
                    bottom.linkTo(parent.bottom, margin = 48.dp) // Positioned above the bottom bar
                    start.linkTo(robotRef.start)
                    end.linkTo(robotRef.end)
                    verticalBias = 0.5f
                }
        ) {
            VoiceInputPanel(
                robotState = robotVisualState,
                isConnected = isConnected,
                isTimerActive = isTimerActive,
                isTimerPaused = isTimerPaused,
                audioLevel = audioLevel,
                onStopListening = onStopListening,
                onInterruptSpeak = onInterruptSpeak,
                onTimerControl = onTimerControl,
                onCommandClick = { command ->
                    if (command == "WAKE_UP") {
                        onStartListening()
                    } else {
                        onCommandClick(command)
                    }
                }
            )
        }

        // 3. AI 对话气泡 (Always placed on the right of the robot character)
        Box(
            modifier = Modifier
                .constrainAs(aiBubbleRef) {
                    start.linkTo(robotRef.end, margin = (-50).dp) // Shifted rightwards to avoid covering face
                    top.linkTo(robotRef.top)
                    bottom.linkTo(robotRef.bottom)
                    verticalBias = 0.42f
                }
        ) {
            BubbleAiDialogue(
                robotState = robotVisualState,
                aiMsg = currentRoundAiText,
                onAiSpeechComplete = {},
                onClose = onBubbleClose,
                bubbleMaxWidth = if (isCardMode) 180.dp else 360.dp // Made shorter in card mode to avoid overlapping the card on the right
            )
        }

        // 4. 用户对话气泡 (显示在提示词位置，遮挡提示词；当对话结束/非对话状态时收起)
        Box(
            modifier = Modifier
                .constrainAs(userBubbleRef) {
                    bottom.linkTo(voicePanelRef.bottom)
                    start.linkTo(voicePanelRef.start)
                    end.linkTo(voicePanelRef.end)
                }
        ) {
            BubbleUserMessage(
                message = if (robotVisualState.isDialogueFamily) (currentRoundUserText ?: "") else ""
            )
        }
    }
}

