package com.airobot.features.clock.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
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
import com.airobot.features.clock.cards.alarm.AlarmListItem
import com.airobot.features.clock.cards.widgets.AiListContainer
import com.airobot.features.clock.data.model.AlarmItem
import com.airobot.framework.cards.ModuleServiceCard
import com.airobot.framework.theme.RobotTheme


/**
 * Alarm Card — alarm list and management.
 *
 * Prototype ref: AlarmView.tsx
 *
 * Shows list of alarms with toggle switches, expandable metaphorical configurations,
 * and voice prompt to manage alarms.
 */
@Composable
fun AlarmCard(
    modifier: Modifier = Modifier,
    alarms: List<AlarmItem> = emptyList(),
    onToggleAlarm: (String) -> Unit = {},
    onUpdateAlarm: (String, AlarmItem) -> Unit = { _, _ -> },
    onItemClick: (AlarmItem) -> Unit = {}
) {
    val isDark = RobotTheme.isDark
    var editingId by remember { mutableStateOf<String?>(null) }

    // Use real alarms from DB
    val displayAlarms = alarms

    val typeLabels = mapOf(
        "everyday" to "每天",
        "workday" to "工作日",
        "temporary" to "临时"
    )

    ModuleServiceCard(
        title = stringResource(R.string.alarm_title),
        subtitle = stringResource(R.string.alarm_subtitle),
        icon = Icons.Outlined.Notifications, // Corrected to Notifications (Bell) icon
        iconColor = if (isDark) Color(0xFFF97316) else Color(0xFFEA580C),
        iconBgColor = if (isDark) Color(0xFFF97316).copy(alpha = 0.20f) else Color(0xFFFFEDD5),
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        // Alarm list
        AiListContainer {
            items(displayAlarms) { alarm ->
                AlarmListItem(
                    alarm = alarm,
                    isDark = isDark,
                    typeLabels = typeLabels,
                    isEditing = editingId == alarm.id,
                    onToggle = { onToggleAlarm(alarm.id) },
                    onUpdate = { updated -> onUpdateAlarm(alarm.id, updated) },
                    onEditClick = {
                        editingId = if (editingId == alarm.id) null else alarm.id
                    },
                    onItemClick = { onItemClick(alarm) }
                )
            }
        }

        // Push the tip banner to the bottom, eliminating excessive empty gap
        Spacer(modifier = Modifier.weight(1f))

        // Bottom static tip banner
        AetherTipBanner(
            promptText = stringResource(R.string.alarm_prompt_text)
        )
    }
}


