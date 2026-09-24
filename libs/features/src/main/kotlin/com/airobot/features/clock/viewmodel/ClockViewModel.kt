package com.airobot.features.clock.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airobot.features.aiserv.guidance.RemindEngine
import com.airobot.features.aiserv.guidance.data.RemindCard
import com.airobot.features.FeatureCards
import com.airobot.features.clock.data.model.AlarmItem
import com.airobot.features.clock.data.model.HourlyChimeConfig
import com.airobot.features.clock.data.model.PresetItem
import com.airobot.features.clock.data.model.TimerMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ClockViewModel — manages clock-specific feature states, persistence,
 * countdown timers (Timer/Focus), alarms, and local BroadcastReceiver actions.
 *
 * Implements a Clean Architecture Facade by delegating distinct responsibilities
 * to AlarmDelegate, TimerEngine, and HourlyChimeManager helper classes.
 *
 * Outdated KDoc note: Design is now standard with 4 fixed alarm slots.
 */
@HiltViewModel
class ClockViewModel @Inject constructor(
    private val alarmDelegate: AlarmDelegate,
    private val timerEngine: TimerEngine,
    private val hourlyChimeManager: HourlyChimeManager,
    private val remindEngine: RemindEngine
) : ViewModel() {

    /** Clock-specific remind cards filtered by clock-related overlay tags. */
    val remindCards: Flow<List<RemindCard>> = remindEngine.getRemindCards(
        listOf(FeatureCards.ALARM, FeatureCards.TIMER, FeatureCards.FOCUS, FeatureCards.LOGBOOK)
    )

    companion object {
        private const val TAG = "ClockViewModel"
    }

    // --- StateFlow Exposures Delegated to Sub-Modules ---
    val alarms: StateFlow<List<AlarmItem>> = alarmDelegate.alarms
    val timerPresets: StateFlow<List<PresetItem>> = timerEngine.timerPresets
    val ringingAlarmId: StateFlow<String?> = alarmDelegate.ringingAlarmId
    val selectedAlarmId: StateFlow<String?> = alarmDelegate.selectedAlarmId
    val hourlyChimeEnabled: StateFlow<Boolean> = hourlyChimeManager.hourlyChimeEnabled
    val chimeConfig: StateFlow<HourlyChimeConfig> = hourlyChimeManager.chimeConfig
    val isChiming: StateFlow<Boolean> = hourlyChimeManager.isChiming
    val isTimerBackgrounded: StateFlow<Boolean> = timerEngine.isTimerBackgrounded
    val isAlarmBackgrounded: StateFlow<Boolean> = alarmDelegate.isAlarmBackgrounded
    val isFocusLocked: StateFlow<Boolean> = timerEngine.isFocusLocked
    val timerSeconds: StateFlow<Int> = timerEngine.timerSeconds
    val totalTimerSeconds: StateFlow<Int> = timerEngine.totalTimerSeconds
    val isTimerRunning: StateFlow<Boolean> = timerEngine.isTimerRunning
    val activeTimerPreset: StateFlow<PresetItem?> = timerEngine.activeTimerPreset
    val activeTimerMode: StateFlow<TimerMode> = timerEngine.activeTimerMode
    val pendingAlarms: StateFlow<List<AlarmItem>> = alarmDelegate.pendingAlarms
    val activeInstances: StateFlow<Map<String, TimerInstance>> = timerEngine.activeInstances

    init {
        Log.d(TAG, "Initializing ClockViewModel Facade")

        // Initialize independent sub-managers
        hourlyChimeManager.initialize(
            coroutineScope = viewModelScope,
            isCountdownModeActive = { timerEngine.isCountdownActive() },
            onCountdownModeRunning = { timerEngine.sendAllCountdownTimersToBackground() }
        )
        timerEngine.initialize(
            viewModelScope,
            alarmDelegate.pendingAlarms,
            { alarmDelegate.clearPendingAlarms() }
        )

        // Initialize alarm delegate with query states and callback linkages
        alarmDelegate.initialize(
            coroutineScope = viewModelScope,
            isFocusModeActive = { timerEngine.isFocusActive() },
            isCountdownModeActive = { timerEngine.isCountdownActive() },
            onCountdownModeRunning = { timerEngine.sendAllCountdownTimersToBackground() }
        )
    }


    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: cleaning up delegate resources")
        alarmDelegate.onDestroy()
        timerEngine.onDestroy()
    }

    // ============================================================
    // Delegated Operations & Mutators
    // ============================================================

    fun setSelectedAlarmId(id: String?) {
        alarmDelegate.setSelectedAlarmId(id)
    }

    fun toggleAlarm(id: String) {
        alarmDelegate.toggleAlarm(id, viewModelScope)
    }

    fun updateAlarm(id: String, updatedItem: AlarmItem) {
        alarmDelegate.updateAlarm(id, updatedItem, viewModelScope)
    }

    fun dismissAlarm() {
        alarmDelegate.dismissAlarm(viewModelScope)
    }

    fun minimizeAlarm(silence: Boolean = true) {
        alarmDelegate.minimizeAlarm(silence)
    }

    fun clearAlarmBackgrounded() {
        alarmDelegate.clearAlarmBackgrounded()
    }

    fun toggleHourlyChime() {
        hourlyChimeManager.toggleHourlyChime(viewModelScope)
    }

    fun updateChimeConfig(config: HourlyChimeConfig) {
        hourlyChimeManager.updateConfig(viewModelScope, config)
    }

    fun dismissChime() {
        hourlyChimeManager.dismissChime()
    }

    fun updateTimerPreset(id: String, updatedItem: PresetItem) {
        timerEngine.updateTimerPreset(id, updatedItem, viewModelScope)
    }

    fun startTimerSession(preset: PresetItem, autoStart: Boolean = false): TimerStartResult {
        return timerEngine.startTimerSession(preset, autoStart, viewModelScope)
    }

    fun toggleTimerRunning() {
        timerEngine.toggleTimerRunning(viewModelScope)
    }

    fun resetTimerSession() {
        timerEngine.resetTimerSession()
    }

    fun emergencyStopTimer() {
        timerEngine.emergencyStopTimer()
    }

    fun sendTimerToBackground() {
        timerEngine.sendTimerToBackground()
    }

    fun bringTimerToForeground() {
        timerEngine.bringTimerToForeground()
    }

    fun bringTimerToForeground(id: String) {
        timerEngine.bringTimerToForeground(id)
    }

    fun clearPendingAlarms() {
        alarmDelegate.clearPendingAlarms()
    }
}
