package com.airobot.features.clock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.app.PendingIntent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground Service that manages the lifecycle of a ringing alarm, including the repeat sequence and interval timers.
 */
@AndroidEntryPoint
class AlarmRingingService : Service() {

    @Inject
    lateinit var soundPlayer: SoundPlayer

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var sequenceJob: Job? = null

    private var alarmId: String = ""
    private var label: String = ""
    private var repeatCount: Int = 3
    private var intervalMin: Int = 5
    private var voiceMode: String = "standard"
    private var dismissMode: String = "manual"
    private var autoDismissSeconds: Int = 10
    private var soundId: String = "system_default"

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Dismiss broadcast received, stopping service")
            stopSelf()
        }
    }

    private val minimizeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Minimize broadcast received, silencing current ringing")
            soundPlayer.stopSound()
        }
    }

    companion object {
        private const val TAG = "AlarmRingingService"
        private const val NOTIFICATION_ID = 8888
        private const val CHANNEL_ID = "airobot_alarm_channel"
        private const val CHANNEL_NAME = "AIRobot Alarm Channel"

        // Broadcast actions
        const val ACTION_ALARM_RINGING = "com.airobot.clock.ALARM_RINGING"
        const val ACTION_ALARM_SEQUENCE_DONE = "com.airobot.clock.ALARM_SEQUENCE_DONE"
        const val ACTION_ALARM_DISMISS = "com.airobot.clock.ALARM_DISMISS"
        const val ACTION_ALARM_MINIMIZE = "com.airobot.clock.ALARM_MINIMIZE"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        
        // Register dismiss receiver — internal broadcast only, not exported
        ContextCompat.registerReceiver(
            this,
            dismissReceiver,
            IntentFilter(ACTION_ALARM_DISMISS),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Register minimize receiver
        ContextCompat.registerReceiver(
            this,
            minimizeReceiver,
            IntentFilter(ACTION_ALARM_MINIMIZE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun wakeUpScreenAndBringToForeground(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                powerManager.isInteractive
            } else {
                @Suppress("DEPRECATION")
                powerManager.isScreenOn
            }

            Log.d(TAG, "wakeUpScreenAndBringToForeground: isScreenOn=$isScreenOn")

            // Acquire temporary WakeLock to wake screen up and restore system adaptive brightness
            @Suppress("DEPRECATION")
            val wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "airobot:AlarmWakeLock"
            )
            wakeLock.acquire(10000L) // Hold for 10s to ensure system adjusts brightness and shows Activity
            Log.d(TAG, "WakeLock acquired for 10s successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock: ${e.message}", e)
        }

        try {
            // Explicitly launch MainActivity to bring it to the foreground
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                Log.d(TAG, "Successfully started MainActivity from service to show alarm overlay")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MainActivity from background: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")

        if (intent != null) {
            alarmId = intent.getStringExtra("alarmId") ?: ""
            label = intent.getStringExtra("label") ?: getString(com.airobot.features.R.string.clock_alarm)
            repeatCount = intent.getIntExtra("repeatCount", 3)
            intervalMin = intent.getIntExtra("interval", 5)
            voiceMode = intent.getStringExtra("voiceMode") ?: "standard"
            dismissMode = intent.getStringExtra("dismissMode") ?: "manual"
            autoDismissSeconds = intent.getIntExtra("autoDismissSeconds", 10)
            soundId = intent.getStringExtra("soundId") ?: "system_default"
        }

        Log.d(TAG, "Starting ringing sequence: alarmId=$alarmId, label=$label, repeatCount=$repeatCount, interval=$intervalMin, voiceMode=$voiceMode")

        // Create notification channel
        createNotificationChannel()

        // Show foreground notification
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start ringing loop sequence in coroutine
        startRingingSequence()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: cleaning up alarm ringing service")
        sequenceJob?.cancel()
        serviceJob.cancel()
        soundPlayer.stopSound()
        
        try {
            unregisterReceiver(dismissReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
        try {
            unregisterReceiver(minimizeReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering minimizeReceiver: ${e.message}")
        }

        // Notify ViewModel that ringing has completed/dismissed
        sendSequenceDoneBroadcast()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification for active ringing alarms"
                enableVibration(true)
                setSound(null, null) // Sound is handled manually by SoundPlayer
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(com.airobot.features.R.string.alarm_ringing_notification_title))
            .setContentText(label)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
    }

    private fun startRingingSequence() {
        sequenceJob?.cancel()
        sequenceJob = serviceScope.launch {
            if (voiceMode == "hint") {
                Log.d(TAG, "Hint mode: playing looping sound for autoDismissSeconds")
                // Wake up screen and launch Activity for hint/chime mode
                wakeUpScreenAndBringToForeground(this@AlarmRingingService)
                delay(800)
                sendRingingBroadcast()
                soundPlayer.playAlarmSound("hint", soundId, autoDismissSeconds * 1000L)
                // In hint mode, we don't repeat or wait 30s. 
                // We leave the service running so the UI overlay can do its auto-dismiss countdown.
                // The UI will broadcast ACTION_ALARM_DISMISS when the countdown finishes.
            } else {
                for (repeat in 1..repeatCount) {
                    Log.d(TAG, "Ringing sequence: playing alarm, repeat $repeat of $repeatCount")
                    
                    // Wake up the screen and bring to foreground on each repeat iteration (e.g. after sleep or minimize)
                    wakeUpScreenAndBringToForeground(this@AlarmRingingService)

                    // Delay slightly to allow MainActivity to register its local receiver
                    delay(800)

                    // Send ringing broadcast to wake up card UI on all repetitions
                    sendRingingBroadcast()
                    
                    // Symmetrically scale duration (45s for urgent, 30s for standard/others)
                    val playDurationMs = if (voiceMode == "urgent") 45_000L else 30_000L
                    
                    // Play sound for playDurationMs
                    soundPlayer.playAlarmSound(voiceMode, soundId, playDurationMs)
                    
                    // Wait for sound to play
                    delay(playDurationMs)
                    
                    if (repeat < repeatCount) {
                        val waitMs = intervalMin * 60_000L
                        Log.d(TAG, "Ringing sequence: sleeping for interval $intervalMin minute(s) ($waitMs ms)")
                        delay(waitMs)
                    }
                }
                Log.d(TAG, "Ringing sequence reached max repeatCount, stopping service")
                stopSelf()
            }
        }
    }

    private fun sendRingingBroadcast() {
        val intent = Intent(ACTION_ALARM_RINGING).apply {
            setPackage(packageName)
            putExtra("alarmId", alarmId)
            putExtra("label", label)
            putExtra("voiceMode", voiceMode)
            putExtra("dismissMode", dismissMode)
            putExtra("autoDismissSeconds", autoDismissSeconds)
            putExtra("soundId", soundId)
        }
        sendBroadcast(intent)
    }

    private fun sendSequenceDoneBroadcast() {
        val intent = Intent(ACTION_ALARM_SEQUENCE_DONE).apply {
            setPackage(packageName)
            putExtra("alarmId", alarmId)
        }
        sendBroadcast(intent)
    }
}
