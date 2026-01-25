package com.airobotcomm.tablet.airobotui.subpages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airobotcomm.tablet.airobotui.framework.ConfigTextField
import com.airobotcomm.tablet.domain.model.DeviceConfig
import com.airobotcomm.tablet.airobotui.theme.*

@Composable
fun ServiceConfigPage(
    config: DeviceConfig,
    onConfigChange: (DeviceConfig) -> Unit
) {
    var editedConfig by remember(config) { mutableStateOf(config) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ConfigTextField(
            label = "设备名称",
            value = editedConfig.name,
            onValueChange = { editedConfig = editedConfig.copy(name = it) }
        )

        ConfigTextField(
            label = "OTA 地址",
            value = editedConfig.otaUrl,
            onValueChange = { editedConfig = editedConfig.copy(otaUrl = it) }
        )

        ConfigTextField(
            label = "WSS 地址",
            value = editedConfig.websocketUrl,
            onValueChange = { editedConfig = editedConfig.copy(websocketUrl = it) }
        )

        ConfigTextField(
            label = "Token",
            value = editedConfig.token,
            onValueChange = { editedConfig = editedConfig.copy(token = it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onConfigChange(editedConfig) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RobotPrimaryCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("保存配置", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}