package com.airobotcomm.tablet.airobotui.subpages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobotcomm.tablet.airobotui.framework.drawer.ConfigTextField
import com.airobotcomm.tablet.airobotui.framework.theme.RobotPrimaryCyan
import com.airobotcomm.tablet.airobotui.framework.theme.RobotTextSecondary
import com.airobotcomm.tablet.domain.model.DeviceConfig

@Composable
fun SystemAuthPage(
    config: DeviceConfig,
    onConfigChange: (DeviceConfig) -> Unit
) {
    var editedConfig by remember(config) { mutableStateOf(config) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "系统认证信息",
            color = RobotTextSecondary,
            fontSize = 14.sp
        )

        ConfigTextField(
            label = "设备 ID (UUID)",
            // 使用 orEmpty() 防御式编程，防止反序列化产生的 null
            value = (editedConfig.clientId as String?).orEmpty(),
            onValueChange = { editedConfig = editedConfig.copy(clientId = it) }
        )

        ConfigTextField(
            label = "MAC 地址",
            value = (editedConfig.deviceId as String?).orEmpty(),
            onValueChange = { editedConfig = editedConfig.copy(deviceId = it) },
            trailingIcon = {
                IconButton(onClick = {
                    val newMac = (1..6).joinToString(":") { "%02x".format((0..255).random()) }
                    editedConfig = editedConfig.copy(deviceId = newMac)
                }) {
                    Icon(Icons.Default.Refresh, "生成", tint = RobotPrimaryCyan)
                }
            }
        )

        ConfigTextField(
            label = "激活码",
            value = (editedConfig.activationCode as String?).orEmpty(),
            onValueChange = { editedConfig = editedConfig.copy(activationCode = it) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onConfigChange(editedConfig) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RobotPrimaryCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("更新认证信息", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
