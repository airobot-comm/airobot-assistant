package com.airobotcomm.tablet.audio.tools.kws

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.IOException

class KwsManager(private val context: Context) {
    companion object {
        private const val TAG = "KwsManager"
        private const val SAMPLE_RATE = 16000
        private const val ASSET_DIR = "sherpa-onnx-kws-zipformer-zh-en-3M-2025-12-20"
    }

    private var spotter: KeywordSpotter? = null
    private var stream: OnlineStream? = null
    
    // Ring buffer to store audio history (2 seconds)
    private val bufferSize = SAMPLE_RATE * 2 * 2
    private val audioBuffer = ByteArray(bufferSize)
    private var bufferHead = 0

    private val _kwsEvents = MutableSharedFlow<KwsEvent>()
    val kwsEvents: SharedFlow<KwsEvent> = _kwsEvents
    
    private var isInitialized = false

    fun init() {
        if (isInitialized) return
        
        try {
            val config = KeywordSpotterConfig()
            config.featConfig.sampleRate = SAMPLE_RATE
            config.featConfig.featureDim = 80
            
            // Use relative paths in assets
            config.modelConfig.transducer.encoder = "$ASSET_DIR/encoder-epoch-13-avg-2-chunk-16-left-64.onnx"
            config.modelConfig.transducer.decoder = "$ASSET_DIR/decoder-epoch-13-avg-2-chunk-16-left-64.onnx"
            config.modelConfig.transducer.joiner = "$ASSET_DIR/joiner-epoch-13-avg-2-chunk-16-left-64.onnx"
            config.modelConfig.tokens = "$ASSET_DIR/tokens.txt"
            
            config.modelConfig.numThreads = 1
            config.modelConfig.provider = "cpu"
            config.modelConfig.modelType = "zipformer2" // Explicitly set model type if needed, or let it infer?
            // Actually, for zipformer, we might not need to set modelType if we set transducer paths, but let's be careful.
            // Let's rely on inference first, but definitely set tokens.
            
            config.keywordsFile = "$ASSET_DIR/keywords.txt"
            
            Log.d(TAG, "Configuring KWS with:")
            Log.d(TAG, "Encoder: ${config.modelConfig.transducer.encoder}")
            Log.d(TAG, "Decoder: ${config.modelConfig.transducer.decoder}")
            Log.d(TAG, "Joiner: ${config.modelConfig.transducer.joiner}")
            Log.d(TAG, "Tokens: ${config.modelConfig.tokens}")
            Log.d(TAG, "Keywords: ${config.keywordsFile}")
            
            // Initialize with AssetManager
            Log.d(TAG, "Creating KeywordSpotter...")
            spotter = KeywordSpotter(context.assets, config)
            Log.d(TAG, "Creating OnlineStream...")
            stream = spotter?.createStream()
            
            isInitialized = true
            Log.d(TAG, "KWS Initialized successfully with assets")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize KWS", e)
            e.printStackTrace()
        }
    }

    fun process(audioData: ByteArray) {
        val currentStream = stream
        val currentSpotter = spotter
        
        if (!isInitialized || currentSpotter == null || currentStream == null) return

        // 1. Buffer the raw bytes
        addToBuffer(audioData)
        
        // 2. Convert ByteArray to FloatArray for Sherpa Onnx
        val samples = FloatArray(audioData.size / 2)
        for (i in samples.indices) {
            val sample = (audioData[i * 2].toInt() and 0xFF) or (audioData[i * 2 + 1].toInt() shl 8)
            samples[i] = sample.toShort() / 32768f
        }

        // 3. Feed to spotter stream
        try {
            currentStream.acceptWaveform(samples, SAMPLE_RATE)
            while (currentSpotter.isReady(currentStream)) {
                currentSpotter.decode(currentStream)
                val result = currentSpotter.getResult(currentStream)
                
                if (result != null && result.keyword.isNotEmpty()) {
                    Log.d(TAG, "Detected keyword: ${result.keyword}")
                    
                    // Retrieve context audio
                    val wakeupAudio = getBufferedAudio()
                    _kwsEvents.tryEmit(KwsEvent.Wakeup(result.keyword, wakeupAudio))
                    
                    // Optionally reset or just continue
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio for KWS", e)
        }
    }
    
    fun cleanup() {
        stream?.release()
        spotter?.release()
        stream = null
        spotter = null
        isInitialized = false
    }
    
    private fun addToBuffer(data: ByteArray) {
        if (data.size >= bufferSize) {
             System.arraycopy(data, data.size - bufferSize, audioBuffer, 0, bufferSize)
             bufferHead = 0
             return
        }
        
        val spaceToEnd = bufferSize - bufferHead
        if (data.size <= spaceToEnd) {
            System.arraycopy(data, 0, audioBuffer, bufferHead, data.size)
            bufferHead += data.size
            if (bufferHead == bufferSize) bufferHead = 0
        } else {
            System.arraycopy(data, 0, audioBuffer, bufferHead, spaceToEnd)
            System.arraycopy(data, spaceToEnd, audioBuffer, 0, data.size - spaceToEnd)
            bufferHead = data.size - spaceToEnd
        }
    }
    
    private fun getBufferedAudio(): ByteArray {
        val result = ByteArray(bufferSize)
        val spaceToEnd = bufferSize - bufferHead
        System.arraycopy(audioBuffer, bufferHead, result, 0, spaceToEnd)
        System.arraycopy(audioBuffer, 0, result, spaceToEnd, bufferHead)
        return result
    }
}

sealed class KwsEvent {
    data class Wakeup(val keyword: String, val audioData: ByteArray) : KwsEvent()
}
