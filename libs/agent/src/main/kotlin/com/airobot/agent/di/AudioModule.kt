package com.airobot.agent.di

import com.airobot.agent.audio.AudioService
import com.airobot.agent.audio.AudioServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Audio module dependency injection configuration.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindAudioService(
        audioServiceImpl: AudioServiceImpl
    ): AudioService

}

