package com.airobotcomm.tablet.airobotui.subpage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airobotcomm.tablet.airobotui.framework.comp.ConfigTextField
import com.airobotcomm.tablet.airobotui.framework.theme.RobotPrimaryCyan
import com.airobotcomm.tablet.airobotui.framework.theme.RobotTextSecondary
import com.airobotcomm.tablet.airobotui.viewmodel.RobotMainViewModel
import com.airobotcomm.tablet.system.model.AiRobot

@Composable
fun AiRobotConfig(
    viewModel: RobotMainViewModel = hiltViewModel()
) {
    val systemInfo by viewModel.systemInfo.collectAsState()
    val aiAgent by viewModel.aiAgent.collectAsState()
    val isActivated by viewModel.isAiRobotActivated.collectAsState()
    
    // UI state for agent configuration
    var editedAgentUrl by remember(aiAgent) { mutableStateOf(aiAgent.agentUrl) }
    var editedModel by remember(aiAgent) { mutableStateOf(aiAgent.model) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "智能体配置",
            color = RobotTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "智能体服务地址",
            value = editedAgentUrl,
            onValueChange = { editedAgentUrl = it }
        )

        ConfigTextField(
            label = "AI 模型 (Model)",
            value = editedModel,
            onValueChange = { editedModel = it }
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "智能体激活状态 (OTA 自动下发)",
            color = RobotTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "激活凭证 (Activation Code)",
            value = aiAgent.activationCode.ifEmpty { "尚未生成" },
            onValueChange = {},
            // readOnly = true
        )

        // Connection Credentials Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("WS 连接凭证:", color = RobotTextSecondary, fontSize = 14.sp)
            Text(
                if (aiAgent.commCredentials != null) "已下发凭证" else "尚未下发",
                color = if (aiAgent.commCredentials != null) RobotPrimaryCyan else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.configureAndActivateAiAgent(editedAgentUrl, editedModel) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RobotPrimaryCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("保存配置并启动激活", color = Color.White, fontWeight = FontWeight.Bold)
        }

        if (isActivated) {
            Text(
                "智能体已就绪，当前模型: ${aiAgent.model}",
                color = RobotPrimaryCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
