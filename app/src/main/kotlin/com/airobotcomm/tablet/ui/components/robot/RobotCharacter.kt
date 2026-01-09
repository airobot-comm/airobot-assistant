package com.airobotcomm.tablet.ui.components.robot

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 机器人角色主组件
 * 
 * Web原型对应: IPCharacter.tsx
 * 
 * 功能:
 * - 机器人头部外壳
 * - 双天线动画
 * - 动态眼睛
 * - 说话嘴巴动画
 * - 状态提示气泡
 * - 背景光晕
 * - 头部跟随/悬浮动画
 */
@Composable
fun RobotCharacter(
    state: RobotVisualState,
    statusTip: String? = null,
    headSize: Dp = 280.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "robotAnimation")
    
    // 悬浮动画（仅IDLE状态）
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    
    // 背景光晕脉冲
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == RobotVisualState.FOCUS) 0.8f else 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraScale"
    )
    
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = if (state == RobotVisualState.FOCUS) 0.05f else 0.1f,
        targetValue = if (state == RobotVisualState.FOCUS) 0.05f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraAlpha"
    )
    
    // 眨眼状态
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            if (state != RobotVisualState.LISTENING && 
                state != RobotVisualState.THINKING && 
                state != RobotVisualState.FOCUS) {
                isBlinking = true
                kotlinx.coroutines.delay(150)
                isBlinking = false
            }
        }
    }

    Box(
        modifier = modifier
            .width(headSize * 1.8f)
            .height(headSize * 1.5f),
        contentAlignment = Alignment.Center
    ) {
        // 背景光晕
        Box(
            modifier = Modifier
                .size(headSize * 1.5f)
                .scale(auraScale)
                .clip(CircleShape)
                .background(
                    Color(0xFF22D3EE).copy(alpha = auraAlpha) // cyan-500
                )
                .blur(100.dp)
        )
        
        // 主体结构（带悬浮动画）
        Column(
            modifier = Modifier
                .offset(y = if (state == RobotVisualState.IDLE) floatOffset.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 状态提示气泡
            AnimatedVisibility(
                visible = (state == RobotVisualState.IDLE || state == RobotVisualState.FOCUS) && statusTip != null,
                enter = scaleIn(
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f)
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                StatusTipBubble(
                    tip = statusTip ?: "",
                    isFocusMode = state == RobotVisualState.FOCUS,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // 机器人头部
            RobotHead(
                state = state,
                isBlinking = isBlinking,
                headSize = headSize
            )
            
            // 投影
            Box(
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .width(headSize * 0.7f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .blur(30.dp)
            )
        }
    }
}

/**
 * 机器人头部组件
 */
@Composable
private fun RobotHead(
    state: RobotVisualState,
    isBlinking: Boolean,
    headSize: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "headAnimation")
    
    Box(
        modifier = modifier
            .width(headSize)
            .height(headSize * 0.72f)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(headSize * 0.4f),
                ambientColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(headSize * 0.4f))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A).copy(alpha = 0.95f), // slate-900
                        Color(0xFF020617).copy(alpha = 0.95f)  // slate-950
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 天线
        RobotAntennas(
            state = state,
            headSize = headSize,
            infiniteTransition = infiniteTransition
        )
        
        // 内部深色区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(headSize * 0.35f))
                .background(Color.Black.copy(alpha = 0.5f))
        )
        
        // 眼睛和嘴巴
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 眼睛
            if (isBlinking && state == RobotVisualState.IDLE) {
                // 眨眼状态
                Row(
                    horizontalArrangement = Arrangement.spacedBy(headSize * 0.2f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BlinkingEye(size = headSize * 0.17f)
                    BlinkingEye(size = headSize * 0.17f)
                }
            } else {
                DynamicEyes(
                    state = state,
                    eyeSize = headSize * 0.17f,
                    eyeGap = headSize * 0.2f
                )
            }
            
            // 说话嘴巴（仅SPEAKING状态）
            AnimatedVisibility(
                visible = state == RobotVisualState.SPEAKING,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                SpeakingMouth(
                    infiniteTransition = infiniteTransition,
                    modifier = Modifier.padding(top = headSize * 0.12f)
                )
            }
        }
    }
}

/**
 * 机器人天线
 */
@Composable
private fun RobotAntennas(
    state: RobotVisualState,
    headSize: Dp,
    infiniteTransition: InfiniteTransition,
    modifier: Modifier = Modifier
) {
    // 天线动画
    val antennaRotation by infiniteTransition.animateFloat(
        initialValue = when (state) {
            RobotVisualState.IDLE -> -8f
            RobotVisualState.THINKING -> 0f
            else -> 0f
        },
        targetValue = when (state) {
            RobotVisualState.IDLE -> 8f
            RobotVisualState.THINKING -> 360f
            else -> 0f
        },
        animationSpec = when (state) {
            RobotVisualState.IDLE -> infiniteRepeatable(
                animation = tween(5000, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            )
            RobotVisualState.THINKING -> infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing)
            )
            else -> infiniteRepeatable(
                animation = tween(500, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            )
        },
        label = "antennaRotation"
    )
    
    val antennaScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == RobotVisualState.LISTENING) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "antennaScale"
    )
    
    val antennaOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (state == RobotVisualState.SPEAKING) -5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "antennaOffsetY"
    )
    
    val isFocusMode = state == RobotVisualState.FOCUS
    val antennaLightColor = if (isFocusMode) Color(0xFFF87171) else Color(0xFF22D3EE) // red-400 or cyan-400
    val antennaLightColor2 = if (isFocusMode) Color(0xFFF87171) else Color(0xFF6366F1) // red-400 or indigo-500
    
    // 左天线
    Box(
        modifier = Modifier
            .offset(x = (-headSize * 0.25f), y = (-headSize * 0.22f))
            .graphicsLayer {
                rotationZ = antennaRotation
                scaleX = antennaScale
                scaleY = antennaScale
                translationY = antennaOffsetY
            }
    ) {
        // 天线杆
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(headSize * 0.22f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E293B)) // slate-800
        )
        // 天线灯
        Box(
            modifier = Modifier
                .offset(x = (-8).dp, y = (-16).dp)
                .size(28.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = antennaLightColor.copy(alpha = 0.5f)
                )
                .clip(CircleShape)
                .background(antennaLightColor)
        )
    }
    
    // 右天线
    Box(
        modifier = Modifier
            .offset(x = (headSize * 0.25f), y = (-headSize * 0.22f))
            .graphicsLayer {
                rotationZ = -antennaRotation
                scaleX = antennaScale
                scaleY = antennaScale
                translationY = antennaOffsetY
            }
    ) {
        // 天线杆
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(headSize * 0.22f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1E293B)) // slate-800
        )
        // 天线灯
        Box(
            modifier = Modifier
                .offset(x = (-8).dp, y = (-16).dp)
                .size(28.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = antennaLightColor2.copy(alpha = 0.5f)
                )
                .clip(CircleShape)
                .background(antennaLightColor2)
        )
    }
}

/**
 * 说话嘴巴动画
 */
@Composable
private fun SpeakingMouth(
    infiniteTransition: InfiniteTransition,
    modifier: Modifier = Modifier
) {
    val mouthScale by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouthScale"
    )
    
    Box(
        modifier = modifier
            .width(48.dp * mouthScale)
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .blur(1.dp)
    )
}

/**
 * 状态提示气泡
 */
@Composable
private fun StatusTipBubble(
    tip: String,
    isFocusMode: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tipPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    val indicatorColor = if (isFocusMode) Color(0xFFF87171) else Color(0xFF22D3EE) // red-400 or cyan-400
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 脉冲指示灯
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(indicatorColor.copy(alpha = pulseAlpha))
        )
        
        Text(
            text = tip,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
