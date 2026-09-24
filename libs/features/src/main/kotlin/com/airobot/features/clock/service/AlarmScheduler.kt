package com.airobot.features.clock.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface representing an exact alarm and hourly chime scheduler.
 */
interface AlarmScheduler {
    fun scheduleAlarm(
        alarmId: String,
        triggerTimeMillis: Long,
        label: String,
        repeatCount: Int,
        interval: Int,
        voiceMode: String,
        dismissMode: String = "manual",
        autoDismissSeconds: Int = 10,
        soundId: String = "system_default"
    )
    fun cancelAlarm(alarmId: String)
    fun scheduleNextOccurrence(
        alarmId: String,
        time: String,
        days: List<Int>,
        label: String,
        repeatCount: Int,
        interval: Int,
        voiceMode: String,
        dismissMode: String = "manual",
        autoDismissSeconds: Int = 10,
        soundId: String = "system_default"
    )
    fun scheduleHourlyChime(triggerTimeMillis: Long)
    fun cancelHourlyChime()
    fun hasExactAlarmPermission(): Boolean
}

/**
 * Implementation of AlarmScheduler using Android's AlarmManager.
 */
@Singleton
class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
        
        // Broadcast actions matching receiver intent filters
        const val ACTION_ALARM_TRIGGER = "com.airobot.clock.ALARM_TRIGGER"
        const val ACTION_HOURLY_CHIME = "com.airobot.clock.HOURLY_CHIME"
        
        // Request code offsets to prevent request code collisions
        private const val REQUEST_CODE_CHIME = 999999

        /**
         * Calculates the next epoch timestamp in milliseconds for an alarm.
         * @param time HH:mm format time string.
         * @param days Days of week list, matching React prototype (0 is Sunday, 1 is Monday... 6 is Saturday).
         */
        fun calculateNextTriggerTime(time: String, days: List<Int>): Long {
            val parts = time.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
    
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
    
            val now = System.currentTimeMillis()
            
            if (days.isEmpty()) {
                // One-off alarm
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            } else {
                // In Android Calendar: Sunday = 1, Monday = 2 ... Saturday = 7
                // React representation: Sunday = 0, Monday = 1 ... Saturday = 6
                // Mapping: Android day = React day + 1
                val targetDaysAndroid = days.map { it + 1 }
                
                var bestTime = Long.MAX_VALUE
                for (targetDay in targetDaysAndroid) {
                    val testCal = Calendar.getInstance().apply {
                        timeInMillis = calendar.timeInMillis
                    }
                    val todayAndroid = testCal.get(Calendar.DAY_OF_WEEK)
                    
                    var daysDiff = targetDay - todayAndroid
                    if (daysDiff < 0 || (daysDiff == 0 && testCal.timeInMillis <= now)) {
                        daysDiff += 7
                    }
                    testCal.add(Calendar.DAY_OF_YEAR, daysDiff)
                    
                    if (testCal.timeInMillis < bestTime) {
                        bestTime = testCal.timeInMillis
                    }
                }
                return bestTime
            }
        }
    }

    override fun scheduleAlarm(
        alarmId: String,
        triggerTimeMillis: Long,
        label: String,
        repeatCount: Int,
        interval: Int,
        voiceMode: String,
        dismissMode: String,
        autoDismissSeconds: Int,
        soundId: String
    ) {
        Log.d(TAG, "scheduleAlarm: alarmId=$alarmId, triggerTime=${java.util.Date(triggerTimeMillis)}, label=$label, repeatCount=$repeatCount, interval=$interval, voiceMode=$voiceMode, dismissMode=$dismissMode, soundId=$soundId")
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra("alarmId", alarmId)
            putExtra("label", label)
            putExtra("repeatCount", repeatCount)
            putExtra("interval", interval)
            putExtra("voiceMode", voiceMode)
            putExtra("dismissMode", dismissMode)
            putExtra("autoDismissSeconds", autoDismissSeconds)
            putExtra("soundId", soundId)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val requestCode = alarmId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission()) {
                    Log.w(TAG, "Exact alarm permission not granted, falling back to inexact alarm")
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                } else {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                    } catch (se: SecurityException) {
                        Log.e(TAG, "SecurityException while scheduling exact alarm, falling back to inexact: ${se.message}")
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                    }
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            }
            Log.d(TAG, "Alarm $alarmId scheduled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm $alarmId: ${e.message}", e)
        }
    }

    override fun cancelAlarm(alarmId: String) {
        Log.d(TAG, "cancelAlarm: alarmId=$alarmId")
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

        val requestCode = alarmId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Alarm $alarmId cancelled successfully")
        } else {
            Log.d(TAG, "No pending intent found for alarmId=$alarmId to cancel")
        }
    }

    override fun scheduleNextOccurrence(
        alarmId: String,
        time: String,
        days: List<Int>,
        label: String,
        repeatCount: Int,
        interval: Int,
        voiceMode: String,
        dismissMode: String,
        autoDismissSeconds: Int,
        soundId: String
    ) {
        Log.d(TAG, "scheduleNextOccurrence: alarmId=$alarmId, time=$time, days=$days")
        try {
            val nextTrigger = calculateNextTriggerTime(time, days)
            scheduleAlarm(alarmId, nextTrigger, label, repeatCount, interval, voiceMode, dismissMode, autoDismissSeconds, soundId)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating or scheduling next occurrence of alarm $alarmId: ${e.message}", e)
        }
    }

    override fun scheduleHourlyChime(triggerTimeMillis: Long) {
        Log.d(TAG, "scheduleHourlyChime: scheduling next chime at ${java.util.Date(triggerTimeMillis)}")

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_HOURLY_CHIME
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_CHIME, intent, flags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasExactAlarmPermission()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                } else {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                    } catch (se: SecurityException) {
                        Log.e(TAG, "SecurityException while scheduling exact chime, falling back to inexact: ${se.message}")
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                    }
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            }
            Log.d(TAG, "Hourly chime scheduled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling hourly chime: ${e.message}", e)
        }
    }

    override fun cancelHourlyChime() {
        Log.d(TAG, "cancelHourlyChime called")
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_HOURLY_CHIME
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_CHIME, intent, flags)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Hourly chime cancelled successfully")
        } else {
            Log.d(TAG, "No pending intent found for hourly chime to cancel")
        }
    }

    override fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }


}
