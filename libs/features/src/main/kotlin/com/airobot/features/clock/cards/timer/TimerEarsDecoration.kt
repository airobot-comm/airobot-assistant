package com.airobot.features.clock.cards.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Renders the symmetrical animated decorative ears on the sides of the dial:
 * - Hourglass decorations for Countdown Mode
 * - Lobe line-art brain decorations for Focus Mode
 */
@Composable
fun BoxScope.TimerEarsDecoration(
    isFocusMode: Boolean,
    isRunning: Boolean,
    isDark: Boolean,
    accentColor: Color,
    sandScaleYTop: Float,
    sandScaleYBottom: Float,
    earRotation: Float,
    earYOffset: Float,
    brainScale: Float,
    brainGlowAlpha: Float
) {
    val actualLeftRotation = if (isRunning) earRotation else 0f
    val actualRightRotation = if (isRunning) -earRotation else 0f
    val actualYOffset = if (isRunning) earYOffset.dp else 0.dp
    val actualScale = if (isRunning) brainScale else 1f

    if (isFocusMode) {
        // --- FOCUS MODE (Brain lobes Ears) ---
        // Left Ear
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-128).dp, y = 22.dp)
                .size(96.dp, 64.dp)
                .graphicsLayer {
                    rotationZ = actualLeftRotation
                    scaleX = actualScale
                    scaleY = actualScale
                }
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        if (isDark) listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.03f)
                        )
                        else listOf(
                            Color.White.copy(alpha = 0.75f),
                            Color.White.copy(alpha = 0.45f)
                        )
                    )
                )
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = brainGlowAlpha }
                        .background(accentColor.copy(alpha = 0.2f))
                )
            }
            BrainCanvas(isDark = isDark)
        }

        // Right Ear
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 128.dp, y = 22.dp)
                .size(96.dp, 64.dp)
                .graphicsLayer {
                    rotationZ = actualRightRotation
                    scaleX = actualScale
                    scaleY = actualScale
                }
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        if (isDark) listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.03f)
                        )
                        else listOf(
                            Color.White.copy(alpha = 0.75f),
                            Color.White.copy(alpha = 0.45f)
                        )
                    )
                )
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = brainGlowAlpha }
                        .background(accentColor.copy(alpha = 0.2f))
                )
            }
            BrainCanvas(isDark = isDark)
        }
    } else {
        // --- COUNTDOWN MODE (Hourglass Ears) ---
        // Left Ear
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-128).dp, y = (22.dp + actualYOffset))
                .size(96.dp, 64.dp)
                .graphicsLayer { rotationZ = actualLeftRotation }
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        if (isDark) listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color(0xFF0891B2).copy(alpha = 0.05f)
                        )
                        else listOf(
                            Color.White.copy(alpha = 0.75f),
                            Color(0xFFCFFAFE).copy(alpha = 0.45f)
                        )
                    )
                )
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            HourglassContent(
                isRunning = isRunning,
                accentColor = accentColor,
                sandScaleYTop = sandScaleYTop,
                sandScaleYBottom = sandScaleYBottom,
                isDark = isDark
            )
        }

        // Right Ear
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 128.dp, y = (22.dp + actualYOffset))
                .size(96.dp, 64.dp)
                .graphicsLayer { rotationZ = actualRightRotation }
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        if (isDark) listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color(0xFF0891B2).copy(alpha = 0.05f)
                        )
                        else listOf(
                            Color.White.copy(alpha = 0.75f),
                            Color(0xFFCFFAFE).copy(alpha = 0.45f)
                        )
                    )
                )
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            HourglassContent(
                isRunning = isRunning,
                accentColor = accentColor,
                sandScaleYTop = sandScaleYTop,
                sandScaleYBottom = sandScaleYBottom,
                isDark = isDark
            )
        }
    }
}

@Composable
private fun HourglassContent(
    isRunning: Boolean,
    accentColor: Color,
    sandScaleYTop: Float,
    sandScaleYBottom: Float,
    isDark: Boolean
) {
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(36.dp, 16.dp)
                .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                .graphicsLayer {
                    scaleY = if (isRunning) sandScaleYTop else 1f
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .background(accentColor.copy(alpha = 0.4f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(3.dp, 16.dp)
                .background(accentColor.copy(alpha = if (isRunning) 0.3f else 0.05f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(36.dp, 16.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .graphicsLayer {
                    scaleY = if (isRunning) sandScaleYBottom else 0.5f
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .background(accentColor.copy(alpha = 0.4f))
        )
    }
    Icon(
        imageVector = Icons.Outlined.HourglassEmpty,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.15f)
    )
}

@Composable
private fun BrainCanvas(isDark: Boolean) {
    Canvas(modifier = Modifier.size(36.dp)) {
        val strokeWidthPx = 2.dp.toPx()
        val color = (if (isDark) Color(0xFFC7D2FE) else Color(0xFF4F46E5)).copy(alpha = 0.8f)

        val leftPath = Path().apply {
            moveTo(16.dp.toPx(), 6.dp.toPx())
            cubicTo(12.dp.toPx(), 5.dp.toPx(), 8.dp.toPx(), 7.dp.toPx(), 8.dp.toPx(), 11.dp.toPx())
            cubicTo(6.dp.toPx(), 12.dp.toPx(), 5.dp.toPx(), 15.dp.toPx(), 6.dp.toPx(), 18.dp.toPx())
            cubicTo(
                5.dp.toPx(),
                21.dp.toPx(),
                7.dp.toPx(),
                24.dp.toPx(),
                10.dp.toPx(),
                25.dp.toPx()
            )
            cubicTo(
                10.dp.toPx(),
                28.dp.toPx(),
                13.dp.toPx(),
                30.dp.toPx(),
                16.dp.toPx(),
                29.dp.toPx()
            )
            cubicTo(
                17.dp.toPx(),
                29.dp.toPx(),
                17.5f.dp.toPx(),
                27.dp.toPx(),
                17.5f.dp.toPx(),
                25.dp.toPx()
            )
            lineTo(17.5f.dp.toPx(), 9.dp.toPx())
            close()
        }
        val rightPath = Path().apply {
            moveTo(20.dp.toPx(), 6.dp.toPx())
            cubicTo(
                24.dp.toPx(),
                5.dp.toPx(),
                28.dp.toPx(),
                7.dp.toPx(),
                28.dp.toPx(),
                11.dp.toPx()
            )
            cubicTo(
                30.dp.toPx(),
                12.dp.toPx(),
                31.dp.toPx(),
                15.dp.toPx(),
                30.dp.toPx(),
                18.dp.toPx()
            )
            cubicTo(
                31.dp.toPx(),
                21.dp.toPx(),
                29.dp.toPx(),
                24.dp.toPx(),
                26.dp.toPx(),
                25.dp.toPx()
            )
            cubicTo(
                26.dp.toPx(),
                28.dp.toPx(),
                23.dp.toPx(),
                30.dp.toPx(),
                20.dp.toPx(),
                29.dp.toPx()
            )
            cubicTo(
                19.dp.toPx(),
                29.dp.toPx(),
                18.5f.dp.toPx(),
                27.dp.toPx(),
                18.5f.dp.toPx(),
                25.dp.toPx()
            )
            lineTo(18.5f.dp.toPx(), 9.dp.toPx())
            close()
        }
        val leftInner = Path().apply {
            moveTo(11.dp.toPx(), 12.dp.toPx())
            cubicTo(
                13.dp.toPx(),
                13.dp.toPx(),
                14.dp.toPx(),
                11.dp.toPx(),
                17.5f.dp.toPx(),
                12.dp.toPx()
            )
            moveTo(8.dp.toPx(), 18.dp.toPx())
            cubicTo(
                11.dp.toPx(),
                18.dp.toPx(),
                13.dp.toPx(),
                16.dp.toPx(),
                17.5f.dp.toPx(),
                17.dp.toPx()
            )
            moveTo(9.dp.toPx(), 22.dp.toPx())
            cubicTo(
                12.dp.toPx(),
                21.dp.toPx(),
                13.dp.toPx(),
                23.dp.toPx(),
                17.5f.dp.toPx(),
                22.dp.toPx()
            )
        }
        val rightInner = Path().apply {
            moveTo(25.dp.toPx(), 12.dp.toPx())
            cubicTo(
                23.dp.toPx(),
                13.dp.toPx(),
                22.dp.toPx(),
                11.dp.toPx(),
                18.5f.dp.toPx(),
                12.dp.toPx()
            )
            moveTo(28.dp.toPx(), 18.dp.toPx())
            cubicTo(
                25.dp.toPx(),
                18.dp.toPx(),
                23.dp.toPx(),
                16.dp.toPx(),
                18.5f.dp.toPx(),
                17.dp.toPx()
            )
            moveTo(27.dp.toPx(), 22.dp.toPx())
            cubicTo(
                24.dp.toPx(),
                21.dp.toPx(),
                23.dp.toPx(),
                23.dp.toPx(),
                18.5f.dp.toPx(),
                22.dp.toPx()
            )
        }

        drawPath(leftPath, color, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round))
        drawPath(rightPath, color, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round))
        drawPath(leftInner, color, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round))
        drawPath(rightInner, color, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round))
    }
}
