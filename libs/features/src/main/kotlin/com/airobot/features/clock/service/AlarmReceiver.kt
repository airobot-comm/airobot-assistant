package com.airobot.features.clock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver triggered by AlarmManager exact alarms and hourly chimes.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var clockRepository: com.airobot.features.clock.data.ClockRepository

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive: action=$action")

        when (action) {
            AlarmSchedulerImpl.ACTION_ALARM_TRIGGER -> {
                val alarmId = intent.getStringExtra("alarmId") ?: ""
                val label = intent.getStringExtra("label") ?: "闹钟"
                val repeatCount = intent.getIntExtra("repeatCount", 3)
                val interval = intent.getIntExtra("interval", 5)
                val voiceMode = intent.getStringExtra("voiceMode") ?: "standard"
                val dismissMode = intent.getStringExtra("dismissMode") ?: "manual"
                val autoDismissSeconds = intent.getIntExtra("autoDismissSeconds", 10)
                val soundId = intent.getStringExtra("soundId") ?: "system_default"

                Log.d(TAG, "Alarm triggered: alarmId=$alarmId, label=$label, repeatCount=$repeatCount, interval=$interval, voiceMode=$voiceMode, dismissMode=$dismissMode, soundId=$soundId")

                val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                    putExtra("alarmId", alarmId)
                    putExtra("label", label)
                    putExtra("repeatCount", repeatCount)
                    putExtra("interval", interval)
                    putExtra("voiceMode", voiceMode)
                    putExtra("dismissMode", dismissMode)
                    putExtra("autoDismissSeconds", autoDismissSeconds)
                    putExtra("soundId", soundId)
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Log.d(TAG, "Starting AlarmRingingService as Foreground Service")
                        context.startForegroundService(serviceIntent)
                    } else {
                        Log.d(TAG, "Starting AlarmRingingService")
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start AlarmRingingService: ${e.message}", e)
                }
            }

            AlarmSchedulerImpl.ACTION_HOURLY_CHIME -> {
                Log.d(TAG, "Hourly chime triggered")
                val pendingResult = goAsync()
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val calendar = java.util.Calendar.getInstance()
                        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                        val today = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1
                        
                        val alarms = clockRepository.loadAlarms()
                        val hasConflict = alarms.any { alarm ->
                            alarm.enabled && (alarm.days.isEmpty() || alarm.days.contains(today)) && alarm.time == String.format("%02d:00", currentHour)
                        }

                        if (hasConflict) {
                            Log.d(TAG, "Hourly chime skipped due to alarm conflict at ${currentHour}:00")
                        } else {
                            Log.d(TAG, "Broadcasting HOURLY_CHIME_TRIGGERED to ViewModel")
                            
                            // Send local broadcast to notify VM
                            val localIntent = Intent("com.airobot.clock.HOURLY_CHIME_TRIGGERED").apply {
                                setPackage(context.packageName)
                            }
                            context.sendBroadcast(localIntent)
                        }
                        
                        // Reschedule the next hourly chime
                        val config = clockRepository.loadHourlyChimeConfig()
                        if (config.enabled) {
                            val nextTriggerMillis = calculateNextChimeTime(config)
                            alarmScheduler.scheduleHourlyChime(nextTriggerMillis)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling hourly chime: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            
            else -> {
                Log.w(TAG, "Unhandled action: $action")
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
