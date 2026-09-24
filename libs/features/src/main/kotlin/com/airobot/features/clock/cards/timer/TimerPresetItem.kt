package com.airobot.features.clock.cards.timer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.R
import com.airobot.features.clock.cards.widgets.AiListItem
import com.airobot.features.clock.cards.widgets.AiSettingDropdown
import com.airobot.features.clock.cards.widgets.AiSettingDurationField
import com.airobot.features.clock.cards.widgets.AiSettingGearButton
import com.airobot.features.clock.cards.widgets.AiSettingSegmentedButton
import com.airobot.features.clock.cards.widgets.AiSettingSwitch
import com.airobot.features.clock.cards.widgets.AiSettingTextField
import com.airobot.features.clock.data.model.PresetItem
import com.airobot.features.clock.data.model.TimerMode
import com.airobot.framework.theme.RobotTheme

/**
 * Single timer/focus preset row with metaphorical setting support.
 * Optimized compact layout matching prototype.
 */
@Composable
fun TimerPresetItem(
    preset: PresetItem,
    isDark: Boolean,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onUpdate: (PresetItem) -> Unit,
    onClick: () -> Unit
) {
    val isFocusMode = preset.mode == TimerMode.FOCUS

    val accentColor =
        if (isFocusMode) RobotTheme.colors.focusAccent else RobotTheme.colors.timerAccent

    val musicLabel = when (preset.bgMusic) {
        "nature" -> stringResource(R.string.timer_music_nature)
        "lofi" -> stringResource(R.string.timer_music_lofi)
        "piano" -> stringResource(R.string.timer_music_piano)
        "cyberpunk" -> stringResource(R.string.timer_music_cyberpunk)
        "library" -> stringResource(R.string.timer_music_library)
        "zen" -> stringResource(R.string.timer_music_zen)
        "tick" -> stringResource(R.string.timer_music_tick)
        else -> preset.bgMusic.ifEmpty {
            if (isFocusMode) stringResource(R.string.timer_music_nature)
            else stringResource(R.string.timer_music_tick)
        }
    }

    AiListItem(
        isDark = isDark,
        isEditing = isEditing,
        accentColor = accentColor,
        onItemClick = onClick,
        icon = {
            Icon(
                imageVector = if (isFocusMode) Icons.Outlined.Psychology
                else Icons.Outlined.HourglassEmpty,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = accentColor
            )
        },
        title = {
            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AiSettingTextField(
                        value = preset.label,
                        onValueChange = { onUpdate(preset.copy(label = it)) },
                        placeholder = stringResource(R.string.timer_preset_name_placeholder),
                        accentColor = accentColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        showUnderline = false,
                        modifier = Modifier.weight(1f)
                    )

                    AiSettingSegmentedButton(
                        options = listOf(
                            "COUNTDOWN" to stringResource(R.string.timer_mode_countdown),
                            "FOCUS" to stringResource(R.string.timer_mode_focus)
                        ),
                        selectedId = preset.mode.name,
                        onSelect = { selectedName ->
                            val newMode = TimerMode.valueOf(selectedName)
                            onUpdate(preset.copy(mode = newMode))
                        },
                        accentColor = accentColor,
                        compact = true,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else {
                Text(
                    text = preset.label,
                    color = RobotTheme.colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
            }
        },
        subtitle = {
            if (!isEditing) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.timer_preset_duration_minutes,
                            preset.seconds / 60
                        ),
                        color = RobotTheme.colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (preset.reminderInterval > 0) {
                        Text(
                            text = stringResource(
                                R.string.timer_preset_reminder_interval,
                                preset.reminderInterval / 60
                            ),
                            color = RobotTheme.colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    if (preset.musicEnabled) {
                        Text(
                            text = stringResource(R.string.timer_preset_music_label, musicLabel),
                            color = RobotTheme.colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        },
        rightContent = {
            AiSettingGearButton(
                isEditing = isEditing,
                onClick = onEditClick,
                accentColor = accentColor,
                icon = Icons.Outlined.Settings
            )
        },
        expandedContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.timer_preset_duration_title),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RobotTheme.colors.textMuted
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        AiSettingDurationField(
                            valueMinutes = preset.seconds / 60,
                            onValueChange = { min ->
                                onUpdate(preset.copy(seconds = min * 60))
                            },
                            accentColor = accentColor,
                            showUnderline = false,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.timer_preset_minutes_unit),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = RobotTheme.colors.textPrimary
                        )
                    }
                }

                AiSettingDropdown(
                    label = stringResource(R.string.timer_preset_reminder_label),
                    selectedValueLabel = if (preset.reminderInterval == 0) stringResource(R.string.timer_preset_reminder_none) else stringResource(
                        R.string.timer_preset_reminder_minutes, preset.reminderInterval / 60
                    ),
                    items = listOf(
                        0 to stringResource(R.string.timer_preset_reminder_none),
                        300 to stringResource(R.string.timer_preset_reminder_minutes, 5),
                        600 to stringResource(R.string.timer_preset_reminder_minutes, 10),
                        900 to stringResource(R.string.timer_preset_reminder_minutes, 15),
                        1200 to stringResource(R.string.timer_preset_reminder_minutes, 20)
                    ),
                    itemLabel = { it.second },
                    onItemSelected = { onUpdate(preset.copy(reminderInterval = it.first)) }
                )

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.timer_preset_bg_music_title),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RobotTheme.colors.textMuted
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        AiSettingSwitch(
                            checked = preset.musicEnabled,
                            onCheckedChange = { onUpdate(preset.copy(musicEnabled = it)) },
                            accentColor = accentColor,
                            modifier = Modifier.graphicsLayer {
                                scaleX = 0.7f
                                scaleY = 0.7f
                            }
                        )

                        if (preset.musicEnabled) {
                            var expandedMusic by remember { mutableStateOf(false) }
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.clickable { expandedMusic = true }
                                ) {
                                    Text(
                                        text = musicLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                    Text(
                                        text = "▾",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RobotTheme.colors.textMuted
                                    )
                                }
                                DropdownMenu(
                                    expanded = expandedMusic,
                                    onDismissRequest = { expandedMusic = false }
                                ) {
                                    listOf(
                                        "nature" to stringResource(R.string.timer_music_nature),
                                        "lofi" to stringResource(R.string.timer_music_lofi),
                                        "piano" to stringResource(R.string.timer_music_piano),
                                        "cyberpunk" to stringResource(R.string.timer_music_cyberpunk),
                                        "library" to stringResource(R.string.timer_music_library),
                                        "zen" to stringResource(R.string.timer_music_zen),
                                        "tick" to stringResource(R.string.timer_music_tick)
                                    ).forEach { (m, text) ->
                                        DropdownMenuItem(
                                            text = { Text(text) },
                                            onClick = {
                                                onUpdate(preset.copy(bgMusic = m))
                                                expandedMusic = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
