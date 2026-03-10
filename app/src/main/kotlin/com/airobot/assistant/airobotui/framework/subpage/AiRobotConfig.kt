package com.airobot.assistant.airobotui.framework.subpage

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
import com.airobot.assistant.airobotui.framework.comp.ConfigTextField
import com.airobot.assistant.airobotui.framework.theme.RobotPrimaryCyan
import com.airobot.assistant.airobotui.framework.theme.RobotTextSecondary
import com.airobot.assistant.airobotui.viewmodel.RobotMainViewModel

@Composable
fun AiRobotConfig(
    viewModel: RobotMainViewModel = hiltViewModel()
) {
    val aiAgent by viewModel.aiAgent.collectAsState()
    val isActivated by viewModel.isAiRobotActivated.collectAsState()
    
    // UI state for agent configuration
    var agentVendor by remember(aiAgent) { mutableStateOf(aiAgent.agentVendor) }
    var editedAgentUrl by remember(aiAgent) { mutableStateOf(aiAgent.agentUrl) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "智能体配置",
            color = RobotTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "AI智能体选择",
            value = agentVendor,
            onValueChange = {},
            readOnly = true
        )

        ConfigTextField(
            label = "智能体服务地址",
            value = editedAgentUrl,
            onValueChange = { if (!isActivated) editedAgentUrl = it },
            readOnly = isActivated
        )

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "智能体激活状态 (自动下发)",
            color = RobotTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "激活凭证 (Activation Code)",
            value = aiAgent.activationCode,
            onValueChange = {},
            readOnly = true
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
        
        if (isActivated) {
            Text(
                "智能体已就绪，当前智能体: ${aiAgent.agentVendor}",
                color = RobotPrimaryCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.configureAndActivateAiAgent(editedAgentUrl,
                    agentVendor) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isActivated,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActivated) Color.Gray else RobotPrimaryCyan,
                disabledContainerColor = Color.Gray,
                disabledContentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (isActivated) "Ai智能体已激活" else "保存配置并激活",
                color = Color.White, 
                fontWeight = FontWeight.Bold
            )
        }
    }
}

