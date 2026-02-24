package com.airobotcomm.tablet.comm.di

import android.content.Context
import com.airobotcomm.tablet.comm.NetCommService
import com.airobotcomm.tablet.comm.NetCommServiceImpl
import com.airobotcomm.tablet.comm.protocol.ProtocolAdapter
import com.airobotcomm.tablet.comm.transport.SingletonWebSocket
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
    abstract fun bindAiRobotProtocol(impl: com.airobotcomm.tablet.comm.protocol.CommProtocolImpl): com.airobotcomm.tablet.comm.protocol.CommProtocol

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