package com.airobot.airbot.api.di

import com.airobot.airbot.api.AirbotCharacterApi
import com.airobot.airbot.api.AirbotEngineApi
import com.airobot.airbot.domain.CharacterManagerImpl
import com.airobot.airbot.domain.RobotStateEngineImpl
import com.airobot.airbot.data.CharacterRepo
import com.airobot.airbot.data.repository.CharacterRepoImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AirbotApiModule {

    @Binds
    @Singleton
    abstract fun bindAirbotCharacterApi(
        impl: CharacterManagerImpl
    ): AirbotCharacterApi

    @Binds
    @Singleton
    abstract fun bindAirbotEngineApi(
        impl: RobotStateEngineImpl
    ): AirbotEngineApi

    @Binds
    @Singleton
    abstract fun bindCharacterRepo(
        impl: CharacterRepoImpl
    ): CharacterRepo
}
