package com.airobot.agent.skills.volume

import android.content.Context
import android.media.AudioManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android system implementation of VolumeProvider.
 * Controls system music stream volume directly using AudioManager.
 */
@Singleton
class VolumeProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VolumeProvider {

    companion object {
        private const val TAG = "VolumeProviderImpl"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun setVolume(volume: Int) {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val mappedVolume = (volume / 100f * maxVolume).toInt().coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mappedVolume, 0)
            Log.d(TAG, "setVolume: set system volume to $volume% (AudioManager stream level: $mappedVolume/$maxVolume)")
        } catch (e: Exception) {
            Log.e(TAG, "setVolume: failed to set system volume", e)
        }
    }
}
