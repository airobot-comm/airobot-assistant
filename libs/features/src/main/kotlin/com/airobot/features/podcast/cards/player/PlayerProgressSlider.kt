package com.airobot.features.podcast.cards.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme

/**
 * Reusable progress slider section (time labels + Slider).
 */
@Composable
fun PlayerProgressSlider(
    currentFormatted: String,
    totalFormatted: String,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentFormatted,
                color = RobotTheme.colors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = totalFormatted,
                color = RobotTheme.colors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = progress,
            onValueChange = onSeek,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = RobotTheme.colors.accent,
                activeTrackColor = RobotTheme.colors.accent,
                inactiveTrackColor = if (isDark) Color(0xFF3E4554) else Color(0xFFE2E8F0)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        )
    }
}
