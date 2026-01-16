package com.airobotcomm.tablet.network.di

import android.content.Context
import com.airobotcomm.tablet.network.NetworkService
import com.airobotcomm.tablet.network.NetworkServiceImpl
import com.airobotcomm.tablet.network.protocol.OtaService
import com.airobotcomm.tablet.network.protocol.ProtocolAdapter
import com.airobotcomm.tablet.network.transport.SingletonWebSocket
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
    abstract fun bindNetworkService(impl: NetworkServiceImpl): NetworkService

    @Binds
    @Singleton
    abstract fun bindAiRobotProtocol(impl: com.airobotcomm.tablet.network.protocol.AiRobotProtocolImpl): com.airobotcomm.tablet.network.protocol.AiRobotProtocol

    companion object {
        @Provides
        @Singleton
        fun provideOtaService(): OtaService = OtaService()

        @Provides
        @Singleton
        fun provideWebSocketManager(@ApplicationContext context: Context): SingletonWebSocket =
            SingletonWebSocket(context)

        @Provides
        @Singleton
        fun provideProtocolAdapter(): ProtocolAdapter = ProtocolAdapter()
    }
}
