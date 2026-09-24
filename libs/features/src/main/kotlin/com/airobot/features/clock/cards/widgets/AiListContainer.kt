package com.airobot.features.clock.cards.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airobot.framework.theme.RobotTheme

/**
 * A reusable AI-styled list container with a rounded semi-transparent background.
 * Matches the AIRobot skeuomorphic card design language.
 * Height limit increased to accommodate 3–4 items without scrolling in typical card sizes.
 */
@Composable
fun AiListContainer(
    modifier: Modifier = Modifier,
    maxHeight: Dp = 480.dp,
    content: LazyListScope.() -> Unit
) {
    val isDark = RobotTheme.isDark
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(48.dp))
            .background(
                if (isDark) Color.White.copy(alpha = 0.05f)
                else Color(0xFFF8FAFC)
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(48.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}
