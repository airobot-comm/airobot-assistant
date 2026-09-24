package com.airobot.features.schedule.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airobot.features.schedule.cards.SchedulePlannerOverlay
import com.airobot.features.schedule.viewmodel.ScheduleViewModel

@Composable
fun SchedulePlannerOverlayRoute(
    modifier: Modifier = Modifier,
    scheduleViewModel: ScheduleViewModel = hiltViewModel(),
    onHideOverlay: () -> Unit
) {
    SchedulePlannerOverlay(
        scheduleViewModel = scheduleViewModel,
        onClose = { onHideOverlay() }
    )
}
