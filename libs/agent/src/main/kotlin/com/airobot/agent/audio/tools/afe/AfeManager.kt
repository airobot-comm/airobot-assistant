package com.airobot.agent.audio.tools.afe

import android.content.Context
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import com.airobot.agent.audio.AudioConfig
import java.util.LinkedList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of AFE/VAD processing.
 */
data class AfeResult(
    val pcmFrames: List<ByteArray>,
    val event: AfeEvent?
)

/**
 * High-level VAD events.
 */
enum class AfeEvent {
    SPEECH_START,
    SPEECH_END
}

/**
 * AFE Manager - Unifies hardware AFE (AEC/NS/AGC) and software AI VAD configurations.
 */
@Singleton
class AfeManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val vadManager: VadManager
) {
    companion object {
        private const val TAG = "AfeManager"
        private const val PRE_ROLL_FRAMES = 15 // 15 * 60ms = 900ms
        private const val TAIL_PAD_FRAMES = 5  // 5 * 60ms = 300ms
    }

    private var acousticEchoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null

    private val lock = Any()

    private val preRollBuffer = LinkedList<ByteArray>()
    private var isUserSpeaking = false
    private var tailPadCountRemaining = -1

    fun init(config: AudioConfig): Boolean {
        Log.d(TAG, "Initializing AFE (VAD)...")
        return vadManager.init(config)
    }

    fun setupHardwareAfe(audioSessionId: Int, enableAec: Boolean, enableNs: Boolean) {
        synchronized(lock) {
            try {
                Log.d(TAG, "Setting up hardware AFE effects for session: $audioSessionId")

                // Enable AEC
                if (enableAec && AcousticEchoCanceler.isAvailable()) {
                    acousticEchoCanceler = AcousticEchoCanceler.create(audioSessionId).apply {
                        enabled = true
                    }
                    Log.d(TAG, "Hardware AEC enabled: ${acousticEchoCanceler?.enabled == true}")
                } else {
                    Log.w(
                        TAG, "Hardware AEC not enabled. enableAec: $enableAec, " +
                            "isAvailable: ${AcousticEchoCanceler.isAvailable()}"
                    )
                }

                // Enable NS
                if (enableNs && NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(audioSessionId).apply {
                        enabled = true
                    }
                    Log.d(TAG, "Hardware NS enabled: ${noiseSuppressor?.enabled == true}")
                } else {
                    Log.w(
                        TAG, "Hardware NS not enabled. enableNs: $enableNs, " +
                            "isAvailable: ${NoiseSuppressor.isAvailable()}"
                    )
                }

                // Enable AGC
                if (AutomaticGainControl.isAvailable()) {
                    automaticGainControl = AutomaticGainControl.create(audioSessionId).apply {
                        enabled = true
                    }
                    Log.d(TAG, "Hardware AGC enabled: ${automaticGainControl?.enabled == true}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup hardware AFE effects", e)
            }
        }
    }

    fun processFrame(pcmData: ByteArray): AfeResult {
        synchronized(lock) {
            val floatSamples = AudioCalculator.pcmBytesToFloatSamples(pcmData)
            val isSpeech = vadManager.isSpeech(floatSamples)

            if (!isUserSpeaking) {
                if (isSpeech) {
                    isUserSpeaking = true
                    tailPadCountRemaining = -1

                    val framesToFlush = mutableListOf<ByteArray>()
                    framesToFlush.addAll(preRollBuffer)
                    preRollBuffer.clear()
                    framesToFlush.add(pcmData)

                    Log.d(
                        TAG,
                        "VAD Speech Start detected. Flushing ${framesToFlush.size} pre-roll frames."
                    )
                    return AfeResult(framesToFlush, AfeEvent.SPEECH_START)
                } else {
                    if (preRollBuffer.size >= PRE_ROLL_FRAMES) {
                        preRollBuffer.removeFirst()
                    }
                    preRollBuffer.addLast(pcmData)
                    return AfeResult(emptyList(), null)
                }
            } else {
                if (isSpeech) {
                    tailPadCountRemaining = -1
                    return AfeResult(listOf(pcmData), null)
                } else {
                    if (tailPadCountRemaining == -1) {
                        tailPadCountRemaining = TAIL_PAD_FRAMES
                    }

                    if (tailPadCountRemaining > 0) {
                        tailPadCountRemaining--
                        return AfeResult(listOf(pcmData), null)
                    } else {
                        isUserSpeaking = false
                        tailPadCountRemaining = -1
                        Log.d(TAG, "VAD Speech End detected after tail padding.")
                        return AfeResult(emptyList(), AfeEvent.SPEECH_END)
                    }
                }
            }
        }
    }

    fun reset() {
        synchronized(lock) {
            vadManager.reset()
            preRollBuffer.clear()
            isUserSpeaking = false
            tailPadCountRemaining = -1
        }
    }

    fun releaseHardwareAfe() {
        synchronized(lock) {
            try {
                acousticEchoCanceler?.release()
                noiseSuppressor?.release()
                automaticGainControl?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing hardware AFE", e)
            } finally {
                acousticEchoCanceler = null
                noiseSuppressor = null
                automaticGainControl = null
            }
        }
    }

    fun cleanup() {
        releaseHardwareAfe()
        vadManager.cleanup()
        synchronized(lock) {
            preRollBuffer.clear()
            isUserSpeaking = false
            tailPadCountRemaining = -1
        }
    }
}
