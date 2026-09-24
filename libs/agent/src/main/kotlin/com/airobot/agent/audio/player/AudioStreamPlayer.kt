package com.airobot.agent.audio.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

class AudioStreamPlayer(
    private val sampleRate: Int,
    private val channels: Int,
    frameSizeMs: Int,
    private val context: Context? = null
) {
    companion object {
        private const val TAG = "AudioStreamPlayer"
    }

    private var audioTrack: AudioTrack
    private var isPlaying = false

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    init {
        val channelConfig = if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val bufferSize = calculateOptimalBufferSize(sampleRate, channelConfig)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        context?.let {
            audioManager = it.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            setupAudioFocus()
        }
    }

    private fun calculateOptimalBufferSize(sampleRate: Int, channelConfig: Int): Int {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )
        return minBufferSize * 3
    }

    private fun setupAudioFocus() {
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setOnAudioFocusChangeListener { focusChange ->
                        handleAudioFocusChange(focusChange)
                    }
                    .build()
            }
        }
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                Log.d(TAG, "Audio focus gained")
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                Log.d(TAG, "Audio focus lost, stopping playback")
                stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus transiently lost, allowed to duck")
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        audioManager?.let { am ->
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.requestAudioFocus(it) } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    { focusChange -> handleAudioFocusChange(focusChange) },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }

            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            Log.d(TAG, "Request audio focus status: $hasAudioFocus")
            return hasAudioFocus
        }
        return true
    }

    private fun abandonAudioFocus() {
        if (hasAudioFocus) {
            audioManager?.let { am ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                } else {
                    @Suppress("DEPRECATION")
                    am.abandonAudioFocus { focusChange -> handleAudioFocusChange(focusChange) }
                }
            }
            hasAudioFocus = false
            Log.d(TAG, "Audio focus abandoned")
        }
    }

    fun start() {
        synchronized(this) {
            if (isPlaying) {
                Log.d(TAG, "Audio is already playing, ignoring start request")
                return
            }

            if (!requestAudioFocus()) {
                Log.w(TAG, "Could not obtain audio focus, playback might be silent")
            }

            isPlaying = true
            if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack.play()
                Log.d(TAG, "AudioTrack started, playState: ${audioTrack.playState}")
            } else {
                Log.e(TAG, "AudioTrack not initialized, state: ${audioTrack.state}")
                isPlaying = false
                return
            }
        }
    }

    fun writeDirectly(pcmData: ByteArray) {
        if (isPlaying) {
            writeAudioData(pcmData)
        }
    }

    private fun writeAudioData(pcmData: ByteArray) {
        try {
            var written = 0
            while (written < pcmData.size && isPlaying) {
                val toWrite = minOf(pcmData.size - written, 4096)
                val bytesWritten = audioTrack.write(pcmData, written, toWrite)
                if (bytesWritten > 0) {
                    written += bytesWritten
                } else {
                    Log.e(TAG, "AudioTrack write failed: $bytesWritten")
                    when (bytesWritten) {
                        AudioTrack.ERROR_INVALID_OPERATION -> Log.e(TAG, "Invalid AudioTrack write operation")
                        AudioTrack.ERROR_BAD_VALUE -> Log.e(TAG, "Invalid argument in AudioTrack write")
                        AudioTrack.ERROR_DEAD_OBJECT -> Log.e(TAG, "AudioTrack is dead")
                        AudioTrack.ERROR -> Log.e(TAG, "Generic AudioTrack error")
                    }
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception writing audio data to AudioTrack", e)
        }
    }

    fun stop() {
        synchronized(this) {
            if (isPlaying) {
                isPlaying = false

                if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.pause()
                    audioTrack.flush()
                    Log.d(TAG, "AudioTrack paused and flushed")
                }
                abandonAudioFocus()
            }
        }
    }

    fun release() {
        stop()
        audioTrack.release()
    }

    suspend fun waitForPlaybackCompletion() {
        var position = 0
        while (isPlaying && audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING && audioTrack.playbackHeadPosition != position) {
            Log.i(TAG, "audioTrack.playState: ${audioTrack.playState}, playbackHeadPosition: ${audioTrack.playbackHeadPosition}")
            position = audioTrack.playbackHeadPosition
            delay(100)
        }
    }

    fun isCurrentlyPlaying(): Boolean {
        return isPlaying && audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING
    }

    protected fun finalize() {
        release()
    }
}
