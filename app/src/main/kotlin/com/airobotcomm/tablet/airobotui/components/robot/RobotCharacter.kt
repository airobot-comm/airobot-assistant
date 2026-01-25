package com.airobotcomm.tablet.airobotui.components.robot

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobotcomm.tablet.airobotui.state.RobotVisualState

/**
 * 机器人角色主组件 - 增强版
 * 
 * 对应原型: IPCharacter.tsx
 */
@Composable
fun RobotCharacter(
    state: com.airobotcomm.tablet.airobotui.state.RobotVisualState,
    statusTip: String? = null,
    ttsProgressNormalized: Float = 0f,
    audioLevel: Float = 0f, // 传入音频等级 0-1
    headSize: Dp = 280.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "robotAnimation")
    
    // 悬浮动画 (Floating)
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    
    // 眨眼状态逻辑 (使用 Random 实现随机性)
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        while (true) {
            // 随机间隔 2s - 7s
            val delayTime = kotlin.random.Random.nextLong(2000, 7000)
            kotlinx.coroutines.delay(delayTime)
            
            if (state == RobotVisualState.IDLE || state == RobotVisualState.HAPPY || state == RobotVisualState.LISTENING) {
                isBlinking = true
                kotlinx.coroutines.delay(150)
                isBlinking = false
                
                // 偶尔双眨眼 (15% 概率)
                if (kotlin.random.Random.nextFloat() < 0.15f) {
                    kotlinx.coroutines.delay(100)
                    isBlinking = true
                    kotlinx.coroutines.delay(150)
                    isBlinking = false
                }
            }
        }
    }

    Box(
        modifier = modifier
            .width(headSize * 1.8f)
            .height(headSize * 1.6f),
        contentAlignment = Alignment.Center
    ) {
        // ... (背景环境光保持不变)
        Box(
            modifier = Modifier
                .size(headSize * 1.5f)
                .graphicsLayer { alpha = 0.15f }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF22D3EE), // cyan-400
                            Color.Transparent
                        )
                    )
                )
                .blur(80.dp)
        )
        
        // 主体结构
        Column(
            modifier = Modifier
                .offset(y = if (state == RobotVisualState.IDLE) floatOffset.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 状态提示气泡
            AnimatedVisibility(
                visible = statusTip != null && state == RobotVisualState.IDLE,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                StatusTipBubble(
                    tip = statusTip ?: "",
                    isFocusMode = state == RobotVisualState.FOCUS,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
            
            // 机器人头部
            RobotHead(
                state = state,
                isBlinking = isBlinking,
                ttsProgressNormalized = ttsProgressNormalized,
                audioLevel = audioLevel,
                headSize = headSize
            )
            
            // ... (地面阴影保持不变)
            Box(
                modifier = Modifier
                    .offset(y = (-10).dp)
                    .width(headSize * 0.7f)
                    .height(24.dp)
                    .graphicsLayer { 
                        alpha = if (state == RobotVisualState.IDLE) 0.3f else 0.5f
                        scaleX = if (state == RobotVisualState.IDLE) 0.8f + (floatOffset / 60f) else 1f
                    }
                    .clip(CircleShape)
                    .background(Color.Black)
                    .blur(15.dp)
            )
        }
    }
}

/**
 * 机器人头部组件
 */
@Composable
private fun RobotHead(
    state: com.airobotcomm.tablet.airobotui.state.RobotVisualState,
    isBlinking: Boolean,
    ttsProgressNormalized: Float,
    audioLevel: Float,
    headSize: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "headAnimation")
    
    Box(
        modifier = modifier
            .width(headSize)
            .height(headSize * 0.75f),
        contentAlignment = Alignment.Center
    ) {
        // 天线 (放置在头部后面)
        RobotAntennas(
            state = state,
            headSize = headSize,
            infiniteTransition = infiniteTransition
        )
        
        // ... (头部外壳保持不变)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(headSize * 0.25f),
                    ambientColor = Color.Black
                )
                .clip(RoundedCornerShape(headSize * 0.25f))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A), // slate-900
                            Color(0xFF020617)  // slate-950
                        )
                    )
                )
        )
        
        // 内部显示屏区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(headSize * 0.04f)
                .clip(RoundedCornerShape(headSize * 0.21f))
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            // 眼睛和嘴巴
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isBlinking) {
                    // 眨眼动画 (保持长椭圆)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(headSize * 0.2f)
                    ) {
                        BlinkingEye(size = headSize * 0.17f)
                        BlinkingEye(size = headSize * 0.17f)
                    }
                } else {
                    EnhancedDynamicEyes(
                        state = state,
                        ttsProgressNormalized = ttsProgressNormalized,
                        audioLevel = audioLevel,
                        eyeSize = headSize * 0.17f,
                        eyeGap = headSize * 0.2f
                    )
                }
                
                // ... (嘴巴动画保持不变)
                AnimatedVisibility(
                    visible = state == RobotVisualState.SPEAKING,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SpeakingMouth(
                        infiniteTransition = infiniteTransition,
                        modifier = Modifier.padding(top = headSize * 0.1f)
                    )
                }
            }
        }
    }
}

/**
 * 眨眼时的眼睛形状
 */
@Composable
private fun BlinkingEye(size: Dp) {
    Box(
        modifier = Modifier
            .width(size * 1.2f) // 稍微宽一点
            .height(size * 0.1f) // 很扁
            .clip(RoundedCornerShape(50)) // 胶囊形状
            .background(Color.White.copy(alpha = 0.8f))
    )
}

/**
 * 机器人天线 - 增强发光效果
 */
@Composable
private fun RobotAntennas(
    state: com.airobotcomm.tablet.airobotui.state.RobotVisualState,
    headSize: Dp,
    infiniteTransition: InfiniteTransition,
    modifier: Modifier = Modifier
) {
    // ... (旋转动画保持不变)
    val antennaRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "antennaRotation"
    )

    val isFocus = state == RobotVisualState.FOCUS
    val color1 = if (isFocus) Color(0xFFF87171) else Color(0xFF22D3EE) // cyan
    val color2 = if (isFocus) Color(0xFFF87171) else Color(0xFF818CF8) // indigo

    Row(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = (-headSize * 0.35f)), // 向上移动更多，因为天线变长了
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 左天线
        AntennaItem(color = color1, rotation = antennaRotation, headSize = headSize)
        // 右天线
        AntennaItem(color = color2, rotation = -antennaRotation, headSize = headSize)
    }
}

@Composable
private fun AntennaItem(color: Color, rotation: Float, headSize: Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { rotationZ = rotation }
    ) {
        // 灯头 + 发光 (保持不变)
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
                    .blur(10.dp)
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .shadow(elevation = 8.dp, shape = CircleShape, clip = false, spotColor = color)
            )
        }
        // 天线杆 - 变长
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(headSize * 0.35f) // 增加长度 (原 0.2f)
                .clip(RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                .background(Color(0xFF1E293B)) // slate-800
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
        initialValue = 0.5f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouthScale"
    )
    
    Box(
        modifier = modifier
            .width(32.dp * mouthScale)
            .height(8.dp)
            .clip(CircleShape)
            .background(Color.White)
            .blur(0.5.dp)
    )
}

/**
 * 状态提示气泡 (考考你的知识)
 */
@Composable
private fun StatusTipBubble(
    tip: String,
    isFocusMode: Boolean,
    modifier: Modifier = Modifier
) {
    val indicatorColor = if (isFocusMode) Color(0xFFF87171) else Color(0xFF22D3EE)
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
        Text(
            text = tip,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
