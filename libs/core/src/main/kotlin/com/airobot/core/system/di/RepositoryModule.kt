package com.airobot.core.system.di

import com.airobot.core.system.repository.SysInfoRepo
import com.airobot.core.system.remote.OtaNetRepo
import com.airobot.core.system.remote.OtaNetXiaozhi
import com.airobot.core.system.repository.SysInfoRepoImpl
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

