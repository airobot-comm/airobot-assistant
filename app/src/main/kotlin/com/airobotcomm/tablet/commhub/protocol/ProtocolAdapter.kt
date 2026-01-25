package com.airobotcomm.tablet.commhub.protocol

import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * 协议适配器，负责将原始 JSON 消息解析为 AiRobotEvent
 */
class ProtocolAdapter(private val gson: Gson = Gson()) {

    /**
     * 解析文本消息
     */
    fun parseTextMessage(message: String): AiRobotEvent? {
        return try {
            val json = gson.fromJson(message, JsonObject::class.java)
            val type = json.get("type")?.asString ?: return null

            when (type) {
                "stt" -> {
                    val text = json.get("text")?.asString
                    if (!text.isNullOrEmpty()) AiRobotEvent.STT(text) else null
                }
                "llm" -> {
                    val emotion = json.get("emotion")?.asString
                    val text = json.get("text")?.asString
                    AiRobotEvent.LLM(emotion, text)
                }
                "tts" -> {
                    val state = json.get("state")?.asString ?: return null
                    val sessionId = json.get("session_id")?.asString ?: ""
                    when (state) {
                        "start" -> AiRobotEvent.TtsStart(sessionId)
                        "stop" -> AiRobotEvent.TtsStop(sessionId)
                        "sentence_start" -> {
                            val text = json.get("text")?.asString ?: ""
                            AiRobotEvent.TtsSentence(text)
                        }
                        else -> null
                    }
                }
                "iot" -> {
                    val command = json.get("command")?.asString ?: ""
                    AiRobotEvent.IoT(command)
                }
                "dialogue_end" -> {
                    AiRobotEvent.DialogueEnd
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}