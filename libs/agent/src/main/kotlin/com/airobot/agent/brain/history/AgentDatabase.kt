package com.airobot.agent.brain.history

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MessageEntity::class], version = 1, exportSchema = false)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
