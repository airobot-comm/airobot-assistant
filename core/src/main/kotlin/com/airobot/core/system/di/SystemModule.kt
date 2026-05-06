package com.airobot.core.system.di

import com.airobot.core.system.SysManage
import com.airobot.core.system.SysManageImpl
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

    @Binds
    @Singleton
    abstract fun bindCommSysProvider(impl: com.airobot.core.system.CommSysProviderImpl): com.airobot.core.comm.provider.CommSysProvider
}


