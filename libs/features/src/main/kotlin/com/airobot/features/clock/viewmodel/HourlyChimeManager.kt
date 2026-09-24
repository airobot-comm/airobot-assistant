package com.airobot.features.clock.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.airobot.features.aiserv.event.AiEvent
import com.airobot.features.aiserv.event.AiEventDispatcher
import com.airobot.features.FeatureCards
import com.airobot.features.aiserv.popup.PopupQueueService
import com.airobot.features.aiserv.popup.PopupServiceItem
import com.airobot.features.clock.cards.HourlyChimeOverlay
import com.airobot.features.clock.data.ClockRepository
import com.airobot.features.clock.data.model.ChimeMode
import com.airobot.features.clock.data.model.HourlyChimeConfig
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
import javax.inject.Singleton

/**
 * HourlyChimeManager — helper class managing the hourly chime settings, active chiming state,
 * broadcast receivers, and custom scheduling algorithm.
 */
@Singleton
class HourlyChimeManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clockRepository: ClockRepository,
    private val alarmScheduler: AlarmScheduler,
    private val soundPlayer: SoundPlayer,
    private val aiEventDispatcher: AiEventDispatcher,
    private val popupQueueService: PopupQueueService
) {
    companion object {
        private const val TAG = "HourlyChimeManager"
    }

    private val _hourlyChimeEnabled = MutableStateFlow(false)
    val hourlyChimeEnabled: StateFlow<Boolean> = _hourlyChimeEnabled.asStateFlow()

    private val _chimeConfig = MutableStateFlow(HourlyChimeConfig())
    val chimeConfig: StateFlow<HourlyChimeConfig> = _chimeConfig.asStateFlow()

    private val _isChiming = MutableStateFlow(false)
    val isChiming: StateFlow<Boolean> = _isChiming.asStateFlow()

    private var isCountdownModeActive: (() -> Boolean)? = null
    private var onCountdownModeRunning: (() -> Unit)? = null

    private val chimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == "com.airobot.clock.HOURLY_CHIME_TRIGGERED") {
                handleChimeTriggered()
            }
        }
    }

    fun initialize(
        coroutineScope: CoroutineScope,
        isCountdownModeActive: () -> Boolean,
        onCountdownModeRunning: () -> Unit
    ) {
        Log.d(TAG, "Initializing HourlyChimeManager")
        this.isCountdownModeActive = isCountdownModeActive
        this.onCountdownModeRunning = onCountdownModeRunning
        coroutineScope.launch {
            val config = clockRepository.loadHourlyChimeConfig()
            _chimeConfig.value = config
            _hourlyChimeEnabled.value = config.enabled

            // Schedule hourly chime on start if enabled
            if (config.enabled) {
                Log.d(TAG, "Scheduling hourly chime on initialization")
                val nextTime = calculateNextChimeTime(config)
                alarmScheduler.scheduleHourlyChime(nextTime)
            }
            registerReceiver()
        }

        coroutineScope.launch {
            combine(_hourlyChimeEnabled, _chimeConfig, _isChiming) { enabled, config, isChiming ->
                if (!enabled) return@combine null

                val now = System.currentTimeMillis()
                val nextTime = if (isChiming) {
                    now - 1000 // Highest priority if currently chiming
                } else {
                    calculateNextChimeTime(config)
                }

                ChimeServiceData(
                    isForeground = isChiming,
                    nextEventTimeMs = nextTime,
                    config = config
                )
            }.collect { chimeData ->
                if (chimeData != null) {
                    popupQueueService.enqueuePopup(ChimePopupItem(chimeData))
                } else {
                    popupQueueService.removePopup("chime_hourly")
                }
            }
        }
    }

    private data class ChimeServiceData(
        val isForeground: Boolean,
        val nextEventTimeMs: Long,
        val config: HourlyChimeConfig
    )

    private inner class ChimePopupItem(
        val data: ChimeServiceData
    ) : PopupServiceItem() {
        override val id: String = "chime_hourly"
        override val serviceType: String = FeatureCards.CHIME
        override val displayName: String = "整点报时"
        override val priority: Int = if (data.isForeground) 90 else 10
        override val timeoutDurationMs: Long =
            if (data.isForeground) 10_000L else 0L // Auto dismiss after 10s ringing
        override val nextEventTimeMs: Long? = data.nextEventTimeMs
        override val needsForegroundLock: Boolean = false
        override val isFullScreen: Boolean = data.isForeground

        override val subDialTitle: String = "整点报时"
        override val subDialValue: String
            get() {
                val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                return timeFormat.format(java.util.Date(data.nextEventTimeMs))
            }
        override val subDialIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
        override val subDialCustomIcon: (@Composable () -> Unit) = {
            com.airobot.features.clock.cards.chime.CuckooBird(
                modifier = androidx.compose.ui.Modifier.size(24.dp)
            )
        }

        @Composable
        override fun getSubDialColor(): androidx.compose.ui.graphics.Color {
            return com.airobot.framework.theme.RobotTheme.colors.alarmAccent
        }

        override val subDialOnClick: () -> Unit = { }

        override val onDismiss: () -> Unit = { dismissChime() }
        override val onTimeout: () -> Unit = { dismissChime() }

        @Composable
        override fun Content() {
            val config by _chimeConfig.collectAsState()
            val isChimingState by _isChiming.collectAsState()

            HourlyChimeOverlay(
                config = config,
                isChiming = isChimingState,
                onConfigChange = {
                    updateConfig(
                        CoroutineScope(kotlinx.coroutines.Dispatchers.Main),
                        it
                    )
                },
                onDismissChime = {
                    dismissChime()
                },
                onClose = { dismissChime() }
            )
        }
    }


    fun toggleHourlyChime(coroutineScope: CoroutineScope) {
        val config = _chimeConfig.value
        val newEnabled = !config.enabled
        val newConfig = config.copy(enabled = newEnabled)
        updateConfig(coroutineScope, newConfig)
    }

    fun updateConfig(coroutineScope: CoroutineScope, config: HourlyChimeConfig) {
        Log.d(TAG, "updateConfig: config=$config")
        _chimeConfig.value = config
        _hourlyChimeEnabled.value = config.enabled
        coroutineScope.launch {
            clockRepository.saveHourlyChimeConfig(config)
        }

        if (config.enabled) {
            val nextTime = calculateNextChimeTime(config)
            alarmScheduler.scheduleHourlyChime(nextTime)
        } else {
            alarmScheduler.cancelHourlyChime()
        }
    }

    fun dismissChime() {
        Log.d(TAG, "dismissChime")
        _isChiming.value = false
    }

    private fun registerReceiver() {
        try {
            val filter = IntentFilter("com.airobot.clock.HOURLY_CHIME_TRIGGERED")
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                chimeReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
            Log.d(TAG, "Registered HOURLY_CHIME_TRIGGERED receiver")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register receiver", e)
        }
    }

    private fun handleChimeTriggered() {
        Log.d(TAG, "Hourly chime trigger callback received, dispatching event and playing sound")

        // If in Countdown mode, auto-background the timer
        if (isCountdownModeActive?.invoke() == true) {
            Log.d(TAG, "Timer running in Countdown mode, auto-backgrounding for hourly chime")
            onCountdownModeRunning?.invoke()
        }

        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val timeStr = String.format("%02d:00", hour)

        // Set chiming state to true so ringing overlay pops up
        _isChiming.value = true

        // Play the hint sound looping for 10 seconds (matches auto-dismiss duration)
        soundPlayer.playAlarmSound("hint", "system_default", 10_000)

        aiEventDispatcher.dispatch(
            AiEvent.AlarmTriggered(
                id = java.util.UUID.randomUUID().toString(),
                label = context.getString(com.airobot.features.R.string.hourly_chime_label),
                time = timeStr,
                triggerTime = System.currentTimeMillis()
            )
        )

    }

    fun calculateNextChimeTime(config: HourlyChimeConfig): Long {
        val calendar = java.util.Calendar.getInstance()

        calendar.add(java.util.Calendar.HOUR_OF_DAY, 1)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        var found = false
        for (i in 0 until 48) {
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

            val matchesMode = when (config.mode) {
                ChimeMode.EVERY_HOUR -> true
                ChimeMode.ODD_HOUR -> hour % 2 != 0
                ChimeMode.EVEN_HOUR -> hour % 2 == 0
            }

            val inRange = if (config.startHour <= config.endHour) {
                hour in config.startHour..config.endHour
            } else {
                hour >= config.startHour || hour <= config.endHour
            }

            if (matchesMode && inRange) {
                found = true
                break
            }
            calendar.add(java.util.Calendar.HOUR_OF_DAY, 1)
        }

        if (!found) {
            calendar.timeInMillis = System.currentTimeMillis() + 3600 * 1000
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
        }

        return calendar.timeInMillis
    }
}
