package com.airobot.tablet.airobotui.framework.statusbar

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
import com.airobot.tablet.R
import com.airobot.tablet.airobotui.state.RobotState
import com.airobot.tablet.airobotui.framework.theme.RobotTheme
import com.airobot.tablet.airobotui.framework.theme.StatusRed
import com.airobot.tablet.airobotui.framework.theme.StatusAmber
import com.airobot.tablet.airobotui.framework.theme.StatusEmerald
import com.airobot.tablet.airobotui.framework.theme.StatusCyan

/**
 * 集中化 TopBar 组件
 */
@Composable
fun RobotTopBar(
    robotState: RobotState,
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

            // 机器人一级状态 Badge
            RobotStateBadge(robotState = robotState)
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
 * 机器人状态标识组件
 */
@Composable
private fun RobotStateBadge(robotState: RobotState) {
    val stateText = when (robotState) {
        is RobotState.Offline -> "OFFLINE"
        is RobotState.Initializing -> "INITIALIZING"
        is RobotState.Unauthorized -> "UNAUTHORIZED"
        is RobotState.Connecting -> "CONNECTING"
        is RobotState.Ready -> "READY"
        is RobotState.Conversation -> "CONVERSATION"
        is RobotState.FunctionService -> "SERVICE MODE"
    }
    
    val stateColor = when (robotState) {
        is RobotState.Offline -> StatusRed
        is RobotState.Initializing -> StatusAmber
        is RobotState.Unauthorized -> StatusRed
        is RobotState.Connecting -> StatusCyan
        is RobotState.Ready -> StatusCyan
        else -> StatusEmerald
    }

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


