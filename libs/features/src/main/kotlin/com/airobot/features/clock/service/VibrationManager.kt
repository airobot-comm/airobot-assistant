package com.airobot.features.clock.service

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface representing a device vibration manager for skeuomorphic alarm/timer alerts.
 */
interface VibrationManager {
    /**
     * Triggers vibration matching the alert mode profile.
     *
     * @param voiceMode The alert mode profile: "hint", "standard", or "urgent".
     */
    fun vibrateAlarm(voiceMode: String)

    /**
     * Cancels any active vibration.
     */
    fun cancelVibration()
}

/**
 * Implementation of VibrationManager using Android's Vibrator and AudioManager.
 */
@Singleton
class VibrationManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : VibrationManager {

    private val vibrator: Vibrator? by lazy {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    companion object {
        private const val TAG = "VibrationManager"
    }

    override fun vibrateAlarm(voiceMode: String) {
        Log.d(TAG, "vibrateAlarm called with voiceMode=$voiceMode")
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val ringerMode = audioManager.ringerMode

            // Handle Hint Mode system settings compliance
            if (voiceMode == "hint") {
                if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                    Log.d(TAG, "Hint mode: System is silent, skipping vibration")
                    return
                }

                vibrator?.let { v ->
                    if (v.hasVibrator()) {
                        Log.d(TAG, "Hint mode: triggering single haptic pulse")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            v.vibrate(200)
                        }
                    }
                }
            }

            // Handle Standard/Urgent continuous looping vibration
            if (voiceMode == "standard" || voiceMode == "urgent") {
                vibrator?.let { v ->
                    if (v.hasVibrator()) {
                        val pattern = if (voiceMode == "urgent") {
                            Log.d(TAG, "Urgent mode: triggering high-intensity haptic loop")
                            longArrayOf(0, 800, 400)
                        } else {
                            Log.d(TAG, "Standard mode: triggering standard haptic loop")
                            longArrayOf(0, 1000, 1000)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            v.vibrate(VibrationEffect.createWaveform(pattern, 0))
                        } else {
                            @Suppress("DEPRECATION")
                            v.vibrate(pattern, 0)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play alarm vibration: ${e.message}", e)
        }
    }

    override fun cancelVibration() {
        Log.d(TAG, "cancelVibration called")
        try {
            vibrator?.cancel()
            Log.d(TAG, "Vibrator cancelled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibration: ${e.message}", e)
        }
    }
}
