package com.airobot.framework.comp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme
import com.airobot.framework.theme.StatusAmber
import com.airobot.framework.theme.StatusEmerald
import com.airobot.framework.theme.StatusRed

/**
 * TopAlertSeverity — enum representing the severity level of the alert
 */
enum class TopAlertSeverity {
    INFO, WARNING, ERROR
}

/**
 * TopAlertBanner — A premium top-aligned notification banner.
 * Custom styled based on severity, complete with animations and border accents.
 */
@Composable
fun TopAlertBanner(
    visible: Boolean,
    message: String,
    severity: TopAlertSeverity,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        val (bgColor, borderColor, icon, iconColor) = when (severity) {
            TopAlertSeverity.INFO -> Quadruple(
                if (RobotTheme.isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                if (RobotTheme.isDark) Color(0xFF3B82F6).copy(alpha = 0.3f) else Color(0xFFBFDBFE),
                Icons.Default.CheckCircle,
                StatusEmerald
            )
            TopAlertSeverity.WARNING -> Quadruple(
                if (RobotTheme.isDark) Color(0xFF2E251B) else Color(0xFFFFFBEB),
                if (RobotTheme.isDark) StatusAmber.copy(alpha = 0.3f) else Color(0xFFFDE68A),
                Icons.Default.Warning,
                StatusAmber
            )
            TopAlertSeverity.ERROR -> Quadruple(
                if (RobotTheme.isDark) Color(0xFF2D1B1B) else Color(0xFFFEF2F2),
                if (RobotTheme.isDark) StatusRed.copy(alpha = 0.3f) else Color(0xFFFCA5A5),
                Icons.Default.Error,
                StatusRed
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(12.dp))
                    .widthIn(max = 480.dp)
                    .background(bgColor, RoundedCornerShape(12.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = severity.name,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
                    color = RobotTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

private data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
