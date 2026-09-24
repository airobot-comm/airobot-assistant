package com.airobot.agent.brain.history

import com.airobot.agent.brain.model.Message
import com.airobot.agent.brain.model.MessageRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentHistoryRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao
) : AgentHistoryRepository {

    override suspend fun saveMessage(roleName: String, message: Message) {
        val entity = MessageEntity(
            id = message.id,
            roleName = roleName,
            messageRole = message.role.name,
            content = message.content,
            timestamp = message.timestamp,
            isAudio = message.isAudio
        )
        messageDao.insertMessage(entity)
    }

    override suspend fun getMessagesForRole(roleName: String): List<Message> {
        return messageDao.getMessagesForRole(roleName).map { entity ->
            Message(
                id = entity.id,
                role = MessageRole.valueOf(entity.messageRole),
                content = entity.content,
                timestamp = entity.timestamp,
                isAudio = entity.isAudio
            )
        }
    }

    override suspend fun searchMessagesForRole(roleName: String, query: String): List<Message> {
        return messageDao.searchMessagesForRole(roleName, query).map { entity ->
            Message(
                id = entity.id,
                role = MessageRole.valueOf(entity.messageRole),
                content = entity.content,
                timestamp = entity.timestamp,
                isAudio = entity.isAudio
            )
        }
    }

    override suspend fun clearMessagesForRole(roleName: String) {
        messageDao.deleteMessagesForRole(roleName)
    }
}
