package com.airobotcomm.tablet.airobotui.subpage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airobotcomm.tablet.airobotui.framework.comp.ConfigTextField
import com.airobotcomm.tablet.airobotui.framework.theme.RobotPrimaryCyan
import com.airobotcomm.tablet.airobotui.viewmodel.RobotMainViewModel
import com.airobotcomm.tablet.system.model.AiRobot

@Composable
fun ServiceConfigPage(
    viewModel: RobotMainViewModel = hiltViewModel()
) {
    val config by viewModel.systemConfig.collectAsState()
    var editedConfig by remember(config) { mutableStateOf(config) }
    
    // Sync local state when config updates from VM
    LaunchedEffect(config) {
        editedConfig = config
    }
    
    // Helper to get the first robot or a default one
    val currentRobot = editedConfig.aiRobotArray.firstOrNull() ?: AiRobot()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ConfigTextField(
            label = "OTA 地址",
            value = editedConfig.otaUrl ?: "",
            onValueChange = { editedConfig = editedConfig.copy(otaUrl = it) }
        )

        ConfigTextField(
            label = "角色名称",
            value = currentRobot.roleName ?: "",
            onValueChange = { newName ->
                val newRobot = currentRobot.copy(roleName = newName)
                val newArray = editedConfig.aiRobotArray.clone()
                if (newArray.isNotEmpty()) {
                    newArray[0] = newRobot
                } else {
                    // Handle empty array case if needed, though usually initialized
                }
                editedConfig = editedConfig.copy(aiRobotArray = newArray)
            }
        )

        ConfigTextField(
            label = "角色 ID (UUID)",
            value = currentRobot.roleId ?: "",
            onValueChange = { newId ->
                val newRobot = currentRobot.copy(roleId = newId)
                val newArray = editedConfig.aiRobotArray.clone()
                if (newArray.isNotEmpty()) {
                    newArray[0] = newRobot
                    editedConfig = editedConfig.copy(aiRobotArray = newArray)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.updateConfig(editedConfig) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RobotPrimaryCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("保存配置", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
