package com.airobotcomm.tablet.audio.recorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.airobotcomm.tablet.audio.AudioConfig
import com.airobotcomm.tablet.audio.AudioEvent
import com.airobotcomm.tablet.audio.tools.codec.OpusEncoder

/**
 * 默认音频录制器实现
 */
class DefaultAudioRecorder(private val context: Context) : AudioRecorder {
    companion object {
        private const val TAG = "DefaultAudioRecorder"
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _audioEvents = MutableSharedFlow<AudioEvent>()
    override val audioEvents: SharedFlow<AudioEvent> = _audioEvents

    private val _onRecordingStateChanged = MutableSharedFlow<Boolean>()
    override val onRecordingStateChanged: SharedFlow<Boolean> = _onRecordingStateChanged

    // AEC和NS处理器
    private var acousticEchoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    // Opus编码器
    private var opusEncoder: OpusEncoder? = null

    // 配置参数
    private lateinit var config: AudioConfig

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun initialize(config: AudioConfig): Boolean {
        this.config = config
        return try {
            // 初始化Opus编码器
            opusEncoder = OpusEncoder(config.recordSampleRate, config.channels, config.frameDurationMs)

            setupAudioRecord()
            Log.d(TAG, "音频录制器初始化成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "音频录制器初始化失败", e)
            false
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun setupAudioRecord() {
        if (!checkPermissions()) {
            throw SecurityException("缺少录音权限")
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

        // 设置AEC和NS
        setupAudioEffects()
    }

    private fun setupAudioEffects() {
        audioRecord?.let { record ->
            try {
                // 回音消除
                if (config.enableAec && AcousticEchoCanceler.isAvailable()) {
                    acousticEchoCanceler = AcousticEchoCanceler.create(record.audioSessionId)
                    acousticEchoCanceler?.enabled = true
                    Log.d(TAG, "AEC已启用")
                } else {
                    Log.w(TAG, "设备不支持AEC或AEC被禁用")
                }

                // 噪声抑制
                if (config.enableNs && NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)
                    noiseSuppressor?.enabled = true
                    Log.d(TAG, "NS已启用")
                } else {
                    Log.w(TAG, "设备不支持NS或NS被禁用")
                }
            } catch (e: Exception) {
                Log.e(TAG, "设置音频效果失败", e)
            }
        }
    }

    override fun startRecording() {
        if (isRecording) return

        audioRecord?.let { record ->
            try {
                record.startRecording()
                isRecording = true
                scope.launch {
                    _onRecordingStateChanged.emit(true)
                }

                scope.launch {
                    val frameSize = (config.recordSampleRate * config.frameDurationMs) / 1000 * 2 // 16bit = 2 bytes
                    val buffer = ByteArray(frameSize)
                    while (isRecording) {
                        val bytesRead = record.read(buffer, 0, buffer.size)
                        if (bytesRead > 0) {
                            val audioLevel = calculateRmsLevel(buffer, bytesRead)
                            scope.launch { _audioEvents.emit(AudioEvent.AudioLevel(audioLevel)) }

                            opusEncoder?.let { encoder ->
                                val opusData = encoder.encode(buffer.copyOf(bytesRead))
                                opusData?.let { 
                                    scope.launch { _audioEvents.emit(AudioEvent.AudioData(it)) }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "录音失败", e)
                scope.launch { _audioEvents.emit(AudioEvent.Error("录音失败: ${e.message}")) }
            }
        }
    }

    override fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        audioRecord?.stop()
        scope.launch {
            _onRecordingStateChanged.emit(false)
        }
    }

    override fun isRecording(): Boolean = isRecording

    override fun cleanup() {
        stopRecording()

        acousticEchoCanceler?.release()
        noiseSuppressor?.release()
        audioRecord?.release()

        opusEncoder?.release()

        scope.cancel()
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun calculateRmsLevel(buffer: ByteArray, bytesRead: Int): Float {
        val shorts = ShortArray(bytesRead / 2)
        for (i in 0 until bytesRead step 2) {
            shorts[i / 2] = ((buffer[i + 1].toInt() and 0xFF) shl 8 or (buffer[i].toInt() and 0xFF)).toShort()
        }

        var sumOfSquares = 0.0
        for (sample in shorts) {
            val normalizedSample = sample / 32768.0
            sumOfSquares += normalizedSample * normalizedSample
        }

        val rms = Math.sqrt(sumOfSquares / shorts.size)
        return Math.min(1.0, rms * 3.0).toFloat()
    }
}