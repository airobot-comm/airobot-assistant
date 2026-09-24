package com.airobot.agent.brain.history

import com.airobot.agent.brain.model.Message

interface AgentHistoryRepository {
    suspend fun saveMessage(roleName: String, message: Message)
    suspend fun getMessagesForRole(roleName: String): List<Message>
    suspend fun searchMessagesForRole(roleName: String, query: String): List<Message>
    suspend fun clearMessagesForRole(roleName: String)
}
