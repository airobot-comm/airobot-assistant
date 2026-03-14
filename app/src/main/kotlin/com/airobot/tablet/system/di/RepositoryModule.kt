package com.airobot.tablet.system.di

import com.airobot.tablet.system.repository.SysInfoRepo
import com.airobot.tablet.system.remote.OtaNetRepo
import com.airobot.tablet.system.remote.OtaNetXiaozhi
import com.airobot.tablet.system.repository.SysInfoRepoImpl
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
    abstract fun bindOtaRepository(impl: OtaNetXiaozhi): OtaNetRepo
}
