package com.airobot.airbot.components.dialogue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme

/**
 * 用户输入气泡组件 - 增强设计版，单独的对话输入气泡
 */
@Composable
fun BubbleUserMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message.isNotBlank(),
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(
                        topStart = 32.dp,
                        topEnd = 32.dp,
                        bottomStart = 32.dp,
                        bottomEnd = 4.dp
                    ),
                    spotColor = Color.Black.copy(alpha = 0.05f)
                )
                .clip(
                    RoundedCornerShape(
                        topStart = 32.dp,
                        topEnd = 32.dp,
                        bottomStart = 32.dp,
                        bottomEnd = 4.dp
                    )
                )
                .background(
                    if (RobotTheme.isDark) {
                        Color(0xFFFFFAF0).copy(alpha = 0.1f) // 浅橙色调 (Dark mode)
                    } else {
                        Color(0xFFFFFAF0) // 浅橙色调 (Light mode)
                    }
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFFFDE68A).copy(alpha = 0.3f), // amber-200
                    shape = RoundedCornerShape(
                        topStart = 32.dp,
                        topEnd = 32.dp,
                        bottomStart = 32.dp,
                        bottomEnd = 4.dp
                    )
                )
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = message,
                color = RobotTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
