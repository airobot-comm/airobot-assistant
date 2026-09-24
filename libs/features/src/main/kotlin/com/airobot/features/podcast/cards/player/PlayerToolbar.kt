package com.airobot.features.podcast.cards.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airobot.framework.theme.RobotTheme

/**
 * Reusable NotebookLM-style feature toolbar row with 4 buttons.
 */
@Composable
fun PlayerToolbar(
    onWakeupAirobot: () -> Unit,
    onPlaceholderClick: (featureName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark
    val iconTint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val btnBg = if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color.White
    val btnBorder = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. 音频概要 (Waveform / GraphicEq)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(btnBg)
                .border(1.dp, btnBorder, CircleShape)
                .clickable {
                    onPlaceholderClick("语音概要")
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.GraphicEq,
                contentDescription = "语音概要",
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
        }

        // 2. 知识卡片 (Layers)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(btnBg)
                .border(1.dp, btnBorder, CircleShape)
                .clickable {
                    onPlaceholderClick("知识卡片")
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Layers,
                contentDescription = "知识卡片",
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
        }

        // 3. 答题卡 (Checklist / Assignment)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(btnBg)
                .border(1.dp, btnBorder, CircleShape)
                .clickable {
                    onPlaceholderClick("答题卡")
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Assignment,
                contentDescription = "答题卡",
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
        }

        // 4. 对话 (ChatBubbleOutline)
        val chatActiveTint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
        val chatActiveBg = if (isDark) Color(0xFF312E81).copy(alpha = 0.5f) else Color(0xFFEEF2FF)
        val chatActiveBorder = if (isDark) Color(0xFF4338CA).copy(alpha = 0.4f) else Color(0xFFC7D2FE)

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(chatActiveBg)
                .border(1.dp, chatActiveBorder, CircleShape)
                .clickable {
                    onWakeupAirobot()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = "对话",
                tint = chatActiveTint,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
