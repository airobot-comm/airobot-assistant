package com.airobot.agent.audio.tools.afe

import kotlin.math.min
import kotlin.math.sqrt

/**
 * Audio processing utility class for level calculations and format conversions.
 */
object AudioCalculator {

    /**
     * Converts a 16-bit PCM Little-Endian byte array to a FloatArray of normalized samples (-1.0f to 1.0f).
     */
    fun pcmBytesToFloatSamples(pcmBuffer: ByteArray): FloatArray {
        val samples = FloatArray(pcmBuffer.size / 2)
        for (i in samples.indices) {
            val sample = (pcmBuffer[i * 2].toInt() and 0xFF) or (pcmBuffer[i * 2 + 1].toInt() shl 8)
            samples[i] = sample.toShort() / 32768f
        }
        return samples
    }

    /**
     * Calculates the Root Mean Square (RMS) level directly from a 16-bit PCM byte buffer.
     * This zero-allocation method avoids creating temporary FloatArrays during the hot recording loop.
     * Returns a normalized level (0.0 to 1.0).
     */
    fun calculateRmsLevel(pcmBuffer: ByteArray): Float {
        if (pcmBuffer.isEmpty()) return 0f
        var sumOfSquares = 0.0
        val numSamples = pcmBuffer.size / 2
        for (i in 0 until numSamples) {
            val sample = (pcmBuffer[i * 2].toInt() and 0xFF) or (pcmBuffer[i * 2 + 1].toInt() shl 8)
            val floatSample = sample.toShort() / 32768f
            sumOfSquares += floatSample * floatSample
        }
        val rms = sqrt(sumOfSquares / numSamples)
        return min(1.0, rms * 3.0).toFloat()
    }
}
