package com.airobot.features.clock.viewmodel

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.airobot.features.aiserv.event.AiEvent
import com.airobot.features.aiserv.event.AiEventDispatcher
import com.airobot.features.FeatureCards
import com.airobot.features.aiserv.popup.PopupQueueService
import com.airobot.features.aiserv.popup.PopupServiceItem
import com.airobot.features.clock.cards.AlarmOverlay
import com.airobot.features.clock.data.ClockRepository
import com.airobot.features.clock.data.model.AlarmItem
import com.airobot.features.clock.service.AlarmRingingService
import com.airobot.features.clock.service.AlarmScheduler
import com.airobot.features.clock.service.SoundPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AlarmDelegate — helper delegate responsible for managing alarm CRUD operations,
 * suppression lists, background ringing notifications, and local BroadcastReceiver actions.
 */
class AlarmDelegate @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clockRepository: ClockRepository,
    private val alarmScheduler: AlarmScheduler,
    private val soundPlayer: SoundPlayer,
    private val aiEventDispatcher: AiEventDispatcher,
    private val popupQueueService: PopupQueueService
) {
    companion object {
        private const val TAG = "AlarmDelegate"
    }

    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    private val _ringingAlarmId = MutableStateFlow<String?>(null)
    val ringingAlarmId: StateFlow<String?> = _ringingAlarmId.asStateFlow()

    private val _selectedAlarmId = MutableStateFlow<String?>(null)
    val selectedAlarmId: StateFlow<String?> = _selectedAlarmId.asStateFlow()

    private val _isAlarmBackgrounded = MutableStateFlow(false)
    val isAlarmBackgrounded: StateFlow<Boolean> = _isAlarmBackgrounded.asStateFlow()

    private val _pendingAlarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val pendingAlarms: StateFlow<List<AlarmItem>> = _pendingAlarms.asStateFlow()

    private var coroutineScope: CoroutineScope? = null
    private var isFocusModeActive: (() -> Boolean)? = null
    private var isCountdownModeActive: (() -> Boolean)? = null
    private var onCountdownModeRunning: (() -> Unit)? = null

    // Broadcast receiver for internal service notifications
    private val alarmReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val action = intent?.action
            Log.d(TAG, "Local receiver received action: $action")
            when (action) {
                AlarmRingingService.ACTION_ALARM_RINGING -> {
                    val id = intent.getStringExtra("alarmId") ?: ""
                    Log.d(TAG, "Alarm triggered broadcast received for ID: $id")
                    onAlarmTriggered(id)
                }

                AlarmRingingService.ACTION_ALARM_SEQUENCE_DONE -> {
                    val id = intent.getStringExtra("alarmId") ?: ""
                    Log.d(TAG, "Alarm ringing sequence complete broadcast received for ID: $id")
                    // If sequence is fully complete, treat it as dismissed so it doesn't snooze
                    coroutineScope?.let { dismissAlarm(it) }
                }

            }
        }
    }

    /**
     * Initializes alarms, registers BroadcastReceivers, and maps callbacks.
     */
    fun initialize(
        coroutineScope: CoroutineScope,
        isFocusModeActive: () -> Boolean,
        isCountdownModeActive: () -> Boolean,
        onCountdownModeRunning: () -> Unit
    ) {
        Log.d(TAG, "Initializing AlarmDelegate")
        this.coroutineScope = coroutineScope
        this.isFocusModeActive = isFocusModeActive
        this.isCountdownModeActive = isCountdownModeActive
        this.onCountdownModeRunning = onCountdownModeRunning

        coroutineScope.launch {
            val loadedAlarms = clockRepository.loadAlarms()
            val defaults = listOf(
                AlarmItem(
                    id = "workday-morning",
                    time = "08:30",
                    label = context.getString(com.airobot.features.R.string.alarm_default_workday),
                    enabled = true,
                    days = listOf(1, 2, 3, 4, 5),
                    type = "workday",
                    repeatCount = 3,
                    interval = 5,
                    voiceMode = "standard",
                    requireName = false,
                    dismissMode = "voice",
                    autoDismissSeconds = 10,
                    soundId = "system_default"
                ),
                AlarmItem(
                    id = "everyday-sleep",
                    time = "22:30",
                    label = context.getString(com.airobot.features.R.string.alarm_default_sleep),
                    enabled = true,
                    days = listOf(0, 1, 2, 3, 4, 5, 6),
                    type = "everyday",
                    repeatCount = 3,
                    interval = 5,
                    voiceMode = "hint",
                    requireName = false,
                    dismissMode = "auto",
                    autoDismissSeconds = 10,
                    soundId = "system_default"
                ),
                AlarmItem(
                    id = "temporary-alarm",
                    time = "13:40",
                    label = context.getString(com.airobot.features.R.string.alarm_default_lunch),
                    enabled = true,
                    days = emptyList(),
                    type = "temporary",
                    repeatCount = 1,
                    interval = 5,
                    voiceMode = "standard",
                    requireName = false,
                    dismissMode = "voice",
                    autoDismissSeconds = 10,
                    soundId = "system_default"
                )
            )

            val finalAlarms = if (loadedAlarms.isEmpty()) {
                defaults
            } else {
                val baseList = (loadedAlarms + defaults).take(3)
                baseList.mapIndexed { index, alarm ->
                    if (index == 2) {
                        alarm.copy(
                            id = "temporary-alarm",
                            type = "temporary",
                            days = emptyList()
                        )
                    } else if (index == 0 && alarm.id != "workday-morning") {
                        alarm.copy(id = "workday-morning")
                    } else if (index == 1 && alarm.id != "everyday-sleep") {
                        alarm.copy(id = "everyday-sleep")
                    } else {
                        alarm
                    }
                }
            }
            _alarms.value = finalAlarms
            clockRepository.saveAlarms(finalAlarms)
            _selectedAlarmId.value =
                _alarms.value.firstOrNull { it.enabled }?.id ?: _alarms.value.firstOrNull()?.id

            // Schedule enabled alarms on start
            _alarms.value.forEach { alarm ->
                if (alarm.enabled) {
                    Log.d(TAG, "Scheduling enabled alarm on initialization: ${alarm.id}")
                    alarmScheduler.scheduleNextOccurrence(
                        alarm.id,
                        alarm.time,
                        alarm.days,
                        alarm.label,
                        alarm.repeatCount,
                        alarm.interval,
                        alarm.voiceMode,
                        alarm.dismissMode,
                        alarm.autoDismissSeconds,
                        alarm.soundId
                    )
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(AlarmRingingService.ACTION_ALARM_RINGING)
            addAction(AlarmRingingService.ACTION_ALARM_SEQUENCE_DONE)
        }

        ContextCompat.registerReceiver(
            context,
            alarmReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        coroutineScope.launch {
            combine(
                _alarms,
                _ringingAlarmId,
                _isAlarmBackgrounded
            ) { alarmList, ringingId, isBackgrounded ->
                alarmList.mapNotNull { alarm ->
                    val isRingingThis = (alarm.id == ringingId)
                    if (alarm.enabled || isRingingThis) {
                        val isForeground = isRingingThis && !isBackgrounded
                        val nextTime = if (isRingingThis) {
                            if (isBackgrounded) {
                                System.currentTimeMillis() + (alarm.interval * 60 * 1000L)
                            } else {
                                System.currentTimeMillis() - 1000
                            }
                        } else {
                            com.airobot.features.clock.service.AlarmSchedulerImpl.calculateNextTriggerTime(
                                alarm.time,
                                alarm.days
                            )
                        }
                        AlarmServiceData(
                            alarmId = alarm.id,
                            label = alarm.label,
                            isForeground = isForeground,
                            nextEventTimeMs = nextTime,
                            isRinging = isRingingThis
                        )
                    } else {
                        null
                    }
                }
            }.collect { activeAlarms ->
                // First remove all alarm popups to ensure stale ones are cleaned up
                popupQueueService.removePopupsByPrefix("alarm_")

                // Enqueue all active/enabled alarms as separate popup items
                for (data in activeAlarms) {
                    popupQueueService.enqueuePopup(AlarmPopupItem(data))
                }
            }
        }
    }

    private data class AlarmServiceData(
        val alarmId: String,
        val label: String,
        val isForeground: Boolean,
        val nextEventTimeMs: Long,
        val isRinging: Boolean
    )

    private inner class AlarmPopupItem(
        val data: AlarmServiceData
    ) : PopupServiceItem() {
        override val id: String = "alarm_${data.alarmId}"
        override val serviceType: String = FeatureCards.ALARM
        override val displayName: String = data.label.take(4)

        override val priority: Int = if (data.isForeground) {
            100
        } else if (data.isRinging) {
            60
        } else {
            10
        }

        override val timeoutDurationMs: Long = 0L // Managed by AlarmRingingService
        override val nextEventTimeMs: Long? = data.nextEventTimeMs
        override val needsForegroundLock: Boolean = data.isForeground
        override val isFullScreen: Boolean = data.isForeground

        override val subDialTitle: String
            get() = if (data.isRinging) "闹铃响铃" else "闹钟"

        override val subDialValue: String
            get() {
                val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                return timeFormat.format(java.util.Date(data.nextEventTimeMs))
            }

        override val subDialIcon: androidx.compose.ui.graphics.vector.ImageVector =
            Icons.Outlined.Notifications

        @Composable
        override fun getSubDialColor(): androidx.compose.ui.graphics.Color {
            return com.airobot.framework.theme.RobotTheme.colors.alarmAccent
        }

        override val subDialOnClick: () -> Unit = {}

        override val onDismiss: () -> Unit = {
            coroutineScope?.let { dismissAlarm(it) }
        }

        override val onTimeout: () -> Unit = {}

        @Composable
        override fun Content() {
            val alarms by _alarms.collectAsState()
            val ringingId by _ringingAlarmId.collectAsState()

            val activeAlarm = alarms.find { it.id == data.alarmId } ?: alarms.firstOrNull()

            AlarmOverlay(
                alarm = activeAlarm,
                ringingAlarmId = ringingId,
                alarms = alarms,
                onToggleAlarm = { activeAlarm?.let { toggleAlarm(it.id, coroutineScope!!) } },
                onDismissAlarm = {
                    coroutineScope?.let { dismissAlarm(it) }
                },
                onMinimizeAlarm = { silence ->
                    minimizeAlarm(silence)
                },
                onClose = {
                    if (ringingId != null) {
                        minimizeAlarm(silence = true)
                    }
                }
            )
        }
    }

    /**
     * Cleans up receiver resources.
     */
    fun onDestroy() {
        Log.d(TAG, "onDestroy: cleaning up receiver")
        try {
            context.unregisterReceiver(alarmReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
    }

    fun setSelectedAlarmId(id: String?) {
        Log.d(TAG, "setSelectedAlarmId: id=$id")
        _selectedAlarmId.value = id
    }

    fun toggleAlarm(id: String, coroutineScope: CoroutineScope) {
        Log.d(TAG, "toggleAlarm: id=$id")
        val updated = _alarms.value.map {
            if (it.id == id) {
                val newEnabled = !it.enabled
                if (newEnabled) {
                    Log.d(TAG, "Enabling alarm $id - scheduling")
                    alarmScheduler.scheduleNextOccurrence(
                        it.id,
                        it.time,
                        it.days,
                        it.label,
                        it.repeatCount,
                        it.interval,
                        it.voiceMode,
                        it.dismissMode,
                        it.autoDismissSeconds,
                        it.soundId
                    )
                } else {
                    Log.d(TAG, "Disabling alarm $id - cancelling")
                    alarmScheduler.cancelAlarm(it.id)
                }
                it.copy(enabled = newEnabled)
            } else it
        }
        _alarms.value = updated
        coroutineScope.launch { clockRepository.saveAlarms(updated) }
    }

    fun updateAlarm(id: String, updatedItem: AlarmItem, coroutineScope: CoroutineScope) {
        Log.d(TAG, "updateAlarm: id=$id, updatedTime=${updatedItem.time}")
        alarmScheduler.cancelAlarm(id)
        if (updatedItem.enabled) {
            Log.d(TAG, "Alarm is enabled, scheduling next occurrence")
            alarmScheduler.scheduleNextOccurrence(
                updatedItem.id,
                updatedItem.time,
                updatedItem.days,
                updatedItem.label,
                updatedItem.repeatCount,
                updatedItem.interval,
                updatedItem.voiceMode,
                updatedItem.dismissMode,
                updatedItem.autoDismissSeconds,
                updatedItem.soundId
            )
        }
        val updated = _alarms.value.map {
            if (it.id == id) updatedItem else it
        }
        _alarms.value = updated
        coroutineScope.launch { clockRepository.saveAlarms(updated) }
    }

    private fun onAlarmTriggered(alarmId: String) {
        Log.d(TAG, "onAlarmTriggered: alarmId=$alarmId")
        val alarm = _alarms.value.find { it.id == alarmId }
        val label = alarm?.label ?: context.getString(com.airobot.features.R.string.clock_alarm)
        val timeStr = alarm?.time ?: "00:00"

        // If in Focus mode, do not show ringing screen. Queue the alarm.
        if (isFocusModeActive?.invoke() == true) {
            Log.d(TAG, "Focus mode active, suppressing alarm $alarmId and queuing it")
            alarm?.let {
                val updated = _pendingAlarms.value + it
                _pendingAlarms.value = updated
            }

            // Still dispatch event to notifier
            aiEventDispatcher.dispatch(
                AiEvent.AlarmTriggered(
                    id = java.util.UUID.randomUUID().toString(),
                    label = label,
                    time = timeStr,
                    triggerTime = System.currentTimeMillis()
                )
            )
            return
        }

        // If in Countdown mode, auto-background the timer
        if (isCountdownModeActive?.invoke() == true) {
            Log.d(TAG, "Timer running in Countdown mode, auto-backgrounding for alarm")
            onCountdownModeRunning?.invoke()
        }

        _ringingAlarmId.value = alarmId
        _isAlarmBackgrounded.value = false // Reset background state on new repetition

        // Dispatch Alarm Event
        aiEventDispatcher.dispatch(
            AiEvent.AlarmTriggered(
                id = java.util.UUID.randomUUID().toString(),
                label = label,
                time = timeStr,
                triggerTime = System.currentTimeMillis()
            )
        )
    }

    fun dismissAlarm(coroutineScope: CoroutineScope) {
        Log.d(TAG, "dismissAlarm called")
        val currentRingingId = _ringingAlarmId.value
        _ringingAlarmId.value = null
        _isAlarmBackgrounded.value = false // Reset background state

        // Stop AlarmRingingService
        val intent = Intent(AlarmRingingService.ACTION_ALARM_DISMISS).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)

        // Stop any sounds started directly from ViewModel (e.g. hourly chime)
        soundPlayer.stopSound()

        if (currentRingingId != null) {
            val alarm = _alarms.value.find { it.id == currentRingingId }
            if (alarm != null) {
                if (alarm.days.isEmpty()) {
                    Log.d(TAG, "One-off alarm dismissed - disabling it")
                    val updated = _alarms.value.map {
                        if (it.id == currentRingingId) it.copy(enabled = false) else it
                    }
                    _alarms.value = updated
                    coroutineScope.launch { clockRepository.saveAlarms(updated) }
                } else {
                    Log.d(TAG, "Repeating alarm dismissed - scheduling next weekday")
                    alarmScheduler.scheduleNextOccurrence(
                        alarm.id,
                        alarm.time,
                        alarm.days,
                        alarm.label,
                        alarm.repeatCount,
                        alarm.interval,
                        alarm.voiceMode,
                        alarm.dismissMode,
                        alarm.autoDismissSeconds,
                        alarm.soundId
                    )
                }
            }
        }
    }

    fun minimizeAlarm(silence: Boolean = true) {
        Log.d(TAG, "minimizeAlarm called, silence=$silence")
        _isAlarmBackgrounded.value = true

        if (silence) {
            // Send minimize broadcast to service to silence current ringing
            val intent = Intent(AlarmRingingService.ACTION_ALARM_MINIMIZE).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        }
    }

    fun clearAlarmBackgrounded() {
        _isAlarmBackgrounded.value = false
    }

    private fun clearRingingState(alarmId: String) {
        Log.d(TAG, "clearRingingState called for alarmId=$alarmId")
        // User clarification: When alarm rings and ends (auto-silence) without user interaction,
        // it actively releases lock (foreground) and goes to background, waiting for next wakeup.
        // We do not set ringingId to null yet, because it's technically still active in its snooze cycle.
        // We just minimize it.
        _isAlarmBackgrounded.value = true
        // The service flow combiner will see it as backgrounded and calculate its nextEventTimeMs as the next snooze time.
    }

    fun clearPendingAlarms() {
        Log.d(TAG, "clearPendingAlarms")
        _pendingAlarms.value = emptyList()
    }
}
