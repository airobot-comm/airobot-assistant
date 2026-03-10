package com.airobot.assistant.system.di

import com.airobot.assistant.system.repository.SysInfoRepo
import com.airobot.assistant.system.remote.OtaNetRepo
import com.airobot.assistant.system.remote.OtaNetXiaozhi
import com.airobot.assistant.system.repository.SysInfoRepoImpl
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
