package com.airobot.framework.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.R
import com.airobot.framework.theme.RobotTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OverlayCloseMode {
    SIMPLE,
    TRIPLE_CONFIRM
}

/**
 * Shared overlay close button component.
 *
 * Renders as a skeuomorphic Power Button tab arching over the dial face.
 * Mode SIMPLE: Single click triggers [onClose].
 * Mode TRIPLE_CONFIRM: Requires 3 consecutive clicks within a 5-second timeout window.
 * Shows progressive tooltip indicators and plays a shake animation on click.
 */
@Composable
fun OverlayCloseButton(
    closeMode: OverlayCloseMode,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF06B6D4), // Cyan or Indigo depending on mode
    tooltipStep1: String = stringResource(R.string.overlay_close_step1),
    tooltipStep2: String = stringResource(R.string.overlay_close_step2),
    tooltipStep3: String = stringResource(R.string.overlay_close_step3)
) {
    var clickCount by remember { mutableStateOf(0) }
    var tooltipText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var resetJob by remember { mutableStateOf<Job?>(null) }

    // Animatable for horizontal shake effect
    val shakeOffset = remember { Animatable(0f) }

    fun triggerShake() {
        scope.launch {
            // Shake back and forth: 0f -> -15f -> 15f -> -10f -> 10f -> 0f
            val spec = tween<Float>(durationMillis = 80, easing = LinearEasing)
            shakeOffset.animateTo(-15f, spec)
            shakeOffset.animateTo(15f, spec)
            shakeOffset.animateTo(-10f, spec)
            shakeOffset.animateTo(10f, spec)
            shakeOffset.animateTo(0f, spec)
        }
    }

    val warningColor = Color(0xFFEF4444) // Red warning color

    val buttonColor = when {
        closeMode == OverlayCloseMode.TRIPLE_CONFIRM && clickCount == 1 -> warningColor.copy(alpha = 0.6f)
        closeMode == OverlayCloseMode.TRIPLE_CONFIRM && clickCount == 2 -> warningColor
        else -> RobotTheme.colors.chassisButtonBg
    }

    val iconColor = when {
        closeMode == OverlayCloseMode.TRIPLE_CONFIRM && clickCount > 0 -> Color.White
        else -> Color(0xFFEF4444) // The red power icon symbol
    }

    val borderColor = RobotTheme.colors.chassisBorderSubtle

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tooltip Banner for confirmations
            tooltipText?.let { text ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(warningColor.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // The close button itself (Skeuomorphic Power Button tab arching over the bezel)
            Box(
                modifier = Modifier
                    .graphicsLayer(translationX = shakeOffset.value)
                    .size(80.dp, 40.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(buttonColor)
                    .clickable {
                        if (closeMode == OverlayCloseMode.SIMPLE) {
                            onClose()
                        } else {
                            // TRIPLE CONFIRM logic
                            triggerShake()
                            clickCount += 1

                            // Reset existing timer job
                            resetJob?.cancel()

                            when (clickCount) {
                                1 -> {
                                    tooltipText = tooltipStep1
                                }

                                2 -> {
                                    tooltipText = tooltipStep2
                                }

                                3 -> {
                                    tooltipText = tooltipStep3
                                    scope.launch {
                                        delay(800)
                                        clickCount = 0
                                        tooltipText = null
                                        onClose()
                                    }
                                    return@clickable
                                }
                            }

                            // Start a 5-second timeout to reset click count
                            resetJob = scope.launch {
                                delay(5000)
                                clickCount = 0
                                tooltipText = null
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Custom U-shape border (top, left, right sides only) for seamless bezel merging
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 2.dp.toPx()
                    val cornerRadius = 16.dp.toPx()
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(0f, cornerRadius)
                        arcTo(
                            rect = Rect(0f, 0f, cornerRadius * 2, cornerRadius * 2),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        lineTo(size.width - cornerRadius, 0f)
                        arcTo(
                            rect = Rect(
                                size.width - cornerRadius * 2,
                                0f,
                                size.width,
                                cornerRadius * 2
                            ),
                            startAngleDegrees = 270f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        lineTo(size.width, size.height)
                    }
                    drawPath(
                        path = path,
                        color = borderColor,
                        style = Stroke(width = strokeWidth)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Close overlay",
                        modifier = Modifier.size(18.dp),
                        tint = iconColor
                    )
                }
            }
        }
    }
}
