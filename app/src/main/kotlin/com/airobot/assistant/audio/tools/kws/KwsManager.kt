package com.airobot.assistant.audio.tools.kws

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineStream

/**
 * KWS工具类 - 纯粹的关键词检测功能，无状态
 * 
 * 使用方式：
 * 1. 调用 init() 初始化
 * 2. 调用 process() 处理音频帧，返回检测到的关键词（如果有）
 * 3. 调用 cleanup() 释放资源
 */
class KwsManager(private val context: Context) {
    companion object {
        private const val TAG = "KwsManager"
        private const val SAMPLE_RATE = 16000
        private const val ASSET_DIR = "kws-zipformer-zh-en-3M-2025-12-20"
    }

    private var spotter: KeywordSpotter? = null
    private var stream: OnlineStream? = null
    private var isInitialized = false

    /**
     * 初始化KWS引擎
     */
    fun init(): Boolean {
        if (isInitialized) return true
        
        try {
            val config = KeywordSpotterConfig()
            config.featConfig.sampleRate = SAMPLE_RATE
            config.featConfig.featureDim = 80
            
            // 模型文件路径
            config.modelConfig.transducer.encoder = "$ASSET_DIR/encoder-epoch-13-avg-2-chunk-16-left-64.onnx"
            config.modelConfig.transducer.decoder = "$ASSET_DIR/decoder-epoch-13-avg-2-chunk-16-left-64.onnx"
            config.modelConfig.transducer.joiner = "$ASSET_DIR/joiner-epoch-13-avg-2-chunk-16-left-64.onnx"
            config.modelConfig.tokens = "$ASSET_DIR/tokens.txt"
            
            config.modelConfig.numThreads = 1
            config.modelConfig.provider = "cpu"
            config.modelConfig.modelType = "zipformer2"
            
            config.keywordsFile = "$ASSET_DIR/keywords.txt"
            
            // 优化参数：降低阈值可以提高灵敏度，但误差也会提升
            config.keywordsScore = 0.85f      // (原 1.0f) 降低可信度要求
            config.keywordsThreshold = 0.15f // (原 0.20f) 降低触发概率阈值
            
            Log.d(TAG, "初始化KWS: $ASSET_DIR")
            
            // 创建引擎
            spotter = KeywordSpotter(context.assets, config)
            stream = spotter?.createStream()
            
            isInitialized = true
            Log.d(TAG, "KWS初始化成功")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "KWS初始化失败", e)
            return false
        }
    }

    /**
     * 处理音频数据，返回检测到的关键词
     * 
     * @param pcmData PCM音频数据 (16kHz, 16bit, mono)
     * @return 检测到的关键词，如果没有检测到返回null
     */
    fun process(pcmData: ByteArray): String? {
        val currentStream = stream
        val currentSpotter = spotter
        
        if (!isInitialized || currentSpotter == null || currentStream == null) {
            Log.w(TAG, "KWS not initialized properly")
            return null
        }

        try {
            // 转换为float数组
            val samples = FloatArray(pcmData.size / 2)
            for (i in samples.indices) {
                val sample = (pcmData[i * 2].toInt() and 0xFF) or (pcmData[i * 2 + 1].toInt() shl 8)
                samples[i] = sample.toShort() / 32768f
            }

            // 送入KWS引擎
            currentStream.acceptWaveform(samples, SAMPLE_RATE)
            while (currentSpotter.isReady(currentStream)) {
                currentSpotter.decode(currentStream)
                val result = currentSpotter.getResult(currentStream)
                
                if (result != null && result.keyword.isNotEmpty()) {
                    Log.d(TAG, "KWS Detected Wake Word: ${result.keyword}")
                    return result.keyword
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "KWS processing failed: ${e.message}", e)
        }
        
        return null
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        stream?.release()
        spotter?.release()
        stream = null
        spotter = null
        isInitialized = false
        Log.d(TAG, "KWS已清理")
    }
}

