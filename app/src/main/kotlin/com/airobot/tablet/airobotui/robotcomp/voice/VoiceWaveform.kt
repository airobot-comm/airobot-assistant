package com.airobot.tablet.airobotui.robotcomp.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 语音动态波形组件
 * 根据实际音频强度显示实时起伏
 */
@Composable
fun VoiceWaveform(
    isActive: Boolean,
    barColor: Color,
    audioLevel: Float = 0.0f,
    modifier: Modifier = Modifier
) {
    val barCount = 5
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        modifier = modifier.height(36.dp), // 稍微增加高度
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            // 基础动画，模拟不同频率的波动
            val baseScale by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 500 + (index * 120),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "baseHeight"
            )
            
            // 总高度：基础动画 + 音频强度影响
            val totalScale = if (isActive) {
                (baseScale + audioLevel * 1.2f + (index * 0.05f)).coerceIn(0.15f, 1f)
            } else {
                0.15f
            }

            Box(
                modifier = Modifier
                    .width(6.dp) // 更宽一点
                    .fillMaxHeight(totalScale)
                    .background(
                        color = if (isActive) barColor else barColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(100) // 完全圆角
                    )
            )
        }
    }
}

/**
 * 正在播报时的三个跳动点
 */
@Composable
fun SpeakingDots(
    dotColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "speakingDots")
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val offset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotOffset"
            )
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = offset.dp)
                    .background(dotColor, RoundedCornerShape(4.dp))
            )
        }
    }
}


