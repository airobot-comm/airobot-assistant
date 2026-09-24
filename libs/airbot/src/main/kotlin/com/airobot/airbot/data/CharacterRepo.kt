package com.airobot.airbot.data

import com.airobot.airbot.domain.model.Character
import kotlinx.serialization.Serializable

@Serializable
data class CharacterConfig(
    val activeRoleIndex: Int = 0,
    val characterList: List<Character> = emptyList()
)

interface CharacterRepo {
    suspend fun saveConfig(config: CharacterConfig)
    suspend fun loadConfig(): CharacterConfig
}
