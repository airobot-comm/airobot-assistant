package com.airobot.features.clock.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing bindings for clock data repository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ClockHiltModule {

    @Binds
    @Singleton
    abstract fun bindClockRepository(impl: ClockRepositoryImpl): ClockRepository
}
