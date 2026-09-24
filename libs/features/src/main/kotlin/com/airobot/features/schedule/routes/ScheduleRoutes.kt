package com.airobot.features.schedule.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airobot.features.FeatureCards
import com.airobot.features.schedule.cards.ScheduleBoardCard
import com.airobot.features.schedule.cards.ScheduleHomeCard
import com.airobot.features.schedule.cards.ScheduleListCard
import com.airobot.features.schedule.viewmodel.ScheduleViewModel

@Composable
fun ScheduleHomeRoute(
    modifier: Modifier = Modifier,
    scheduleViewModel: ScheduleViewModel = hiltViewModel(),
    onShowOverlay: (String) -> Unit,
    onNavigateToBoard: () -> Unit
) {
    ScheduleHomeCard(
        modifier = modifier,
        scheduleViewModel = scheduleViewModel,
        onNavigateToBoard = onNavigateToBoard,
        onRemindClick = { action ->
            when (action) {
                "schedule" -> onShowOverlay(FeatureCards.SCHEDULE_PLANNER)
                "logbook" -> onShowOverlay(FeatureCards.LOGBOOK)
                "focus" -> onShowOverlay(FeatureCards.TIMER)
            }
        }
    )
}

@Composable
fun ScheduleBoardRoute(
    modifier: Modifier = Modifier,
    scheduleViewModel: ScheduleViewModel = hiltViewModel(),
    onShowOverlay: (String) -> Unit
) {
    ScheduleBoardCard(
        modifier = modifier,
        scheduleViewModel = scheduleViewModel,
        onRemindClick = { action ->
            when (action) {
                "schedule" -> onShowOverlay(FeatureCards.SCHEDULE_PLANNER)
                "logbook" -> onShowOverlay(FeatureCards.LOGBOOK)
                "focus" -> onShowOverlay(FeatureCards.TIMER)
            }
        }
    )
}

@Composable
fun ScheduleListRoute(
    modifier: Modifier = Modifier,
    scheduleViewModel: ScheduleViewModel = hiltViewModel(),
    onShowOverlay: (String) -> Unit
) {
    ScheduleListCard(
        modifier = modifier,
        scheduleViewModel = scheduleViewModel,
        onRemindClick = { action ->
            when (action) {
                "schedule" -> onShowOverlay(FeatureCards.SCHEDULE_PLANNER)
                "logbook" -> onShowOverlay(FeatureCards.LOGBOOK)
                "focus" -> onShowOverlay(FeatureCards.TIMER)
            }
        }
    )
}
