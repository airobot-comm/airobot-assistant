package com.airobot.features.clock.cards.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.airobot.framework.theme.RobotTheme

/**
 * Renders the skeuomorphic mechanical hammer and shaking bell dome caps.
 */
@Composable
fun AlarmChassisDecoration(
    isRinging: Boolean,
    bellRotation: Float,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Top Metal Hammer Link (Sitting on top of the dial bezel)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 40.dp)
                .size(32.dp, 54.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            RobotTheme.colors.skeuoMetalGradientStart,
                            RobotTheme.colors.skeuoMetalGradientEnd
                        )
                    )
                )
                .border(
                    1.dp,
                    RobotTheme.colors.chassisBorderSubtle,
                    RoundedCornerShape(8.dp)
                )
        )

        // 2. Physical Bell "Ears" — blue-gray dome caps, submerged behind dial
        val bellBrush = Brush.linearGradient(
            listOf(
                RobotTheme.colors.skeuoMetalGradientStart,
                RobotTheme.colors.skeuoMetalGradientEnd
            )
        )

        // Left Bell cap
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-138).dp, y = 28.dp)
                .size(102.dp, 80.dp)
                .graphicsLayer(
                    rotationZ = if (isRinging) -15f + bellRotation * 0.5f else 0f,
                    transformOrigin = TransformOrigin(0.5f, 1f)
                )
                .shadow(6.dp, RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp))
                .clip(RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp))
                .background(bellBrush)
        )

        // Right Bell cap
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 138.dp, y = 28.dp)
                .size(102.dp, 80.dp)
                .graphicsLayer(
                    rotationZ = if (isRinging) 15f - bellRotation * 0.5f else 0f,
                    transformOrigin = TransformOrigin(0.5f, 1f)
                )
                .shadow(6.dp, RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp))
                .clip(RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp))
                .background(bellBrush)
        )
    }
}
