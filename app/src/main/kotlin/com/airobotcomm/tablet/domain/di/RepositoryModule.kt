package com.airobotcomm.tablet.domain.di

import com.airobotcomm.tablet.domain.repository.OtaConfigRepo
import com.airobotcomm.tablet.domain.repository.OtaNetRepo
import com.airobotcomm.tablet.infra.remote.OtaNetRepoImpl
import com.airobotcomm.tablet.infra.repository.OtaConfigRepoImpl
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
    abstract fun bindConfigRepository(impl: OtaConfigRepoImpl): OtaConfigRepo

    @Binds
    @Singleton
    abstract fun bindOtaRepository(impl: OtaNetRepoImpl): OtaNetRepo
}