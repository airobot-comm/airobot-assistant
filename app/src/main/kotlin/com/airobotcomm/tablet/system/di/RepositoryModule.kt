package com.airobotcomm.tablet.system.di

import com.airobotcomm.tablet.system.repository.SysInfoRepo
import com.airobotcomm.tablet.system.remote.OtaNetRepo
import com.airobotcomm.tablet.system.remote.OtaNetRepoImpl
import com.airobotcomm.tablet.system.repository.SysInfoRepoImpl
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
    abstract fun bindConfigRepository(impl: SysInfoRepoImpl): SysInfoRepo

    @Binds
    @Singleton
    abstract fun bindOtaRepository(impl: OtaNetRepoImpl): OtaNetRepo
}