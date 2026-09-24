package com.airobot.features.aiprovider.di

import com.airobot.agent.skills.podcast.PodcastProvider
import com.airobot.features.aiprovider.PodcastProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Unified DI module for feature capability providers.
 * Binds provider implementations inside `:features` to capability interfaces inside `:agent`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FeaturesProviderModule {

    @Binds
    @Singleton
    abstract fun bindPodcastProvider(
        podcastProviderImpl: PodcastProviderImpl
    ): PodcastProvider
}
