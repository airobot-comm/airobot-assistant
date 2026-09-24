package com.airobot.features.aiserv.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airobot.features.aiserv.notepad.data.AiNotepadRepository
import com.airobot.features.aiserv.notepad.data.AlarmRecord
import com.airobot.features.aiserv.notepad.data.FocusRecord
import com.airobot.features.aiserv.notepad.data.PodcastActivityRecord
import com.airobot.features.aiserv.notepad.data.TimerRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * AiServViewModel — UI controller for AI Services, exposing reactive notepad history state.
 */
@HiltViewModel
class AiServViewModel @Inject constructor(
    private val aiNotepadRepository: AiNotepadRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AiServViewModel"
    }

    // --- Notepad Persistent History States (Derived directly from repository flows) ---
    val alarmHistory: StateFlow<List<AlarmRecord>> = aiNotepadRepository.alarmHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timerHistory: StateFlow<List<TimerRecord>> = aiNotepadRepository.timerHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusHistory: StateFlow<List<FocusRecord>> = aiNotepadRepository.focusHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val podcastHistory: StateFlow<List<PodcastActivityRecord>> = aiNotepadRepository.podcastHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        Log.d(TAG, "Initializing AiServViewModel UI data controller")
    }
}
