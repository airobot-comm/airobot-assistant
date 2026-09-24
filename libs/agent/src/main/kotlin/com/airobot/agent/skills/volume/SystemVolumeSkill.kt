package com.airobot.agent.skills.volume

import android.util.Log
import com.airobot.agent.skills.AiSkill
import com.airobot.agent.skills.SkillResult
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP skill for system audio volume control.
 * Maps 0-100 volume range to the device's AudioManager stream volume via VolumeProvider.
 */
@Singleton
class SystemVolumeSkill @Inject constructor(
    private val volumeProvider: VolumeProvider
) : AiSkill {

    companion object {
        private const val TAG = "SystemVolumeSkill"
    }

    override val name = "self.audio_speaker.set_volume"
    override val description = "Set the volume of the audio speaker (0-100)."

    override val inputSchema: JsonObject
        get() {
            val schema = JsonObject()
            schema.addProperty("type", "object")
            val props = JsonObject()
            val volumeProp = JsonObject()
            volumeProp.addProperty("type", "integer")
            volumeProp.addProperty("minimum", 0)
            volumeProp.addProperty("maximum", 100)
            volumeProp.addProperty("description", "Volume level from 0 (mute) to 100 (max)")
            props.add("volume", volumeProp)
            schema.add("properties", props)
            val required = JsonArray()
            required.add("volume")
            schema.add("required", required)
            return schema
        }

    override suspend fun execute(arguments: Map<String, Any>): SkillResult {
        val volumeRaw = arguments["volume"]
        val volume = when (volumeRaw) {
            is Number -> volumeRaw.toInt()
            is String -> volumeRaw.toIntOrNull()
            else -> null
        }

        if (volume == null || volume !in 0..100) {
            Log.w(TAG, "execute: invalid volume argument: $volumeRaw")
            return SkillResult.Failure("Invalid volume: expected integer 0-100, got '$volumeRaw'")
        }

        volumeProvider.setVolume(volume)
        Log.d(TAG, "execute: volume set to $volume")
        return SkillResult.Success("Volume set to $volume")
    }
}
