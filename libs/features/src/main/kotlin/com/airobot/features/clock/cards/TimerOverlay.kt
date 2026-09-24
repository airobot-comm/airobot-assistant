package com.airobot.features.clock.cards

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airobot.features.R
import com.airobot.features.clock.cards.timer.PendingAlarmsOverlay
import com.airobot.features.clock.cards.timer.TimerControlPanel
import com.airobot.features.clock.cards.timer.TimerDialFace
import com.airobot.features.clock.cards.timer.TimerEarsDecoration
import com.airobot.features.clock.cards.timer.TimerFocusScenarios
import com.airobot.features.clock.data.model.AlarmItem
import com.airobot.features.clock.data.model.PresetItem
import com.airobot.features.clock.data.model.TimerMode
import com.airobot.framework.cards.OverlayBackdrop
import com.airobot.framework.cards.OverlayCloseButton
import com.airobot.framework.cards.OverlayCloseMode
import com.airobot.framework.theme.RobotTheme

/**
 * Unified Timer Overlay — Orchestrates both COUNTDOWN and FOCUS modes
 * by utilizing highly cohesive and separated sub-composables.
 *
 * Implements a Clean UI structure, dividing responsibilities to sub-composables
 * inside the 'timer' subdirectory.
 */
@Composable
fun TimerOverlay(
    timerSeconds: Int = 1800,
    totalSeconds: Int = 1800,
    isRunning: Boolean = false,
    preset: PresetItem? = null,
    mode: TimerMode = TimerMode.COUNTDOWN,
    pendingAlarms: List<AlarmItem> = emptyList(),
    onToggle: () -> Unit = {},
    onReset: () -> Unit = {},
    onSendToBackground: () -> Unit = {},
    onEmergencyStop: () -> Unit = {},
    onClearPendingAlarms: () -> Unit = {},
    onShowConstraintAlert: (String) -> Unit = {},
    onClose: () -> Unit
) {
    val isDark = RobotTheme.isDark
    val isFocusMode = mode == TimerMode.FOCUS
    val isFinished = timerSeconds == 0 && totalSeconds > 0

    // BackHandler intercepts back presses strictly when a focus session is active
    val isLocked = isFocusMode && timerSeconds > 0 && isRunning
    BackHandler(enabled = isLocked) {
        // Blocks back button interaction
    }

    // Auto-close when pressing system back gesture or key in non-locked state
    BackHandler(enabled = !isLocked) {
        onReset()
        onClose()
    }

    val displayLabel = preset?.label
        ?: if (isFocusMode) stringResource(R.string.timer_mode_focus_label) else stringResource(
            R.string.timer_mode_countdown_label
        )
    val progress =
        if (isFinished) 1f else if (totalSeconds > 0) (totalSeconds - timerSeconds) / totalSeconds.toFloat() else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "timerRestoration")

    // --- ANIMATIONS FOR COUNTDOWN MODE (Hourglass Ears) ---
    val sandScaleYTop by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "sandScaleYTop"
    )
    val sandScaleYBottom by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "sandScaleYBottom"
    )

    // Symmetrical ears rotation and translation animations when active
    val earRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "earRotation"
    )
    val earYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "earYOffset"
    )
    val ledAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "ledAlpha"
    )

    // --- ANIMATIONS FOR FOCUS MODE (Brain lobes Ears) ---
    val brainScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "brainScale"
    )
    val brainGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "brainGlowAlpha"
    )

    // Scenario-specific background breathing scale
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "breathingScale"
    )
    val breathingOpacity by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "breathingOpacity"
    )

    // Matrix Rain animation progress
    val animateProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "animateProgress"
    )

    // Meditation Ripple zenProgress
    val zenProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "zenProgress"
    )

    // Flashing finish animation
    val finishFlash by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "finishFlash"
    )

    // Pulsing scale finish animation
    val finishScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "finishScale"
    )

    // 3-Bar vertical visualizer
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "bar3"
    )

    // Theme Configs
    val accentColor =
        if (isFocusMode) RobotTheme.colors.focusAccent else RobotTheme.colors.timerAccent

    val secondaryAccentColor =
        if (isFocusMode) RobotTheme.colors.focusSecondaryAccent else RobotTheme.colors.timerAccent.copy(
            alpha = 0.6f
        )

    val min = timerSeconds / 60
    val sec = timerSeconds % 60
    val timeString = String.format("%02d:%02d", min, sec)

    // Pending alarms list overlay state
    var showPendingAlarmsOverlay by remember { mutableStateOf(false) }

    OverlayBackdrop(
        onClose = {
            if (isFinished) {
                onReset()
                onClose()
            } else if (!isLocked && !isRunning) {
                onReset()
                onClose()
            }
        },
        enabled = isFinished || (!isLocked && !isRunning),
        isKeepAlive = isRunning
    ) {
        Box(
            modifier = Modifier
                .size(width = 554.dp, height = 598.dp)
                .offset(x = (-10).dp, y = (-15).dp),
            contentAlignment = Alignment.Center
        ) {
            // 1. Decorative side side Side Ears (Hourglass or Brain Lobe)
            TimerEarsDecoration(
                isFocusMode = isFocusMode,
                isRunning = isRunning,
                isDark = isDark,
                accentColor = accentColor,
                sandScaleYTop = sandScaleYTop,
                sandScaleYBottom = sandScaleYBottom,
                earRotation = earRotation,
                earYOffset = earYOffset,
                brainScale = brainScale,
                brainGlowAlpha = brainGlowAlpha
            )

            TimerDialFace(
                isFocusMode = isFocusMode,
                isDark = isDark,
                isRunning = isRunning,
                isFinished = isFinished,
                timeString = timeString,
                displayLabel = displayLabel,
                progress = progress,
                preset = preset,
                accentColor = accentColor,
                secondaryAccentColor = secondaryAccentColor,
                finishFlash = finishFlash,
                finishScale = finishScale,
                ledAlpha = ledAlpha,
                bar1Height = bar1Height,
                bar2Height = bar2Height,
                bar3Height = bar3Height,
                pendingAlarms = pendingAlarms,
                onShowPendingAlarms = { showPendingAlarmsOverlay = true },
                modifier = Modifier.offset(y = 20.dp)
            ) {
                // Focus Mode Ambient Scenarios drawn under the concentric circles
                if (isFocusMode && isRunning && !isFinished) {
                    TimerFocusScenarios(
                        isDark = isDark,
                        displayLabel = displayLabel,
                        preset = preset,
                        secondaryAccentColor = secondaryAccentColor,
                        animateProgress = animateProgress,
                        zenProgress = zenProgress,
                        breathingScale = breathingScale,
                        breathingOpacity = breathingOpacity
                    )
                }
            }

            // 3. Top Close Button
            OverlayCloseButton(
                closeMode = if (isFocusMode && isRunning) OverlayCloseMode.TRIPLE_CONFIRM else OverlayCloseMode.SIMPLE,
                onClose = {
                    if (isFocusMode && isRunning) {
                        onEmergencyStop()
                    } else {
                        onReset()
                    }
                    onClose()
                },
                accentColor = accentColor,
                tooltipStep1 = stringResource(R.string.focus_close_step1),
                tooltipStep2 = stringResource(R.string.focus_close_step2),
                tooltipStep3 = stringResource(R.string.focus_close_step3),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 47.dp)
            )


            // 4. Asymmetric Right-Side Controls
            TimerControlPanel(
                isFocusMode = isFocusMode,
                isDark = isDark,
                isRunning = isRunning,
                accentColor = accentColor,
                onToggle = onToggle,
                onSendToBackground = onSendToBackground,
                onReset = onReset,
                onClose = onClose,
                isFinished = isFinished,
                onShowConstraintAlert = onShowConstraintAlert
            )

        }
    }

    // 5. suppressed Alarms Overlay during focus mode
    if (showPendingAlarmsOverlay) {
        PendingAlarmsOverlay(
            pendingAlarms = pendingAlarms,
            accentColor = accentColor,
            onConfirmAndClear = onClearPendingAlarms,
            onClose = { showPendingAlarmsOverlay = false }
        )
    }
}
