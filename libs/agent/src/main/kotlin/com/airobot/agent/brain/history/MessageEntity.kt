package com.airobot.agent.brain.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dialogue_messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val roleName: String,
    val messageRole: String, // "USER", "AGENT", "SYSTEM"
    val content: String,
    val timestamp: Long,
    val isAudio: Boolean
)
