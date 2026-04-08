package com.airobot.character.airobotui.comp.voice

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
 * 璇煶鍔ㄦ€佹尝褰㈢粍浠?
 * 鏍规嵁瀹為檯闊抽寮哄害鏄剧ず瀹炴椂璧蜂紡
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
        modifier = modifier.height(36.dp), // 绋嶅井澧炲姞楂樺害
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            // 鍩虹鍔ㄧ敾锛屾ā鎷熶笉鍚岄鐜囩殑娉㈠姩
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
            
            // 鎬婚珮搴︼細鍩虹鍔ㄧ敾 + 闊抽寮哄害褰卞搷
            val totalScale = if (isActive) {
                (baseScale + audioLevel * 1.2f + (index * 0.05f)).coerceIn(0.15f, 1f)
            } else {
                0.15f
            }

            Box(
                modifier = Modifier
                    .width(6.dp) // 鏇村涓€鐐?
                    .fillMaxHeight(totalScale)
                    .background(
                        color = if (isActive) barColor else barColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(100) // 瀹屽叏鍦嗚
                    )
            )
        }
    }
}

/**
 * 姝ｅ湪鎾姤鏃剁殑涓変釜璺冲姩鐐?
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


