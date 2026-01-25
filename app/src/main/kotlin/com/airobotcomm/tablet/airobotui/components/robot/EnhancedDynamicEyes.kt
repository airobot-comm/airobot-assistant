package com.airobotcomm.tablet.airobotui.components.robot

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 增强的动态眼睛组件 - 支持微表情同步
 *
 * Web原型对应: IPCharacter.tsx 中的 getEyes() 函数
 */
@Composable
fun EnhancedDynamicEyes(
    state: com.airobotcomm.tablet.airobotui.state.RobotVisualState,
    ttsProgressNormalized: Float = 0f, // 0-1, TTS播放进度
    audioLevel: Float = 0f, // 传入音频等级 0-1
    eyeSize: Dp = 48.dp,
    eyeGap: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "enhancedEyeAnimation")

    // 说话时的眼睛转动偏移（缓慢左右移动）
    val speakingEyeLookX by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speakingEyeLook"
    )

    // 思考时的眼睛飘移（更大幅度的上下左右移动）
    val thinkingEyeOffsetX by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinkingEyeOffsetX"
    )

    val thinkingEyeOffsetY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinkingEyeOffsetY"
    )

    // 计算眼睛的实际偏移
    val eyeOffsetX = when (state) {
        _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.SPEAKING -> speakingEyeLookX.dp
        _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.THINKING -> thinkingEyeOffsetX.dp
        else -> 0.dp
    }

    val eyeOffsetY = when (state) {
        _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.THINKING -> thinkingEyeOffsetY.dp
        else -> 0.dp
    }

    Row(
        modifier = modifier
            .offset(x = eyeOffsetX, y = eyeOffsetY),
        horizontalArrangement = Arrangement.spacedBy(eyeGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EnhancedDynamicEye(
            state = state,
            size = eyeSize,
            ttsProgressNormalized = ttsProgressNormalized,
            audioLevel = audioLevel
        )
        EnhancedDynamicEye(
            state = state,
            size = eyeSize,
            ttsProgressNormalized = ttsProgressNormalized,
            audioLevel = audioLevel
        )
    }
}

/**
 * 单个眼睛组件 - 增强版 (带有发光效果)
 */
@Composable
private fun EnhancedDynamicEye(
    state: com.airobotcomm.tablet.airobotui.state.RobotVisualState,
    size: Dp,
    ttsProgressNormalized: Float = 0f,
    audioLevel: Float = 0f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 外部发光层 (Bloom Effect) - 改为长椭圆
        Box(
            modifier = Modifier
                .size(width = size * 1.5f, height = size * 1.2f)
                .clip(RoundedCornerShape(size))
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            getEyeColor(state).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .blur(size * 0.25f)
        )

        // 核心眼睛组件
        when (state) {
            _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.IDLE -> IdleEyeEnhanced(size = size)
            _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.LISTENING -> ListeningEyeEnhanced(size = size, audioLevel = audioLevel)
            _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.THINKING -> ThinkingEyeEnhanced(size = size)
            _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.SPEAKING -> SpeakingEyeEnhanced(
                size = size,
                ttsProgressNormalized = ttsProgressNormalized,
                audioLevel = audioLevel
            )
            _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.FOCUS -> FocusEyeEnhanced(size = size)
            _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.HAPPY -> HappyEyeEnhanced(size = size)
            _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.SLEEPING -> SleepingEyeEnhanced(size = size)
        }
    }
}

private fun getEyeColor(state: com.airobotcomm.tablet.airobotui.state.RobotVisualState): Color {
    return when (state) {
        _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.IDLE -> Color(0xFF6366F1)
        _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.LISTENING -> Color.White
        _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.THINKING -> Color(0xFF22D3EE)
        _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.SPEAKING -> Color.White
        _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.FOCUS -> Color(0xFF67E8F9)
        _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.HAPPY -> Color(0xFF10B981)
        _root_ide_package_.com.airobotcomm.tablet.airobotui.state.RobotVisualState.SLEEPING -> Color(0xFF94A3B8)
    }
}

/**
 * IDLE 状态眼睛 - 椭圆形 + 高光
 */
@Composable
private fun IdleEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(size)
            .height(size * 0.85f)
            .clip(RoundedCornerShape(50))
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
        // 高光 - 也要变椭圆
        Box(
            modifier = Modifier
                .size(width = size * 0.45f, height = size * 0.35f)
                .offset(x = (size * 0.1f), y = (-size * 0.1f))
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.9f))
                .blur(1.dp)
        )
    }
}

/**
 * LISTENING 状态眼睛 - 音频感应高度
 */
@Composable
private fun ListeningEyeEnhanced(
    size: Dp,
    audioLevel: Float, // 增加音频等级调制
    modifier: Modifier = Modifier
) {
    // 动态计算高度，根据音量在 0.4 到 1.2 之间波动
    val dynamicHeight = size * (0.4f + audioLevel * 1.0f)
    
    Box(
        modifier = modifier
            .width(size * 0.9f)
            .height(dynamicHeight)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.95f))
            .blur(0.5.dp)
    )
}

/**
 * THINKING 状态眼睛 - 旋转加载环
 */
@Composable
private fun ThinkingEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinkingEye")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thinkingRotation"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
        contentAlignment = Alignment.Center
    ) {
        // 旋转外圈 (椭圆路径感)
        Box(
            modifier = Modifier
                .width(size)
                .height(size * 0.8f)
                .clip(RoundedCornerShape(50))
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF22D3EE),     // cyan-400
                            Color(0xFF22D3EE).copy(alpha = 0f)
                        )
                    )
                )
        )
    }
}

/**
 * SPEAKING 状态眼睛 - 缩放脉冲 + 音频调制
 */
@Composable
private fun SpeakingEyeEnhanced(
    size: Dp,
    ttsProgressNormalized: Float = 0f,
    audioLevel: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "speakingEye")

    // 基础脉冲 (减弱，更多靠音频调制)
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speakingPulse"
    )

    // 音频等级调制缩放
    val dynamicScale = idlePulse + (audioLevel * 0.4f)
    
    // TTS进度可以额外调制亮度
    val brightness = 0.85f + (ttsProgressNormalized * 0.15f)

    Box(
        modifier = modifier
            .width(size * dynamicScale)
            .height(size * dynamicScale * 0.8f)
            .clip(RoundedCornerShape(50))
            .background(
                Color.White.copy(alpha = brightness)
            )
    )
}

/**
 * FOCUS 状态眼睛 - 扁平禅意眼睛 (极窄椭圆)
 */
@Composable
private fun FocusEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(size * 1.3f)
            .height(size * 0.25f)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF67E8F9).copy(alpha = 0.85f)) // cyan-300
    )
}

/**
 * HAPPY 状态眼睛 - 弯弯笑眼
 */
@Composable
private fun HappyEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    // 依然使用椭圆作为基础
    Box(
        modifier = modifier
            .width(size)
            .height(size * 0.8f)
            .clip(RoundedCornerShape(50))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF10B981), // green-500
                        Color(0xFF059669)  // green-600
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 笑眼的弧形遮挡 (简单实现：通过上方颜色覆盖)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (size * 0.35f))
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.8f))
        )
    }
}

/**
 * SLEEPING 状态眼睛 - 闭眼
 */
@Composable
private fun SleepingEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sleepingEye")

    // 缓慢呼吸动画
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sleepingBreath"
    )

    Box(
        modifier = modifier
            .width(size * 1.1f * breathScale)
            .height(size * 0.08f)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF94A3B8).copy(alpha = 0.6f)) // slate-400
    )
}
