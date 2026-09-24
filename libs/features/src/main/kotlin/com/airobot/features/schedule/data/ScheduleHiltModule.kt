package com.airobot.features.schedule.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing bindings for schedule data repository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScheduleHiltModule {

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository
}
