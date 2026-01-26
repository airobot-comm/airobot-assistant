package com.airobotcomm.tablet.airobotui.framework.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobotcomm.tablet.airobotui.framework.theme.RobotBackgroundDark
import com.airobotcomm.tablet.airobotui.framework.theme.RobotPrimaryCyan
import com.airobotcomm.tablet.airobotui.framework.theme.RobotSurface
import com.airobotcomm.tablet.airobotui.framework.theme.RobotTextPrimary
import com.airobotcomm.tablet.domain.ota.model.DeviceConfig
import com.airobotcomm.tablet.airobotui.subpages.ServiceConfigPage
import com.airobotcomm.tablet.airobotui.subpages.SystemAuthPage

/**
 * 侧边栏菜单内容
 * 扩大显示区域 (640dp，约占据主流平板半屏)
 */
@Composable
fun RobotDrawerContent(
    currentConfig: DeviceConfig,
    onConfigChange: (DeviceConfig) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: 服务配置, 1: 系统认证

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(640.dp), // 扩大到半屏左右
        color = RobotBackgroundDark,
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 左侧：菜单选项 (保持窄边)
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .background(RobotSurface.copy(alpha = 0.5f))
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DrawerMenuItem(
                    icon = Icons.Default.Settings,
                    label = "服务配置",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                DrawerMenuItem(
                    icon = Icons.Default.Lock,
                    label = "系统认证",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }

            // 右侧：子页面展示区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (selectedTab == 0) "服务配置" else "系统认证",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = RobotTextPrimary,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(width = 40.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(RobotPrimaryCyan)
                        )
                    }
                    
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = RobotTextPrimary.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // 统一风格的子页面容器
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (selectedTab == 0) {
                        ServiceConfigPage(
                            config = currentConfig,
                            onConfigChange = onConfigChange
                        )
                    } else {
                        SystemAuthPage(
                            config = currentConfig,
                            onConfigChange = onConfigChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) RobotPrimaryCyan.copy(alpha = 0.15f) else Color.Transparent
    val contentColor = if (isSelected) RobotPrimaryCyan else RobotTextPrimary.copy(alpha = 0.4f)

    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}