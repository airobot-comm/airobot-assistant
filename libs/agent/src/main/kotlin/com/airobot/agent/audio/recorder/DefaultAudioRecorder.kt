package com.airobot.agent.audio.recorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.airobot.agent.audio.AudioConfig
import com.airobot.agent.audio.AudioEvent
import com.airobot.agent.audio.AudioWorkState
import com.airobot.agent.audio.tools.afe.AfeManager
import com.airobot.agent.audio.tools.kws.KwsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Audio recorder implementation - Responsible for hardware capture and lifecycle management.
 *
 * Responsibilities:
 * 1. Manage AudioRecord hardware state
 * 2. Handle hardware-level Acoustic Echo Cancellation (AEC) and Noise Suppression (NS)
 * 3. Drive AudioRecordPipeline for business data processing
 */
class DefaultAudioRecorder(
    private val context: Context,
    private val kwsManager: KwsManager,
    private val afeManager: AfeManager
) : AudioRecorder {
    companion object {
        private const val TAG = "DefaultAudioRecorder"
    }

    private var audioRecord: AudioRecord? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Audio recording pipeline
    private var pipeline: AudioRecordPipeline? = null

    private val _onRecordingStateChanged = MutableSharedFlow<Boolean>(replay = 1)
    override val onRecordingStateChanged: SharedFlow<Boolean> = _onRecordingStateChanged

    private lateinit var config: AudioConfig
    private var isRunning = false
    private var recordingJob: Job? = null

    // Unified event flow reference
    private var externalEvents: MutableSharedFlow<AudioEvent>? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun initialize(config: AudioConfig, events: MutableSharedFlow<AudioEvent>): Boolean {
        this.config = config
        this.externalEvents = events
        return try {
            // Initialize the recording pipeline, passing the external event flow directly
            val newPipeline = AudioRecordPipeline(context, config, events, kwsManager, afeManager)
            pipeline = newPipeline

            setupAudioRecord()
            Log.d(TAG, "Audio Recorder initialized with injected pipeline and events")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Audio Recorder", e)
            false
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun setupAudioRecord() {
        if (!checkPermissions()) {
            throw SecurityException("Permissions denied for audio recording")
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            config.recordSampleRate,
            if (config.channels == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO,
            config.audioFormat
        )

        if (minBufferSize <= 0) {
            throw IllegalStateException("Invalid buffer size: $minBufferSize. Check sample rate: ${config.recordSampleRate}")
        }

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            config.recordSampleRate,
            if (config.channels == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO,
            config.audioFormat,
            minBufferSize * 2
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("AudioRecord failed to initialize. State: ${record.state}")
        }

        audioRecord = record
        afeManager.setupHardwareAfe(record.audioSessionId, config.enableAec, config.enableNs)
    }

    override fun startRecording() {
        if (isRunning) return
        val record = audioRecord ?: return

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord is not initialized, cannot start recording")
            externalEvents?.tryEmit(AudioEvent.SystemError("Recorder not initialized"))
            return
        }

        isRunning = true
        recordingJob = scope.launch {
            try {
                record.startRecording()
                _onRecordingStateChanged.emit(true)
                Log.d(TAG, "Recording started")

                runReadLoop(record)
            } catch (e: Exception) {
                Log.e(TAG, "Error in recording job", e)
                externalEvents?.emit(AudioEvent.SystemError("Recording error: ${e.message}"))
            } finally {
                try {
                    if (record.state == AudioRecord.STATE_INITIALIZED &&
                        record.recordingState == AudioRecord.RECORDSTATE_RECORDING
                    ) {
                        record.stop()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping record", e)
                }
                _onRecordingStateChanged.emit(false)
                Log.d(TAG, "Recording stopped")
            }
        }
    }

    private suspend fun runReadLoop(record: AudioRecord) {
        val frameSize = (config.recordSampleRate * config.frameDurationMs) / 1000 * 2
        val buffer = ByteArray(frameSize)

        while (isRunning && recordingJob?.isActive == true) {
            val bytesRead = record.read(buffer, 0, buffer.size)
            if (bytesRead <= 0) {
                if (bytesRead < 0) Log.e(TAG, "Read error: $bytesRead")
                continue
            }

            // Send raw captured data into the pipeline
            pipeline?.processFrame(buffer.copyOf(bytesRead))
        }
    }

    override fun stopRecording() {
        isRunning = false
        recordingJob?.cancel()
    }

    override fun isRecording(): Boolean = isRunning

    override fun setWorkState(state: AudioWorkState) {
        pipeline?.setWorkState(state)
    }

    override fun cleanup() {
        stopRecording()
        afeManager.releaseHardwareAfe()
        audioRecord?.release()
        pipeline?.cleanup()
        scope.cancel()
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
