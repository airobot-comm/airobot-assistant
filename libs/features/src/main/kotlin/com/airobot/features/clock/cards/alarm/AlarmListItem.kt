package com.airobot.features.clock.cards.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.R
import com.airobot.features.clock.cards.widgets.AiListItem
import com.airobot.features.clock.cards.widgets.AiSettingDropdown
import com.airobot.features.clock.cards.widgets.AiSettingGearButton
import com.airobot.features.clock.cards.widgets.AiSettingSegmentedButton
import com.airobot.features.clock.cards.widgets.AiSettingSwitch
import com.airobot.features.clock.cards.widgets.AiSettingTextField
import com.airobot.features.clock.cards.widgets.AiSettingTimeField
import com.airobot.features.clock.data.model.AlarmItem
import com.airobot.framework.theme.RobotTheme

/**
 * Single alarm item row with compact 2-row expand layout matching prototype.
 *
 * Layout (matches prototype AlarmView.tsx):
 *
 * Collapsed:
 *   [icon]  [08:30]          [subtitle text]       [toggle][gear]
 *
 * Expanded — title and subtitle positions are UNCHANGED, expand adds only 1 extra row:
 *   [icon]  [08:30][温柔][标准][急切]               [toggle][gear]
 *           [工作早起 ────────────────────────────]
 *   ─────────────────────────────────────────────────────────────
 *           [唤醒次数 3次▾] [间隔时间 5分钟▾] [叫名字关闭]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListItem(
    alarm: AlarmItem,
    isDark: Boolean,
    typeLabels: Map<String, String>,
    isEditing: Boolean,
    onToggle: () -> Unit,
    onUpdate: (AlarmItem) -> Unit,
    onEditClick: () -> Unit,
    onItemClick: () -> Unit
) {
    val accentColor = RobotTheme.colors.alarmAccent

    AiListItem(
        isDark = isDark,
        isEditing = isEditing,
        accentColor = accentColor,
        onItemClick = onItemClick,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (alarm.enabled) RobotTheme.colors.accent
                else RobotTheme.colors.textMuted
            )
        },
        title = {
            if (isEditing) {
                // Title row in edit mode: time field + compact voice mode chips — SAME position as time text
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AiSettingTimeField(
                        value = alarm.time,
                        onValueChange = { onUpdate(alarm.copy(time = it, enabled = true)) },
                        accentColor = accentColor,
                        showUnderline = false,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                    AiSettingSegmentedButton(
                        options = listOf(
                            "hint" to stringResource(R.string.alarm_voice_mode_hint),
                            "standard" to stringResource(R.string.alarm_voice_mode_standard),
                            "urgent" to stringResource(R.string.alarm_voice_mode_urgent)
                        ),
                        selectedId = alarm.voiceMode,
                        onSelect = { newVoiceMode ->
                            val defaultDismissMode = when (newVoiceMode) {
                                "hint" -> "auto"
                                "standard" -> "voice"
                                "urgent" -> "manual"
                                else -> "manual"
                            }
                            onUpdate(alarm.copy(voiceMode = newVoiceMode, dismissMode = defaultDismissMode, enabled = true))
                        },
                        accentColor = accentColor,
                        compact = true
                    )
                }
            } else {
                // Collapsed: plain time text
                Text(
                    text = alarm.time,
                    color = if (alarm.enabled) RobotTheme.colors.textPrimary
                    else RobotTheme.colors.textMuted,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
            }
        },
        subtitle = {
            if (isEditing) {
                // Subtitle row in edit mode: editable label field — offset to align with segmented button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(76.dp))
                    AiSettingTextField(
                        value = alarm.label,
                        onValueChange = { onUpdate(alarm.copy(label = it)) },
                        placeholder = stringResource(R.string.alarm_label_placeholder),
                        modifier = Modifier.weight(1f),
                        accentColor = accentColor
                    )
                }
            } else {


                // Collapsed: info summary text
                val voiceModeStr = when (alarm.voiceMode) {
                    "hint" -> stringResource(R.string.alarm_voice_mode_hint)
                    "urgent" -> stringResource(R.string.alarm_voice_mode_urgent)
                    else -> stringResource(R.string.alarm_voice_mode_standard)
                }
                val dismissModeStr = when (alarm.dismissMode) {
                    "voice" -> stringResource(R.string.alarm_dismiss_mode_voice)
                    "auto" -> stringResource(R.string.alarm_dismiss_mode_auto)
                    else -> stringResource(R.string.alarm_dismiss_mode_manual)
                }
                val modeStr =
                    stringResource(R.string.alarm_mode_format, voiceModeStr, dismissModeStr)
                val repeatStr =
                    typeLabels[alarm.type] ?: stringResource(R.string.alarm_repeat_everyday)

                Text(
                    text = "${alarm.label} • $repeatStr • $modeStr",
                    color = RobotTheme.colors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        rightContent = {
            AiSettingSwitch(
                checked = alarm.enabled,
                onCheckedChange = { onToggle() },
                accentColor = RobotTheme.colors.accent
            )
            AiSettingGearButton(
                isEditing = isEditing,
                onClick = onEditClick,
                accentColor = accentColor,
                icon = Icons.Outlined.Settings
            )
        },
        expandedContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (alarm.dismissMode == "auto") {
                    val autoDismissItems = listOf(
                        5 to stringResource(R.string.alarm_time_seconds_unit, 5),
                        10 to stringResource(R.string.alarm_time_seconds_unit, 10),
                        15 to stringResource(R.string.alarm_time_seconds_unit, 15),
                        30 to stringResource(R.string.alarm_time_seconds_unit, 30)
                    )
                    AiSettingDropdown(
                        label = stringResource(R.string.alarm_expanded_auto_dismiss),
                        selectedValueLabel = stringResource(
                            R.string.alarm_time_seconds_unit,
                            alarm.autoDismissSeconds
                        ),
                        items = autoDismissItems,
                        itemLabel = { it.second },
                        onItemSelected = { onUpdate(alarm.copy(autoDismissSeconds = it.first)) }
                    )
                } else {
                    val repeatCountItems = listOf(
                        1 to stringResource(R.string.alarm_time_times_unit, 1),
                        2 to stringResource(R.string.alarm_time_times_unit, 2),
                        3 to stringResource(R.string.alarm_time_times_unit, 3),
                        4 to stringResource(R.string.alarm_time_times_unit, 4),
                        5 to stringResource(R.string.alarm_time_times_unit, 5)
                    )
                    AiSettingDropdown(
                        label = stringResource(R.string.alarm_expanded_repeat_count),
                        selectedValueLabel = stringResource(
                            R.string.alarm_time_times_unit,
                            alarm.repeatCount
                        ),
                        items = repeatCountItems,
                        itemLabel = { it.second },
                        onItemSelected = { onUpdate(alarm.copy(repeatCount = it.first)) }
                    )

                    val intervalItems = listOf(
                        1 to stringResource(R.string.alarm_time_minutes_unit, 1),
                        3 to stringResource(R.string.alarm_time_minutes_unit, 3),
                        5 to stringResource(R.string.alarm_time_minutes_unit, 5),
                        10 to stringResource(R.string.alarm_time_minutes_unit, 10),
                        15 to stringResource(R.string.alarm_time_minutes_unit, 15)
                    )
                    AiSettingDropdown(
                        label = stringResource(R.string.alarm_expanded_interval),
                        selectedValueLabel = stringResource(
                            R.string.alarm_time_minutes_unit,
                            alarm.interval
                        ),
                        items = intervalItems,
                        itemLabel = { it.second },
                        onItemSelected = { onUpdate(alarm.copy(interval = it.first)) }
                    )
                }

                val dismissModeItems = listOf(
                    "manual" to stringResource(R.string.alarm_dismiss_mode_manual),
                    "voice" to stringResource(R.string.alarm_dismiss_mode_voice),
                    "auto" to stringResource(R.string.alarm_dismiss_mode_auto)
                )
                AiSettingDropdown(
                    label = stringResource(R.string.alarm_expanded_dismiss_mode),
                    selectedValueLabel = when (alarm.dismissMode) {
                        "voice" -> stringResource(R.string.alarm_dismiss_mode_voice)
                        "auto" -> stringResource(R.string.alarm_dismiss_mode_auto)
                        else -> stringResource(R.string.alarm_dismiss_mode_manual)
                    },
                    items = dismissModeItems,
                    itemLabel = { it.second },
                    onItemSelected = { onUpdate(alarm.copy(dismissMode = it.first)) }
                )
            }
        }
    )
}
