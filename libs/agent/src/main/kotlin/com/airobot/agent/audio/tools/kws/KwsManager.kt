package com.airobot.agent.audio.tools.kws

import android.content.Context
import android.util.Log
import com.airobot.agent.audio.tools.afe.AudioCalculator
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * KWS Manager - Keyword Spotting Manager
 * Acts as a Singleton WakeWordManager that handles configuration and audio processing.
 */
@Singleton
class KwsManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "KwsManager"
        private const val SAMPLE_RATE = 16000
        private const val MODELS_DIR = "models"
        private const val ASSET_DIR = "kws-zipformer-zh-en-3M-2025-12-20"
        private const val KEYWORDS_FILE_NAME = "keywords.txt"
    }

    private var spotter: KeywordSpotter? = null
    private var stream: OnlineStream? = null
    private var isInitialized = false
    private val lock = Any()

    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val modelStorageDir: File by lazy {
        File(context.filesDir, "$MODELS_DIR/$ASSET_DIR")
    }

    private val internalKeywordsFile: File by lazy {
        File(modelStorageDir, KEYWORDS_FILE_NAME)
    }

    fun getValidTokens(): Set<String> {
        return loadTokens()
    }

    private fun loadTokens(): Set<String> {
        val tokensSet = mutableSetOf<String>()
        try {
            val tokensFile = File(modelStorageDir, "tokens.txt")
            if (tokensFile.exists()) {
                tokensFile.forEachLine { line ->
                    val token = line.substringBefore(" ").trim()
                    if (token.isNotEmpty()) {
                        tokensSet.add(token)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load tokens.txt", e)
        }
        return tokensSet
    }

    fun updateKeywords(keywordLines: List<String>) {
        backgroundScope.launch {
            try {
                // Ensure models are copied so tokens.txt is available
                if (!modelStorageDir.exists() || !File(modelStorageDir, "tokens.txt").exists()) {
                    copyAssetsToInternalStorage()
                }

                val stringBuilder = StringBuilder()
                for (line in keywordLines) {
                    if (line.isNotBlank()) {
                        stringBuilder.append("$line\n")
                    }
                }

                val newContent = stringBuilder.toString()
                Log.d(TAG, "Updating keywords with content:\n$newContent")

                // Ensure directory exists
                if (!modelStorageDir.exists()) {
                    modelStorageDir.mkdirs()
                }

                // Write to internal storage
                internalKeywordsFile.writeText(newContent, Charsets.UTF_8)

                if (!isInitialized) {
                    Log.d(TAG, "KWS not initialized yet, skipping hot-reload")
                    return@launch
                }

                Log.d(TAG, "Performing Double-Buffering Hot-Reload...")
                val newConfig = createSpotterConfig()
                val newSpotter = KeywordSpotter(assetManager = null, config = newConfig)
                val newStream = newSpotter.createStream()

                // Atomic Swap under lock protection
                var oldSpotter: KeywordSpotter? = null
                var oldStream: OnlineStream? = null
                synchronized(lock) {
                    oldSpotter = spotter
                    oldStream = stream

                    spotter = newSpotter
                    stream = newStream
                }

                // Safe release outside lock to avoid blocking audio thread
                oldStream?.release()
                oldSpotter?.release()

                Log.d(TAG, "Hot-reload successful. New keywords are active.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update keywords", e)
            }
        }
    }

    private fun ensureKeywordsFile() {
        if (!internalKeywordsFile.exists()) {
            try {
                if (!modelStorageDir.exists()) modelStorageDir.mkdirs()
                // Copy default keywords from assets if custom one doesn't exist
                context.assets.open("$ASSET_DIR/keywords.txt").use { input ->
                    internalKeywordsFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy default keywords.txt", e)
            }
        }
    }

    private fun copyAssetsToInternalStorage() {
        if (!modelStorageDir.exists()) {
            modelStorageDir.mkdirs()
            Log.d(TAG, "Copying KWS models to unified external storage...")
            try {
                val assetsToCopy = listOf(
                    "encoder-epoch-13-avg-2-chunk-16-left-64.onnx",
                    "decoder-epoch-13-avg-2-chunk-16-left-64.onnx",
                    "joiner-epoch-13-avg-2-chunk-16-left-64.onnx",
                    "tokens.txt"
                )
                for (file in assetsToCopy) {
                    context.assets.open("$ASSET_DIR/$file").use { input ->
                        File(modelStorageDir, file).outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy KWS assets", e)
            }
        }
    }

    private fun createSpotterConfig(): KeywordSpotterConfig {
        val config = KeywordSpotterConfig()
        config.featConfig.sampleRate = SAMPLE_RATE
        config.featConfig.featureDim = 80

        config.modelConfig.transducer.encoder =
            File(modelStorageDir, "encoder-epoch-13-avg-2-chunk-16-left-64.onnx").absolutePath
        config.modelConfig.transducer.decoder =
            File(modelStorageDir, "decoder-epoch-13-avg-2-chunk-16-left-64.onnx").absolutePath
        config.modelConfig.transducer.joiner =
            File(modelStorageDir, "joiner-epoch-13-avg-2-chunk-16-left-64.onnx").absolutePath
        config.modelConfig.tokens = File(modelStorageDir, "tokens.txt").absolutePath

        config.modelConfig.numThreads = 1
        config.modelConfig.provider = "cpu"
        config.modelConfig.modelType = "zipformer2"

        // Use unified external file path
        config.keywordsFile = internalKeywordsFile.absolutePath

        config.keywordsScore = 0.85f
        config.keywordsThreshold = 0.15f
        return config
    }

    /**
     * Initializes the KWS Engine
     */
    fun init(): Boolean {
        synchronized(lock) {
            if (isInitialized) return true

            try {
                copyAssetsToInternalStorage()
                ensureKeywordsFile()

                val config = createSpotterConfig()

                Log.d(TAG, "Initializing KWS with keywords from ${config.keywordsFile}")

                spotter = KeywordSpotter(assetManager = null, config = config)
                stream = spotter?.createStream()

                isInitialized = true
                Log.d(TAG, "KWS Initialized successfully")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "KWS Initialization failed", e)
                return false
            }
        }
    }

    /**
     * Process audio chunk for keywords
     */
    fun process(pcmData: ByteArray): String? {
        synchronized(lock) {
            val currentStream = stream
            val currentSpotter = spotter

            if (!isInitialized || currentSpotter == null || currentStream == null) {
                return null
            }

            try {
                val samples = AudioCalculator.pcmBytesToFloatSamples(pcmData)

                currentStream.acceptWaveform(samples, SAMPLE_RATE)
                while (currentSpotter.isReady(currentStream)) {
                    currentSpotter.decode(currentStream)
                    val result = currentSpotter.getResult(currentStream)

                    if (result.keyword.isNotEmpty()) {
                        Log.d(TAG, "KWS Detected Wake Word: ${result.keyword}")
                        // Optional: currentSpotter.reset(currentStream) to reset state
                        return result.keyword
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "KWS processing failed: ${e.message}", e)
            }
        }

        return null
    }

    /**
     * Resets the KWS stream state.
     * Useful when starting a new utterance to prevent stale context from degrading performance.
     */
    fun resetStream() {
        synchronized(lock) {
            val currentSpotter = spotter
            val currentStream = stream
            if (currentSpotter != null && currentStream != null) {
                try {
                    currentSpotter.reset(currentStream)
                    Log.d(TAG, "KWS stream state reset successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reset KWS stream", e)
                }
            }
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        synchronized(lock) {
            stream?.release()
            spotter?.release()
            stream = null
            spotter = null
            isInitialized = false
        }
        Log.d(TAG, "KWS Cleaned up")
    }
}
