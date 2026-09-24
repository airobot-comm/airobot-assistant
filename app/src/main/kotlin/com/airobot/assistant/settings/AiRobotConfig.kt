package com.airobot.assistant.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.airobot.assistant.R
import com.airobot.assistant.viewmodel.MainShellViewModel
import com.airobot.framework.comp.ConfigTextField
import com.airobot.framework.theme.RobotTheme

@Composable
fun AiRobotConfig(
    viewModel: MainShellViewModel = hiltViewModel()
) {
    val aiAgent by viewModel.aiAgent.collectAsState()
    val isActivated by viewModel.isAiRobotActivated.collectAsState()
    val isSpeechInterruptionEnabled by viewModel.isSpeechInterruptionEnabled.collectAsState()

    // UI state for agent configuration
    var agentVendor by remember(aiAgent) { mutableStateOf(aiAgent.agentVendor) }
    var editedAgentUrl by remember(aiAgent) { mutableStateOf(aiAgent.agentUrl) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            "智能体激活状态(自动下发)",
            color = RobotTheme.colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "激活凭证(Activation Code)",
            value = aiAgent.activationCode,
            onValueChange = {},
            readOnly = true
        )

        // Connection Credentials Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("WS 连接凭证:", color = RobotTheme.colors.textSecondary, fontSize = 14.sp)
            Text(
                if (aiAgent.commCredentials != null) "已下发凭证" else "尚未下发",
                color = if (aiAgent.commCredentials != null) RobotTheme.colors.accent else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.speech_interruption),
                    color = RobotTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(R.string.speech_interruption_desc),
                    color = RobotTheme.colors.textSecondary,
                    fontSize = 12.sp
                )
            }
            androidx.compose.material3.Switch(
                checked = isSpeechInterruptionEnabled,
                onCheckedChange = { viewModel.setSpeechInterruptionEnabled(it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = RobotTheme.colors.accent,
                    checkedTrackColor = RobotTheme.colors.accent.copy(alpha = 0.5f)
                )
            )
        }

        if (isActivated) {
            Text(
                "智能体已就绪，当前智能体: ${aiAgent.agentVendor}",
                color = RobotTheme.colors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.configureAndActivateAiAgent(
                    editedAgentUrl,
                    agentVendor
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isActivated,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActivated) Color.Gray else RobotTheme.colors.accent,
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
