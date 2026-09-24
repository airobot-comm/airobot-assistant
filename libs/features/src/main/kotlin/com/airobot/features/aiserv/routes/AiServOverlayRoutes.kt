package com.airobot.features.aiserv.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airobot.features.aiserv.notepad.AiNotepadOverlay
import com.airobot.features.aiserv.viewmodel.AiServViewModel

@Composable
fun AiNotepadOverlayRoute(
    modifier: Modifier = Modifier,
    aiServViewModel: AiServViewModel = hiltViewModel(),
    onHideOverlay: () -> Unit
) {
    val alarmHistory by aiServViewModel.alarmHistory.collectAsState()
    val timerHistory by aiServViewModel.timerHistory.collectAsState()
    val focusHistory by aiServViewModel.focusHistory.collectAsState()
    val podcastHistory by aiServViewModel.podcastHistory.collectAsState()

    AiNotepadOverlay(
        alarmHistory = alarmHistory,
        timerHistory = timerHistory,
        focusHistory = focusHistory,
        podcastHistory = podcastHistory,
        onClose = { onHideOverlay() }
    )
}
