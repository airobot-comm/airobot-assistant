package com.airobot.features.clock.cards

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.airobot.features.clock.cards.chime.ChimeChassisDecoration
import com.airobot.features.clock.cards.chime.ChimeControlPanel
import com.airobot.features.clock.cards.chime.ChimeDialFace
import com.airobot.features.clock.data.model.HourlyChimeConfig
import com.airobot.framework.cards.OverlayBackdrop
import com.airobot.framework.theme.RobotTheme
import com.airobot.framework.theme.StatusRed
import kotlinx.coroutines.delay

/**
 * HourlyChimeOverlay — High-fidelity mechanical pop-up overlay for the Hourly Chime.
 * Formatted as a centered, premium retro clock chassis (554dp x 598dp) with:
 * - Two top interactive ear domes acting as start and end hour controls.
 * - Right-mounted physical controls (Master Power Lever, 3-Position Mode Drum slide, push-buttons).
 * - Central enlarged dial face (380dp) with a brass pendulum.
 * - Active resonance shaking (±1.5° chassis rotation) and bell swinging (±6°) when ringing.
 */
@Composable
fun HourlyChimeOverlay(
    config: HourlyChimeConfig,
    isChiming: Boolean,
    onConfigChange: (HourlyChimeConfig) -> Unit,
    onDismissChime: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark
    val backdropAlpha = if (isChiming) 0.5f else 0.95f
    val clickThrough = isChiming // allow clicks to go through if chiming in the background

    // Count down progress float from 1.0 down to 0.0 over 10 seconds
    var progress by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(isChiming) {
        if (isChiming) {
            val totalDurationMs = 10000
            val stepMs = 30
            var elapsedMs = 0
            while (elapsedMs < totalDurationMs) {
                delay(stepMs.toLong())
                elapsedMs += stepMs
                progress = 1f - (elapsedMs.toFloat() / totalDurationMs.toFloat())
            }
            progress = 0f
            onDismissChime()
        }
    }

    // Resonance vibrations and bell swinging infinite transitions
    val infiniteTransition = rememberInfiniteTransition(label = "ringingChimeAnimations")

    // Chassis shake is completely removed for stability as requested by the user
    val shakeTarget = 0f
    val shakeRotation by infiniteTransition.animateFloat(
        initialValue = -shakeTarget,
        targetValue = shakeTarget,
        animationSpec = infiniteRepeatable(
            animation = tween(40, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chassisShake"
    )

    // Gentle mechanical bobbing with a 600ms period (instead of rapid 80ms vibration)
    val bellTarget = if (isChiming) 3f else 0f
    val bellRotation by infiniteTransition.animateFloat(
        initialValue = -bellTarget,
        targetValue = bellTarget,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "earsSwing"
    )

    OverlayBackdrop(
        onClose = onClose,
        enabled = !isChiming,
        backdropAlpha = backdropAlpha,
        clickThrough = clickThrough,
        isKeepAlive = isChiming
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Group the chassis for shake animation
            val chassisModifier = if (isChiming) {
                Modifier.rotate(shakeRotation)
            } else {
                Modifier
            }

            Box(
                modifier = Modifier
                    .size(width = 554.dp, height = 598.dp)
                    .offset(x = (-10).dp, y = (-15).dp),
                contentAlignment = Alignment.Center
            ) {
                // 1. Shaking Chassis Bell ears and Hammer link
                ChimeChassisDecoration(
                    config = config,
                    isRinging = isChiming,
                    bellRotation = bellRotation,
                    isDark = isDark,
                    onConfigChange = onConfigChange,
                    modifier = chassisModifier
                )

                // 2. Central Clock Dial Face
                ChimeDialFace(
                    config = config,
                    isChiming = isChiming,
                    progress = progress,
                    onConfigChange = onConfigChange,
                    modifier = chassisModifier.offset(y = 20.dp).size(485.dp)
                )

                // 3. Top Stop/Power Button sitting on top of the dial bezel chassis
                IconButton(
                    onClick = {
                        if (isChiming) {
                            onDismissChime()
                        } else {
                            onClose()
                        }
                    },
                    modifier = chassisModifier
                        .align(Alignment.TopCenter)
                        .offset(y = 47.dp)
                        .size(80.dp, 40.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(RobotTheme.colors.chassisButtonBg)
                        .border(
                            androidx.compose.foundation.BorderStroke(
                                2.dp,
                                RobotTheme.colors.chassisBorderSubtle
                            ),
                            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        .shadow(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Stop",
                        tint = StatusRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 4. Physical knobs/switches directly on the right edge of the chassis Box
                ChimeControlPanel(
                    config = config,
                    isRinging = isChiming,
                    onConfigChange = onConfigChange,
                    modifier = chassisModifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 12.dp, y = 36.dp)
                )
            }
        }
    }
}
