package com.airobotcomm.tablet.airobotui.robotcomp.dialogue

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobotcomm.tablet.R
import com.airobotcomm.tablet.airobotui.state.RobotVisualState

/**
 * AI对话气泡组件 - 增强设计版
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
    val showBubble = robotState == RobotVisualState.THINKING || 
                       (robotState == RobotVisualState.SPEAKING && aiMsg != null)
    
    AnimatedVisibility(
        visible = showBubble,
        enter = scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 左侧尖角 (Pointer)
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 24.dp)
                    .clip(BubblePointerShape())
                    .background(Color(0xFF0F172A).copy(alpha = 0.95f))
            )

            // 气泡主体
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color.Black.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A).copy(alpha = 0.92f), // slate-900
                                Color(0xFF1E293B).copy(alpha = 0.88f)  // slate-800
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(24.dp)
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
                            .heightIn(min = 80.dp, max = 300.dp)
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
                                    speed = 60L,
                                    onComplete = onAiSpeechComplete
                                )
                            }
                        }
                    }
                    
                    // 底部进度条 (Speaking)
                    if (robotState == RobotVisualState.SPEAKING) {
                        SpeakingProgressBar()
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
            .background(Color.White.copy(alpha = 0.05f))
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
                tint = Color(0xFF22D3EE)
            )
            Text(
                text = "AETHER SYSTEM",
                color = Color(0xFF22D3EE),
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
                    tint = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    modifier = Modifier.size(14.dp),
                    tint = Color.White.copy(alpha = 0.4f)
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
                    .background(Color(0xFF22D3EE).copy(alpha = alpha))
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
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .fillMaxHeight()
                .offset(x = (progress * 340).dp)
                .background(Color(0xFF22D3EE))
        )
    }
}
