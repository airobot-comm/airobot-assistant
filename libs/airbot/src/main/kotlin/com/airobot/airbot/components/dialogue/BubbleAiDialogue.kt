package com.airobot.airbot.components.dialogue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
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
 * AI对话气泡组件 - 增强设计版，单独的ai对话气泡
 */
@Composable
fun BubbleAiDialogue(
    robotState: RobotVisualState,
    aiMsg: String?,
    onAiSpeechComplete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    bubbleMaxWidth: androidx.compose.ui.unit.Dp = 360.dp
) {
    val scrollState = rememberScrollState()
    val showBubble = robotState == RobotVisualState.THINKING ||
        (robotState == RobotVisualState.SPEAKING && aiMsg != null)

    AnimatedVisibility(
        visible = showBubble,
        enter = scaleIn(transformOrigin = TransformOrigin(0f, 0.5f)) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 左侧尖角 (Pointer)
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 24.dp)
                    .clip(BubblePointerShape())
                    .background(RobotTheme.colors.cardBg.copy(alpha = 0.95f))
            )

            // 气泡主体
            Box(
                modifier = Modifier
                    .width(bubbleMaxWidth)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = Color.Black.copy(alpha = 0.1f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        if (RobotTheme.isDark) {
                            Color.White.copy(alpha = 0.08f)
                        } else {
                            Color.White
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (RobotTheme.isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(
                            alpha = 0.05f
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Column {
                    // 内容
                    Box(
                        modifier = Modifier
                            .heightIn(min = 60.dp, max = 300.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .verticalScroll(scrollState)
                    ) {
                        LaunchedEffect(aiMsg, robotState) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }

                        when {
                            robotState == RobotVisualState.THINKING -> {
                                ThinkingIndicator()
                            }

                            aiMsg != null -> {
                                TypewriterText(
                                    text = aiMsg,
                                    speed = 40L, // 稍微快一点
                                    onComplete = onAiSpeechComplete
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 自定义气泡尖角形状
 */
private fun BubblePointerShape() = GenericShape { size, _ ->
    moveTo(size.width, 0f)
    lineTo(0f, size.height / 2f)
    lineTo(size.width, size.height)
    close()
}

@Composable
private fun BubbleHeader(
    robotState: RobotVisualState,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RobotTheme.colors.surfaceOverlay.copy(alpha = 0.05f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.cloud_on),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = RobotTheme.colors.accent
            )
            Text(
                text = stringResource(AirbotR.string.aether_system_label),
                color = RobotTheme.colors.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (robotState == RobotVisualState.SPEAKING) {
                Icon(
                    painter = painterResource(id = R.drawable.volume_up),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = RobotTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(AirbotR.string.common_close),
                    modifier = Modifier.size(14.dp),
                    tint = RobotTheme.colors.textMuted
                )
            }
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    Row(
        modifier = Modifier.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "dot"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(RobotTheme.colors.accent.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun SpeakingProgressBar() {
    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "line"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(RobotTheme.colors.surfaceOverlay.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .fillMaxHeight()
                .offset(x = (progress * 340).dp)
                .background(RobotTheme.colors.accent)
        )
    }
}
