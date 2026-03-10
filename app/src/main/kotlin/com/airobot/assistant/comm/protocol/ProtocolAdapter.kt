package com.airobot.assistant.comm.protocol

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.airobot.assistant.comm.NetCommEvent

/**
 * 协议适配器，负责将原始 JSON 消息解析为 NetCommEvent
 */
class ProtocolAdapter(private val gson: Gson = Gson()) {

    /**
     * 解析文本消息
     */
    fun parseTextMessage(message: String): NetCommEvent? {
        return try {
            val json = gson.fromJson(message, JsonObject::class.java)
            val type = json.get("type")?.asString ?: return null

            when (type) {
                "stt" -> {
                    val text = json.get("text")?.asString
                    if (!text.isNullOrEmpty()) NetCommEvent.STT(text) else null
                }
                "llm" -> {
                    val emotion = json.get("emotion")?.asString
                    val text = json.get("text")?.asString
                    NetCommEvent.LLM(emotion, text)
                }
                "tts" -> {
                    val state = json.get("state")?.asString ?: return null
                    val sessionId = json.get("session_id")?.asString ?: ""
                    when (state) {
                        "start" -> NetCommEvent.TtsStart(sessionId)
                        "stop" -> NetCommEvent.TtsStop(sessionId)
                        "sentence_start" -> {
                            val text = json.get("text")?.asString ?: ""
                            NetCommEvent.TtsSentence(text)
                        }
                        else -> null
                    }
                }
                "iot" -> {
                    val command = json.get("command")?.asString ?: ""
                    NetCommEvent.IoT(command)
                }
                "dialogue_end" -> {
                    NetCommEvent.DialogueEnd
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
