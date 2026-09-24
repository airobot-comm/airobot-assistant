package com.airobot.features.podcast.cards.widgets

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom modifier drawing a vertical scrollbar matching the design.
 */
fun Modifier.verticalScrollbar(
    scrollValue: Int,
    maxValue: Int,
    isDark: Boolean,
    width: Dp = 6.dp,
    paddingRight: Dp = 4.dp
): Modifier = this.drawWithContent {
    drawContent()
    
    if (maxValue > 0) {
        val viewportHeight = size.height
        val totalHeight = viewportHeight + maxValue
        
        val rawScrollbarHeight = (viewportHeight / totalHeight) * viewportHeight
        val minThumbHeight = 24.dp.toPx()
        val maxThumbHeight = (viewportHeight * 0.25f).coerceAtLeast(32.dp.toPx())
        val thumbHeight = rawScrollbarHeight.coerceIn(minThumbHeight, maxThumbHeight)
        
        val scrollbarTop = (scrollValue.toFloat() / maxValue) * (viewportHeight - thumbHeight)
        
        val x = size.width - width.toPx() - paddingRight.toPx()
        
        val trackColor = if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFF1F5F9)
        val thumbColor = if (isDark) Color(0xFF64748B).copy(alpha = 0.8f) else Color(0xFF94A3B8)
        
        // Draw track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(x, 0f),
            size = Size(width.toPx(), viewportHeight),
            cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
        )
        
        // Draw thumb
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(x, scrollbarTop),
            size = Size(width.toPx(), thumbHeight),
            cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
        )
    }
}
