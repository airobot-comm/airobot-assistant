package com.airobot.agent.audio.recorder.pipeline

import com.airobot.agent.audio.AudioEvent
import com.airobot.agent.audio.tools.afe.AfeManager
import com.airobot.agent.audio.tools.afe.AfeResult
import com.airobot.agent.audio.tools.afe.AudioCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class AudioAfeNode(
    private val afeManager: AfeManager,
    private val events: MutableSharedFlow<AudioEvent>
) {
    private val channel =
        Channel<ByteArray>(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun start(scope: CoroutineScope, onResult: suspend (AfeResult) -> Unit) {
        scope.launch {
            for (pcmData in channel) {
                // 1. RMS -> emit to events
                val audioLevel = AudioCalculator.calculateRmsLevel(pcmData)
                events.emit(AudioEvent.VoiceLevel(audioLevel))

                // 2. AFE Gate / VAD Processing
                val afeResult = afeManager.processFrame(pcmData)

                // Callback to router
                onResult(afeResult)
            }
        }
    }

    fun send(pcmData: ByteArray) {
        channel.trySend(pcmData)
    }

    fun reset() {
        afeManager.reset()
    }

    fun release() {
        channel.close()
    }
}
