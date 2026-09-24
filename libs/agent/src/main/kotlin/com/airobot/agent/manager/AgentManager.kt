package com.airobot.agent.manager

import kotlinx.coroutines.flow.StateFlow

interface AgentManager {
    // Persistent Configuration
    val config: StateFlow<AgentConfig>
    suspend fun setSpeechInterruptionEnabled(enabled: Boolean)

    // Dynamic Context (In-Memory Role State)
    val activeRole: StateFlow<String?>
    fun getActiveRole(): String?
    fun switchRole(roleName: String)

    // Wake Word (KWS) Configuration
    fun updateKwsKeywords(keywords: List<String>)
    fun convertTextToTokens(text: String): List<String>
}
