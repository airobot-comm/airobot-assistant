package com.airobot.tablet.system.di

import com.airobot.tablet.system.SysManage
import com.airobot.tablet.system.SysManageImpl
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
    abstract fun bindCommSysProvider(impl: com.airobot.tablet.system.CommSysProviderImpl): com.airobot.core.comm.provider.CommSysProvider
}


