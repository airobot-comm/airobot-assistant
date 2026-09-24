package com.airobot.features.podcast.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airobot.features.podcast.cards.PodcastDiyOverlay
import com.airobot.features.podcast.cards.PodcastPlayerOverlay
import com.airobot.features.podcast.viewmodel.PodcastViewModel

@Composable
fun PodcastOverlayRoute(
    modifier: Modifier = Modifier,
    onHideOverlay: () -> Unit,
    onWakeupAirobot: () -> Unit
) {
    PodcastPlayerOverlay(
        onClose = { onHideOverlay() },
        onWakeupAirobot = onWakeupAirobot
    )
}

@Composable
fun PodcastDiyOverlayRoute(
    modifier: Modifier = Modifier,
    podcastViewModel: PodcastViewModel = hiltViewModel(),
    onHideOverlay: () -> Unit
) {
    PodcastDiyOverlay(
        modifier = modifier,
        podcastViewModel = podcastViewModel,
        onClose = { onHideOverlay() }
    )
}
