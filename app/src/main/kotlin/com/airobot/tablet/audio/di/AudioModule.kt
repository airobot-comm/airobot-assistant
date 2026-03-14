package com.airobot.tablet.audio.di

import com.airobot.tablet.audio.AudioService
import com.airobot.tablet.audio.AudioServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 音频模块依赖注入配置
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
