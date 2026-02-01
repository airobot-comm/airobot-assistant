package com.airobotcomm.tablet.system.di

import com.airobotcomm.tablet.system.SysManage
import com.airobotcomm.tablet.system.SysManagerImpl
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
    abstract fun bindSysManage(impl: SysManagerImpl): SysManage
}
