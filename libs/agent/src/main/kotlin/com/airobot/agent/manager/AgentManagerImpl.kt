package com.airobot.agent.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.airobot.agent.audio.AudioService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

private val Context.agentManagerDataStore: DataStore<Preferences> by preferencesDataStore(name = "agent_manager_settings")

@Singleton
class AgentManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioService: AudioService
) : AgentManager {

    companion object {
        private val KEY_SPEECH_INTERRUPTION = booleanPreferencesKey("speech_interruption")
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Persistent Config (DataStore)
    override val config: StateFlow<AgentConfig> = context.agentManagerDataStore.data
        .map { preferences ->
            AgentConfig(
                isSpeechInterruptionEnabled = preferences[KEY_SPEECH_INTERRUPTION] ?: true
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = AgentConfig(isSpeechInterruptionEnabled = true)
        )

    override suspend fun setSpeechInterruptionEnabled(enabled: Boolean) {
        context.agentManagerDataStore.edit { preferences ->
            preferences[KEY_SPEECH_INTERRUPTION] = enabled
        }
    }

    // Dynamic Context (In-Memory Role State)
    private val _activeRole = MutableStateFlow<String?>(null)
    override val activeRole: StateFlow<String?> = _activeRole.asStateFlow()

    override fun getActiveRole(): String? = _activeRole.value

    override fun switchRole(roleName: String) {
        _activeRole.value = roleName
    }

    // Wake Word (KWS) Configuration
    override fun updateKwsKeywords(keywords: List<String>) {
        audioService.updateKwsKeywords(keywords)
    }

    override fun convertTextToTokens(text: String): List<String> {
        return audioService.convertTextToTokens(text)
    }
}
