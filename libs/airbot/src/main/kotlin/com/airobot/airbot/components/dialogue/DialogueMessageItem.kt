package com.airobot.airbot.components.dialogue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.airobot.agent.brain.model.Message
import com.airobot.agent.brain.model.MessageRole
import com.airobot.framework.theme.RobotTheme

/**
 * Aether 对话流消息项 - 极致拟物面板版
 */
@Composable
internal fun DialogueMessageItem(
    message: Message,
    modifier: Modifier = Modifier,
    isDark: Boolean = RobotTheme.isDark,
    scaleRatio: Float = 1.0f
) {
    if (message.role == MessageRole.SYSTEM) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp * scaleRatio),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp * scaleRatio))
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.06f)
                        else Color.Black.copy(alpha = 0.04f)
                    )
                    .padding(horizontal = 10.dp * scaleRatio, vertical = 4.dp * scaleRatio)
            ) {
                Text(
                    text = message.content,
                    color = if (isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.45f),
                    fontSize = 10.sp * scaleRatio,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        return
    }

    val isAgent = message.role == MessageRole.AGENT

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp * scaleRatio),
        horizontalAlignment = if (isAgent) Alignment.Start else Alignment.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp * scaleRatio)
                .shadow(
                    elevation = if (isAgent) 8.dp else 4.dp,
                    shape = RoundedCornerShape(24.dp * scaleRatio),
                    spotColor = Color.Black.copy(alpha = 0.05f)
                )
                .clip(RoundedCornerShape(24.dp * scaleRatio))
                .background(
                    if (isAgent) {
                        if (isDark) Color.White.copy(alpha = 0.08f) else Color.White
                    } else {
                        // User message color from prototype: light cream/orange
                        if (isDark) Color(0xFFFFFAF0).copy(alpha = 0.15f) else Color(0xFFFFFAF0)
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isAgent) {
                        if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
                    } else {
                        Color(0xFFFDE68A).copy(alpha = 0.4f)
                    },
                    shape = RoundedCornerShape(24.dp * scaleRatio)
                )
                .padding(horizontal = 14.dp * scaleRatio, vertical = 9.dp * scaleRatio)
        ) {
            if (isAgent) {
                TypewriterText(
                    text = message.content,
                    speed = 30L,
                    style = TextStyle(
                        color = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xFF1E293B),
                        fontSize = 12.5.sp * scaleRatio,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp * scaleRatio
                    )
                )
            } else {
                Text(
                    text = message.content,
                    color = if (isDark) Color.White.copy(alpha = 0.95f) else Color(0xFF92400E),
                    fontSize = 12.5.sp * scaleRatio,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp * scaleRatio
                )
            }
        }
    }
}
