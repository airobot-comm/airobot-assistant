package com.airobot.airbot.components.interaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.airbot.viewmodel.RobotVisualState
import com.airobot.framework.R
import com.airobot.framework.theme.RobotTheme
import com.airobot.airbot.R as AirbotR

/**
 * 机器人语音交互面板 - 极致通透动态感应版 (响应式比例版)
 */
@Composable
fun VoiceInputPanel(
    robotState: RobotVisualState,
    isConnected: Boolean,
    isTimerActive: Boolean = false,
    isTimerPaused: Boolean = false,
    audioLevel: Float = 0.0f,
    scaleRatio: Float = 1.0f,
    onStopListening: () -> Unit,
    onInterruptSpeak: () -> Unit,
    onTimerControl: (String) -> Unit,
    onCommandClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isListening = robotState == RobotVisualState.LISTENING
    val isThinking = robotState == RobotVisualState.THINKING
    val isSpeaking = robotState == RobotVisualState.SPEAKING
    val isIdle = robotState.isIdleFamily

    AnimatedVisibility(
        visible = isListening || isThinking || isSpeaking || isTimerActive || isIdle,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            when {
                isTimerActive -> TimerControlPanel(
                    isTimerPaused = isTimerPaused,
                    scaleRatio = scaleRatio,
                    onTimerControl = onTimerControl
                )

                isIdle -> IdleStatusPanel(
                    audioLevel = audioLevel,
                    scaleRatio = scaleRatio,
                    onClick = { onCommandClick("WAKE_UP") }
                )

                else -> ActiveStatusPanel(
                    isListening = isListening,
                    isThinking = isThinking,
                    isSpeaking = isSpeaking,
                    audioLevel = audioLevel,
                    scaleRatio = scaleRatio,
                    onStopListening = onStopListening,
                    onInterruptSpeak = onInterruptSpeak,
                    onCommandClick = onCommandClick
                )
            }
        }
    }
}

@Composable
private fun IdleStatusPanel(audioLevel: Float, scaleRatio: Float, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp * scaleRatio),
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        // Idle status waveform
        VoiceWaveform(
            style = WaveformStyle.IDLE_BARS,
            audioLevel = audioLevel,
            scaleRatio = scaleRatio
        )

        Text(
            text = stringResource(AirbotR.string.voice_idle_tip),
            color = Color(0xFF22D3EE).copy(alpha = 0.8f),
            fontSize = 12.sp * scaleRatio,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun ActiveStatusPanel(
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    audioLevel: Float,
    scaleRatio: Float,
    onStopListening: () -> Unit,
    onInterruptSpeak: () -> Unit,
    onCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp * scaleRatio)
    ) {
        // 核心交互区域：245dp 宽度基准，全圆角
        Box(
            modifier = Modifier
                .width(245.dp * scaleRatio)
                .height(64.dp * scaleRatio)
                .then(
                    if (!isDark) Modifier.shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        clip = false,
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    ) else Modifier
                )
                .clip(CircleShape)
                .background(
                    if (isDark) Color.White.copy(alpha = 0.05f)
                    else Color.White
                )
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = CircleShape
                )
                .clickable {
                    if (isSpeaking) {
                        onInterruptSpeak()
                    } else {
                        onStopListening()
                    }
                }
                .padding(horizontal = 20.dp * scaleRatio),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp * scaleRatio),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 动态感应波形 (橙色 Bars)
                VoiceWaveform(
                    style = WaveformStyle.ACTIVE_BARS,
                    audioLevel = audioLevel,
                    scaleRatio = scaleRatio
                )

                Text(
                    text = if (isThinking) stringResource(AirbotR.string.voice_status_thinking)
                    else if (isSpeaking) stringResource(AirbotR.string.voice_status_speaking)
                    else stringResource(AirbotR.string.voice_status_listening),
                    color = Color(0xFFF97316), // orange-500
                    fontSize = 16.sp * scaleRatio,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        }

        // Suggestion Tags (Always visible during interaction, especially when waiting for user input)
        FlowRow(
            modifier = Modifier.widthIn(max = 282.dp * scaleRatio),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp * scaleRatio)
        ) {
            val suggestions = listOf(
                stringResource(AirbotR.string.voice_suggest_next) to "NEXT",
                stringResource(AirbotR.string.voice_suggest_summary) to "SUMMARY",
                stringResource(AirbotR.string.voice_suggest_recommend) to "RECOMMEND"
            )
            suggestions.forEach { (label, cmd) ->
                SuggestionTag(
                    text = label,
                    scaleRatio = scaleRatio,
                    onClick = {
                        if (isSpeaking) {
                            onInterruptSpeak()
                        } else {
                            onCommandClick(cmd)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SuggestionTag(text: String, scaleRatio: Float, onClick: () -> Unit) {
    val isDark = RobotTheme.isDark
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp * scaleRatio)
            .clip(CircleShape)
            .background(
                if (isDark) Color.White.copy(alpha = 0.05f)
                else Color(0xFFF1F5F9) // slate-100
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                shape = CircleShape
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp * scaleRatio, vertical = 6.dp * scaleRatio)
    ) {
        Text(
            text = text,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B), // slate-400/500
            fontSize = 10.sp * scaleRatio,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
private fun TimerControlPanel(
    isTimerPaused: Boolean,
    scaleRatio: Float,
    onTimerControl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp * scaleRatio, vertical = 8.dp * scaleRatio),
        horizontalArrangement = Arrangement.spacedBy(12.dp * scaleRatio),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimerControlChip(
            iconResId = if (!isTimerPaused) R.drawable.cloud_on else R.drawable.mic,
            text = if (!isTimerPaused) stringResource(AirbotR.string.voice_timer_pause) else stringResource(
                AirbotR.string.voice_timer_resume
            ),
            iconColor = Color(0xFFFACC15),
            scaleRatio = scaleRatio,
            onClick = { onTimerControl(if (!isTimerPaused) "PAUSE" else "RESUME") }
        )
        TimerControlChip(
            iconResId = R.drawable.close,
            text = stringResource(AirbotR.string.voice_timer_stop),
            iconColor = Color(0xFFF87171),
            scaleRatio = scaleRatio,
            onClick = { onTimerControl("STOP") }
        )
    }
}

@Composable
private fun TimerControlChip(
    iconResId: Int,
    text: String,
    iconColor: Color,
    scaleRatio: Float,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp * scaleRatio, vertical = 6.dp * scaleRatio),
        horizontalArrangement = Arrangement.spacedBy(6.dp * scaleRatio),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(16.dp * scaleRatio),
            tint = iconColor
        )
        Text(
            text = text,
            color = Color(0xFF1E293B),
            fontSize = 13.sp * scaleRatio,
            fontWeight = FontWeight.Bold
        )
    }
}
