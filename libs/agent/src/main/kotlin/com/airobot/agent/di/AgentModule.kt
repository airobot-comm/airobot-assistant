package com.airobot.agent.di

import android.content.Context
import androidx.room.Room
import com.airobot.agent.brain.AiBrain
import com.airobot.agent.brain.history.AgentDatabase
import com.airobot.agent.brain.history.AgentHistoryRepository
import com.airobot.agent.brain.history.AgentHistoryRepositoryImpl
import com.airobot.agent.brain.history.MessageDao
import com.airobot.agent.brain.xiaozhi.XiaozhiCloudBrain
import com.airobot.agent.manager.AgentManager
import com.airobot.agent.manager.AgentManagerImpl
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AgentModule {
    @Binds
    @Singleton
    abstract fun bindAiBrain(
        xiaozhiCloudBrain: XiaozhiCloudBrain
    ): AiBrain

    @Binds
    @Singleton
    abstract fun bindAgentHistoryRepository(
        impl: AgentHistoryRepositoryImpl
    ): AgentHistoryRepository

    @Binds
    @Singleton
    abstract fun bindAgentManager(
        impl: AgentManagerImpl
    ): AgentManager

    companion object {
        @Provides
        @Singleton
        fun provideGson(): Gson = Gson()

        @Provides
        @Singleton
        fun provideAgentDatabase(@ApplicationContext context: Context): AgentDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AgentDatabase::class.java,
                "agent_database"
            ).build()
        }

        @Provides
        @Singleton
        fun provideMessageDao(database: AgentDatabase): MessageDao {
            return database.messageDao()
        }
    }
}
