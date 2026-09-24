package com.airobot.agent.di

import com.airobot.agent.skills.AiSkill
import com.airobot.agent.skills.podcast.PodcastSkill
import com.airobot.agent.skills.volume.SystemVolumeSkill
import com.airobot.agent.skills.volume.VolumeProvider
import com.airobot.agent.skills.volume.VolumeProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SkillModule {
    @Binds
    @IntoSet
    abstract fun bindSystemVolumeSkill(
        systemVolumeSkill: SystemVolumeSkill
    ): AiSkill

    @Binds
    @IntoSet
    abstract fun bindPodcastSkill(
        podcastSkill: PodcastSkill
    ): AiSkill

    @Binds
    @Singleton
    abstract fun bindVolumeProvider(
        volumeProviderImpl: VolumeProviderImpl
    ): VolumeProvider
}
