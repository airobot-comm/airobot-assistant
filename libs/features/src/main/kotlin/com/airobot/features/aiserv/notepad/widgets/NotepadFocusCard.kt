package com.airobot.features.aiserv.notepad.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme

@Composable
fun NotepadFocusCard(
    item: NotepadItem.FocusCard,
    isDark: Boolean,
    dividerColor: Color,
    formatTimestamp: (Long) -> String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.02f) else Color.White)
            .border(
                BorderStroke(1.dp, if (isDark) Color.Transparent else Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                // Left-border indigo accent
                drawLine(
                    color = Color(0xFF6366F1),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 4.dp.toPx()
                )
            }
            .padding(start = 14.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "专注领域：${item.task}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF3730A3)
                    )
                }
                Text(
                    text = "${item.duration / 60}m",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF6366F1)
                )
            }
            Text(
                text = "${formatTimestamp(item.timestamp)} · 专注时钟",
                fontSize = 9.sp,
                color = RobotTheme.colors.textMuted,
                fontWeight = FontWeight.Bold
            )
            if (item.insight.isNotEmpty()) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = dividerColor.copy(alpha = 0.5f)
                )
                Text(
                    text = "Aether 洞察：\"${item.insight}\"",
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
