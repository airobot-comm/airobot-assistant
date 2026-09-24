package com.airobot.agent.brain.mcp

import android.util.Log
import com.airobot.agent.skills.SkillManager
import com.airobot.agent.skills.SkillResult
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure JSON-RPC 2.0 MCP protocol execution engine.
 * Decoupled from any network transports or specific AI agents.
 */
@Singleton
class McpHandler @Inject constructor(
    private val skillManager: SkillManager,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "McpHandler"
        private const val MCP_PROTOCOL_VERSION = "2024-11-05"
    }

    /**
     * Processes a raw standard JSON-RPC payload and returns the JSON-RPC response payload.
     */
    suspend fun handleMcpRequest(payload: JsonObject): JsonObject {
        val method = payload.get("method")?.asString ?: return createMcpError(
            id = null,
            code = -32600,
            message = "Invalid Request: missing method"
        )
        val id = payload.get("id")

        Log.d(TAG, "Parsing MCP request: method=$method, id=$id")

        return when (method) {
            "initialize" -> handleInitialize(id)
            "tools/list" -> handleToolsList(id)
            "tools/call" -> handleToolsCall(id, payload.getAsJsonObject("params"))
            else -> {
                Log.w(TAG, "Unknown MCP method: $method")
                createMcpError(id, -32601, "Method not found: $method")
            }
        }
    }

    private fun handleInitialize(id: JsonElement?): JsonObject {
        val result = JsonObject().apply {
            addProperty("protocolVersion", MCP_PROTOCOL_VERSION)
            add("capabilities", JsonObject().apply {
                add("tools", JsonObject())
            })
            add("serverInfo", JsonObject().apply {
                addProperty("name", "AiRobotClock")
                addProperty("version", "1.0.0")
            })
        }
        return createMcpResult(id, result)
    }

    private fun handleToolsList(id: JsonElement?): JsonObject {
        val schemas = skillManager.getAllSchemas()
        val result = JsonObject().apply {
            add("tools", schemas)
        }
        return createMcpResult(id, result)
    }

    private suspend fun handleToolsCall(id: JsonElement?, params: JsonObject?): JsonObject {
        val name = params?.get("name")?.asString
        if (name == null) {
            return createMcpError(id, -32602, "Missing 'name' in params")
        }

        val argsObj = params.getAsJsonObject("arguments")
        val argsMap = mutableMapOf<String, Any>()
        argsObj?.entrySet()?.forEach { entry ->
            val value = entry.value
            argsMap[entry.key] = when {
                value.isJsonPrimitive -> {
                    val prim = value.asJsonPrimitive
                    when {
                        prim.isNumber -> prim.asNumber
                        prim.isBoolean -> prim.asBoolean
                        else -> prim.asString
                    }
                }
                else -> value.toString()
            }
        }

        Log.d(TAG, "Calling tool: name=$name, args=$argsMap")
        val skillResult = skillManager.dispatchToolCall(name, argsMap)
        val isError = skillResult is SkillResult.Failure
        val text = when (skillResult) {
            is SkillResult.Success -> skillResult.message
            is SkillResult.Failure -> skillResult.reason
        }

        val result = JsonObject().apply {
            add("content", gson.toJsonTree(
                listOf(
                    mapOf("type" to "text", "text" to text)
                )
            ))
            addProperty("isError", isError)
        }
        return createMcpResult(id, result)
    }

    private fun createMcpResult(id: JsonElement?, result: JsonObject): JsonObject {
        return JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            id?.let { add("id", it) }
            add("result", result)
        }
    }

    private fun createMcpError(id: JsonElement?, code: Int, message: String): JsonObject {
        return JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            id?.let { add("id", it) }
            add("error", JsonObject().apply {
                addProperty("code", code)
                addProperty("message", message)
            })
        }
    }
}
