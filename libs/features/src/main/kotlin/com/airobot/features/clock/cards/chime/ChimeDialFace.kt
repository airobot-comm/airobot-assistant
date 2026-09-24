package com.airobot.features.clock.cards.chime

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.clock.data.model.ChimeMode
import com.airobot.features.clock.data.model.HourlyChimeConfig
import com.airobot.framework.theme.DialIndicatorOrange
import com.airobot.framework.theme.RobotTheme
import com.airobot.framework.util.insetShadow
import java.util.Calendar

/**
 * Skeuomorphic clock dial face illustrating the hourly chime configuration.
 * Optimizations:
 * 1. Pendulum rod starts directly under pivot Center Cap (no gap, no upper extension pointer).
 * 2. Center display Box at y = 25dp below pivot showing selected mode or current chiming hour.
 * 3. Weather and Schedule Switches horizontally centered in a single row, completely circle-free and label-free.
 */
@Composable
fun ChimeDialFace(
    config: HourlyChimeConfig,
    isChiming: Boolean,
    progress: Float,
    onConfigChange: (HourlyChimeConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark

    // Bezel brush creating a metallic/shadow depth profile
    val bezelBgBrush = Brush.linearGradient(
        listOf(RobotTheme.colors.skeuoMetalGradientStart, RobotTheme.colors.skeuoMetalGradientEnd)
    )
    val highlightBrush = Brush.verticalGradient(
        colors = if (isDark) listOf(
            Color.White.copy(alpha = 0.5f),
            Color.White.copy(alpha = 0.05f)
        ) else listOf(
            Color.White,
            Color.White.copy(alpha = 0.3f)
        )
    )

    // Pendulum swing animation (2s full period)
    val swingTransition = rememberInfiniteTransition(label = "pendulumSwing")
    val pendulumAngle by if (isChiming) {
        swingTransition.animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pendulumAngle"
        )
    } else if (config.enabled) {
        swingTransition.animateFloat(
            initialValue = -4f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pendulumAngle"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Dial tick pulsing effect when ringing
    val glowTransition = rememberInfiniteTransition(label = "markerGlow")
    val glowAlpha by if (isChiming) {
        glowTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
    } else {
        remember { mutableStateOf(0.8f) }
    }

    Box(
        modifier = modifier
            .size(485.dp)
            .shadow(16.dp, CircleShape)
            .clip(CircleShape)
            .background(bezelBgBrush)
            .border(3.dp, highlightBrush, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Core Dial Face Chassis
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .clip(CircleShape)
                .background(RobotTheme.colors.chassisDialFace)
                .border(1.dp, RobotTheme.colors.chassisBorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Style tracks & ringing countdown arc
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF94A3B8).copy(alpha = 0.15f),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    if (isChiming) {
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

            // Draw Hour Ticks (1 to 12)
            val currentHour12 = Calendar.getInstance().get(Calendar.HOUR)
            
            Box(modifier = Modifier.fillMaxSize()) {
                for (hour in 1..12) {
                    val angleDegrees = (hour / 12f) * 360f - 90f
                    val angleRad = Math.toRadians(angleDegrees.toDouble())

                    val isHourActive = isHourConfiguredToChime(hour, config)
                    val isCurrentChiming = isChiming && (currentHour12 == hour || (hour == 12 && currentHour12 == 0))

                    // Draw relative dot marker along the dial face radius
                    val radiusFraction = 0.80f

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(
                                    x = 180.dp * radiusFraction * Math.cos(angleRad).toFloat(),
                                    y = 180.dp * radiusFraction * Math.sin(angleRad).toFloat()
                                )
                                .size(if (isCurrentChiming) 12.dp else 6.dp)
                                .shadow(if (isHourActive) 2.dp else 0.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrentChiming) DialIndicatorOrange.copy(alpha = glowAlpha)
                                    else if (isHourActive) DialIndicatorOrange.copy(alpha = 0.9f)
                                    else if (isDark) Color.White.copy(alpha = 0.1f)
                                    else Color.Black.copy(alpha = 0.1f)
                                )
                                .border(
                                    width = if (isCurrentChiming) 1.5.dp else 1.dp,
                                    color = if (isHourActive || isCurrentChiming) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            // Rotating Pendulum Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(pendulumAngle)
            ) {
                // Pendulum rod starting from pivot downward (aligned center, offset y = 95dp, height = 190dp)
                // This places top of rod exactly at pivot center Cap under y = 0dp, and bottom at y = 190dp
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 95.dp)
                        .width(4.dp) // Wider rod for solid mechanical feel
                        .height(190.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(RobotTheme.colors.skeuoMetalGradientStart, RobotTheme.colors.skeuoMetalGradientEnd)
                            )
                        )
                )

                // Mechanical Anchor Mount Bracket joining rod to bob at y = 190dp
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 175.dp)
                        .size(width = 10.dp, height = 18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF94A3B8))
                        .border(0.5.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                )

                // Pendulum weight/bob (Golden brass/bronze centered exactly at y = 190dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 190.dp)
                        .size(42.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFFCD34D), Color(0xFFD97706))
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
            }

            // Pivot Center Cap
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White, Color(0xFF94A3B8))
                        )
                    )
            )

            // Center status box (LCD style displaying active mode or current chiming hour)
            val currentHour24 = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            
            val lcdBgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0)
            val lcdBorderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)
            val textColor = if (config.enabled || isChiming) {
                DialIndicatorOrange.copy(alpha = 0.90f)
            } else {
                RobotTheme.colors.textMuted
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-65).dp) // Repositioned upward above the center pivot to clear pendulum
                    .size(width = 110.dp, height = 34.dp) // Tighter skeuomorphic footprint
                    .clip(RoundedCornerShape(8.dp))
                    .background(lcdBgColor)
                    .border(
                        width = 1.5.dp,
                        color = lcdBorderColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .insetShadow(
                        color = if (isDark) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.25f),
                        offsetX = 0.dp,
                        offsetY = 2.dp,
                        blurRadius = 4.dp,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isChiming) {
                    val blinkAlpha = glowAlpha
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp) // Snug spacing
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "Chiming",
                            tint = textColor,
                            modifier = Modifier
                                .size(14.dp) // Shrunk icon
                                .graphicsLayer(alpha = blinkAlpha)
                        )
                        Text(
                            text = String.format("%02d:00", currentHour24),
                            fontSize = 14.sp, // Shrunk font size
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Text(
                            text = "报时中",
                            fontSize = 10.sp, // Shrunk font size
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.graphicsLayer(alpha = blinkAlpha)
                        )
                    }
                } else {
                    val (modeText, modeIcon) = when (config.mode) {
                        ChimeMode.EVERY_HOUR -> "整点报时" to (if (config.enabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff)
                        ChimeMode.ODD_HOUR -> "奇点报时" to (if (config.enabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff)
                        ChimeMode.EVEN_HOUR -> "偶点报时" to (if (config.enabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp) // Snug spacing
                    ) {
                        Icon(
                            imageVector = modeIcon,
                            contentDescription = "Chime Mode",
                            tint = textColor,
                            modifier = Modifier.size(14.dp) // Shrunk icon
                        )
                        Text(
                            text = modeText,
                            fontSize = 11.sp, // Shrunk font size
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                        )
                    }
                }
            }

            // Weather & Schedule switches horizontally centered in a single row (circle-free to prevent visual overlap with bob)
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 70.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Weather Switch Image Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onConfigChange(config.copy(weatherReminder = !config.weatherReminder)) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WbSunny,
                        contentDescription = "Weather Chime",
                        tint = if (config.weatherReminder) DialIndicatorOrange else RobotTheme.colors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Schedule Switch Image Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onConfigChange(config.copy(scheduleReminder = !config.scheduleReminder)) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Schedule Chime",
                        tint = if (config.scheduleReminder) DialIndicatorOrange else RobotTheme.colors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Maps standard dial hour ticks to 24-hour range constraints to check if it matches configured hours.
 */
private fun isHourConfiguredToChime(dialHour: Int, config: HourlyChimeConfig): Boolean {
    if (!config.enabled) return false

    val hour1 = if (dialHour == 12) 0 else dialHour
    val hour2 = hour1 + 12

    return isHour24Configured(hour1, config) || isHour24Configured(hour2, config)
}

private fun isHour24Configured(hour24: Int, config: HourlyChimeConfig): Boolean {
    val inRange = if (config.startHour <= config.endHour) {
        hour24 in config.startHour..config.endHour
    } else {
        hour24 >= config.startHour || hour24 <= config.endHour
    }

    if (!inRange) return false

    return when (config.mode) {
        ChimeMode.EVERY_HOUR -> true
        ChimeMode.ODD_HOUR -> hour24 % 2 != 0
        ChimeMode.EVEN_HOUR -> hour24 % 2 == 0
    }
}
