package com.airobotcomm.tablet.domain.di

import com.airobotcomm.tablet.domain.ota.repository.OtaConfigRepository
import com.airobotcomm.tablet.domain.ota.repository.OtaNetRepository
import com.airobotcomm.tablet.infra.remote.OtaNetRepositoryImpl
import com.airobotcomm.tablet.infra.repository.OtaConfigRepositoryImpl
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
    abstract fun bindConfigRepository(impl: OtaConfigRepositoryImpl): OtaConfigRepository

    @Binds
    @Singleton
    abstract fun bindOtaRepository(impl: OtaNetRepositoryImpl): OtaNetRepository
}