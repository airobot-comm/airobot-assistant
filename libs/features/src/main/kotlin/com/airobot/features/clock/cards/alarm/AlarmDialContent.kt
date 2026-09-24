package com.airobot.features.clock.cards.alarm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.R
import com.airobot.features.clock.data.model.AlarmItem
import com.airobot.framework.theme.DialIndicatorOrange
import com.airobot.framework.theme.RobotTheme

/**
 * Renders the skeuomorphic main dial clockface, containing circular progress sweeps,
 * the large digital time, sound visualizer equalizer bars, and name mic checks.
 */
@Composable
fun AlarmDialContent(
    alarm: AlarmItem,
    isRinging: Boolean,
    isDark: Boolean,
    progress: Float,
    onDismissAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bezelBgBrush = Brush.linearGradient(
        listOf(RobotTheme.colors.skeuoMetalGradientStart, RobotTheme.colors.skeuoMetalGradientEnd)
    )
    val highlightBrush = Brush.verticalGradient(
        colors = if (isDark) listOf(
            Color.White.copy(alpha = 0.5f), // Shiny top metal highlight
            Color.White.copy(alpha = 0.05f)
        ) else listOf(
            Color.White, // Premium white shiny highlight
            Color.White.copy(alpha = 0.3f)
        )
    )

    Box(
        modifier = modifier
            .size(485.dp)
            .shadow(16.dp, CircleShape)
            .clip(CircleShape)
            .background(bezelBgBrush)
            .border(3.dp, highlightBrush, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Inner dial face — beautiful compact padding of 18.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .clip(CircleShape)
                .background(RobotTheme.colors.chassisDialFace)
                .border(1.dp, RobotTheme.colors.chassisBorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Concentric circle 1 (outermost, dashed, inset-16.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.08f)
                        else Color(0xFF94A3B8).copy(alpha = 0.15f),
                        CircleShape
                    )
            ) {
                if (isRinging && alarm.dismissMode == "auto") {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = DialIndicatorOrange,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }

            // Concentric circle 2 (innermost, solid, inset-48.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp)
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.04f)
                        else Color(0xFF94A3B8).copy(alpha = 0.08f),
                        CircleShape
                    )
            )

            // Orange time-position dot inside the inner face
            val rotationDegree = remember(alarm.time) {
                try {
                    val parts = alarm.time.split(":")
                    val h = parts[0].toInt() % 12
                    val m = parts[1].toInt()
                    ((h * 60 + m) / 720f) * 360f
                } catch (e: Exception) {
                    0f
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotationDegree)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 9.dp)
                        .size(14.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(DialIndicatorOrange)
                        .border(
                            2.dp,
                            if (isDark) Color(0xFF1E293B) else Color(0xFFF5F7FA),
                            CircleShape
                        )
                )
            }

            // Dial Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isRinging) stringResource(R.string.alarm_ringing_label)
                    else (if (alarm.enabled) stringResource(R.string.alarm_status_enabled) else stringResource(
                        R.string.alarm_status_disabled
                    )),
                    fontSize = 12.sp,
                    color = if (isRinging) RobotTheme.colors.alarmAccent else RobotTheme.colors.dialLabelSubtle,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = alarm.time,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                    color = RobotTheme.colors.dialTimePrimary,
                    letterSpacing = (-2).sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sound visualizer — vertical pill bars (animating when ringing)
                if (isRinging) {
                    val infiniteTransition = rememberInfiniteTransition(label = "ringingVisualizer")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val targetH =
                            if (alarm.voiceMode == "hint") 18f else if (alarm.voiceMode == "urgent") 48f else 36f
                        val barColor =
                            if (alarm.voiceMode == "urgent") Color(0xFFEF4444) else DialIndicatorOrange

                        repeat(5) { i ->
                            val animHeight by infiniteTransition.animateFloat(
                                initialValue = 10f,
                                targetValue = targetH,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        400,
                                        delayMillis = i * 80,
                                        easing = FastOutSlowInEasing
                                    ),
                                    repeatMode = RepeatMode.Reverse
                                ), label = "bar$i"
                            )
                            val animOpacity by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        400,
                                        delayMillis = i * 80,
                                        easing = FastOutSlowInEasing
                                    ),
                                    repeatMode = RepeatMode.Reverse
                                ), label = "opacity$i"
                            )

                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(animHeight.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(barColor.copy(alpha = animOpacity))
                            )
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val baseAlphas = listOf(0.15f, 0.35f, 0.65f, 0.35f, 0.15f)
                        repeat(5) { i ->
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(DialIndicatorOrange.copy(alpha = baseAlphas[i]))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val voiceModeStr = when (alarm.voiceMode) {
                    "hint" -> stringResource(R.string.alarm_voice_mode_hint)
                    "urgent" -> stringResource(R.string.alarm_voice_mode_urgent)
                    else -> stringResource(R.string.alarm_voice_mode_standard)
                }
                val dismissModeStr = when (alarm.dismissMode) {
                    "voice" -> stringResource(R.string.alarm_dismiss_mode_voice)
                    "auto" -> stringResource(R.string.alarm_dismiss_mode_auto)
                    else -> stringResource(R.string.alarm_dismiss_mode_manual)
                }
                Text(
                    text = "${alarm.label} • " + stringResource(
                        R.string.alarm_mode_format,
                        voiceModeStr,
                        dismissModeStr
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RobotTheme.colors.textMuted
                )

                // Name voice check cancellation panel
                AnimatedVisibility(visible = isRinging && alarm.dismissMode == "voice") {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(40.dp))
                                .background(Color(0xFF6366F1).copy(alpha = 0.15f))
                                .border(
                                    1.dp,
                                    Color(0xFF6366F1).copy(alpha = 0.3f),
                                    RoundedCornerShape(40.dp)
                                )
                                .clickable { onDismissAlarm() }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.alarm_voice_dismiss_prompt),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
