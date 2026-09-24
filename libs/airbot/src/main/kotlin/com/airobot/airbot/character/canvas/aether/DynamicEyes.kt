package com.airobot.airbot.character.canvas.aether

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airobot.airbot.viewmodel.RobotVisualState
import com.airobot.framework.theme.RobotEyeActive
import com.airobot.framework.theme.RobotEyeDefault
import com.airobot.framework.theme.StatusCyan

/**
 * 增强的动态眼睛组件 - 支持微表情同步
 *
 * Web原型对应: IPCharacter.tsx 中的 getEyes() 函数
 */
@Composable
internal fun DynamicEyes(
    state: RobotVisualState,
    ttsProgressNormalized: Float = 0f, // 0-1, TTS 播放进度
    audioLevel: () -> Float = { 0f }, // 传入音频等级 0-1 (Lambda)
    eyeSize: Dp = 48.dp,
    eyeGap: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "enhancedEyeAnimation")

    // Speaking: slow left-right eye look
    val speakingEyeLookX by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speakingEyeLook"
    )

    // Thinking: medium drift
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

    // Bored: restless left-right wander
    val boredEyeX by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boredEyeX"
    )

    // Dazing: large dreamy drift
    val dazingEyeX by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dazingEyeX"
    )
    val dazingEyeY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dazingEyeY"
    )

    // Compute actual eye offsets based on state
    val eyeOffsetX = when (state) {
        RobotVisualState.SPEAKING -> speakingEyeLookX.dp
        RobotVisualState.THINKING -> thinkingEyeOffsetX.dp
        RobotVisualState.BORED -> boredEyeX.dp
        RobotVisualState.DAZING -> dazingEyeX.dp
        else -> 0.dp
    }

    val eyeOffsetY = when (state) {
        RobotVisualState.THINKING -> thinkingEyeOffsetY.dp
        RobotVisualState.DAZING -> dazingEyeY.dp
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
    state: RobotVisualState,
    size: Dp,
    ttsProgressNormalized: Float = 0f,
    audioLevel: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eyeGlowPulse")

    // 基础发光呼吸效果
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 外部发光层 (Bloom Effect) - 改为长椭圆
        val glowColor = getEyeColor(state)
        Box(
            modifier = Modifier
                .size(width = size * 1.5f, height = size * 1.2f)
                .graphicsLayer { alpha = 0.4f * glowPulse }
                .clip(RoundedCornerShape(size))
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor,
                            Color.Transparent
                        )
                    )
                )
                .blur(size * 0.25f)
        )

        // Core eye component — dispatched by visual state
        when (state) {
            RobotVisualState.IDLE -> IdleEyeEnhanced(size = size)
            RobotVisualState.BORED -> BoredEyeEnhanced(size = size)
            RobotVisualState.DAZING -> DazingEyeEnhanced(size = size)
            RobotVisualState.DOZING -> DozingEyeEnhanced(size = size)
            RobotVisualState.LISTENING -> ListeningEyeEnhanced(size = size, audioLevel = audioLevel)
            RobotVisualState.THINKING -> ThinkingEyeEnhanced(size = size)
            RobotVisualState.SPEAKING -> SpeakingEyeEnhanced(
                size = size,
                ttsProgressNormalized = ttsProgressNormalized,
                audioLevel = audioLevel
            )

            RobotVisualState.FOCUS -> FocusEyeEnhanced(size = size)
            RobotVisualState.HAPPY -> HappyEyeEnhanced(size = size)
            RobotVisualState.SLEEPING -> SleepingEyeEnhanced(size = size)
            RobotVisualState.WORKING -> WorkingEyeEnhanced(size = size)
        }
    }
}

private fun getEyeColor(state: RobotVisualState): Color {
    return when (state) {
        RobotVisualState.IDLE -> RobotEyeDefault
        RobotVisualState.BORED -> RobotEyeDefault
        RobotVisualState.DAZING -> Color(0xFF94A3B8)  // Dreamy grey
        RobotVisualState.DOZING -> Color(0xFF94A3B8)  // Drowsy grey
        RobotVisualState.LISTENING -> StatusCyan
        RobotVisualState.THINKING -> RobotEyeActive   // Orange
        RobotVisualState.SPEAKING -> RobotEyeActive   // Orange
        RobotVisualState.FOCUS -> Color(0xFF67E8F9)
        RobotVisualState.HAPPY -> Color(0xFF10B981)
        RobotVisualState.SLEEPING -> Color(0xFF94A3B8)
        RobotVisualState.WORKING -> Color(0xFF6366F1) // indigo-500
    }
}

/**
 * IDLE 状态眼睛 - 大圆眼 + 灵动双高光 (卡通风格)
 */
@Composable
private fun IdleEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center
    ) {
        // 主高光 (左上)
        Box(
            modifier = Modifier
                .size(size * 0.35f)
                .offset(x = (-size * 0.15f), y = (-size * 0.15f))
                .clip(CircleShape)
                .background(Color.White)
        )
        // 次高光 (右下)
        Box(
            modifier = Modifier
                .size(size * 0.15f)
                .offset(x = (size * 0.18f), y = (size * 0.18f))
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.7f))
        )
    }
}

/**
 * LISTENING 状态眼睛 - 音频感应高度
 */
@Composable
private fun ListeningEyeEnhanced(
    size: Dp,
    audioLevel: () -> Float, // 增加音频等级调制
    modifier: Modifier = Modifier
) {
    val dynamicHeight = size * (0.4f + audioLevel() * 1.0f)
    Box(
        modifier = modifier
            .width(size * 0.9f)
            .height(dynamicHeight)
            .clip(RoundedCornerShape(50))
            .background(getEyeColor(RobotVisualState.LISTENING).copy(alpha = 0.95f))
            .blur(0.5.dp)
    )
}

/**
 * THINKING 状态眼睛 - 稍微缩小的圆眼 + 呼吸缩放
 */
@Composable
private fun ThinkingEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinkingEye")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
        IdleEyeEnhanced(size = size)
    }
}

/**
 * SPEAKING 状态眼睛 - 剧烈的纵向缩放 (模拟说话)
 */
@Composable
private fun SpeakingEyeEnhanced(
    size: Dp,
    ttsProgressNormalized: Float = 0f,
    audioLevel: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    val audioEffect = audioLevel()

    // 纵向拉伸和压缩
    val scaleY = 1f - (audioEffect * 0.4f)
    val scaleX = 1f + (audioEffect * 0.2f)

    Box(
        modifier = modifier.graphicsLayer {
            this.scaleX = scaleX
            this.scaleY = scaleY
        }
    ) {
        IdleEyeEnhanced(size = size)
    }
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

/**
 * BORED state eye — round eyes with restless pulsing scale
 */
@Composable
private fun BoredEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "boredEye")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "boredScale"
    )

    Box(modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
        // Slightly reduced size to convey disinterest
        Box(
            modifier = Modifier
                .size(size * 0.85f)
                .clip(CircleShape)
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.3f)
                    .offset(x = (-size * 0.12f), y = (-size * 0.12f))
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .size(size * 0.12f)
                    .offset(x = (size * 0.15f), y = (size * 0.15f))
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f))
            )
        }
    }
}

/**
 * DAZING state eye — large unfocused eyes with faded highlights
 */
@Composable
private fun DazingEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dazingEye")
    val highlightAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dazingHighlight"
    )

    Box(
        modifier = modifier
            .size(size * 1.1f)  // Slightly enlarged for dreamy effect
            .clip(CircleShape)
            .background(Color(0xFF475569)), // Slightly lighter than normal
        contentAlignment = Alignment.Center
    ) {
        // Faded, larger highlight — dreamy unfocused look
        Box(
            modifier = Modifier
                .size(size * 0.45f)
                .offset(x = (-size * 0.1f), y = (-size * 0.1f))
                .clip(CircleShape)
                .background(Color.White.copy(alpha = highlightAlpha))
        )
    }
}

/**
 * DOZING state eye — half-closed, taller than sleeping but narrower than idle
 */
@Composable
private fun DozingEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dozingEye")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dozingBreath"
    )

    Box(
        modifier = modifier
            .width(size * 0.9f * breathScale)
            .height(size * 0.25f)  // Half-closed — taller than sleeping (0.08f)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF64748B).copy(alpha = 0.8f)) // slate-500
    )
}

/**
 * WORKING state eye — focused, slightly wide blocky eye
 */
@Composable
private fun WorkingEyeEnhanced(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(size * 1.4f)
            .height(size * 0.7f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF6366F1)), // indigo-500
        contentAlignment = Alignment.Center
    ) {
        // Inner highlights
        Box(
            modifier = Modifier
                .size(size * 0.25f)
                .offset(x = (-size * 0.35f), y = (-size * 0.15f))
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
        )
        Box(
            modifier = Modifier
                .size(size * 0.12f)
                .offset(x = (size * 0.35f), y = (size * 0.15f))
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.7f))
        )
    }
}
