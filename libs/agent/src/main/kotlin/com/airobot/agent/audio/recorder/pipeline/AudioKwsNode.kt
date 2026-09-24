package com.airobot.agent.audio.recorder.pipeline

import android.util.Log
import com.airobot.agent.audio.AudioEvent
import com.airobot.agent.audio.tools.kws.KwsManager
import java.util.LinkedList
import kotlinx.coroutines.launch

class AudioKwsNode(private val kwsManager: KwsManager) {
    companion object {
        private const val TAG = "AudioKwsNode"
        private const val MAX_HISTORY_FRAMES = 32 // ~2 seconds of history context
    }

    private val channel = kotlinx.coroutines.channels.Channel<PipelineMessage>(
        capacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    private val preWakeupBuffer = LinkedList<ByteArray>()

    fun start(
        scope: kotlinx.coroutines.CoroutineScope,
        onTriggered: suspend (List<PipelineMessage>) -> Unit
    ) {
        scope.launch {
            for (message in channel) {
                when (message) {
                    is PipelineMessage.EventMarker -> {
                        if (message.event is AudioEvent.SpeechStart) {
                            Log.d(TAG, "Intercepted SpeechStart, resetting KWS stream state")
                            kwsManager.resetStream()
                        }
                    }

                    is PipelineMessage.Command -> {
                        if (message.action == "ClearBuffer") {
                            preWakeupBuffer.clear()
                        }
                    }

                    is PipelineMessage.AudioFrame -> {
                        val frame = message
                        if (preWakeupBuffer.size >= MAX_HISTORY_FRAMES) {
                            preWakeupBuffer.removeFirst()
                        }
                        preWakeupBuffer.addLast(frame.pcmData)

                        val keyword = kwsManager.process(frame.pcmData)
                        if (keyword != null) {
                            Log.d(
                                TAG,
                                "KWS Detected keyword: \$keyword. Flushing pre-wakeup buffer."
                            )
                            val messages = mutableListOf<PipelineMessage>()
                            // 1. Insert Wakeup marker
                            messages.add(PipelineMessage.EventMarker(AudioEvent.Wakeup))

                            // 2. Insert historical frames
                            while (preWakeupBuffer.isNotEmpty()) {
                                messages.add(PipelineMessage.AudioFrame(preWakeupBuffer.removeFirst()))
                            }
                            onTriggered(messages)
                        }
                    }
                }
            }
        }
    }

    fun send(message: PipelineMessage) {
        channel.trySend(message)
    }

    fun clearBuffer() {
        channel.trySend(PipelineMessage.Command("ClearBuffer"))
    }

    fun release() {
        channel.close()
        clearBuffer()
    }
}
