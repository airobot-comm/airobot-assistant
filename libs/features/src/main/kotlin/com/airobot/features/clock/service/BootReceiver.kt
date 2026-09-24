package com.airobot.features.clock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.airobot.features.clock.data.ClockRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reschedules all enabled alarms upon device boot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var clockRepository: ClockRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted, rescheduling alarms...")
            
            // Using GlobalScope since BroadcastReceiver lifecycle is extremely short
            GlobalScope.launch {
                try {
                    val alarms = clockRepository.loadAlarms()
                    alarms.forEach { alarm ->
                        if (alarm.enabled) {
                            Log.d("BootReceiver", "Rescheduling alarm: ${alarm.id} for time: ${alarm.time}")
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
                    
                    val config = clockRepository.loadHourlyChimeConfig()
                    if (config.enabled) {
                        Log.d("BootReceiver", "Rescheduling hourly chime")
                        val nextTriggerMillis = calculateNextChimeTime(config)
                        alarmScheduler.scheduleHourlyChime(nextTriggerMillis)
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms on boot", e)
                }
            }
        }
    }

    private fun calculateNextChimeTime(config: com.airobot.features.clock.data.model.HourlyChimeConfig): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.HOUR_OF_DAY, 1)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        var found = false
        for (i in 0 until 48) {
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            
            val matchesMode = when (config.mode) {
                com.airobot.features.clock.data.model.ChimeMode.EVERY_HOUR -> true
                com.airobot.features.clock.data.model.ChimeMode.ODD_HOUR -> hour % 2 != 0
                com.airobot.features.clock.data.model.ChimeMode.EVEN_HOUR -> hour % 2 == 0
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
