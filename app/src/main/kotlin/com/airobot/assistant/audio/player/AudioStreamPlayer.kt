package com.airobot.assistant.audio.player

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

/**
 * 音频流播放器 - 负责播放PCM音频数据流
 */
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
    private val playerScope = CoroutineScope(Dispatchers.IO + Job())
    private var isPlaying = false
    private var playbackJob: Job? = null

    // 音频焦点管理
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    init {
        val channelConfig = if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val bufferSize = calculateOptimalBufferSize(sampleRate, channelConfig)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
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

        // 初始化音频焦点管理
        context?.let {
            audioManager = it.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            setupAudioFocus()
        }
    }

    /**
     * 计算最优缓冲区大小
     */
    private fun calculateOptimalBufferSize(sampleRate: Int, channelConfig: Int): Int {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // 使用3倍最小缓冲区大小以确保流畅播放
        return minBufferSize * 3
    }

    private fun setupAudioFocus() {
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
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
                Log.d(TAG, "获得音频焦点")
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                Log.d(TAG, "失去音频焦点")
                stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "音频焦点被降低")
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
            Log.d(TAG, "请求音频焦点结果: $hasAudioFocus")
            return hasAudioFocus
        }
        return true // 如果没有context，假设有焦点
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
            Log.d(TAG, "释放音频焦点")
        }
    }

    /**
     * 开始播放PCM数据流
     */
    fun start(pcmFlow: Flow<ByteArray?>) {
        synchronized(this) {
            // 如果已经在播放，不要重新启动
            if (isPlaying) {
                Log.d(TAG, "播放器已经在运行，跳过重新启动")
                return
            }

            // 取消之前的播放任务
            playbackJob?.cancel()
            
            // 请求音频焦点
            if (!requestAudioFocus()) {
                Log.w(TAG, "无法获得音频焦点，但仍尝试播放")
            }

            isPlaying = true
            if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack.play()
                Log.d(TAG, "AudioTrack开始播放，状态: ${audioTrack.playState}")
            } else {
                Log.e(TAG, "AudioTrack未初始化，状态: ${audioTrack.state}")
                isPlaying = false
                return
            }

            playbackJob = playerScope.launch {
                try {
                    Log.d(TAG, "开始收集PCM数据流")
                    pcmFlow.collect { pcmData ->
                        if (isPlaying && pcmData != null && pcmData.isNotEmpty()) {
                            writeAudioData(pcmData)
                        }
                    }
                    Log.d(TAG, "PCM数据流收集完成")
                } catch (e: Exception) {
                    Log.e(TAG, "播放音频流时出错", e)
                } finally {
                    Log.d(TAG, "播放任务结束")
                    isPlaying = false
                    audioTrack.pause()
                    audioTrack.flush()
                }
            }
        }
    }

    /**
     * 写入音频数据到AudioTrack
     */
    private fun writeAudioData(pcmData: ByteArray) {
        try {
            var written = 0
            while (written < pcmData.size && isPlaying) {
                val toWrite = minOf(pcmData.size - written, 4096) // 每次最多写4KB
                val chunk = pcmData.sliceArray(written until written + toWrite)
                
                val bytesWritten = audioTrack.write(chunk, 0, chunk.size)
                if (bytesWritten > 0) {
                    written += bytesWritten
                } else {
                    Log.e(TAG, "AudioTrack写入失败: $bytesWritten")
                    when (bytesWritten) {
                        AudioTrack.ERROR_INVALID_OPERATION -> Log.e(TAG, "无效操作")
                        AudioTrack.ERROR_BAD_VALUE -> Log.e(TAG, "错误的参数值")
                        AudioTrack.ERROR_DEAD_OBJECT -> Log.e(TAG, "AudioTrack已死")
                        AudioTrack.ERROR -> Log.e(TAG, "通用错误")
                    }
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "写入音频数据异常", e)
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        synchronized(this) {
            if (isPlaying) {
                isPlaying = false
                playbackJob?.cancel()
                playbackJob = null

                if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.pause()
                    audioTrack.flush()
                    Log.d(TAG, "AudioTrack暂停并清空缓冲区")
                }
                abandonAudioFocus()
            }
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        stop()
        playbackJob?.cancel()
        playbackJob = null
        audioTrack.release()
        playerScope.cancel()
    }

    /**
     * 等待播放完成
     */
    suspend fun waitForPlaybackCompletion() {
        var position = 0
        while (isPlaying && audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING && audioTrack.playbackHeadPosition != position) {
            Log.i(TAG, "audioTrack.playState: ${audioTrack.playState}, playbackHeadPosition: ${audioTrack.playbackHeadPosition}")
            position = audioTrack.playbackHeadPosition
            delay(100) // 检查间隔
        }
    }

    /**
     * 检查当前是否正在播放
     */
    fun isCurrentlyPlaying(): Boolean {
        return isPlaying && audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING
    }

    protected fun finalize() {
        release()
    }
}
