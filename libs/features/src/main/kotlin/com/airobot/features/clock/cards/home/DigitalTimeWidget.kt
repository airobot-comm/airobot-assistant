package com.airobot.features.clock.cards.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.util.insetShadow

/**
 * Skeuomorphic recessed LCD digital clock display widget.
 * Features an inset shadow bezel and an active hourly chime indicator bell.
 */
@Composable
fun DigitalTimeWidget(
    timeStr: String,
    hourlyChimeEnabled: Boolean,
    isDark: Boolean,
    scaleRatio: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    // LCD background color: harmonized with the dial color scheme
    val lcdBgColor = if (isDark) {
        Color(0xFF0F172A) // Deep dark slate recess
    } else {
        Color(0xFFE2E8F0) // Soft light slate recess
    }

    // Bezel border color
    val lcdBorderColor = if (isDark) {
        Color(0xFF1E293B)
    } else {
        Color(0xFFCBD5E1)
    }

    // Monospace amber text color matching the existing monospace dial clock face style
    val textColor = Color(0xFFF97316).copy(alpha = 0.90f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp * scaleRatio))
            .background(lcdBgColor)
            .border(
                width = 1.dp * scaleRatio,
                color = lcdBorderColor,
                shape = RoundedCornerShape(8.dp * scaleRatio)
            )
            .insetShadow(
                color = if (isDark) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.25f),
                offsetX = 0.dp,
                offsetY = 2.dp * scaleRatio,
                blurRadius = 4.dp * scaleRatio,
                shape = RoundedCornerShape(8.dp * scaleRatio)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null, // pristine skeuomorphic touch feel without standard overlay ripples
                onClick = onClick
            )
            .padding(horizontal = 10.dp * scaleRatio, vertical = 5.dp * scaleRatio),
        contentAlignment = Alignment.Center
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timeStr,
                color = textColor,
                fontSize = 24.sp * scaleRatio,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(end = if (hourlyChimeEnabled) 8.dp * scaleRatio else 0.dp)
            )

            if (hourlyChimeEnabled) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Hourly Chime Enabled",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp * scaleRatio, y = (-2).dp * scaleRatio)
                        .size(10.dp * scaleRatio),
                    tint = textColor
                )
            }
        }
    }
}
