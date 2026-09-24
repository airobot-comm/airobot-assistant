package com.airobot.features.aiserv.notepad.widgets

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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotepadSummaryCard(
    item: NotepadItem.AiSummary,
    isDark: Boolean,
    formatTimestamp: (Long) -> String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDark) Color(0xFF78350F).copy(alpha = 0.15f) else Color(0xFFFEF3C7).copy(
                    alpha = 0.25f
                )
            )
            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E)
                    )
                }
                Text(
                    text = formatTimestamp(item.timestamp),
                    fontSize = 9.sp,
                    color = Color(0xFFF59E0B).copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFFF59E0B).copy(alpha = 0.15f)
            )
            Text(
                text = item.content,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFFDE68A) else Color(0xFF78350F)
            )
        }
    }
}
