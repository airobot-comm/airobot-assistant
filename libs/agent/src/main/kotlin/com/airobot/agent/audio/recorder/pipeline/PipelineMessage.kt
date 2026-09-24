package com.airobot.agent.audio.recorder.pipeline

import com.airobot.agent.audio.AudioEvent

sealed class PipelineMessage {
    data class AudioFrame(val pcmData: ByteArray) : PipelineMessage() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as AudioFrame
            return pcmData.contentEquals(other.pcmData)
        }

        override fun hashCode(): Int {
            return pcmData.contentHashCode()
        }
    }

    data class EventMarker(val event: AudioEvent) : PipelineMessage()

    data class Command(val action: String) : PipelineMessage()
}
