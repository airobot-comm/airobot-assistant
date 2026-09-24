package com.airobot.airbot.domain

import android.util.Log
import com.airobot.agent.manager.AgentManager
import com.airobot.airbot.api.AirbotCharacterApi
import com.airobot.airbot.data.CharacterConfig
import com.airobot.airbot.data.CharacterRepo
import com.airobot.airbot.domain.model.Character
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterManagerImpl @Inject constructor(
    private val characterRepo: CharacterRepo,
    private val agentConfigManager: AgentManager
) : AirbotCharacterApi {

    companion object {
        private const val TAG = "CharacterManager"

        // 预定义的带多音调变体的映射表（高频唤醒词）
        private val predefinedTokens = mapOf(
            "小叶" to listOf("x iǎo y e", "x iǎo y è", "x iǎo y é"),
            "你好小叶" to listOf("n ǐ h ǎo x iǎo y e", "n ǐ h ǎo x iǎo y è", "n ǐ h ǎo x iǎo y é"),
            "小灵" to listOf("x iǎo l íng", "x iǎo l ing", "x iǎo l ǐng", "x iǎo l ìng"),
            "你好小灵" to listOf(
                "n ǐ h ǎo x iǎo l íng",
                "n ǐ h ǎo x iǎo l ing",
                "n ǐ h ǎo x iǎo l ǐng",
                "n ǐ h ǎo x iǎo l ìng"
            ),
            "小白" to listOf("x iǎo b ái", "x iǎo b ai", "x iǎo b ài"),
            "你好小白" to listOf("n ǐ h ǎo x iǎo b ái", "n ǐ h ǎo x iǎo b ai"),
            "小小" to listOf("x iǎo x iǎo", "x iǎo x iao"),
            "你好小小" to listOf("n ǐ h ǎo x iǎo x iǎo", "n ǐ h ǎo x iǎo x iao"),
            "小安" to listOf("x iǎo ān", "x iǎo an", "x iǎo án")
        )
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _activeCharacter = MutableStateFlow<Character?>(null)
    override val activeCharacter: StateFlow<Character?> = _activeCharacter.asStateFlow()

    private val _allCharacters = MutableStateFlow<List<Character>>(emptyList())
    override val allCharacters: StateFlow<List<Character>> = _allCharacters.asStateFlow()

    private var currentConfig = CharacterConfig()

    init {
        scope.launch {
            loadAndInitializeCharacters()
        }
    }

    private suspend fun loadAndInitializeCharacters() {
        var config = characterRepo.loadConfig()

        // Seed default characters or migrate if contains old names
        val hasOldNames =
            config.characterList.any { it.roleName == "心小安" || it.roleName == "花小龙" || it.roleName == "心小苗" }
        if (config.characterList.isEmpty() || hasOldNames) {
            config = config.copy(characterList = Character.DEFAULT_ROBOTS, activeRoleIndex = 0)
            characterRepo.saveConfig(config)
        } else {
            // Force reset the alias of loaded characters to match the default static aliases
            val updatedList = config.characterList.map { robot ->
                val defaultRobot = Character.DEFAULT_ROBOTS.find { it.roleName == robot.roleName }
                if (defaultRobot != null && robot.alias != defaultRobot.alias) {
                    robot.copy(alias = defaultRobot.alias)
                } else {
                    robot
                }
            }
            if (updatedList != config.characterList) {
                config = config.copy(characterList = updatedList)
                characterRepo.saveConfig(config)
            }
        }

        currentConfig = config
        _allCharacters.value = config.characterList

        val activeIndex = config.activeRoleIndex
        if (activeIndex in config.characterList.indices) {
            val activeRobot = config.characterList[activeIndex]
            _activeCharacter.value = activeRobot
            agentConfigManager.switchRole(activeRobot.roleName)
        }

        syncWakeWordsToAgent()
    }

    override suspend fun switchCharacter(roleName: String): Boolean {
        val robots = currentConfig.characterList

        val newIndex = robots.indexOfFirst { it.roleName == roleName }
        if (newIndex >= 0) {
            currentConfig = currentConfig.copy(activeRoleIndex = newIndex)
            characterRepo.saveConfig(currentConfig)

            val newRobot = robots[newIndex]
            _activeCharacter.value = newRobot
            agentConfigManager.switchRole(roleName)
            syncWakeWordsToAgent()

            Log.d(TAG, "Switched character to $roleName")
            return true
        }

        Log.w(TAG, "Character $roleName not found")
        return false
    }

    override suspend fun updateWakeWord(roleName: String, wakeWord: String): Boolean {
        // Validate wakeWord first before saving to config database
        val tokenVariants = predefinedTokens[wakeWord] ?: agentConfigManager.convertTextToTokens(wakeWord)
        if (tokenVariants.isEmpty()) {
            Log.w(TAG, "Rejecting invalid wakeWord: $wakeWord (Cannot convert to valid tokens)")
            return false
        }

        val robots = currentConfig.characterList.toMutableList()
        val index = robots.indexOfFirst { it.roleName == roleName }
        if (index >= 0) {
            val updatedRobot = robots[index].copy(wakeWord = wakeWord)
            robots[index] = updatedRobot

            currentConfig = currentConfig.copy(characterList = robots)
            characterRepo.saveConfig(currentConfig)

            _allCharacters.value = robots
            if (_activeCharacter.value?.roleName == roleName) {
                _activeCharacter.value = updatedRobot
                syncWakeWordsToAgent()
            }

            Log.d(TAG, "Updated wakeWord for $roleName to $wakeWord")
            return true
        }
        return false
    }

    override fun syncWakeWordsToAgent() {
        val activeRobot = _activeCharacter.value
        if (activeRobot == null) {
            Log.w(TAG, "No active character to sync wake words for")
            return
        }

        val wakeWordsList = activeRobot.getWakeWords()

        val keywordLines = mutableListOf<String>()

        for (word in wakeWordsList) {
            val tokenVariants = predefinedTokens[word] ?: agentConfigManager.convertTextToTokens(word)
            for (tokens in tokenVariants) {
                if (tokens.isNotBlank()) {
                    keywordLines.add("$tokens @$word")
                }
            }
        }

        agentConfigManager.updateKwsKeywords(keywordLines)
        Log.d(
            TAG,
            "Synchronized wake words for active character ${activeRobot.roleName} to Agent: $wakeWordsList"
        )
    }
}
