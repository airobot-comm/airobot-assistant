package com.airobotcomm.tablet.commhub.di

import android.content.Context
import com.airobotcomm.tablet.commhub.NetworkService
import com.airobotcomm.tablet.commhub.NetworkServiceImpl
import com.airobotcomm.tablet.commhub.protocol.ProtocolAdapter
import com.airobotcomm.tablet.commhub.transport.SingletonWebSocket
import com.airobotcomm.tablet.domain.config.ConfigManager
import com.airobotcomm.tablet.domain.ota.OtaManager
import com.airobotcomm.tablet.infra.remote.OtaRepositoryImpl
import com.airobotcomm.tablet.infra.repository.ConfigRepositoryImpl
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
        fun provideOtaManager(otaRepositoryImpl: OtaRepositoryImpl): OtaManager = OtaManager(otaRepositoryImpl)

        @Provides
        @Singleton
        fun provideConfigManager(configRepositoryImpl: ConfigRepositoryImpl): ConfigManager = ConfigManager(configRepositoryImpl)

        @Provides
        @Singleton
        fun provideWebSocketManager(@ApplicationContext context: Context): SingletonWebSocket =
            SingletonWebSocket(context)

        @Provides
        @Singleton
        fun provideProtocolAdapter(): ProtocolAdapter = ProtocolAdapter()
    }
}