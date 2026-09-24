package com.airobot.features.clock.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airobot.features.R
import com.airobot.features.FeatureCards
import com.airobot.features.aiserv.viewmodel.PopupQueueViewModel
import com.airobot.features.clock.cards.AlarmCard
import com.airobot.features.clock.cards.ClockHomeCard
import com.airobot.features.clock.cards.TimerCard
import com.airobot.features.clock.viewmodel.ClockViewModel
import com.airobot.features.clock.viewmodel.TimerStartResult

@Composable
fun ClockHomeRoute(
    modifier: Modifier = Modifier,
    clockViewModel: ClockViewModel = hiltViewModel(),
    popupQueueViewModel: PopupQueueViewModel = hiltViewModel(),
    onShowOverlay: (String) -> Unit,
    onNavigateToAlarm: () -> Unit = {}
) {
    val alarms by clockViewModel.alarms.collectAsState()
    val queueItems by popupQueueViewModel.queue.collectAsState()

    ClockHomeCard(
        modifier = modifier,
        queueItems = queueItems,
        onAlarmClick = {
            val targetAlarm = alarms.firstOrNull { it.enabled } ?: alarms.firstOrNull()
            clockViewModel.setSelectedAlarmId(targetAlarm?.id)
            clockViewModel.clearAlarmBackgrounded()
            onShowOverlay(FeatureCards.ALARM)
        },
        onFocusClick = { onShowOverlay(FeatureCards.TIMER) },
        onTimerClick = { onShowOverlay(FeatureCards.TIMER) },
        onChimeClick = { onShowOverlay(FeatureCards.HOURLY_CHIME) },
        onRemindClick = { action ->
            when (action) {
                "alarm" -> onShowOverlay(FeatureCards.ALARM)
                "clock_alarm" -> onNavigateToAlarm()
                "focus" -> onShowOverlay(FeatureCards.TIMER)
                "logbook" -> onShowOverlay(FeatureCards.LOGBOOK)
                "timer" -> onShowOverlay(FeatureCards.TIMER)
                else -> onShowOverlay(action)
            }
        }
    )
}

@Composable
fun AlarmRoute(
    modifier: Modifier = Modifier,
    clockViewModel: ClockViewModel = hiltViewModel(),
    onShowOverlay: (String) -> Unit
) {
    val alarms by clockViewModel.alarms.collectAsState()

    AlarmCard(
        modifier = modifier,
        alarms = alarms,
        onToggleAlarm = { id -> clockViewModel.toggleAlarm(id) },
        onUpdateAlarm = { id, updated -> clockViewModel.updateAlarm(id, updated) },
        onItemClick = { alarm ->
            clockViewModel.setSelectedAlarmId(alarm.id)
            clockViewModel.clearAlarmBackgrounded()
            onShowOverlay(FeatureCards.ALARM)
        }
    )
}

@Composable
fun TimerRoute(
    modifier: Modifier = Modifier,
    clockViewModel: ClockViewModel = hiltViewModel(),
    onShowOverlay: (String) -> Unit,
    onShowAlert: (String) -> Unit
) {
    val timerPresets by clockViewModel.timerPresets.collectAsState()
    val context = LocalContext.current

    TimerCard(
        modifier = modifier,
        presets = timerPresets,
        onUpdatePreset = { id, preset -> clockViewModel.updateTimerPreset(id, preset) },
        onPresetClick = { preset ->
            val startResult = clockViewModel.startTimerSession(preset)
            when (startResult) {
                TimerStartResult.STARTED -> {
                    onShowOverlay(FeatureCards.TIMER)
                }

                TimerStartResult.DUPLICATE_PRESET -> {
                    clockViewModel.bringTimerToForeground(preset.id)
                    onShowAlert(context.resources.getString(R.string.timer_blocked_duplicate_preset))
                    onShowOverlay(FeatureCards.TIMER)
                }

                TimerStartResult.FOCUS_ACTIVE -> {
                    onShowAlert(context.resources.getString(R.string.timer_blocked_focus_active))
                }

                TimerStartResult.COUNTDOWN_BLOCKS_FOCUS -> {
                    onShowAlert(context.resources.getString(R.string.timer_blocked_concurrent_focus))
                }
            }
        }
    )
}
