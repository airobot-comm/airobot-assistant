package com.airobotcomm.tablet.domain.di

import com.airobotcomm.tablet.domain.config.ConfigManager
import com.airobotcomm.tablet.domain.ota.OtaManager
import com.airobotcomm.tablet.domain.repository.ConfigRepository
import com.airobotcomm.tablet.domain.repository.OtaRepository
import com.airobotcomm.tablet.infra.remote.OtaRepositoryImpl
import com.airobotcomm.tablet.infra.repository.ConfigRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    @Binds
    @Singleton
    abstract fun bindOtaRepository(impl: OtaRepositoryImpl): OtaRepository
}