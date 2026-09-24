package com.airobot.features.clock.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.airobot.features.R
import com.airobot.features.aiserv.guidance.components.AetherTipBanner
import com.airobot.features.clock.cards.timer.TimerPresetItem
import com.airobot.features.clock.cards.widgets.AiListContainer
import com.airobot.features.clock.data.model.PresetItem
import com.airobot.framework.cards.ModuleServiceCard
import com.airobot.framework.theme.RobotTheme


/**
 * Timer/Focus Card — preset list with start trigger and in-place configuration.
 *
 * Prototype ref: TimeView.tsx (timer & focus modes)
 */
@Composable
fun TimerCard(
    modifier: Modifier = Modifier,
    presets: List<PresetItem> = emptyList(),
    onUpdatePreset: (String, PresetItem) -> Unit = { _, _ -> },
    onPresetClick: (PresetItem) -> Unit = {}
) {
    val isDark = RobotTheme.isDark
    var editingId by remember { mutableStateOf<String?>(null) }

    // Use real presets from DB
    val displayPresets = presets

    ModuleServiceCard(
        title = stringResource(R.string.timer_title),
        subtitle = "倒计时与深层专注服务",
        icon = Icons.Outlined.HourglassEmpty,
        iconColor = if (isDark) Color(0xFF22D3EE) else Color(0xFF0891B2),
        iconBgColor = if (isDark) Color(0xFF22D3EE).copy(alpha = 0.20f) else Color(0xFFCFFAFE),
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top // Align to top for compact layout
    ) {
        // Presets list container
        AiListContainer {
            items(displayPresets) { preset ->
                TimerPresetItem(
                    preset = preset,
                    isDark = isDark,
                    isEditing = editingId == preset.id,
                    onEditClick = {
                        editingId = if (editingId == preset.id) null else preset.id
                    },
                    onUpdate = { updated -> onUpdatePreset(preset.id, updated) },
                    onClick = { onPresetClick(preset) }
                )
            }
        }

        // Push the tip banner to the bottom, eliminating excessive empty gap
        Spacer(modifier = Modifier.weight(1f))

        // Bottom static tip banner
        AetherTipBanner(
            promptText = stringResource(R.string.timer_prompt_text)
        )
    }
}


