package com.airobot.agent.skills

import android.util.Log
import com.google.gson.JsonArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central skill registry and dispatcher.
 * Collects all AiSkill implementations via Hilt multibinding and dispatches
 * MCP tool calls to the matching skill by name.
 */
@Singleton
class SkillManager @Inject constructor(
    private val registeredSkills: Set<@JvmSuppressWildcards AiSkill>
) {
    companion object {
        private const val TAG = "SkillManager"
    }

    init {
        Log.d(TAG, "Initialized with ${registeredSkills.size} skill(s): " +
                registeredSkills.joinToString { it.name })
    }

    /**
     * Build a JSON array of all registered tool schemas for MCP tools/list response.
     */
    fun getAllSchemas(): JsonArray {
        val array = JsonArray()
        registeredSkills.forEach { skill ->
            array.add(skill.toMcpSchema())
        }
        return array
    }

    /**
     * Dispatch a tool call to the matching skill by name.
     */
    suspend fun dispatchToolCall(name: String, args: Map<String, Any>): SkillResult {
        val skill = registeredSkills.find { it.name == name }
        if (skill == null) {
            Log.w(TAG, "dispatchToolCall: tool not found: '$name'")
            return SkillResult.Failure("Tool not found: $name")
        }

        Log.d(TAG, "dispatchToolCall: executing '${skill.name}' with args=$args")
        return try {
            val result = skill.execute(args)
            Log.d(TAG, "dispatchToolCall: '${skill.name}' result=$result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "dispatchToolCall: '${skill.name}' threw exception", e)
            SkillResult.Failure("Skill execution error: ${e.message}")
        }
    }
}
