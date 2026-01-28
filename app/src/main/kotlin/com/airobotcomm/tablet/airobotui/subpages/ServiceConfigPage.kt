package com.airobotcomm.tablet.airobotui.subpages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airobotcomm.tablet.airobotui.framework.drawer.ConfigTextField
import com.airobotcomm.tablet.airobotui.framework.theme.RobotPrimaryCyan
import com.airobotcomm.tablet.system.model.SystemConfig

@Composable
fun ServiceConfigPage(
    config: SystemConfig,
    onConfigChange: (SystemConfig) -> Unit
) {
    var editedConfig by remember(config) { mutableStateOf(config) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ConfigTextField(
            label = "OTA 地址",
            value = editedConfig.otaUrl,
            onValueChange = { editedConfig = editedConfig.copy(otaUrl = it) }
        )

        ConfigTextField(
            label = "角色名称",
            value = editedConfig.roleName,
            onValueChange = { editedConfig = editedConfig.copy(roleName = it) }
        )

        ConfigTextField(
            label = "角色 ID (UUID)",
            value = editedConfig.roleId,
            onValueChange = { editedConfig = editedConfig.copy(roleId = it) }
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
