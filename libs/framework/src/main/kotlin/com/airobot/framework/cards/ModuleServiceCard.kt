package com.airobot.framework.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme

@Composable
fun ModuleServiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconColor: Color = RobotTheme.colors.accent,
    iconBgColor: Color = RobotTheme.colors.accent.copy(alpha = 0.1f),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    showSubtitle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement
    ) {
        // Unified Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Module Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = iconColor
                    )
                }

                Text(
                    text = title,
                    color = RobotTheme.colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
            }

            if (showSubtitle) {
                Text(
                    text = subtitle,
                    color = RobotTheme.colors.textMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Card Content
        content()
    }
}
