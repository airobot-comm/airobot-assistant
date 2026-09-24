package com.airobot.features.clock.cards.timer

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.airobot.features.clock.data.model.PresetItem
import com.airobot.framework.theme.RobotTheme

/**
 * TimerFocusScenarios — renders the ambient focus background effects:
 * - Matrix digital rain for programming presets.
 * - Simulating page flip lines for reading presets.
 * - Meditation ripples for zen presets.
 * - Default breathing ambient color pulse.
 */
@Composable
fun BoxScope.TimerFocusScenarios(
    isDark: Boolean,
    displayLabel: String,
    preset: PresetItem?,
    secondaryAccentColor: Color,
    animateProgress: Float,
    zenProgress: Float,
    breathingScale: Float,
    breathingOpacity: Float
) {
    // 1. Default Indigo Breathing Glow
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = breathingScale
                scaleY = breathingScale
                alpha = breathingOpacity
            }
            .clip(CircleShape)
            .background(RobotTheme.colors.focusAccent.copy(alpha = 0.2f))
    )

    // 2. Dynamic Scenario animations based on preset description
    when {
        // Coding matrix rain
        displayLabel.contains("编程") || displayLabel.contains("编码") ||
            displayLabel.contains("Coding", ignoreCase = true) || displayLabel.contains(
            "Code",
            ignoreCase = true
        ) -> {
            // Matrix falling text setup
            val matrixPaint = remember {
                Paint().apply {
                    isAntiAlias = true
                    typeface = Typeface.MONOSPACE
                    textAlign = Paint.Align.CENTER
                }
            }
            val greenColor =
                if (isDark) android.graphics.Color.parseColor("#4ADE80") else android.graphics.Color.parseColor(
                    "#16A34A"
                )
            matrixPaint.color = greenColor
            matrixPaint.textSize = 24f

            Canvas(modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                drawIntoCanvas { canvas ->
                    val cols = 8
                    for (i in 0 until cols) {
                        val colX = w * (0.15f + i * 0.1f)
                        val startOffset = (i * 0.12f) * h
                        val totalPath = startOffset + animateProgress * h
                        val digitsCount = 4
                        for (d in 0 until digitsCount) {
                            val digitY = (totalPath + d * 30f) % h
                            val digitChar = if ((i + d) % 2 == 0) "1" else "0"
                            val dist = Math.hypot((colX - cx).toDouble(), (digitY - cy).toDouble())
                                .toFloat()
                            if (dist < cx - 20.dp.toPx()) {
                                val fadeAlpha = (1f - (digitY / h)) * 0.4f
                                matrixPaint.alpha = (fadeAlpha * 255).toInt().coerceIn(0, 255)
                                canvas.nativeCanvas.drawText(digitChar, colX, digitY, matrixPaint)
                            }
                        }
                    }
                }
            }
        }

        // Reading book page turn simulation
        displayLabel.contains("阅读") ||
            displayLabel.contains("Reading", ignoreCase = true) || displayLabel.contains(
            "Read",
            ignoreCase = true
        ) ||
            preset?.id == "3" -> {
            val infiniteTransition = rememberInfiniteTransition(label = "pageFlipTransition")
            val pageRotationY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 45f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ), label = "pageRotation"
            )
            val pageOpacity by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ), label = "pageOpacity"
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp)
                    .graphicsLayer {
                        rotationY = pageRotationY
                        alpha = pageOpacity
                        cameraDistance = 8f
                    }
            ) {
                drawLine(
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.3f),
                    start = Offset(0f, 10.dp.toPx()),
                    end = Offset(0f, size.height - 10.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                val linesCount = 4
                val spacing = 20.dp.toPx()
                repeat(linesCount) { idx ->
                    val y = size.height / 2f - (linesCount * spacing) / 2f + idx * spacing
                    drawLine(
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.15f),
                        start = Offset(16.dp.toPx(), y),
                        end = Offset(size.width - 24.dp.toPx(), y),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Meditation ripples
        else -> {
            repeat(3) { rippleIndex ->
                val phase = (zenProgress + rippleIndex * 0.33f) % 1f
                val scale = 0.8f + phase * 0.7f
                val alpha = (1f - phase) * 0.6f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .border(1.5.dp, secondaryAccentColor.copy(alpha = 0.4f), CircleShape)
                )
            }
        }
    }
}
