package com.airobot.features.clock.cards.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airobot.features.aiserv.popup.PopupQueueWidgetItem
import com.airobot.features.aiserv.popup.PopupServiceItem
import com.airobot.framework.theme.ClockCenterPin
import com.airobot.framework.theme.ClockHandHour
import com.airobot.framework.theme.ClockHandMinute
import com.airobot.framework.theme.ClockHandSecond
import com.airobot.framework.theme.RobotTheme
import com.airobot.framework.util.insetShadow
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Skeuomorphic Clock Face — analog clock with sub-dial widget.
 *
 * Prototype ref: SkeuomorphicClock.tsx
 *
 * Features:
 *   - Full analog clock face with 12 hour markers
 *   - Hour, minute, second hands (Canvas-drawn)
 *   - Center pin (orange)
 *   - Digital time overlay at bottom
 *   - Sub-dial widget (shows highest priority popup service)
 */
@Composable
fun SkeuomorphicClockFace(
    modifier: Modifier = Modifier,
    size: Dp = 420.dp,
    queueItems: List<PopupServiceItem> = emptyList(),
    hourlyChimeEnabled: Boolean = false,
    onAlarmClick: () -> Unit = {},
    onFocusClick: () -> Unit = {},
    onTimerClick: () -> Unit = {},
    onChimeClick: () -> Unit = {}
) {
    val isDark = RobotTheme.isDark

    // Live time
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            calendar = Calendar.getInstance()
            delay(1000)
        }
    }

    val hours = calendar.get(Calendar.HOUR)
    val minutes = calendar.get(Calendar.MINUTE)
    val seconds = calendar.get(Calendar.SECOND)

    val scaleRatio = (size / 420.dp).coerceIn(0.5f, 1.5f)

    // Resolve mapped items for sub-dial
    val subDialItems = queueItems.map { item ->
        val isActive = when (item.serviceType) {
            "ALARM" -> item.priority >= 60
            "TIMER", "FOCUS" -> true
            "CHIME" -> item.priority >= 90
            else -> false
        }
        PopupQueueWidgetItem(
            id = item.id,
            displayName = item.displayName,
            value = item.subDialValue,
            icon = item.subDialIcon ?: Icons.Outlined.Notifications,
            customIcon = item.subDialCustomIcon,
            color = item.getSubDialColor(),
            isActive = isActive
        )
    }

    Box(
        modifier = modifier.requiredSize(size),
        contentAlignment = Alignment.Center
    ) {
        // Clock face
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    brush = if (isDark) {
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                            center = Offset.Unspecified,
                            radius = Float.POSITIVE_INFINITY
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFF1F5F9)),
                            center = Offset.Unspecified,
                            radius = Float.POSITIVE_INFINITY
                        )
                    }
                )
                .border(
                    width = 20.dp * scaleRatio,
                    brush = Brush.linearGradient(
                        if (isDark) listOf(Color(0xFF334155), Color(0xFF0F172A))
                        else listOf(Color.White, Color(0xFFE2E8F0))
                    ),
                    shape = CircleShape
                )
                .insetShadow(
                    color = if (isDark) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.25f),
                    offsetY = 8.dp,
                    blurRadius = 30.dp,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Spacer(modifier = Modifier.size(0.dp))

            DigitalTimeWidget(
                timeStr = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), minutes),
                hourlyChimeEnabled = hourlyChimeEnabled,
                isDark = isDark,
                scaleRatio = scaleRatio * 0.8f,
                onClick = onChimeClick,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (size / 2) * 0.425f)
            )

            // Sub-dial widget (Placed underneath the Canvas so hands are drawn on top)
            SubDialWidget(
                items = subDialItems,
                scaleRatio = scaleRatio,
                onItemClick = { id ->
                    val clickedItem = queueItems.find { it.id == id }
                    if (clickedItem != null) {
                        clickedItem.subDialOnClick()
                        if (id.startsWith("alarm_")) {
                            onAlarmClick()
                        } else if (id.startsWith("timer_")) {
                            if (clickedItem.needsForegroundLock) onFocusClick() else onTimerClick()
                        } else if (id.startsWith("chime_") || id == "hourly_chime") {
                            onChimeClick()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (size / 2) * 0.425f)
            )

            // Hour markers + Hands (Top Layer — Canvas drawn)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.toPx() / 2f
                val centerY = size.toPx() / 2f
                val faceRadius = (this.size.minDimension / 2f) * 0.85f

                // Draw hour markers
                for (i in 0 until 12) {
                    val angle = (i * 30.0 - 90.0) * PI / 180.0
                    val markerLength = if (i % 3 == 0) faceRadius * 0.08f else faceRadius * 0.05f
                    val markerWidth = if (i % 3 == 0) 14f else 8f
                    val outerX = (centerX + cos(angle) * faceRadius).toFloat()
                    val outerY = (centerY + sin(angle) * faceRadius).toFloat()
                    val innerX = (centerX + cos(angle) * (faceRadius - markerLength)).toFloat()
                    val innerY = (centerY + sin(angle) * (faceRadius - markerLength)).toFloat()

                    drawLine(
                        color = if (isDark) Color(0xFF475569) else Color(0xFF64748B),
                        start = Offset(innerX, innerY),
                        end = Offset(outerX, outerY),
                        strokeWidth = markerWidth,
                        cap = StrokeCap.Round
                    )
                }

                // Hour hand
                val hourAngle = ((hours % 12) * 30.0 + minutes * 0.5 - 90.0) * PI / 180.0
                val hourLength = faceRadius * 0.5f
                drawLine(
                    color = if (isDark) ClockHandHour else Color(0xFF475569),
                    start = Offset(centerX, centerY),
                    end = Offset(
                        (centerX + cos(hourAngle) * hourLength).toFloat(),
                        (centerY + sin(hourAngle) * hourLength).toFloat()
                    ),
                    strokeWidth = 20f,
                    cap = StrokeCap.Round
                )

                // Minute hand
                val minuteAngle = (minutes * 6.0 - 90.0) * PI / 180.0
                val minuteLength = faceRadius * 0.75f
                drawLine(
                    color = if (isDark) ClockHandMinute else Color(0xFF334155),
                    start = Offset(centerX, centerY),
                    end = Offset(
                        (centerX + cos(minuteAngle) * minuteLength).toFloat(),
                        (centerY + sin(minuteAngle) * minuteLength).toFloat()
                    ),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )

                // Second hand
                val secondAngle = (seconds * 6.0 - 90.0) * PI / 180.0
                val secondLength = faceRadius * 0.85f
                drawLine(
                    color = ClockHandSecond,
                    start = Offset(centerX, centerY),
                    end = Offset(
                        (centerX + cos(secondAngle) * secondLength).toFloat(),
                        (centerY + sin(secondAngle) * secondLength).toFloat()
                    ),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )

                // Center pin
                drawCircle(
                    color = ClockCenterPin,
                    radius = 12f,
                    center = Offset(centerX, centerY)
                )
            }
        }

    }
}
