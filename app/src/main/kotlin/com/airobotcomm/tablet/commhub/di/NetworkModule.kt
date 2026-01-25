package com.airobotcomm.tablet.commhub.di

import android.content.Context
import com.airobotcomm.tablet.commhub.NetworkService
import com.airobotcomm.tablet.commhub.NetworkServiceImpl
import com.airobotcomm.tablet.commhub.protocol.OtaService
import com.airobotcomm.tablet.commhub.protocol.ProtocolAdapter
import com.airobotcomm.tablet.commhub.transport.SingletonWebSocket
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
    abstract fun bindAiRobotProtocol(impl: com.airobotcomm.tablet.commhub.protocol.AiRobotProtocolImpl): com.airobotcomm.tablet.commhub.protocol.AiRobotProtocol

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
