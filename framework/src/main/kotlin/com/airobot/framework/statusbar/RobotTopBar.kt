package com.airobot.framework.statusbar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.R
import com.airobot.framework.theme.RobotTheme
import com.airobot.framework.theme.StatusRed
import com.airobot.framework.theme.StatusAmber
import com.airobot.framework.theme.StatusEmerald
import com.airobot.framework.theme.StatusCyan

/**
 * 集中化 TopBar 组件 (UI UI基础组件层)
 */
@Composable
fun RobotTopBar(
    stateText: String,
    stateColor: Color,
    errorMessage: String?,
    onLogoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：Logo 与 状态
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Aether Logo
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                RobotTheme.colors.accent,
                                RobotTheme.colors.accentBg
                            )
                        )
                    )
                    .clickable(onClick = onLogoClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.cloud_on),
                    contentDescription = "菜单",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
            }
            
            Text(
                text = "AETHER",
                color = RobotTheme.colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )

            // 状态 Badge
            RobotEngineStateBadge(stateText = stateText, stateColor = stateColor)
        }
        
        // 右侧：系统信息与设置
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SystemStatusBar(errorMessage = errorMessage)
        }
    }
}

/**
 * 状态标识组件
 */
@Composable
private fun RobotEngineStateBadge(stateText: String, stateColor: Color) {
    Surface(
        color = stateColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, stateColor.copy(alpha = 0.3f))
    ) {
        Text(
            text = stateText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = stateColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}


