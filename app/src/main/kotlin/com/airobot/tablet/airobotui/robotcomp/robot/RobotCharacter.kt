package com.airobot.tablet.airobotui.robotcomp.robot

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.tablet.airobotui.framework.theme.*
import com.airobot.tablet.airobotui.state.RobotVisualState


/**
 * 机器人角色主组件 - 增强版
 * 
 * 对应原型: IPCharacter.tsx
 */
@Composable
fun RobotCharacter(
    state: com.airobot.tablet.airobotui.state.RobotVisualState,
    ttsProgressNormalized: Float = 0f,
    audioLevel: () -> Float = { 0f }, // 传入音频等级 (Lambda)
    headSize: Dp = 320.dp, // 增大默认尺寸以匹配 420px 比例
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "robotAnimation")
    
    // 悬浮动画 (Floating) - 范围加大，更生动
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 18f, // 12 -> 18
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutQuad),
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
        // 背景环境光 - 使用主题色
        val auraColor = RobotTheme.colors.robotAuraStart
        val auraAlpha = if (RobotTheme.isDark) 0.35f else 0.5f
        Box(
            modifier = Modifier
                .size(headSize * 1.6f) // 稍微大一点
                .graphicsLayer { 
                    alpha = auraAlpha
                    scaleX = 1.15f
                    scaleY = 1.15f
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            auraColor,
                            Color.Transparent
                        )
                    )
                )
                .blur(90.dp)
        )
        
        // 主体结构
        Column(
            modifier = Modifier
                .offset(y = if (state == RobotVisualState.IDLE) floatOffset.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 状态提示气泡已移除，迁移到功能卡片组件
            
            // 机器人头部
            RobotHead(
                state = state,
                isBlinking = isBlinking,
                ttsProgressNormalized = ttsProgressNormalized,
                audioLevel = audioLevel,
                headSize = headSize
            )
            
            // 地面阴影
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
            
            // 底部脖子和领子
            RobotNeck(headSize = headSize)
        }
    }
}

/**
 * 机器人头部组件
 */
@Composable
private fun RobotHead(
    state: com.airobot.tablet.airobotui.state.RobotVisualState,
    isBlinking: Boolean,
    ttsProgressNormalized: Float,
    audioLevel: () -> Float,
    headSize: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "headAnimation")
    
    Box(
        modifier = modifier
            .width(headSize)
            .height(headSize * 0.74f), // 310/420 approx 0.74
        contentAlignment = Alignment.Center
    ) {
        // 天线 (放置在头部后面)
        RobotAntennas(
            state = state,
            headSize = headSize,
            infiniteTransition = infiniteTransition
        )
        
        // 头部外壳 - 改为浅蓝色 (sky-200)
        Box(
            modifier = Modifier
                .width(headSize)
                .height(headSize * 0.74f)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(headSize * 0.28f),
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                    spotColor = RobotTheme.colors.robotAuraStart.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(headSize * 0.28f))
                .background(RobotHeadColor) // 固定浅蓝色
        ) {
            // 耳朵 (Anthropomorphic touch) - 改为全圆角
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // 左耳
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-headSize * 0.04f))
                        .size(width = headSize * 0.08f, height = headSize * 0.22f)
                        .clip(RoundedCornerShape(topStart = 100.dp, bottomStart = 100.dp))
                        .background(RobotHeadColor)
                        .border(
                            width = 1.dp, 
                            color = Color(0xFF38BDF8).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(topStart = 100.dp, bottomStart = 100.dp)
                        )
                )
                // 右耳
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (headSize * 0.04f))
                        .size(width = headSize * 0.08f, height = headSize * 0.22f)
                        .clip(RoundedCornerShape(topEnd = 100.dp, bottomEnd = 100.dp))
                        .background(RobotHeadColor)
                        .border(
                            width = 1.dp, 
                            color = Color(0xFF38BDF8).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(topEnd = 100.dp, bottomEnd = 100.dp)
                        )
                )
            }

            // 高光 (Glossy effect)
            Box(
                modifier = Modifier
                    .padding(top = 16.dp, start = 40.dp)
                    .size(width = headSize * 0.25f, height = headSize * 0.08f)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
                    .blur(2.dp)
            )

            // 内部显示屏区域 (Inset Face)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(headSize * 0.085f) // 匹配 inset-9 比例
                    .clip(RoundedCornerShape(headSize * 0.24f))
                    .background(RobotFaceColor) // 固定浅色面部
                    .border(
                        width = 1.dp,
                        color = Color(0xFF7DD3FC).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(headSize * 0.24f)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = headSize * 0.12f)
                        .offset(y = headSize * 0.1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(headSize * 0.08f, headSize * 0.04f).blur(6.dp).background(RobotBlush.copy(alpha = 0.35f), CircleShape))
                    Box(modifier = Modifier.size(headSize * 0.08f, headSize * 0.04f).blur(6.dp).background(RobotBlush.copy(alpha = 0.35f), CircleShape))
                }

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
                    DynamicEyes(
                        state = state,
                        ttsProgressNormalized = ttsProgressNormalized,
                        audioLevel = audioLevel,
                        eyeSize = headSize * 0.17f,
                        eyeGap = headSize * 0.2f
                    )
                
                // 嘴巴动画
                androidx.compose.animation.AnimatedVisibility(
                    visible = state == com.airobot.tablet.airobotui.state.RobotVisualState.SPEAKING,
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
    }
}

/**
 * 机器人天线 - 增强发光效果
 */
@Composable
private fun RobotAntennas(
    state: com.airobot.tablet.airobotui.state.RobotVisualState,
    headSize: Dp,
    infiniteTransition: InfiniteTransition,
    modifier: Modifier = Modifier
) {
    // 旋转动画 - 加大角度
    val antennaRotation by infiniteTransition.animateFloat(
        initialValue = -12f, // -5 -> -12
        targetValue = 12f,   // 5 -> 12
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine), // 3000 -> 2000 speed up
            repeatMode = RepeatMode.Reverse
        ),
        label = "antennaRotation"
    )

    val isFocus = state == com.airobot.tablet.airobotui.state.RobotVisualState.FOCUS
    val antennaColor1 = if (isFocus) Color(0xFFF87171) else Color(0xFF22D3EE) // cyan
    val antennaColor2 = if (isFocus) Color(0xFFF87171) else Color(0xFF818CF8) // indigo

    Row(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = (-headSize * 0.35f)), // 向上移动更多，因为天线变长了
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 左天线
        AntennaItem(color = antennaColor1, rotation = antennaRotation, headSize = headSize)
        // 右天线
        AntennaItem(color = antennaColor2, rotation = -antennaRotation, headSize = headSize)
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
        // 天线杆 - 渐变灰色
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(headSize * 0.35f)
                .clip(RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(RobotAntennaStemLight, RobotAntennaStemDark)
                    )
                )
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

// StatusTipBubble已删除，迁移到功能卡片组件

/**
 * 眨眼时的眼睛形状
 */
@Composable
private fun BlinkingEye(size: Dp) {
    Box(
        modifier = Modifier
            .width(size * 1.2f)
            .height(size * 0.15f)
            .clip(RoundedCornerShape(50))
            .background(RobotEyeDefault.copy(alpha = 0.7f)) // 使用固定的眼睛深色
    )
}

/**
 * 脖子和领子组件
 */
@Composable
private fun RobotNeck(
    headSize: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.offset(y = (-8).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 脖子立管
        Box(
            modifier = Modifier
                .width(headSize * 0.15f)
                .height(headSize * 0.1f)
                .background(RobotNeckColor)
        )
        // 领子/底座
        Box(
            modifier = Modifier
                .width(headSize * 0.45f)
                .height(headSize * 0.12f)
                .clip(RoundedCornerShape(topStart = headSize * 0.1f, topEnd = headSize * 0.1f))
                .background(RobotCollarColor)
                .shadow(4.dp, RoundedCornerShape(topStart = headSize * 0.1f, topEnd = headSize * 0.1f))
        )
    }
}


