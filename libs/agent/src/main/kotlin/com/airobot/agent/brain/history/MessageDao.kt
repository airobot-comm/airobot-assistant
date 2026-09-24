package com.airobot.agent.brain.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
@JvmSuppressWildcards
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM dialogue_messages WHERE roleName = :roleName ORDER BY timestamp ASC")
    suspend fun getMessagesForRole(roleName: String): List<MessageEntity>

    @Query("SELECT * FROM dialogue_messages WHERE roleName = :roleName AND content LIKE '%' || :query || '%' ORDER BY timestamp ASC")
    suspend fun searchMessagesForRole(roleName: String, query: String): List<MessageEntity>

    @Query("DELETE FROM dialogue_messages WHERE roleName = :roleName")
    suspend fun deleteMessagesForRole(roleName: String): Int
}
