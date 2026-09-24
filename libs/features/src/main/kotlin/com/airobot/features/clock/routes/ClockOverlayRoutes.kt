package com.airobot.features.clock.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airobot.features.clock.cards.AlarmOverlay
import com.airobot.features.clock.cards.HourlyChimeOverlay
import com.airobot.features.clock.cards.TimerOverlay
import com.airobot.features.clock.viewmodel.ClockViewModel

@Composable
fun AlarmOverlayRoute(
    modifier: Modifier = Modifier,
    clockViewModel: ClockViewModel = hiltViewModel(),
    onHideOverlay: () -> Unit
) {
    val alarms by clockViewModel.alarms.collectAsState()
    val selectedAlarmId by clockViewModel.selectedAlarmId.collectAsState()
    val ringingAlarmId by clockViewModel.ringingAlarmId.collectAsState()

    val activeAlarm = if (ringingAlarmId != null) {
        alarms.find { it.id == ringingAlarmId }
    } else {
        alarms.find { it.id == selectedAlarmId }
            ?: alarms.firstOrNull { it.enabled } ?: alarms.firstOrNull()
    }

    AlarmOverlay(
        alarm = activeAlarm,
        ringingAlarmId = ringingAlarmId,
        alarms = alarms,
        onToggleAlarm = { activeAlarm?.let { clockViewModel.toggleAlarm(it.id) } },
        onDismissAlarm = {
            clockViewModel.dismissAlarm()
            onHideOverlay()
        },
        onMinimizeAlarm = { silence ->
            clockViewModel.minimizeAlarm(silence)
            onHideOverlay()
        },
        onClose = { onHideOverlay() }
    )
}

@Composable
fun TimerOverlayRoute(
    modifier: Modifier = Modifier,
    clockViewModel: ClockViewModel = hiltViewModel(),
    onHideOverlay: () -> Unit,
    onShowAlert: (String) -> Unit
) {
    val timerSeconds by clockViewModel.timerSeconds.collectAsState()
    val totalTimerSeconds by clockViewModel.totalTimerSeconds.collectAsState()
    val isTimerRunning by clockViewModel.isTimerRunning.collectAsState()
    val activeTimerPreset by clockViewModel.activeTimerPreset.collectAsState()
    val activeTimerMode by clockViewModel.activeTimerMode.collectAsState()
    val pendingAlarms by clockViewModel.pendingAlarms.collectAsState()
    val isTimerBackgrounded by clockViewModel.isTimerBackgrounded.collectAsState()

    LaunchedEffect(isTimerBackgrounded, activeTimerPreset) {
        if (isTimerBackgrounded) {
            onHideOverlay()
        }
    }

    TimerOverlay(
        timerSeconds = timerSeconds,
        totalSeconds = totalTimerSeconds,
        isRunning = isTimerRunning,
        preset = activeTimerPreset,
        mode = activeTimerMode,
        pendingAlarms = pendingAlarms,
        onToggle = { clockViewModel.toggleTimerRunning() },
        onReset = { clockViewModel.resetTimerSession() },
        onSendToBackground = {
            clockViewModel.sendTimerToBackground()
            onHideOverlay()
        },
        onEmergencyStop = {
            clockViewModel.emergencyStopTimer()
            clockViewModel.clearPendingAlarms()
            onHideOverlay()
        },
        onClearPendingAlarms = { clockViewModel.clearPendingAlarms() },
        onShowConstraintAlert = { onShowAlert(it) },
        onClose = {
            clockViewModel.clearPendingAlarms()
            onHideOverlay()
        }
    )
}

@Composable
fun HourlyChimeOverlayRoute(
    modifier: Modifier = Modifier,
    clockViewModel: ClockViewModel = hiltViewModel(),
    onHideOverlay: () -> Unit
) {
    val chimeConfig by clockViewModel.chimeConfig.collectAsState()
    val isChiming by clockViewModel.isChiming.collectAsState()

    HourlyChimeOverlay(
        modifier = modifier,
        config = chimeConfig,
        isChiming = isChiming,
        onConfigChange = { clockViewModel.updateChimeConfig(it) },
        onDismissChime = {
            clockViewModel.dismissChime()
            onHideOverlay()
        },
        onClose = { onHideOverlay() }
    )
}
