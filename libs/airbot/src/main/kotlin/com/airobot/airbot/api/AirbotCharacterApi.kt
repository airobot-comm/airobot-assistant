package com.airobot.airbot.api

import com.airobot.airbot.domain.model.Character
import kotlinx.coroutines.flow.StateFlow

interface AirbotCharacterApi {
    val allCharacters: StateFlow<List<Character>>
    val activeCharacter: StateFlow<Character?>

    suspend fun switchCharacter(roleName: String): Boolean
    suspend fun updateWakeWord(roleName: String, wakeWord: String): Boolean
    fun syncWakeWordsToAgent()
}
