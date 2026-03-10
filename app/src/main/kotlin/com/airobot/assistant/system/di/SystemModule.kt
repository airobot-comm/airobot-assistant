package com.airobot.assistant.system.di

import com.airobot.assistant.system.SysManage
import com.airobot.assistant.system.SysManageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SystemModule {

    @Binds
    @Singleton
    abstract fun bindSysManage(impl: SysManageImpl): SysManage
}

