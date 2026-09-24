package com.airobot.features.aiserv.notepad.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing bindings for AI Notepad data repository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiServHiltModule {

    @Binds
    @Singleton
    abstract fun bindAiNotepadRepository(impl: AiNotepadRepositoryImpl): AiNotepadRepository
}
