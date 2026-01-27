package com.airobotcomm.tablet.comm.di

import android.content.Context
import com.airobotcomm.tablet.comm.NetworkService
import com.airobotcomm.tablet.comm.NetworkServiceImpl
import com.airobotcomm.tablet.comm.protocol.ProtocolAdapter
import com.airobotcomm.tablet.comm.transport.SingletonWebSocket
import com.airobotcomm.tablet.domain.SystemConfig
import com.airobotcomm.tablet.infra.repository.SysConfigRepoImpl
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
    abstract fun bindAiRobotProtocol(impl: com.airobotcomm.tablet.comm.protocol.AiRobotProtocolImpl): com.airobotcomm.tablet.comm.protocol.AiRobotProtocol

    companion object {
        @Provides
        @Singleton
        fun provideConfigManager(configRepositoryImpl: SysConfigRepoImpl): SystemConfig = SystemConfig(configRepositoryImpl)

        @Provides
        @Singleton
        fun provideWebSocketManager(@ApplicationContext context: Context): SingletonWebSocket =
            SingletonWebSocket(context)

        @Provides
        @Singleton
        fun provideProtocolAdapter(): ProtocolAdapter = ProtocolAdapter()
    }
}