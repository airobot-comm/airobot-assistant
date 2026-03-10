package com.airobot.assistant.audio.recorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.airobot.assistant.audio.AudioConfig
import com.airobot.assistant.audio.AudioEvent

/**
 * 录音器实现 - 负责硬件采集与生命周期管理
 * 
 * 职责：
 * 1. 管理 AudioRecord 硬件状态
 * 2. 处理硬件级回音消除 (AEC) 和噪声抑制 (NS)
 * 3. 驱动 AudioRecordPipeline 进行业务数据处理
 */
class DefaultAudioRecorder(private val context: Context) : AudioRecorder {
    companion object {
        private const val TAG = "DefaultAudioRecorder"
    }

    private var audioRecord: AudioRecord? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 录音状态流水线
    private var pipeline: AudioRecordPipeline? = null

    private val _onRecordingStateChanged = MutableSharedFlow<Boolean>(replay = 1)
    override val onRecordingStateChanged: SharedFlow<Boolean> = _onRecordingStateChanged

    private var acousticEchoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null

    private lateinit var config: AudioConfig
    private var isRunning = false
    private var recordingJob: Job? = null
    
    // 统一引用的事件流
    private var externalEvents: MutableSharedFlow<AudioEvent>? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun initialize(config: AudioConfig, events: MutableSharedFlow<AudioEvent>): Boolean {
        this.config = config
        this.externalEvents = events
        return try {
            // 初始化处理流水线，直接传入外部事件流
            val newPipeline = AudioRecordPipeline(context, config, events)
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

        val bufferSize = AudioRecord.getMinBufferSize(
            config.recordSampleRate,
            if (config.channels == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO,
            config.audioFormat
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            config.recordSampleRate,
            if (config.channels == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO,
            config.audioFormat,
            bufferSize * 2
        )

        setupAudioEffects()
    }

    private fun setupAudioEffects() {
        audioRecord?.let { record ->
            try {
                // Aec，NS开启，提升 语音质量
                if (config.enableAec && AcousticEchoCanceler.isAvailable()) {
                    acousticEchoCanceler = AcousticEchoCanceler.create(record.audioSessionId)
                    acousticEchoCanceler?.enabled = true
                }
                if (config.enableNs && NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)
                    noiseSuppressor?.enabled = true
                }
                
                // AGC 自动增益，提升 KWS 灵敏度
                if (AutomaticGainControl.isAvailable()) {
                    automaticGainControl = AutomaticGainControl.create(record.audioSessionId)
                    automaticGainControl?.enabled = true
                    Log.d(TAG, "AutomaticGainControl (AGC) enabled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup audio effects", e)
            }
        }
    }

    override fun startRecording() {
        if (isRunning) return
        val record = audioRecord ?: return

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
                record.stop()
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
            
            // 将采集到的原始数据送入 Pipeline
            pipeline?.processFrame(buffer.copyOf(bytesRead))
        }
    }

    override fun stopRecording() {
        isRunning = false
        recordingJob?.cancel()
    }

    override fun isRecording(): Boolean = isRunning

    override fun setWorkState(state: com.airobot.assistant.audio.AudioWorkState) {
        pipeline?.setWorkState(state)
    }

    override fun cleanup() {
        stopRecording()
        acousticEchoCanceler?.release()
        noiseSuppressor?.release()
        automaticGainControl?.release()
        audioRecord?.release()
        pipeline?.cleanup()
        scope.cancel()
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(context,
            Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
}
