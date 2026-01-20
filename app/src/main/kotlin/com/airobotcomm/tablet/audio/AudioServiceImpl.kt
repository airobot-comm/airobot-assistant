package com.airobotcomm.tablet.audio

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
import kotlinx.coroutines.flow.flow
import com.airobotcomm.tablet.audio.utils.OpusDecoder
import com.airobotcomm.tablet.audio.utils.OpusEncoder
import com.airobotcomm.tablet.audio.utils.OpusStreamPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 增强版音频管理器实现类
 * 使用真正的Opus编解码器和流式播放，实现了 AudioService 接口
 */
@Singleton
class AudioServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioService {
    companion object {
        private const val TAG = "AudioServiceImpl"
        private const val RECORD_SAMPLE_RATE = 16000
        private const val PLAY_SAMPLE_RATE = 24000
        private const val CHANNELS = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_DURATION_MS = 60
        private const val FRAME_SIZE = RECORD_SAMPLE_RATE * FRAME_DURATION_MS / 1000 * 2 // 16bit = 2 bytes
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var isPlayingState = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _audioEvents = MutableSharedFlow<AudioEvent>()
    override val audioEvents: SharedFlow<AudioEvent> = _audioEvents

    // AEC和NS处理器
    private var acousticEchoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    // Opus编解码器
    private var opusEncoder: OpusEncoder? = null
    private var opusDecoder: OpusDecoder? = null
    private var streamPlayer: OpusStreamPlayer? = null
    
    // 音频播放流
    private val _audioPlaybackFlow = MutableSharedFlow<ByteArray>()
    private var playbackJob: Job? = null
    private var isPlaybackSetup = false

    /**
     * 初始化音频系统
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun initialize(): Boolean {
        return try {
            // 初始化Opus编解码器
            opusEncoder = OpusEncoder(RECORD_SAMPLE_RATE, 1, FRAME_DURATION_MS)
            opusDecoder = OpusDecoder(PLAY_SAMPLE_RATE, 1, FRAME_DURATION_MS)
            streamPlayer = OpusStreamPlayer(PLAY_SAMPLE_RATE, 1, FRAME_DURATION_MS, context)
            
            setupAudioRecord()
            Log.d(TAG, "增强版音频系统初始化成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "增强版音频系统初始化失败", e)
            false
        }
    }

    /**
     * 设置录音器
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun setupAudioRecord() {
        if (!checkPermissions()) {
            throw SecurityException("缺少录音权限")
        }

        val bufferSize = AudioRecord.getMinBufferSize(RECORD_SAMPLE_RATE, CHANNELS, AUDIO_FORMAT)
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            RECORD_SAMPLE_RATE,
            CHANNELS,
            AUDIO_FORMAT,
            bufferSize * 2
        )

        // 设置AEC和NS
        setupAudioEffects()
    }

    /**
     * 设置音频效果（AEC+NS）
     */
    private fun setupAudioEffects() {
        audioRecord?.let { record ->
            try {
                // 回音消除
                if (AcousticEchoCanceler.isAvailable()) {
                    acousticEchoCanceler = AcousticEchoCanceler.create(record.audioSessionId)
                    acousticEchoCanceler?.enabled = true
                    Log.d(TAG, "AEC已启用")
                } else {
                    Log.w(TAG, "设备不支持AEC")
                }

                // 噪声抑制
                if (NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)
                    noiseSuppressor?.enabled = true
                    Log.d(TAG, "NS已启用")
                } else {
                    Log.w(TAG, "设备不支持NS")
                }
            } catch (e: Exception) {
                Log.e(TAG, "设置音频效果失败", e)
            }
        }
    }

    /**
     * 设置音频播放流
     */
    private fun setupAudioPlayback() {
        if (isPlaybackSetup) return
        
        isPlaybackSetup = true
        
        val pcmFlow = flow {
            _audioPlaybackFlow.collect { opusData ->
                try {
                    opusDecoder?.let { decoder ->
                        val pcmData = decoder.decode(opusData)
                        pcmData?.let { emit(it) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解码音频数据失败", e)
                }
            }
        }
        
        streamPlayer?.start(pcmFlow)
    }

    /**
     * 开始录音
     */
    override fun startRecording() {
        if (isRecording) return

        audioRecord?.let { record ->
            try {
                record.startRecording()
                isRecording = true

                scope.launch {
                    val buffer = ByteArray(FRAME_SIZE)
                    while (isRecording) {
                        val bytesRead = record.read(buffer, 0, buffer.size)
                        if (bytesRead > 0) {
                            val audioLevel = calculateRmsLevel(buffer, bytesRead)
                            _audioEvents.emit(AudioEvent.AudioLevel(audioLevel))
                            
                            opusEncoder?.let { encoder ->
                                val opusData = encoder.encode(buffer.copyOf(bytesRead))
                                opusData?.let { _audioEvents.emit(AudioEvent.AudioData(it)) }
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

    /**
     * 停止录音
     */
    override fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        audioRecord?.stop()
    }

    /**
     * 播放音频数据（单次播放）
     */
    override fun playAudio(audioData: ByteArray) {
        scope.launch {
            try {
                if (!isPlayingState) {
                    isPlayingState = true
                    setupAudioPlayback()
                }
                _audioPlaybackFlow.emit(audioData)
            } catch (e: Exception) {
                Log.e(TAG, "播放音频失败", e)
            }
        }
    }

    /**
     * 停止播放
     */
    override fun stopPlaying() {
        isPlayingState = false
        isPlaybackSetup = false
        streamPlayer?.stop()
    }

    /**
     * 开始流式播放
     */
    override fun startStreamPlayback(opusDataFlow: SharedFlow<ByteArray>) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            isPlayingState = true
            opusDataFlow.collect { opusData ->
                _audioPlaybackFlow.emit(opusData)
            }
        }
    }

    /**
     * 停止流式播放
     */
    override fun stopStreamPlayback() {
        isPlayingState = false
        isPlaybackSetup = false
        playbackJob?.cancel()
        streamPlayer?.stop()
    }

    /**
     * 等待播放完成
     */
    override suspend fun waitForPlaybackCompletion() {
        streamPlayer?.waitForPlaybackCompletion()
    }

    /**
     * 检查权限
     */
    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 清理资源
     */
    override fun cleanup() {
        stopRecording()
        stopStreamPlayback()
        
        acousticEchoCanceler?.release()
        noiseSuppressor?.release()
        audioRecord?.release()
        
        opusEncoder?.release()
        opusDecoder?.release()
        streamPlayer?.release()
        
        scope.cancel()
    }

    override fun isRecording(): Boolean = isRecording
    override fun isPlaying(): Boolean = isPlayingState

    /**
     * 计算音频强度 (RMS)
     */
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
    
    /**
     * 测试音频播放
     */
    override fun testAudioPlayback() {
        scope.launch {
            try {
                val testPlayer = OpusStreamPlayer(PLAY_SAMPLE_RATE, 1, FRAME_DURATION_MS, context)
                val sampleRate = PLAY_SAMPLE_RATE
                val duration = 1.0
                val frequency = 440.0
                val samples = (sampleRate * duration).toInt()
                val pcmData = ByteArray(samples * 2)
                
                for (i in 0 until samples) {
                    val sample = (32767 * kotlin.math.sin(2 * kotlin.math.PI * frequency * i / sampleRate)).toInt().toShort()
                    pcmData[i * 2] = (sample.toInt() and 0xFF).toByte()
                    pcmData[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                }
                
                val testFlow = flow { emit(pcmData) }
                testPlayer.start(testFlow)
                delay(1500)
                testPlayer.stop()
                testPlayer.release()
            } catch (e: Exception) {
                Log.e(TAG, "测试音频播放失败", e)
            }
        }
    }
}
