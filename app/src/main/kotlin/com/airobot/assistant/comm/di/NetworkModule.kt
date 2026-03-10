package com.airobot.assistant.comm.di

import android.content.Context
import com.airobot.assistant.comm.NetCommService
import com.airobot.assistant.comm.NetCommServiceImpl
import com.airobot.assistant.comm.protocol.ProtocolAdapter
import com.airobot.assistant.comm.transport.SingletonWebSocket
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindNetworkService(impl: NetCommServiceImpl): NetCommService

    @Binds
    @Singleton
    abstract fun bindAiRobotProtocol(impl: com.airobot.assistant.comm.protocol.CommProtocolImpl): com.airobot.assistant.comm.protocol.CommProtocol

    companion object {
        @Provides
        @Singleton
        fun provideWebSocketManager(@ApplicationContext context: Context): SingletonWebSocket =
            SingletonWebSocket(context)

        @Provides
        @Singleton
        fun provideProtocolAdapter(): ProtocolAdapter = ProtocolAdapter()
    }
}
