package com.airobot.assistant.airobotui.robotcomp.voice

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
        modifier = modifier.height(30.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            // 基础动画，模拟不同频率的波动
            val baseScale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + (index * 100),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "baseHeight"
            )
            
            // 音频强度影响，实际音频强度会放大波形效果
            val audioInfluence = audioLevel * 0.5f // 音频强度贡献0-50%的高度
            
            // 总高度：基础动画 + 音频强度影响
            val totalScale = if (isActive) {
                baseScale + audioInfluence + (index * 0.1f) // 每个柱子有不同的基础高度
            } else {
                0.2f
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(totalScale)
                    .background(
                        color = if (isActive) barColor else barColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp)
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

