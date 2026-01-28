package com.airobotcomm.tablet.airobotui.subpages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.airobotcomm.tablet.system.model.SystemConfig

@Composable
fun SystemAuthPage(
    deviceId: String,
    macAddress: String,
    config: SystemConfig,
    onConfigChange: (SystemConfig) -> Unit
) {
    var editedConfig by remember(config) { mutableStateOf(config) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "系统设备信息 (只读)",
            color = RobotTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "设备 ID (Android ID)",
            value = deviceId,
            onValueChange = {},
        )

        ConfigTextField(
            label = "MAC 地址",
            value = macAddress,
            onValueChange = {},
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "认证信息",
            color = RobotTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "激活码",
            value = editedConfig.activationCode,
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
