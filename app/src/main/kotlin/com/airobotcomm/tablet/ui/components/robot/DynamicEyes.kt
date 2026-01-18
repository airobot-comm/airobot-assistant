package com.airobotcomm.tablet.ui.components.robot

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 动态眼睛组件 - 根据机器人状态展示不同的眼睛动画
 * 
 * Web原型对应: IPCharacter.tsx 中的 getEyes() 函数
 * 
 * 动画效果:
 * - IDLE: 渐变圆形眼睛 + 高光追踪
 * - LISTENING: 高度脉冲动画 (竖线形)
 * - THINKING: 旋转加载环
 * - SPEAKING: 缩放脉冲
 * - FOCUS: 扁平禅意眼睛
 */
@Composable
fun DynamicEyes(
    state: com.airobotcomm.tablet.ui.state.RobotVisualState,
    highlightOffset: Offset = Offset.Zero,
    eyeSize: Dp = 48.dp,
    eyeGap: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(eyeGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DynamicEye(
            state = state,
            highlightOffset = highlightOffset,
            size = eyeSize
        )
        DynamicEye(
            state = state,
            highlightOffset = highlightOffset,
            size = eyeSize
        )
    }
}

@Composable
private fun DynamicEye(
    state: com.airobotcomm.tablet.ui.state.RobotVisualState,
    highlightOffset: Offset,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eyeAnimation")
    
    when (state) {
        _root_ide_package_.com.airobotcomm.tablet.ui.state.RobotVisualState.IDLE -> IdleEye(
            highlightOffset = highlightOffset,
            size = size,
            modifier = modifier
        )
        _root_ide_package_.com.airobotcomm.tablet.ui.state.RobotVisualState.LISTENING -> ListeningEye(
            infiniteTransition = infiniteTransition,
            size = size,
            modifier = modifier
        )
        _root_ide_package_.com.airobotcomm.tablet.ui.state.RobotVisualState.THINKING -> ThinkingEye(
            infiniteTransition = infiniteTransition,
            size = size,
            modifier = modifier
        )
        _root_ide_package_.com.airobotcomm.tablet.ui.state.RobotVisualState.SPEAKING -> SpeakingEye(
            infiniteTransition = infiniteTransition,
            size = size,
            modifier = modifier
        )
        _root_ide_package_.com.airobotcomm.tablet.ui.state.RobotVisualState.FOCUS -> FocusEye(
            size = size,
            modifier = modifier
        )
        _root_ide_package_.com.airobotcomm.tablet.ui.state.RobotVisualState.HAPPY -> HappyEye(
            size = size,
            modifier = modifier
        )
        _root_ide_package_.com.airobotcomm.tablet.ui.state.RobotVisualState.SLEEPING -> SleepingEye(
            size = size,
            modifier = modifier
        )
    }
}

/**
 * 空闲状态眼睛 - 渐变圆形 + 高光
 */
@Composable
private fun IdleEye(
    highlightOffset: Offset,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6366F1), // indigo-500
                        Color(0xFF1D4ED8)  // blue-700
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 高光 - 跟随偏移
        Box(
            modifier = Modifier
                .size(size * 0.35f)
                .offset(
                    x = (highlightOffset.x * 2).dp,
                    y = (highlightOffset.y * 2).dp
                )
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .blur(1.dp)
        )
        
        // 渐变覆盖层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * 聆听状态眼睛 - 高度脉冲动画
 */
@Composable
private fun ListeningEye(
    infiniteTransition: InfiniteTransition,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val height by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "listeningHeight"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "listeningScale"
    )
    
    Box(
        modifier = modifier
            .width(size)
            .height(size * height * scale)
            .clip(CircleShape)
            .background(Color.White)
            .blur(1.dp)
    )
}

/**
 * 思考状态眼睛 - 旋转加载环
 */
@Composable
private fun ThinkingEye(
    infiniteTransition: InfiniteTransition,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "thinkingRotation"
    )
    
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = size.toPx() * 0.15f
        val radius = (size.toPx() - strokeWidth) / 2
        
        rotate(rotation) {
            // 背景圆环
            drawCircle(
                color = Color(0xFF22D3EE).copy(alpha = 0.3f), // cyan-400
                radius = radius,
                style = Stroke(width = strokeWidth)
            )
            
            // 旋转的弧形
            drawArc(
                color = Color(0xFF22D3EE), // cyan-400
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(radius * 2, radius * 2)
            )
        }
    }
}

/**
 * 说话状态眼睛 - 缩放脉冲
 */
@Composable
private fun SpeakingEye(
    infiniteTransition: InfiniteTransition,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speakingScale"
    )
    
    Box(
        modifier = modifier
            .size(size * scale)
            .clip(CircleShape)
            .background(Color.White)
            .blur(2.dp)
    )
}

/**
 * 专注状态眼睛 - 扁平禅意眼睛
 */
@Composable
private fun FocusEye(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(size * 1.1f)
            .height(size * 0.15f)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFA5F3FC)) // cyan-200
            .blur(0.5.dp)
    )
}

/**
 * 开心状态眼睛 - 弯弯笑眼
 */
@Composable
private fun HappyEye(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = size.toPx() * 0.2f
        
        // 画弯弯的眼睛（上弧）
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            topLeft = Offset(strokeWidth / 2, size.toPx() * 0.3f),
            size = Size(size.toPx() - strokeWidth, size.toPx() * 0.5f)
        )
    }
}

/**
 * 睡眠状态眼睛 - 闭眼横线
 */
@Composable
private fun SleepingEye(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(size)
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF22D3EE).copy(alpha = 0.5f)) // cyan-400
    )
}

/**
 * 眨眼效果眼睛
 */
@Composable
fun BlinkingEye(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(size)
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF22D3EE)) // cyan-300
            .blur(0.5.dp)
    )
}
