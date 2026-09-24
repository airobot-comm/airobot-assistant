package com.airobot.agent.skills

import com.google.gson.JsonObject

sealed class SkillResult {
    data class Success(val message: String) : SkillResult()
    data class Failure(val reason: String) : SkillResult()
}

interface AiSkill {
    val name: String
    val description: String
    val inputSchema: JsonObject

    suspend fun execute(arguments: Map<String, Any>): SkillResult
    
    fun toMcpSchema(): JsonObject {
        val schema = JsonObject()
        schema.addProperty("name", name)
        schema.addProperty("description", description)
        schema.add("inputSchema", inputSchema)
        return schema
    }
}
