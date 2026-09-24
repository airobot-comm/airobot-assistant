package com.airobot.agent.audio.recorder

import android.content.Context
import android.util.Log
import com.airobot.agent.audio.AudioConfig
import com.airobot.agent.audio.AudioEvent
import com.airobot.agent.audio.AudioWorkState
import com.airobot.agent.audio.recorder.pipeline.AudioAfeNode
import com.airobot.agent.audio.recorder.pipeline.AudioEncoderNode
import com.airobot.agent.audio.recorder.pipeline.AudioKwsNode
import com.airobot.agent.audio.recorder.pipeline.PipelineMessage
import com.airobot.agent.audio.tools.afe.AfeEvent
import com.airobot.agent.audio.tools.afe.AfeManager
import com.airobot.agent.audio.tools.kws.KwsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Audio recording pipeline - Core business logic router.
 * Refactored to a Dual-Stream, Event-in-Data architecture with encapsulated Actor Nodes.
 *
 * Responsibilities:
 * 1. Wire input PCM to AFE Node
 * 2. Route AFE outputs to KWS Node or Encoder Node based on AudioWorkState
 */
class AudioRecordPipeline(
    private val context: Context,
    private val config: AudioConfig,
    private val events: MutableSharedFlow<AudioEvent>,
    private val kwsManager: KwsManager,
    private val afeManager: AfeManager
) {
    companion object {
        private const val TAG = "AudioRecordPipeline"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Node Processors
    private val afeNode = AudioAfeNode(afeManager, events)
    private val kwsNode = AudioKwsNode(kwsManager)
    private val encoderNode = AudioEncoderNode(config, events)

    // State management
    private val stateLock = Any()

    @Volatile
    private var currentState = AudioWorkState.WAITING

    init {
        if (!kwsManager.init()) {
            Log.e(TAG, "KWS Manager init failed in pipeline")
        }
        if (!afeManager.init(config)) {
            Log.e(TAG, "AFE Manager init failed in pipeline")
        }
        startPipeline()
    }

    fun processFrame(pcmData: ByteArray) {
        afeNode.send(pcmData)
    }

    private fun startPipeline() {
        Log.d(TAG, "Starting Audio Pipeline (Actor Node Router)...")

        // 1. Start Encoder Node (Sink)
        encoderNode.start(scope)

        // 2. Start KWS Node (Detector Branch)
        kwsNode.start(scope) { triggerMessages ->
            // Check state again to prevent late races
            synchronized(stateLock) {
                if (currentState == AudioWorkState.WAITING) {
                    Log.d(TAG, "KWS Triggered, switching state to ACTIVE and flushing.")
                    currentState = AudioWorkState.ACTIVE
                    triggerMessages.forEach { encoderNode.send(it) }
                }
            }
        }

        // 3. Start AFE Node (Source Router)
        afeNode.start(scope) { afeResult ->
            if (currentState == AudioWorkState.ACTIVE) {
                // Route to Encoder
                if (afeResult.event == AfeEvent.SPEECH_START) {
                    encoderNode.send(PipelineMessage.EventMarker(AudioEvent.SpeechStart()))
                }
                afeResult.pcmFrames.forEach { frame ->
                    encoderNode.send(PipelineMessage.AudioFrame(frame))
                }
                if (afeResult.event == AfeEvent.SPEECH_END) {
                    encoderNode.send(PipelineMessage.EventMarker(AudioEvent.SpeechEnd()))
                }
            } else {
                // Route to KWS
                if (afeResult.event == AfeEvent.SPEECH_START) {
                    kwsNode.send(PipelineMessage.EventMarker(AudioEvent.SpeechStart()))
                }
                afeResult.pcmFrames.forEach { frame ->
                    kwsNode.send(PipelineMessage.AudioFrame(frame))
                }
            }
        }
    }

    fun setWorkState(newState: AudioWorkState) {
        synchronized(stateLock) {
            if (currentState == newState) return

            Log.d(TAG, "Pipeline state transition: $currentState -> $newState")
            currentState = newState

            if (newState == AudioWorkState.WAITING) {
                kwsNode.clearBuffer()
            }

            // Reset VAD state during transition to prevent carrying over old classification states
            afeNode.reset()
        }
    }

    fun cleanup() {
        afeNode.release()
        kwsNode.release()
        encoderNode.release()
        scope.cancel()
    }
}
