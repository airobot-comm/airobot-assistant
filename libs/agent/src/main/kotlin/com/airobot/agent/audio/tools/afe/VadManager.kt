package com.airobot.agent.audio.tools.afe

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import com.airobot.agent.audio.AudioConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VadManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "VadManager"
        private const val SAMPLE_RATE = 16000
        private const val VAD_WINDOW_SIZE = 512
        private const val VAD_MODEL_FILENAME = "silero_vad.onnx"
    }

    private var vad: Vad? = null
    private var isInitialized = false
    private val lock = Any()

    private val buffer = FloatArray(2048)
    private var bufferSize = 0
    private val window = FloatArray(VAD_WINDOW_SIZE)

    private val modelStorageDir: File by lazy {
        File(context.filesDir, "models/vad")
    }

    private val modelFile: File by lazy {
        File(modelStorageDir, VAD_MODEL_FILENAME)
    }

    fun init(config: AudioConfig): Boolean {
        synchronized(lock) {
            if (isInitialized) return true

            try {
                copyAssetsToInternalStorage()
                if (!modelFile.exists()) {
                    Log.e(TAG, "VAD model file does not exist: ${modelFile.absolutePath}")
                    return false
                }

                val sileroConfig = SileroVadModelConfig().apply {
                    model = modelFile.absolutePath
                    threshold = config.vadThreshold
                    minSilenceDuration = config.vadMinSilenceDuration
                    minSpeechDuration = config.vadMinSpeechDuration
                    windowSize = VAD_WINDOW_SIZE
                    maxSpeechDuration = config.vadMaxSpeechDuration
                }

                val config = VadModelConfig().apply {
                    sileroVadModelConfig = sileroConfig
                    sampleRate = SAMPLE_RATE
                    numThreads = 1
                    provider = "cpu"
                    debug = false
                }

                Log.d(TAG, "Initializing VAD with model ${modelFile.absolutePath}")
                vad = Vad(null, config)
                isInitialized = true
                Log.d(TAG, "VAD Initialized successfully")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "VAD Initialization failed", e)
                return false
            }
        }
    }

    fun isSpeech(floatSamples: FloatArray): Boolean {
        synchronized(lock) {
            val currentVad = vad
            if (!isInitialized || currentVad == null) {
                return false
            }

            // Append incoming samples to buffer
            if (bufferSize + floatSamples.size > buffer.size) {
                Log.w(TAG, "Buffer overflow, resetting buffer")
                bufferSize = 0
            }
            System.arraycopy(floatSamples, 0, buffer, bufferSize, floatSamples.size)
            bufferSize += floatSamples.size

            var speechDetected = false
            while (bufferSize >= VAD_WINDOW_SIZE) {
                // Extract window
                System.arraycopy(buffer, 0, window, 0, VAD_WINDOW_SIZE)

                // Shift buffer
                System.arraycopy(buffer, VAD_WINDOW_SIZE, buffer, 0, bufferSize - VAD_WINDOW_SIZE)
                bufferSize -= VAD_WINDOW_SIZE

                // Process in VAD
                currentVad.acceptWaveform(window)
                if (currentVad.isSpeechDetected()) {
                    speechDetected = true
                }

                // Pop completed segments to prevent queue accumulation
                while (!currentVad.empty()) {
                    currentVad.pop()
                }
            }

            return speechDetected
        }
    }

    fun reset() {
        synchronized(lock) {
            vad?.reset()
            bufferSize = 0
        }
    }

    fun cleanup() {
        synchronized(lock) {
            try {
                vad?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing VAD", e)
            } finally {
                vad = null
                isInitialized = false
                bufferSize = 0
            }
        }
    }

    private fun copyAssetsToInternalStorage() {
        if (!modelStorageDir.exists()) {
            modelStorageDir.mkdirs()
        }
        if (!modelFile.exists()) {
            Log.d(TAG, "Copying VAD model to unified external storage...")
            try {
                context.assets.open(VAD_MODEL_FILENAME).use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "VAD model copied successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy VAD assets", e)
            }
        }
    }
}
