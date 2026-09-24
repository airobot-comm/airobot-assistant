package com.airobot.features.clock.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.airobot.features.aiserv.event.AiEvent
import com.airobot.features.aiserv.event.AiEventDispatcher
import com.airobot.features.FeatureCards
import com.airobot.features.aiserv.popup.TopAlertCoordinator
import com.airobot.features.clock.data.model.toAiCategory
import com.airobot.features.aiserv.popup.PopupQueueService
import com.airobot.features.aiserv.popup.PopupServiceItem
import com.airobot.features.clock.cards.TimerOverlay
import com.airobot.features.clock.data.ClockRepository
import com.airobot.features.clock.data.model.AlarmItem
import com.airobot.features.clock.data.model.PresetItem
import com.airobot.features.clock.data.model.TimerMode
import com.airobot.features.clock.service.SoundPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result returned when attempting to start a timer session.
 */
enum class TimerStartResult {
    STARTED,
    DUPLICATE_PRESET,
    FOCUS_ACTIVE,
    COUNTDOWN_BLOCKS_FOCUS
}

/**
 * Represents an active, running, or paused timer instance.
 */
data class TimerInstance(
    val id: String,
    val preset: PresetItem,
    val mode: TimerMode,
    val totalSeconds: Int,
    val timerSeconds: MutableStateFlow<Int>,
    val isRunning: MutableStateFlow<Boolean>,
    val isBackgrounded: MutableStateFlow<Boolean>,
    var timerJob: Job? = null,
    var collectorJob: Job? = null,
    var sessionStartTime: Long = 0L,
    var remindersCount: Int = 0,
    var pausedAtTimestamp: Long = 0L
)

/**
 * TimerEngine — manages concurrent countdown and focus session states,
 * tick loops, checkpoints, background/foreground state, and AI event emission.
 */
@Singleton
class TimerEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clockRepository: ClockRepository,
    private val soundPlayer: SoundPlayer,
    private val aiEventDispatcher: AiEventDispatcher,
    private val popupQueueService: PopupQueueService,
    private val topAlertCoordinator: TopAlertCoordinator
) {
    companion object {
        private const val TAG = "TimerEngine"
    }

    private val _timerPresets = MutableStateFlow<List<PresetItem>>(emptyList())
    val timerPresets: StateFlow<List<PresetItem>> = _timerPresets.asStateFlow()

    // Foreground proxy state flows for backward compatibility
    private val _timerSeconds = MutableStateFlow(1800)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _totalTimerSeconds = MutableStateFlow(1800)
    val totalTimerSeconds: StateFlow<Int> = _totalTimerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _activeTimerPreset = MutableStateFlow<PresetItem?>(null)
    val activeTimerPreset: StateFlow<PresetItem?> = _activeTimerPreset.asStateFlow()

    private val _activeTimerMode = MutableStateFlow(TimerMode.COUNTDOWN)
    val activeTimerMode: StateFlow<TimerMode> = _activeTimerMode.asStateFlow()

    private val _isTimerBackgrounded = MutableStateFlow(false)
    val isTimerBackgrounded: StateFlow<Boolean> = _isTimerBackgrounded.asStateFlow()

    private val _isFocusLocked = MutableStateFlow(false)
    val isFocusLocked: StateFlow<Boolean> = _isFocusLocked.asStateFlow()

    // Multi-instance tracking
    private val _foregroundInstanceId = MutableStateFlow<String?>(null)
    private val _activeInstances = MutableStateFlow<Map<String, TimerInstance>>(emptyMap())
    val activeInstances: StateFlow<Map<String, TimerInstance>> = _activeInstances.asStateFlow()

    private var pendingAlarmsFlow: StateFlow<List<AlarmItem>>? = null
    private var onClearPendingAlarms: (() -> Unit)? = null

    /**
     * Initializes the timer preset list, loading from persistence.
     */
    fun initialize(
        coroutineScope: CoroutineScope,
        pendingAlarmsFlow: StateFlow<List<AlarmItem>>,
        onClearPendingAlarms: () -> Unit
    ) {
        Log.d(TAG, "Initializing TimerEngine")
        this.pendingAlarmsFlow = pendingAlarmsFlow
        this.onClearPendingAlarms = onClearPendingAlarms
        coroutineScope.launch {
            val loadedTimerPresets = clockRepository.loadTimerPresets()
            val defaults = listOf(
                PresetItem(
                    "1",
                    context.getString(com.airobot.features.R.string.timer_preset_ramen),
                    180,
                    0,
                    "",
                    true,
                    TimerMode.COUNTDOWN
                ),
                PresetItem(
                    "2",
                    context.getString(com.airobot.features.R.string.timer_preset_mask),
                    900,
                    0,
                    "",
                    true,
                    TimerMode.COUNTDOWN
                ),
                PresetItem(
                    "4",
                    context.getString(com.airobot.features.R.string.timer_preset_meditation),
                    1800,
                    0,
                    "meditation.mp3",
                    true,
                    TimerMode.FOCUS
                )
            )
            if (loadedTimerPresets.size < 3) {
                val merged = defaults.map { def ->
                    loadedTimerPresets.find { it.id == def.id } ?: def
                }
                _timerPresets.value = merged
                clockRepository.saveTimerPresets(merged)
            } else if (loadedTimerPresets.size > 3) {
                // If it's already 4 or more, remove the "reading" one or keep first 3
                val truncated = loadedTimerPresets.filter { it.id != "3" }.take(3)
                _timerPresets.value = truncated
                clockRepository.saveTimerPresets(truncated)
            } else {
                _timerPresets.value = loadedTimerPresets
            }

            // Set default active preset
            val defaultPreset = _timerPresets.value.firstOrNull()
            _activeTimerPreset.value = defaultPreset
            _activeTimerMode.value = defaultPreset?.mode ?: TimerMode.COUNTDOWN
            _timerSeconds.value = defaultPreset?.seconds ?: 1800
            _totalTimerSeconds.value = defaultPreset?.seconds ?: 1800
        }
    }

    private data class TimerServiceData(
        val id: String,
        val label: String,
        val isForeground: Boolean,
        val timeoutDurationMs: Long,
        val needsForegroundLock: Boolean,
        val isFocusMode: Boolean
    )

    private fun startPopupCollection(instance: TimerInstance, coroutineScope: CoroutineScope) {
        instance.collectorJob?.cancel()
        instance.collectorJob = coroutineScope.launch {
            combine(
                instance.isRunning,
                instance.isBackgrounded,
                instance.timerSeconds,
                _foregroundInstanceId
            ) { isRunning, isBackgrounded, timerSeconds, foregroundId ->
                val isForeground =
                    (isRunning || timerSeconds == 0) && !isBackgrounded && (foregroundId == instance.id)

                // Sync to global flows if this is the foreground instance
                if (foregroundId == instance.id) {
                    _timerSeconds.value = timerSeconds
                    _totalTimerSeconds.value = instance.totalSeconds
                    _isTimerRunning.value = isRunning
                    _activeTimerPreset.value = instance.preset
                    _activeTimerMode.value = instance.mode
                    _isTimerBackgrounded.value = isBackgrounded
                }

                val timeoutMs =
                    if (isRunning || (instance.mode == TimerMode.FOCUS && timerSeconds > 0)) {
                        0L
                    } else if (timerSeconds == 0) {
                        if (instance.mode == TimerMode.FOCUS) 10000L else 15000L // Align with completion alert sound duration
                    } else {
                        instance.totalSeconds * 1000L // Original duration is pause timeout
                    }

                TimerServiceData(
                    id = instance.id,
                    label = instance.preset.label,
                    isForeground = isForeground,
                    timeoutDurationMs = timeoutMs,
                    needsForegroundLock = instance.mode == TimerMode.FOCUS,
                    isFocusMode = instance.mode == TimerMode.FOCUS
                )
            }.collect { data ->
                val popupItem = TimerPopupItem(instance, data)
                popupQueueService.enqueuePopup(popupItem)
            }
        }
    }

    private inner class TimerPopupItem(
        val instance: TimerInstance,
        val data: TimerServiceData
    ) : PopupServiceItem() {
        override val id: String = "timer_${data.id}"
        override val serviceType: String =
            if (data.isFocusMode) FeatureCards.FOCUS else FeatureCards.TIMER
        override val displayName: String = data.label.take(4)
        override val priority: Int = if (data.isForeground) 80 else 50
        override val timeoutDurationMs: Long = data.timeoutDurationMs
        override val nextEventTimeMs: Long? = null
        override val needsForegroundLock: Boolean = data.needsForegroundLock
        override val isFullScreen: Boolean = data.isForeground

        override val subDialTitle: String
            get() = if (data.needsForegroundLock) "专注" else "计时"
        override val subDialValue: String
            get() {
                val sec = instance.timerSeconds.value
                return String.format("%02d:%02d", sec / 60, sec % 60)
            }
        override val subDialIcon: androidx.compose.ui.graphics.vector.ImageVector
            get() = if (data.needsForegroundLock) Icons.Outlined.Psychology else Icons.Outlined.HourglassEmpty

        @Composable
        override fun getSubDialColor(): androidx.compose.ui.graphics.Color {
            return if (data.needsForegroundLock) com.airobot.framework.theme.RobotTheme.colors.focusAccent else com.airobot.framework.theme.RobotTheme.colors.timerAccent
        }

        override val subDialOnClick: () -> Unit = {
            bringTimerToForeground(instance.id)
        }

        override val onDismiss: () -> Unit = {
            if (instance.timerSeconds.value == 0) {
                resetTimerSession(instance.id)
            } else {
                sendTimerToBackground(instance.id)
            }
        }

        override val onTimeout: () -> Unit = {
            if (instance.timerSeconds.value == 0) {
                resetTimerSession(instance.id)
            } else {
                Log.d(TAG, "Pause timeout reached for timer ${instance.id}, clearing")
                aiEventDispatcher.dispatch(
                    AiEvent.TimerFinished(
                        id = java.util.UUID.randomUUID().toString(),
                        label = instance.preset.label,
                        duration = instance.totalSeconds - instance.timerSeconds.value,
                        targetDuration = instance.totalSeconds,
                        category = instance.mode.toAiCategory(),
                        timestamp = System.currentTimeMillis(),
                        closeReason = "pause_timeout",
                        startTime = instance.sessionStartTime,
                        remindersCount = instance.remindersCount
                    )
                )
                removeInstance(instance.id)
            }
        }

        @Composable
        override fun Content() {
            val timerSeconds by instance.timerSeconds.collectAsState()
            val totalSeconds = instance.totalSeconds
            val isRunning by instance.isRunning.collectAsState()
            val pendingAlarms by (pendingAlarmsFlow
                ?: MutableStateFlow(emptyList())).collectAsState()

            TimerOverlay(
                timerSeconds = timerSeconds,
                totalSeconds = totalSeconds,
                isRunning = isRunning,
                preset = instance.preset,
                mode = instance.mode,
                pendingAlarms = pendingAlarms,
                onToggle = { toggleTimerRunning(instance.id, CoroutineScope(Dispatchers.Main)) },
                onReset = { resetTimerSession(instance.id) },
                onSendToBackground = {
                    sendTimerToBackground(instance.id)
                },
                onEmergencyStop = {
                    emergencyStopTimer(instance.id)
                    onClearPendingAlarms?.invoke()
                },
                onClearPendingAlarms = { onClearPendingAlarms?.invoke() },
                onShowConstraintAlert = { topAlertCoordinator.showAlert(it) },
                onClose = {
                    onClearPendingAlarms?.invoke()
                }
            )
        }
    }

    fun onDestroy() {
        Log.d(TAG, "onDestroy: cancelling all active timer jobs")
        _activeInstances.value.values.forEach { instance ->
            instance.timerJob?.cancel()
            instance.collectorJob?.cancel()
        }
    }

    fun updateTimerPreset(id: String, updatedItem: PresetItem, coroutineScope: CoroutineScope) {
        Log.d(TAG, "updateTimerPreset: id=$id")
        val updated = _timerPresets.value.map {
            if (it.id == id) updatedItem else it
        }
        _timerPresets.value = updated
        coroutineScope.launch { clockRepository.saveTimerPresets(updated) }

        val activeInst = _activeInstances.value[id]
        if (activeInst != null && !activeInst.isRunning.value) {
            activeInst.timerSeconds.value = updatedItem.seconds
        }
    }

    /**
     * Starts a countdown session.
     */
    fun startTimerSession(
        preset: PresetItem,
        autoStart: Boolean,
        coroutineScope: CoroutineScope
    ): TimerStartResult {
        if (_activeInstances.value.containsKey(preset.id)) {
            Log.w(TAG, "Cannot start timer session: Preset ${preset.id} is already active.")
            return TimerStartResult.DUPLICATE_PRESET
        }

        val hasActiveFocus = _activeInstances.value.values.any { it.mode == TimerMode.FOCUS }
        if (hasActiveFocus) {
            Log.w(TAG, "Cannot start timer session: Focus mode is active.")
            return TimerStartResult.FOCUS_ACTIVE
        }

        if (preset.mode == TimerMode.FOCUS && _activeInstances.value.isNotEmpty()) {
            Log.w(TAG, "Cannot start focus: running countdown timers exist.")
            return TimerStartResult.COUNTDOWN_BLOCKS_FOCUS
        }

        Log.d(
            TAG,
            "startTimerSession: preset=${preset.label}, mode=${preset.mode}, autoStart=$autoStart"
        )

        val instance = TimerInstance(
            id = preset.id,
            preset = preset,
            mode = preset.mode,
            totalSeconds = preset.seconds,
            timerSeconds = MutableStateFlow(preset.seconds),
            isRunning = MutableStateFlow(autoStart),
            isBackgrounded = MutableStateFlow(false),
            sessionStartTime = if (autoStart) System.currentTimeMillis() else 0L
        )

        val updated = _activeInstances.value.toMutableMap()
        updated[preset.id] = instance
        _activeInstances.value = updated

        _foregroundInstanceId.value = preset.id
        updated.forEach { (id, inst) ->
            if (id != preset.id) {
                inst.isBackgrounded.value = true
            }
        }

        startPopupCollection(instance, coroutineScope)
        updateFocusLockedState()

        if (autoStart) {
            runCountdownJob(instance, coroutineScope)
        }

        return TimerStartResult.STARTED
    }

    private fun runCountdownJob(instance: TimerInstance, coroutineScope: CoroutineScope) {
        instance.timerJob?.cancel()

        if (instance.sessionStartTime == 0L) {
            instance.sessionStartTime = System.currentTimeMillis()
            instance.remindersCount = 0

            aiEventDispatcher.dispatch(
                AiEvent.TimerStarted(
                    id = java.util.UUID.randomUUID().toString(),
                    label = instance.preset.label,
                    duration = instance.totalSeconds,
                    category = instance.mode.toAiCategory(),
                    timestamp = instance.sessionStartTime
                )
            )
        }

        var lastReminderSeconds = instance.timerSeconds.value
        val reminderInterval = instance.preset.reminderInterval

        instance.timerJob = coroutineScope.launch {
            while (instance.timerSeconds.value > 0) {
                delay(1000)
                if (instance.isRunning.value) {
                    instance.timerSeconds.value -= 1

                    if (reminderInterval > 0) {
                        val elapsed = lastReminderSeconds - instance.timerSeconds.value
                        if (elapsed >= reminderInterval) {
                            Log.d(
                                TAG,
                                "Timer checkpoint reached: elapsed=$elapsed seconds - playing beep"
                            )
                            bringTimerToForeground(instance.id)
                            soundPlayer.playReminderSound()
                            lastReminderSeconds = instance.timerSeconds.value

                            instance.remindersCount++
                            aiEventDispatcher.dispatch(
                                AiEvent.TimerReminderTriggered(
                                    id = java.util.UUID.randomUUID().toString(),
                                    label = instance.preset.label,
                                    elapsedSeconds = instance.totalSeconds - instance.timerSeconds.value,
                                    totalSeconds = instance.totalSeconds,
                                    category = instance.mode.toAiCategory(),
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }

            instance.isRunning.value = false
            updateFocusLockedState()
            Log.d(TAG, "Timer countdown completed successfully")
            bringTimerToForeground(instance.id)
            soundPlayer.playTimerCompletionAlert(instance.mode == TimerMode.FOCUS)

            aiEventDispatcher.dispatch(
                AiEvent.TimerFinished(
                    id = java.util.UUID.randomUUID().toString(),
                    label = instance.preset.label,
                    duration = instance.totalSeconds,
                    targetDuration = instance.totalSeconds,
                    category = instance.mode.toAiCategory(),
                    timestamp = System.currentTimeMillis(),
                    closeReason = "completed",
                    startTime = instance.sessionStartTime,
                    remindersCount = instance.remindersCount
                )
            )

            instance.sessionStartTime = 0L
            instance.remindersCount = 0
            instance.isBackgrounded.value = false
        }
    }

    fun toggleTimerRunning(id: String, coroutineScope: CoroutineScope) {
        val instance = _activeInstances.value[id] ?: return
        val nextRunning = !instance.isRunning.value
        Log.d(TAG, "toggleTimerRunning: id=$id, nextRunning=$nextRunning")

        if (instance.mode == TimerMode.FOCUS && instance.isRunning.value) {
            Log.w(TAG, "Cannot toggle running state: Focus mode is locked and cannot be paused")
            return
        }

        instance.isRunning.value = nextRunning
        updateFocusLockedState()
        if (nextRunning) {
            if (instance.timerJob?.isActive != true) {
                runCountdownJob(instance, coroutineScope)
            }
        } else {
            instance.pausedAtTimestamp = System.currentTimeMillis()
        }
    }

    fun resetTimerSession(id: String) {
        Log.d(TAG, "resetTimerSession: id=$id")
        val instance = _activeInstances.value[id] ?: return
        if (instance.mode == TimerMode.FOCUS && instance.isRunning.value) {
            Log.w(TAG, "Cannot reset running Focus session directly")
            return
        }

        val actualSpent = instance.totalSeconds - instance.timerSeconds.value
        if (instance.mode == TimerMode.COUNTDOWN && actualSpent > 0) {
            aiEventDispatcher.dispatch(
                AiEvent.TimerFinished(
                    id = java.util.UUID.randomUUID().toString(),
                    label = instance.preset.label,
                    duration = actualSpent,
                    targetDuration = instance.totalSeconds,
                    category = TimerMode.COUNTDOWN.toAiCategory(),
                    timestamp = System.currentTimeMillis(),
                    closeReason = "user_interrupted",
                    startTime = instance.sessionStartTime,
                    remindersCount = instance.remindersCount
                )
            )
        }

        removeInstance(id)
    }

    fun emergencyStopTimer(id: String) {
        Log.d(TAG, "emergencyStopTimer: id=$id")
        val instance = _activeInstances.value[id] ?: return

        val actualSpent = instance.totalSeconds - instance.timerSeconds.value
        if (instance.mode == TimerMode.FOCUS && actualSpent > 0) {
            aiEventDispatcher.dispatch(
                AiEvent.TimerFinished(
                    id = java.util.UUID.randomUUID().toString(),
                    label = instance.preset.label,
                    duration = actualSpent,
                    targetDuration = instance.totalSeconds,
                    category = TimerMode.FOCUS.toAiCategory(),
                    timestamp = System.currentTimeMillis(),
                    closeReason = "emergency_stopped",
                    startTime = instance.sessionStartTime,
                    remindersCount = instance.remindersCount
                )
            )
        }

        removeInstance(id)
    }

    fun sendTimerToBackground(id: String) {
        val instance = _activeInstances.value[id] ?: return
        if (instance.mode == TimerMode.FOCUS) {
            Log.w(TAG, "Focus mode cannot be sent to background")
            return
        }
        Log.d(TAG, "sendTimerToBackground: id=$id")
        instance.isBackgrounded.value = true
    }

    fun bringTimerToForeground(id: String) {
        val instance = _activeInstances.value[id] ?: return
        Log.d(TAG, "bringTimerToForeground: id=$id")
        instance.isBackgrounded.value = false
        _foregroundInstanceId.value = id
        _activeInstances.value.forEach { (instId, inst) ->
            if (instId != id) {
                inst.isBackgrounded.value = true
            }
        }
    }

    private fun removeInstance(id: String) {
        val instance = _activeInstances.value[id]
        if (instance != null) {
            instance.timerJob?.cancel()
            instance.collectorJob?.cancel()
            popupQueueService.removePopup("timer_$id")

            soundPlayer.stopSound() // Stop alarm/vibration immediately on removal

            val updated = _activeInstances.value.toMutableMap()
            updated.remove(id)
            _activeInstances.value = updated

            updateFocusLockedState()

            if (_foregroundInstanceId.value == id) {
                val nextForeground = updated.values.firstOrNull { it.isRunning.value }
                    ?: updated.values.firstOrNull()
                _foregroundInstanceId.value = nextForeground?.id

                if (nextForeground == null) {
                    val defaultPreset = _timerPresets.value.firstOrNull()
                    _activeTimerPreset.value = defaultPreset
                    _activeTimerMode.value = defaultPreset?.mode ?: TimerMode.COUNTDOWN
                    _timerSeconds.value = defaultPreset?.seconds ?: 1800
                    _totalTimerSeconds.value = defaultPreset?.seconds ?: 1800
                    _isTimerRunning.value = false
                    _isTimerBackgrounded.value = false
                }
            }
        }
    }

    private fun updateFocusLockedState() {
        _isFocusLocked.value =
            _activeInstances.value.values.any { it.mode == TimerMode.FOCUS && it.isRunning.value }
    }

    // --- Helper methods for query states across instances ---

    fun isFocusActive(): Boolean {
        return _activeInstances.value.values.any { it.mode == TimerMode.FOCUS && it.isRunning.value }
    }

    fun isCountdownActive(): Boolean {
        return _activeInstances.value.values.any { it.mode == TimerMode.COUNTDOWN && it.isRunning.value }
    }

    fun sendAllCountdownTimersToBackground() {
        _activeInstances.value.values.forEach { inst ->
            if (inst.mode == TimerMode.COUNTDOWN) {
                inst.isBackgrounded.value = true
            }
        }
    }

    // --- Backward Compatible Facade Methods delegating to foreground instance ---

    fun toggleTimerRunning(coroutineScope: CoroutineScope) {
        val id = _foregroundInstanceId.value ?: return
        toggleTimerRunning(id, coroutineScope)
    }

    fun resetTimerSession() {
        val id = _foregroundInstanceId.value ?: return
        resetTimerSession(id)
    }

    fun emergencyStopTimer() {
        val id = _foregroundInstanceId.value ?: return
        emergencyStopTimer(id)
    }

    fun sendTimerToBackground() {
        val id = _foregroundInstanceId.value ?: return
        sendTimerToBackground(id)
    }

    fun bringTimerToForeground() {
        val id = _foregroundInstanceId.value ?: return
        bringTimerToForeground(id)
    }
}
