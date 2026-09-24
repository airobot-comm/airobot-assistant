package com.airobot.features.podcast.service

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing bindings for podcast service layer.
 *
 * Follows the same pattern as
 * [com.airobot.features.clock.service.ClockServiceHiltModule].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PodcastServiceHiltModule {

    @Binds
    @Singleton
    abstract fun bindMediaScannerService(
        impl: MediaScannerServiceImpl
    ): MediaScannerService

    @Binds
    @Singleton
    abstract fun bindMediaImportService(
        impl: MediaImportServiceImpl
    ): MediaImportService

    @Binds
    @Singleton
    abstract fun bindPodcastPlaybackService(
        impl: PodcastPlaybackServiceImpl
    ): PodcastPlaybackService
}
