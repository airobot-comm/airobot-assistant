package com.airobot.features.clock.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface representing a system sound player.
 */
interface SoundPlayer {
    /**
     * Plays an alarm sound and triggers vibration matching the alert mode profile.
     *
     * @param voiceMode The alert mode profile: "hint" (strictly adheres to ringer mode; notifications stream),
     *                  "standard" (looping alarm stream sound + 30s regular vibration), or
     *                  "urgent" (looping max-volume ringtone stream sound + 45s high-intensity vibration).
     * @param soundId The resource identifier of the sound file, or "system_default".
     * @param durationMs The duration in milliseconds before automatically stopping the sound and vibration.
     */
    fun playAlarmSound(voiceMode: String, soundId: String = "system_default", durationMs: Long = 30_000)
    fun playChimeSound(soundId: String = "system_default")
    fun playReminderSound()
    fun playTimerCompletionAlert(isFocusMode: Boolean)
    fun stopSound()
}

/**
 * Implementation of SoundPlayer using Android's RingtoneManager, AudioManager, and MediaPlayer.
 */
@Singleton
class SoundPlayerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val vibrationManager: VibrationManager
) : SoundPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var stopJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    private var originalAlarmVolume: Int? = null

    companion object {
        private const val TAG = "SoundPlayer"
    }

    override fun playAlarmSound(voiceMode: String, soundId: String, durationMs: Long) {
        Log.d(TAG, "playAlarmSound called with voiceMode=$voiceMode, soundId=$soundId, durationMs=$durationMs")
        
        // Stop any currently playing sound and cancel any active vibration
        stopSound()

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val ringerMode = audioManager.ringerMode

            // Handle Hint Mode system settings compliance (Silent = no sound/vibe, Vibrate = vibe only, Normal = sound+vibe)
            if (voiceMode == "hint") {
                if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                    Log.d(TAG, "Hint mode: System is silent, doing nothing")
                    return
                }

                if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                    Log.d(TAG, "Hint mode: System is vibrate-only, skipping audio playback")
                    return
                }
            }

            // Trigger alarm vibration via separate VibrationManager
            vibrationManager.vibrateAlarm(voiceMode)

            // Resolve sound type dynamically based on alert mode
            val type = when (voiceMode) {
                "hint" -> RingtoneManager.TYPE_NOTIFICATION
                "urgent" -> RingtoneManager.TYPE_RINGTONE
                else -> RingtoneManager.TYPE_ALARM
            }

            val urisToTry = mutableListOf<Uri>()
            
            if (soundId != "system_default") {
                val resId = context.resources.getIdentifier(soundId, "raw", context.packageName)
                if (resId != 0) {
                    urisToTry.add(Uri.parse("android.resource://${context.packageName}/$resId"))
                } else {
                    Log.w(TAG, "Raw resource $soundId not found, falling back to system default")
                }
            }
            
            urisToTry.addAll(listOfNotNull(
                RingtoneManager.getDefaultUri(type),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                android.provider.Settings.System.DEFAULT_RINGTONE_URI
            ))

            var played = false
            for (soundUri in urisToTry) {
                try {
                    Log.d(TAG, "Trying to play sound URI: $soundUri")
                    val player = MediaPlayer().apply {
                        setDataSource(context, soundUri)
                        
                        val usage = AudioAttributes.USAGE_ALARM
                        
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(usage)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        
                        isLooping = (voiceMode != "hint")
                        prepare()
                    }
                    
                    mediaPlayer = player
                    played = true
                    Log.d(TAG, "MediaPlayer prepared successfully for URI: $soundUri")
                    break
                } catch (ex: Exception) {
                    Log.w(TAG, "Failed to prepare MediaPlayer for URI $soundUri: ${ex.message}")
                }
            }

            if (!played) {
                Log.e(TAG, "All sound URIs failed to prepare. Alarm will be silent.")
                return
            }

            // Configure stream volume for urgent mode
            if (voiceMode == "urgent") {
                val streamType = AudioManager.STREAM_ALARM
                originalAlarmVolume = audioManager.getStreamVolume(streamType)
                val maxVolume = audioManager.getStreamMaxVolume(streamType)
                Log.d(TAG, "Urgent mode: setting alarm stream volume to max ($maxVolume), original volume was $originalAlarmVolume")
                audioManager.setStreamVolume(streamType, maxVolume, 0)
            }

            mediaPlayer?.start()
            Log.d(TAG, "MediaPlayer started successfully")

            // Automatically stop after durationMs
            stopJob = scope.launch {
                delay(durationMs)
                Log.d(TAG, "Ringing duration elapsed, stopping alarm automatically")
                stopSound()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to play alarm sound: ${e.message}", e)
        }
    }

    override fun playChimeSound(soundId: String) {
        Log.d(TAG, "playChimeSound called with soundId=$soundId")
        try {
            val urisToTry = mutableListOf<Uri>()
            
            if (soundId != "system_default") {
                val resId = context.resources.getIdentifier(soundId, "raw", context.packageName)
                if (resId != 0) {
                    urisToTry.add(Uri.parse("android.resource://${context.packageName}/$resId"))
                }
            }
            
            urisToTry.addAll(listOfNotNull(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
            ))

            var played = false
            for (soundUri in urisToTry) {
                try {
                    val tempPlayer = MediaPlayer().apply {
                        setDataSource(context, soundUri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setOnCompletionListener {
                            it.release()
                        }
                        prepare()
                    }
                    tempPlayer.start()
                    played = true
                    Log.d(TAG, "Chime sound started successfully for URI: $soundUri")
                    break
                } catch (ex: Exception) {
                    Log.w(TAG, "Failed to prepare chime MediaPlayer for URI $soundUri: ${ex.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play chime sound: ${e.message}", e)
        }
    }

    override fun playReminderSound() {
        Log.d(TAG, "playReminderSound called")
        try {
            val urisToTry = listOfNotNull(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
            )

            var played = false
            for (soundUri in urisToTry) {
                try {
                    // Play notification sound exactly once (not looping)
                    val tempPlayer = MediaPlayer().apply {
                        setDataSource(context, soundUri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setOnCompletionListener {
                            Log.d(TAG, "Reminder sound play complete, releasing MediaPlayer")
                            it.release()
                        }
                        prepare()
                    }
                    tempPlayer.start()
                    played = true
                    Log.d(TAG, "Reminder sound player started successfully for URI: $soundUri")
                    break
                } catch (ex: Exception) {
                    Log.w(TAG, "Failed to prepare reminder MediaPlayer for URI $soundUri: ${ex.message}")
                }
            }

            if (!played) {
                Log.e(TAG, "All reminder sound URIs failed to prepare.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play reminder sound: ${e.message}", e)
        }
    }

    override fun playTimerCompletionAlert(isFocusMode: Boolean) {
        Log.d(TAG, "playTimerCompletionAlert called with isFocusMode=$isFocusMode")
        if (isFocusMode) {
            // Focus complete: gentle notification + single haptic (10s)
            playAlarmSound("hint", "system_default", 10_000)
        } else {
            // Countdown complete: standard alarm + vibration (15s)
            playAlarmSound("standard", "system_default", 15_000)
        }
    }

    override fun stopSound() {
        Log.d(TAG, "stopSound called")
        stopJob?.cancel()
        stopJob = null
        
        try {
            originalAlarmVolume?.let { originalVol ->
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                Log.d(TAG, "Restoring original alarm stream volume to $originalVol")
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVol, 0)
                originalAlarmVolume = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring alarm stream volume: ${e.message}", e)
        }
        
        // Cancel any active haptic vibration
        vibrationManager.cancelVibration()
        
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                    Log.d(TAG, "MediaPlayer stopped")
                }
                it.release()
                Log.d(TAG, "MediaPlayer released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while stopping sound player: ${e.message}", e)
        } finally {
            mediaPlayer = null
        }
    }
}
