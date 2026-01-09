package com.airobotcomm.tablet.ui.components.dialogue

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobotcomm.tablet.R
import com.airobotcomm.tablet.ui.components.robot.RobotVisualState

/**
 * AI对话气泡组件
 * 
 * Web原型对应: VoiceDialoguePanel.tsx
 * 
 * 功能:
 * - 显示AI回复气泡
 * - 思考中加载动画
 * - 打字机效果
 * - 关闭按钮
 * - 进度条动画（说话中）
 */
@Composable
fun DialogueBubble(
    robotState: RobotVisualState,
    aiMsg: String?,
    onAiSpeechComplete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val showBubble = aiMsg != null || robotState == RobotVisualState.THINKING
    
    AnimatedVisibility(
        visible = showBubble,
        enter = scaleIn(
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
        ) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A).copy(alpha = 0.95f), // slate-900
                            Color(0xFF1E293B).copy(alpha = 0.90f)  // slate-800
                        )
                    )
                )
        ) {
            Column {
                // 头部
                BubbleHeader(
                    robotState = robotState,
                    onClose = onClose
                )
                
                // 内容
                Box(
                    modifier = Modifier
                        .heightIn(min = 60.dp, max = 240.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(scrollState)
                ) {
                    when {
                        robotState == RobotVisualState.THINKING -> {
                            ThinkingIndicator()
                        }
                        aiMsg != null -> {
                            TypewriterText(
                                text = aiMsg,
                                onComplete = onAiSpeechComplete
                            )
                        }
                    }
                }
                
                // 底部进度条（说话中）
                AnimatedVisibility(
                    visible = robotState == RobotVisualState.SPEAKING,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SpeakingProgressBar()
                }
            }
        }
    }
}

/**
 * 气泡头部
 */
@Composable
private fun BubbleHeader(
    robotState: RobotVisualState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧 - 系统标识
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.cloud_on),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF22D3EE) // cyan-400
            )
            Text(
                text = "AETHER SYSTEM",
                color = Color(0xFF22D3EE),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 音量图标（说话中显示）
            AnimatedVisibility(
                visible = robotState == RobotVisualState.SPEAKING,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "volumePulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "volumeAlpha"
                )
                Icon(
                    painter = painterResource(id = R.drawable.volume_up),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = alpha)
                )
            }
            
            // 关闭按钮
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    modifier = Modifier.size(14.dp),
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * 思考中指示器
 */
@Composable
private fun ThinkingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    
    Row(
        modifier = modifier.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "dotScale$index"
            )
            
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "dotAlpha$index"
            )
            
            Box(
                modifier = Modifier
                    .size(10.dp * scale)
                    .clip(CircleShape)
                    .background(Color(0xFF22D3EE).copy(alpha = alpha))
                    .blur(1.dp)
            )
        }
    }
}

/**
 * 说话进度条
 */
@Composable
private fun SpeakingProgressBar(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "progressBar")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "progressOffset"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .fillMaxHeight()
                .offset(x = (offsetX * 160).dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF22D3EE), // cyan-400
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * 用户消息气泡
 */
@Composable
fun UserMessageBubble(
    message: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message.isNotBlank(),
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 4.dp
                    )
                )
                .background(Color(0xFF22D3EE).copy(alpha = 0.15f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = "\"$message\"",
                color = Color(0xFFA5F3FC).copy(alpha = 0.9f), // cyan-200
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                lineHeight = 20.sp
            )
        }
    }
}
