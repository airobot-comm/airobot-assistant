package com.airobot.features.podcast.cards.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme

/**
 * Pill button representing a category filter in the Podcast Library card.
 */
@Composable
fun LibraryFilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) {
                    if (isDark) Color(0xFF6366F1) else Color(0xFF4F46E5)
                } else {
                    if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else RobotTheme.colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}
