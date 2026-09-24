package com.airobot.agent.audio.recorder.pipeline

import com.airobot.agent.audio.AudioConfig
import com.airobot.agent.audio.AudioEvent
import com.airobot.agent.audio.tools.codec.OpusEncoder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class AudioEncoderNode(config: AudioConfig, private val eventsBus: MutableSharedFlow<AudioEvent>) {
    private val channel = kotlinx.coroutines.channels.Channel<PipelineMessage>(
        capacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    private val encoder = OpusEncoder(
        config.recordSampleRate,
        config.channels,
        config.frameDurationMs
    )

    fun start(scope: kotlinx.coroutines.CoroutineScope) {
        scope.launch {
            for (message in channel) {
                when (message) {
                    is PipelineMessage.EventMarker -> {
                        if (message.event is AudioEvent.SpeechStart) {
                            encoder.reset()
                        }
                        // Unified Event Exit
                        eventsBus.emit(message.event)
                    }

                    is PipelineMessage.AudioFrame -> {
                        // Encode and push
                        val opusData = encoder.encode(message.pcmData)
                        if (opusData != null) {
                            eventsBus.emit(AudioEvent.SpeechData(opusData))
                        }
                    }

                    is PipelineMessage.Command -> {
                        // Ignored by Encoder Node
                    }
                }
            }
        }
    }

    fun send(message: PipelineMessage) {
        channel.trySend(message)
    }

    fun release() {
        channel.close()
        encoder.release()
    }
}
