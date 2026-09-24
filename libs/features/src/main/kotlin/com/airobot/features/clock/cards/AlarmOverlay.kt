package com.airobot.features.clock.cards

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.airobot.features.clock.cards.alarm.AlarmChassisDecoration
import com.airobot.features.clock.cards.alarm.AlarmControlPanel
import com.airobot.features.clock.cards.alarm.AlarmDialContent
import com.airobot.features.clock.data.model.AlarmItem
import com.airobot.framework.cards.OverlayBackdrop
import com.airobot.framework.theme.RobotTheme
import kotlinx.coroutines.delay

/**
 * Alarm Overlay — Premium skeuomorphic alarm clock chassis and ringing screen.
 * Orchestrated utilizing separated sub-composables under overlays/alarm directory.
 *
 * Implements three distinct behavioral modes based on alarm.voiceMode:
 * - 'hint': Gentle breathing effect, no physical shaking, semi-transparent click-through backdrop.
 * - 'standard': Normal mechanical shaking and bell swinging, 90% dark backdrop.
 * - 'urgent': Violent shaking, red high-amplitude audio visualizer, 95% full backdrop.
 *
 * Also supports auto-dismissal (drawing a countdown progress arc) and voice-dismissal (microphone prompt).
 */
@Composable
fun AlarmOverlay(
    alarm: AlarmItem? = null,
    ringingAlarmId: String? = null,
    alarms: List<AlarmItem> = emptyList(),
    onToggleAlarm: () -> Unit = {},
    onDismissAlarm: () -> Unit = {},
    onMinimizeAlarm: (Boolean) -> Unit = {},
    onClose: () -> Unit
) {
    val isDark = RobotTheme.isDark

    // Find the current alarm if ringing, fallback to passed alarm or default
    val displayAlarm = remember(alarm, ringingAlarmId, alarms) {
        if (ringingAlarmId != null) {
            alarms.find { it.id == ringingAlarmId } ?: alarm ?: AlarmItem(
                id = "default",
                time = "22:30",
                label = "准备睡觉",
                enabled = true,
                days = listOf(0, 1, 2, 3, 4, 5, 6),
                voiceMode = "standard",
                dismissMode = "manual"
            )
        } else {
            alarm ?: AlarmItem(
                id = "default",
                time = "22:30",
                label = "准备睡觉",
                enabled = true,
                days = listOf(0, 1, 2, 3, 4, 5, 6),
                voiceMode = "standard",
                dismissMode = "manual"
            )
        }
    }

    val isRinging = ringingAlarmId != null

    // Ringing shake & bell animations
    val infiniteTransition = rememberInfiniteTransition(label = "ringingAnimations")

    val shakeTarget =
        if (!isRinging || displayAlarm.voiceMode == "hint") 0f else if (displayAlarm.voiceMode == "urgent") 6f else 3f
    val shakeDuration = if (displayAlarm.voiceMode == "urgent") 30 else 50
    val shakeRotation by infiniteTransition.animateFloat(
        initialValue = -shakeTarget,
        targetValue = shakeTarget,
        animationSpec = infiniteRepeatable(
            animation = tween(shakeDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shake"
    )

    val bellTarget =
        if (!isRinging || displayAlarm.voiceMode == "hint") 0f else if (displayAlarm.voiceMode == "urgent") 20f else 12f
    val bellDuration = if (displayAlarm.voiceMode == "urgent") 50 else 80
    val bellRotation by infiniteTransition.animateFloat(
        initialValue = -bellTarget,
        targetValue = bellTarget,
        animationSpec = infiniteRepeatable(
            animation = tween(bellDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "bell"
    )

    val backdropAlpha = when (displayAlarm.voiceMode) {
        "hint" -> 0.5f
        "urgent" -> 0.97f
        else -> 0.90f
    }
    val clickThrough = (displayAlarm.voiceMode == "hint")

    // Auto dismiss for auto mode
    var progress by remember { mutableFloatStateOf(1f) }
    if (isRinging && displayAlarm.dismissMode == "auto") {
        LaunchedEffect(ringingAlarmId) {
            val autoDismissMs = displayAlarm.autoDismissSeconds * 1000L
            val step = 16L // ~60fps
            val startTime = System.currentTimeMillis()
            while (true) {
                delay(step)
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= autoDismissMs) break
                progress = 1f - (elapsed.toFloat() / autoDismissMs.toFloat())
            }
            progress = 0f
            onDismissAlarm()
        }
    }

    // Frosted Glass Screen Backdrop using base OverlayBackdrop
    OverlayBackdrop(
        onClose = onClose,
        enabled = !isRinging,
        backdropAlpha = backdropAlpha,
        clickThrough = clickThrough,
        isKeepAlive = isRinging
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Group the chassis for shake animation
            val chassisModifier = if (isRinging) {
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
                // 1. Shaking Chassis Bell and Hammer link
                AlarmChassisDecoration(
                    isRinging = isRinging,
                    bellRotation = bellRotation,
                    isDark = isDark,
                    modifier = chassisModifier
                )

                // 2. Central Clock Dial Content Face
                AlarmDialContent(
                    alarm = displayAlarm,
                    isRinging = isRinging,
                    isDark = isDark,
                    progress = progress,
                    onDismissAlarm = onDismissAlarm,
                    modifier = chassisModifier.offset(y = 20.dp)
                )

                // 3. Static Top shutoff and right slim mechanical switches
                AlarmControlPanel(
                    alarm = displayAlarm,
                    isRinging = isRinging,
                    isDark = isDark,
                    onDismissAlarm = onDismissAlarm,
                    onToggleAlarm = onToggleAlarm,
                    onMinimizeAlarm = onMinimizeAlarm,
                    onClose = onClose
                )
            }
        }
    }
}
