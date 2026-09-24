package com.airobot.assistant.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airobot.assistant.viewmodel.MainShellViewModel
import com.airobot.framework.comp.ConfigTextField
import com.airobot.framework.theme.RobotTheme

/**
 * 角色管理配置页面
 * 展示当前角色的切换 Tab 按钮，以及对应的动画引擎、语音模型、唤醒词等配置。
 */
@Composable
fun RoleConfig(
    viewModel: MainShellViewModel = hiltViewModel()
) {
    val roles by viewModel.allCharacters.collectAsState(initial = emptyList())
    val activeRole by viewModel.activeCharacter.collectAsState(initial = null)
    val activeIndex =
        roles.indexOfFirst { it.roleName == activeRole?.roleName }.takeIf { it >= 0 } ?: 0

    // Local cached state for the custom wake word text
    var wakeWordText by remember(activeRole) { mutableStateOf(activeRole?.wakeWord ?: "") }

    // Observe validation result to automatically revert the local state on failure
    LaunchedEffect(Unit) {
        viewModel.wakeWordValidationResult.collect { success ->
            if (!success) {
                wakeWordText = activeRole?.wakeWord ?: ""
            }
        }
    }

    // Save when screen is disposed
    val currentWakeWordText by rememberUpdatedState(wakeWordText)
    DisposableEffect(Unit) {
        onDispose {
            val sanitized = currentWakeWordText.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "")
            viewModel.updateWakeWord(sanitized)
        }
    }

    val localFocusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                localFocusManager.clearFocus()
            },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // —— 当前角色 ——
        Text(
            "当前角色",
            color = RobotTheme.colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        // Pill-segmented Tab Row for switching roles (maximum 4)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RobotTheme.colors.cardBg)
                .border(1.dp, RobotTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            roles.forEachIndexed { index, robot ->
                val isSelected = activeIndex == index
                val bgSelectedColor = RobotTheme.colors.accent.copy(alpha = 0.15f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) bgSelectedColor else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable {
                            if (!isSelected) {
                                // Clear focus so keyboard collapses and saves
                                localFocusManager.clearFocus()
                                // Save the current wakeWord before switching
                                val sanitized =
                                    wakeWordText.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "")
                                viewModel.updateWakeWord(sanitized)
                                viewModel.updateActiveRoleIndex(index)
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = robot.roleName,
                        color = if (isSelected) RobotTheme.colors.accent else RobotTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // —— 角色详情配置 ——
        val currentRole = activeRole
        val engineDisplay = when (currentRole?.characterType) {
            "ANDROID_CANVAS" -> "原生 Canvas 动画 (${currentRole.roleName})"
            "RIVE_IP" -> "Rive 引擎动画 (${currentRole.roleName})"
            else -> currentRole?.characterType ?: "未知引擎"
        }

        ConfigTextField(
            label = "动画引擎",
            value = engineDisplay,
            onValueChange = {},
            readOnly = true
        )

        ConfigTextField(
            label = "性格特征",
            value = activeRole?.personality ?: "无",
            onValueChange = {},
            readOnly = true
        )

        ConfigTextField(
            label = "语音模型",
            value = activeRole?.voiceModel ?: "火山模型",
            onValueChange = {},
            readOnly = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ConfigTextField(
                    label = "别名",
                    value = activeRole?.alias ?: "无",
                    onValueChange = {},
                    readOnly = true
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                var isFocused by remember { mutableStateOf(false) }
                ConfigTextField(
                    label = "唤醒词",
                    value = wakeWordText,
                    onValueChange = { newWakeWord ->
                        // Filter out all special characters, keeping only letters, digits, and Chinese characters
                        // Limit to maximum 4 characters
                        val sanitized =
                            newWakeWord.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "")
                        wakeWordText = sanitized.take(4)
                    },
                    modifier = Modifier.onFocusChanged { focusState ->
                        if (isFocused && !focusState.hasFocus) {
                            val sanitized =
                                wakeWordText.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "")
                            viewModel.updateWakeWord(sanitized)
                        }
                        isFocused = focusState.hasFocus
                    },
                    readOnly = false
                )
            }
        }

        // —— 底部提示 ——
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RobotTheme.colors.accent.copy(alpha = 0.10f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "提示",
                    tint = RobotTheme.colors.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "切换角色后，系统形象与交互风格将自动切换。别名为默认唤醒词，您也可以配置修改默认的唤醒词。",
                    color = RobotTheme.colors.textPrimary.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
