package com.airobotcomm.tablet

import com.airobotcomm.tablet.domain.config.ConfigManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 用于在UI层访问ConfigManager的EntryPoint
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ConfigManagerEntryPoint {
    fun configManager(): ConfigManager
}