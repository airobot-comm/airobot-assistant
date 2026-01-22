package com.airobotcomm.tablet.audio

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import com.airobotcomm.tablet.audio.recorder.AudioRecorder
import com.airobotcomm.tablet.audio.recorder.DefaultAudioRecorder
import com.airobotcomm.tablet.audio.player.AudioPlayer
import com.airobotcomm.tablet.audio.player.DefaultAudioPlayer

/**
 * 音频状态枚举
 */
enum class AudioState {
    IDLE,           // 空闲
    INITIALIZING,   // 初始化中
    RECORDING,      // 录音中
    PLAYING,        // 播放中
    STREAM_PLAYING  // 流式播放中
}

/**
 * 音频状态数据类
 */
data class AudioStatus(
    val state: AudioState = AudioState.IDLE,
    val recordingLevel: Float = 0f,
    val errorMessage: String? = null
)

/**
 * 音频质量指标
 */
data class AudioQualityMetrics(
    val avgLevel: Float = 0f,
    val peakLevel: Float = 0f,
    val noiseFloor: Float = 0f,
    val signalToNoiseRatio: Float = 0f,
    val packetLossRate: Float = 0f,
    val latencyMs: Long = 0
)

/**
 * 音频质量监控器
 */
class AudioQualityMonitor {
    private val levels = mutableListOf<Float>()
    private var peakLevel = 0f
    private var noiseFloor = 0f
    
    fun addLevel(level: Float) {
        levels.add(level)
        peakLevel = maxOf(peakLevel, level)
        
        // 更新噪声基底（最近N个最小值的平均值）
        val recentLevels = levels.takeLast(100).sorted().take(20)
        noiseFloor = if (recentLevels.isNotEmpty()) recentLevels.average().toFloat() else 0f
    }
    
    fun getMetrics(): AudioQualityMetrics {
        val avgLevel = if (levels.isNotEmpty()) levels.average().toFloat() else 0f
        val snr = if (noiseFloor > 0) avgLevel / noiseFloor else 999f // 避免除零
        
        return AudioQualityMetrics(
            avgLevel = avgLevel,
            peakLevel = peakLevel,
            noiseFloor = noiseFloor,
            signalToNoiseRatio = snr,
            packetLossRate = 0f, // 可以通过网络层获取
            latencyMs = 0 // 可以通过时间戳计算
        )
    }
    
    fun clear() {
        levels.clear()
        peakLevel = 0f
        noiseFloor = 0f
    }
}

/**
 * 音频预处理器接口
 */
interface AudioPreprocessor {
    fun process(input: ByteArray): ByteArray
}

/**
 * 自适应噪声抑制预处理器
 */
class AdaptiveNoiseSuppressor : AudioPreprocessor {
    override fun process(input: ByteArray): ByteArray {
        // 这里可以实现更复杂的自适应噪声抑制算法
        // 或者调用Native库中的实现
        return input
    }
}

/**
 * 自动增益控制预处理器
 */
class AutomaticGainControl : AudioPreprocessor {
    private var gainFactor = 1.0f
    
    override fun process(input: ByteArray): ByteArray {
        // 简化的自动增益控制实现
        val output = input.clone()
        for (i in output.indices) {
            val sample = output[i].toInt() and 0xFF
            val adjustedSample = (sample * gainFactor).toInt().coerceIn(0, 255)
            output[i] = adjustedSample.toByte()
        }
        return output
    }
    
    fun adjustGain(targetLevel: Float, currentLevel: Float) {
        if (currentLevel > 0) {
            gainFactor = targetLevel / currentLevel
            // 限制增益范围，避免过度放大噪声
            gainFactor = gainFactor.coerceIn(0.5f, 2.0f)
        }
    }
}

/**
 * 错误恢复策略
 */
class ErrorRecoveryStrategy {
    private var retryCount = 0
    private val maxRetries = 3
    private val retryDelay = 1000L // 1秒重试延迟
    
    suspend fun <T> executeWithRecovery(operation: suspend () -> T?): T? {
        var lastError: Exception? = null
        
        repeat(maxRetries + 1) { attempt ->
            if (attempt > 0) {
                Log.d("ErrorRecovery", "重试第 $attempt 次...")
                delay(retryDelay * attempt) // 递增延迟
            }
            
            try {
                val result = operation()
                if (result != null) {
                    retryCount = 0 // 成功后重置计数
                    return result
                }
            } catch (e: Exception) {
                lastError = e
                Log.e("ErrorRecovery", "操作失败 (尝试 $attempt): ${e.message}", e)
            }
        }
        
        Log.e("ErrorRecovery", "所有重试都失败了", lastError)
        return null
    }
}

/**
 * 主音频管理器 - 协调录音和播放模块
 */
@Singleton
class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioManager"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 分离的录音和播放模块
    private val audioRecorder: AudioRecorder
    private val audioPlayer: AudioPlayer

    // 合并的音频事件流
    private val _audioEvents = MutableSharedFlow<AudioEvent>()
    val audioEvents: SharedFlow<AudioEvent> = _audioEvents

    // 状态管理
    private val _audioStatus = MutableStateFlow(AudioStatus())
    val audioStatus: StateFlow<AudioStatus> = _audioStatus.asStateFlow()
    
    // 质量监控
    private val audioQualityMonitor = AudioQualityMonitor()
    
    // 预处理器
    private var audioPreprocessor: AudioPreprocessor? = null
    
    // 错误恢复策略
    private val errorRecoveryStrategy = ErrorRecoveryStrategy()
    
    // 当前配置
    private var currentConfig: AudioConfig? = null

    init {
        // 初始化录音和播放模块
        audioRecorder = DefaultAudioRecorder(context)
        audioPlayer = DefaultAudioPlayer(context)
        
        // 监听录音和播放事件
        scope.launch {
            audioRecorder.audioEvents.collect { event ->
                when (event) {
                    is AudioEvent.AudioLevel -> {
                        _audioStatus.update { it.copy(recordingLevel = event.level) }
                        audioQualityMonitor.addLevel(event.level)
                    }
                    is AudioEvent.Error -> {
                        _audioStatus.update { it.copy(errorMessage = event.message) }
                    }
                    else -> {}
                }
                _audioEvents.emit(event)
            }
        }
        
        scope.launch {
            audioPlayer.onPlayingStateChanged.collect { isPlaying ->
                val newState = if (isPlaying) AudioState.PLAYING else AudioState.IDLE
                _audioStatus.update { it.copy(state = newState) }
            }
        }
        
        scope.launch {
            audioRecorder.onRecordingStateChanged.collect { isRecording ->
                val newState = if (isRecording) AudioState.RECORDING else AudioState.IDLE
                _audioStatus.update { it.copy(state = newState) }
            }
        }
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun initialize(config: AudioConfig): Boolean {
        currentConfig = config
        _audioStatus.update { it.copy(state = AudioState.INITIALIZING) }
        
        // 初始化录音模块
        val recorderInitialized = audioRecorder.initialize(config)
        if (!recorderInitialized) {
            Log.e(TAG, "录音模块初始化失败")
            _audioStatus.update { it.copy(state = AudioState.IDLE, errorMessage = "录音模块初始化失败") }
            return false
        }
        
        // 初始化播放模块
        val playerInitialized = audioPlayer.initialize(config)
        if (!playerInitialized) {
            Log.e(TAG, "播放模块初始化失败")
            _audioStatus.update { it.copy(state = AudioState.IDLE, errorMessage = "播放模块初始化失败") }
            return false
        }
        
        _audioStatus.update { it.copy(state = AudioState.IDLE) }
        Log.d(TAG, "音频系统初始化成功")
        return true
    }

    fun startRecording() {
        audioRecorder.startRecording()
    }

    fun stopRecording() {
        audioRecorder.stopRecording()
    }

    fun playAudio(audioData: ByteArray) {
        scope.launch {
            errorRecoveryStrategy.executeWithRecovery {
                try {
                    audioPlayer.playAudio(audioData)
                    Unit // 返回Unit表示成功
                } catch (e: Exception) {
                    Log.e(TAG, "播放音频失败", e)
                    null
                }
            }
        }
    }

    fun stopPlaying() {
        audioPlayer.stopPlaying()
    }

    fun startStreamPlayback(opusDataFlow: SharedFlow<ByteArray>) {
        audioPlayer.startStreamPlayback(opusDataFlow)
    }

    fun stopStreamPlayback() {
        audioPlayer.stopStreamPlayback()
    }

    suspend fun waitForPlaybackCompletion() {
        audioPlayer.waitForPlaybackCompletion()
    }

    fun cleanup() {
        audioRecorder.cleanup()
        audioPlayer.cleanup()
        scope.cancel()
    }

    fun isRecording(): Boolean = audioRecorder.isRecording()
    fun isPlaying(): Boolean = audioPlayer.isPlaying()

    fun getAudioQualityMetrics(): AudioQualityMetrics = audioQualityMonitor.getMetrics()
    
    fun setAudioPreprocessor(preprocessor: AudioPreprocessor?) {
        this.audioPreprocessor = preprocessor
    }
    
    fun getCurrentConfig(): AudioConfig? = currentConfig
}