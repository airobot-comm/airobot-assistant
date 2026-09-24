package com.airobot.features.podcast.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing bindings for podcast data repository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PodcastHiltModule {

    @Binds
    @Singleton
    abstract fun bindPodcastRepository(impl: PodcastRepositoryImpl): PodcastRepository
}
