package com.airobot.tablet.airobotui.comp.robot

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airobot.tablet.airobotui.state.RobotVisualState
import com.airobot.tablet.framework.theme.RobotAntennaStemDark
import com.airobot.tablet.framework.theme.RobotAntennaStemLight
import com.airobot.tablet.framework.theme.RobotBlush
import com.airobot.tablet.framework.theme.RobotCollarColor
import com.airobot.tablet.framework.theme.RobotEyeDefault
import com.airobot.tablet.framework.theme.RobotFaceColor
import com.airobot.tablet.framework.theme.RobotHeadBorder
import com.airobot.tablet.framework.theme.RobotHeadColor
import com.airobot.tablet.framework.theme.RobotNeckColor
import com.airobot.tablet.framework.theme.RobotTheme


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
            .width(headSize * 2.0f) 
            .height(headSize * 1.8f),
        contentAlignment = Alignment.Center
    ) {
        // 增强型背景环境光 (双层光晕 + 呼吸感)
        val auraColor = RobotTheme.colors.robotAuraStart
        val auraAlpha = if (RobotTheme.isDark) 0.55f else 0.7f // 提高不透明度
        
        val auraScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "auraScale"
        )

        // 核心内层光
        Box(
            modifier = Modifier
                .size(headSize * 1.5f)
                .graphicsLayer { 
                    alpha = auraAlpha
                    scaleX = auraScale
                    scaleY = auraScale
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            auraColor,
                            Color.Transparent
                        )
                    )
                )
                .blur(60.dp)
        )
        
        // 外层大光晕 (扩散感)
        Box(
            modifier = Modifier
                .size(headSize * 2.1f)
                .graphicsLayer { 
                    alpha = auraAlpha * 0.5f
                    scaleX = auraScale * 1.3f
                    scaleY = auraScale * 1.3f
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            auraColor.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
                .blur(100.dp)
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
            
            // 地面阴影 - 跟随浮动轻微缩放
            Box(
                modifier = Modifier
                    .offset(y = (-10).dp)
                    .width(headSize * 0.6f)
                    .height(20.dp)
                    .graphicsLayer { 
                        alpha = if (state == RobotVisualState.IDLE) 0.2f else 0.4f
                        scaleX = if (state == RobotVisualState.IDLE) 0.85f + (floatOffset / 120f) else 1f
                    }
                    .clip(CircleShape)
                    .background(Color.Black)
                    .blur(20.dp)
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
            .width(headSize * 1.1f) // 扩宽容器，容纳突出的耳朵
            .height(headSize * 0.74f),
        contentAlignment = Alignment.Center
    ) {
        // 天线 (放置在头部后面)
        RobotAntennas(
            state = state,
            headSize = headSize,
            infiniteTransition = infiniteTransition
        )
        
        // 耳朵 (移出头部裁剪 Box，并添加 3D 轮廓层)
        Box(
            modifier = Modifier.width(headSize * 1.08f),
            contentAlignment = Alignment.Center
        ) {
            // 左耳 (两层叠加实现突起的边框感)
            Box(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                // 外边框/背板
                Box(
                    modifier = Modifier
                        .size(width = headSize * 0.08f, height = headSize * 0.19f)
                        .clip(RoundedCornerShape(topStart = headSize * 0.1f, bottomStart = headSize * 0.1f))
                        .background(RobotHeadBorder.copy(alpha = 0.4f))
                        .blur(0.5.dp)
                )
                // 内主体
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp, top = 2.dp, bottom = 2.dp)
                        .size(width = headSize * 0.065f, height = headSize * 0.165f)
                        .clip(RoundedCornerShape(topStart = headSize * 0.08f, bottomStart = headSize * 0.08f))
                        .background(RobotHeadColor)
                )
            }
            
            // 右耳
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                // 外边框/背板
                Box(
                    modifier = Modifier
                        .size(width = headSize * 0.08f, height = headSize * 0.19f)
                        .clip(RoundedCornerShape(topEnd = headSize * 0.1f, bottomEnd = headSize * 0.1f))
                        .background(RobotHeadBorder.copy(alpha = 0.4f))
                        .blur(0.5.dp)
                )
                // 内主体
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp, top = 2.dp, bottom = 2.dp)
                        .size(width = headSize * 0.065f, height = headSize * 0.165f)
                        .clip(RoundedCornerShape(topEnd = headSize * 0.08f, bottomEnd = headSize * 0.08f))
                        .background(RobotHeadColor)
                )
            }
        }
        
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

            // 高光 (Glossy effect)
            Box(
                modifier = Modifier
                    .padding(top = headSize * 0.05f, start = headSize * 0.15f)
                    .size(width = headSize * 0.3f, height = headSize * 0.08f)
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
                    Box(modifier = Modifier.size(headSize * 0.08f, headSize * 0.04f).blur(6.dp).background(
                        RobotBlush.copy(alpha = 0.35f), CircleShape))
                    Box(modifier = Modifier.size(headSize * 0.08f, headSize * 0.04f).blur(6.dp).background(
                        RobotBlush.copy(alpha = 0.35f), CircleShape))
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
                val isSpeaking = state == com.airobot.tablet.airobotui.state.RobotVisualState.SPEAKING
                val isIdle = state == com.airobot.tablet.airobotui.state.RobotVisualState.IDLE || state == com.airobot.tablet.airobotui.state.RobotVisualState.LISTENING

                Box(modifier = Modifier.padding(top = headSize * 0.1f)) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = state,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "mouthTransition"
                    ) { targetState ->
                        when (targetState) {
                            com.airobot.tablet.airobotui.state.RobotVisualState.SPEAKING -> {
                                SpeakingMouth(
                                    infiniteTransition = infiniteTransition,
                                    modifier = Modifier
                                )
                            }
                            com.airobot.tablet.airobotui.state.RobotVisualState.IDLE, 
                            com.airobot.tablet.airobotui.state.RobotVisualState.LISTENING -> {
                                StaticMouth(size = 32.dp)
                            }
                            else -> {
                                // Other states might hide mouth or use static
                                Spacer(modifier = Modifier.size(1.dp))
                            }
                        }
                    }
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
    val antennaColor1 = if (isFocus) Color(0xFFF87171) else Color(0xFF7DD3FC) // sky-300 for neutral
    val antennaColor2 = if (isFocus) Color(0xFFF87171) else Color(0xFF7DD3FC) // same for neutral as per prototype

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
                .width(headSize * 0.025f) // 约 8-10dp
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
        initialValue = 0.8f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouthScale"
    )
    
    Box(
        modifier = modifier
            .width(32.dp * mouthScale)
            .height(10.dp) // 稍微加厚一点
            .clip(CircleShape)
            .background(Color(0xFF334155)) // 改为深灰色，匹配原型风格 (slate-700)
            .blur(0.3.dp)
    )
}

/**
 * 静态嘴巴 (IDLE 状态)
 */
@Composable
private fun StaticMouth(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(size)
            .height(5.dp)
            .clip(CircleShape)
            .background(Color(0xFF334155).copy(alpha = 0.8f)) // 深灰色横线
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
        modifier = modifier.offset(y = (-15).dp), // 向上稍微缩进，显得脖子更短更稳固
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 脖子立管 - 配合新颜色
        Box(
            modifier = Modifier
                .width(headSize * 0.14f)
                .height(headSize * 0.08f) // 缩短
                .background(RobotNeckColor)
        )
        // 领子/底座 - 极高圆角模仿原型粘土感
        Box(
            modifier = Modifier
                .width(headSize * 0.55f) // 加宽
                .height(headSize * 0.14f)
                .clip(RoundedCornerShape(topStart = 100.dp, topEnd = 100.dp)) // 完美半圆顶
                .background(RobotCollarColor)
        )
    }
}


