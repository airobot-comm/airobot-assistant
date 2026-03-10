package com.airobot.assistant.airobotui.framework.subpage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.assistant.airobotui.framework.comp.ConfigTextField
import com.airobot.assistant.airobotui.framework.theme.RobotPrimaryCyan
import com.airobot.assistant.airobotui.framework.theme.RobotSecondaryIndigo
import com.airobot.assistant.airobotui.framework.theme.RobotSurface
import com.airobot.assistant.airobotui.framework.theme.RobotTextPrimary
import com.airobot.assistant.airobotui.framework.theme.RobotTextSecondary

/**
 * 角色管理配置页面
 * 展示当前角色的形象模型、语音模型、唤醒词等固定配置
 */
@Composable
fun RoleConfig() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // ── 形象模型 ──
        Text(
            "形象模型",
            color = RobotTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "当前形象引擎",
            value = "Rive动画",
            onValueChange = {},
            readOnly = true
        )

        // ── 语音模型 ──
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "语音模型",
            color = RobotTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "当前语音模型",
            value = "火山模型",
            onValueChange = {},
            readOnly = true
        )

        // ── 唤醒词 ──
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "唤醒词",
            color = RobotTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        ConfigTextField(
            label = "唤醒词列表",
            value = "小叶，小安",
            onValueChange = {},
            readOnly = true
        )

        // ── 底部提示 ──
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RobotSecondaryIndigo.copy(alpha = 0.10f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "提示",
                    tint = RobotPrimaryCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "角色模型可修改功能已在新版本计划中，敬请期待。",
                    color = RobotTextPrimary.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
