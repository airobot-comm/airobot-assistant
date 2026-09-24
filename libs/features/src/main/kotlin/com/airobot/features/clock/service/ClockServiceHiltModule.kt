package com.airobot.features.clock.service

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing bindings for clock-related services.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ClockServiceHiltModule {

    @Binds
    @Singleton
    abstract fun bindSoundPlayer(impl: SoundPlayerImpl): SoundPlayer

    @Binds
    @Singleton
    abstract fun bindVibrationManager(impl: VibrationManagerImpl): VibrationManager

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: AlarmSchedulerImpl): AlarmScheduler
}
