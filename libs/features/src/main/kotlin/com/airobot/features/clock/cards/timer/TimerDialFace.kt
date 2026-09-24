package com.airobot.features.clock.cards.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.R
import com.airobot.features.clock.data.model.AlarmItem
import com.airobot.features.clock.data.model.PresetItem
import com.airobot.framework.theme.RobotTheme

/**
 * TimerDialFace — renders the main mechanical dial, progress rings, digital clock readout,
 * labels, music status indicators, and the Suppressed Alarm Badge for Focus mode.
 */
@Composable
fun TimerDialFace(
    isFocusMode: Boolean,
    isDark: Boolean,
    isRunning: Boolean,
    isFinished: Boolean,
    timeString: String,
    displayLabel: String,
    progress: Float,
    preset: PresetItem?,
    accentColor: Color,
    secondaryAccentColor: Color,
    finishFlash: Float,
    finishScale: Float,
    ledAlpha: Float,
    bar1Height: Float,
    bar2Height: Float,
    bar3Height: Float,
    pendingAlarms: List<AlarmItem>,
    onShowPendingAlarms: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundScenarioContent: @Composable (BoxScope.() -> Unit)? = null
) {
    val bezelBgBrush = Brush.linearGradient(
        colors = listOf(
            RobotTheme.colors.skeuoMetalGradientStart,
            RobotTheme.colors.skeuoMetalGradientEnd
        )
    )
    val highlightBrush = Brush.verticalGradient(
        colors = if (isDark) listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.05f))
        else listOf(Color.White, Color.White.copy(alpha = 0.2f))
    )
    val dialBorderBrush = Brush.linearGradient(
        colors = listOf(
            RobotTheme.colors.skeuoControlGradientStart,
            RobotTheme.colors.skeuoControlGradientEnd
        )
    )

    Box(
        modifier = modifier
            .size(485.dp)
            .shadow(32.dp, CircleShape)
            .clip(CircleShape)
            .background(bezelBgBrush)
            .border(16.dp, dialBorderBrush, CircleShape)
            .border(2.dp, highlightBrush, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Inner Dial Face
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .clip(CircleShape)
                .background(RobotTheme.colors.chassisDialFace)
                .border(1.dp, RobotTheme.colors.chassisBorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Render any active background scenarios if provided
            backgroundScenarioContent?.invoke(this)

            // Concentric circle 1 (outermost, dashed, inset-16.dp)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                drawArc(
                    color = accentColor.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f)
                    )
                )
            }

            // Rotating indicator dot on concentric circle 1
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(progress * 360f)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 12.dp)
                        .size(10.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(accentColor)
                        .border(
                            width = 1.5.dp,
                            color = if (isDark) Color.Black else Color.White,
                            shape = CircleShape
                        )
                )
            }

            // Concentric circle 2 (innermost, solid, inset-48.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp)
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(
                            alpha = 0.05f
                        ),
                        shape = CircleShape
                    )
            )

            val trackColor = if (isFocusMode) {
                RobotTheme.colors.focusAccent.copy(alpha = 0.1f)
            } else {
                RobotTheme.colors.timerAccent.copy(alpha = 0.1f)
            }

            // Circular Progress Dial Ring
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(54.dp)
            ) {
                val strokeWidthPx = 6.dp.toPx()

                // Background track
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx)
                )

                // Active progress sweep
                drawArc(
                    color = if (isFinished) accentColor.copy(alpha = finishFlash) else accentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            // Dial Content Text Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(24.dp)
                    .graphicsLayer {
                        if (isFinished) {
                            scaleX = finishScale
                            scaleY = finishScale
                        }
                    }
            ) {
                if (isFinished) {
                    // Preset Label Name
                    Text(
                        text = displayLabel.uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(width = 320.dp, height = 110.dp)
                    ) {
                        // Radial glow pulse behind the original duration time text
                        Canvas(modifier = Modifier.size(140.dp)) {
                            val radius = size.minDimension / 2f
                            val pulseProgress = 1f - (finishFlash - 0.3f) / 0.7f // 0f to 1f
                            drawCircle(
                                color = accentColor,
                                radius = radius * (0.8f + pulseProgress * 0.4f),
                                alpha = (1f - pulseProgress) * 0.25f
                            )
                        }
                        
                        val totalSecs = totalSecondsOfPreset(preset)
                        val totalMin = totalSecs / 60
                        val totalSec = totalSecs % 60
                        val originalTimeString = String.format("%02d:%02d", totalMin, totalSec)
                        
                        Text(
                            text = originalTimeString,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor.copy(alpha = finishFlash),
                            letterSpacing = (-2).sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isFocusMode) stringResource(R.string.timer_finished_focus) else stringResource(
                            R.string.timer_finished_countdown
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.airobot.framework.theme.StatusRed
                    )
                    if (isFocusMode) {
                        Text(
                            text = stringResource(
                                R.string.timer_focus_duration_stat,
                                totalSecondsOfPreset(preset) / 60
                            ),
                            fontSize = 12.sp,
                            color = RobotTheme.colors.textMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.timer_click_to_close),
                        fontSize = 10.sp,
                        color = RobotTheme.colors.textMuted.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                } else {
                    // Preset Label Name ABOVE
                    Text(
                        text = displayLabel.uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Large digital countdown time display
                    Text(
                        text = timeString,
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Black,
                        color = RobotTheme.colors.textPrimary,
                        letterSpacing = (-2).sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Music and reminder details with visualizer
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (preset?.reminderInterval ?: 0 > 0) {
                            Text(
                                text = stringResource(
                                    R.string.timer_preset_reminder_minutes_chime,
                                    (preset?.reminderInterval ?: 0) / 60
                                ),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor.copy(alpha = 0.8f)
                            )
                        }
                        if (preset?.musicEnabled == true) {
                            val musicNameLabel = when (preset.bgMusic) {
                                "nature" -> stringResource(R.string.timer_music_nature)
                                "lofi" -> stringResource(R.string.timer_music_lofi)
                                "piano" -> stringResource(R.string.timer_music_piano)
                                "cyberpunk" -> stringResource(R.string.timer_music_cyberpunk)
                                "library" -> stringResource(R.string.timer_music_library)
                                "zen" -> stringResource(R.string.timer_music_zen)
                                "tick" -> stringResource(R.string.timer_music_tick)
                                else -> preset.bgMusic.ifEmpty {
                                    if (isFocusMode) stringResource(R.string.timer_music_nature)
                                    else stringResource(R.string.timer_music_tick)
                                }
                            }

                            val isTickMusic =
                                preset.bgMusic == "tick" || (preset.bgMusic.isEmpty() && !isFocusMode)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isTickMusic) "⏱️ " + stringResource(R.string.timer_music_tick)
                                    else "🎵 $musicNameLabel",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RobotTheme.colors.textSecondary.copy(alpha = 0.5f)
                                )

                                if (isRunning) {
                                    if (isTickMusic) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(accentColor.copy(alpha = ledAlpha))
                                        )
                                    } else {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                                            verticalAlignment = Alignment.Bottom,
                                            modifier = Modifier.height(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(bar1Height.dp)
                                                    .clip(CircleShape)
                                                    .background(secondaryAccentColor)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(bar2Height.dp)
                                                    .clip(CircleShape)
                                                    .background(secondaryAccentColor)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(bar3Height.dp)
                                                    .clip(CircleShape)
                                                    .background(secondaryAccentColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Suppressed alarms badge during focus mode
            if (isFocusMode && pendingAlarms.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-36).dp, y = (-36).dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .clickable { onShowPendingAlarms() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Pending Alarms",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = pendingAlarms.size.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun totalSecondsOfPreset(preset: PresetItem?): Int {
    return preset?.seconds ?: 1800
}
