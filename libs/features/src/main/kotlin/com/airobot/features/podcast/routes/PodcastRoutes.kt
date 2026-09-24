package com.airobot.features.podcast.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.airobot.features.FeatureCards
import com.airobot.features.podcast.cards.PodcastHomeCard
import com.airobot.features.podcast.cards.PodcastLibraryCard
import com.airobot.features.podcast.cards.PodcastSubscribeCard
import androidx.hilt.navigation.compose.hiltViewModel
import com.airobot.features.podcast.viewmodel.PodcastViewModel

@Composable
fun PodcastHomeRoute(
    modifier: Modifier = Modifier,
    podcastViewModel: PodcastViewModel = hiltViewModel(),
    onShowOverlay: (String) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    PodcastHomeCard(
        modifier = modifier,
        onPlayClick = { onShowOverlay(FeatureCards.PODCAST) },
        onRemindClick = { action ->
            when (action) {
                "podcast" -> {
                    val activeEp = podcastViewModel.activeEpisode.value
                    if (activeEp == null) {
                        val firstRealEp = podcastViewModel.recommendedEpisodes.value.firstOrNull { it.isDiy && !it.mediaUri.isNullOrEmpty() }
                        if (firstRealEp != null) {
                            podcastViewModel.playEpisode(firstRealEp)
                        } else {
                            podcastViewModel.recommendedEpisodes.value.firstOrNull()?.let {
                                podcastViewModel.playEpisode(it)
                            }
                        }
                    }
                    onShowOverlay(FeatureCards.PODCAST)
                }
                "logbook" -> onShowOverlay(FeatureCards.LOGBOOK)
                "diy" -> onShowOverlay(FeatureCards.DIY_PODCAST)
                else -> onShowOverlay(action)
            }
        },
        onNavigateToLibrary = onNavigateToLibrary
    )
}

@Composable
fun PodcastLibraryRoute(
    modifier: Modifier = Modifier,
    onShowOverlay: (String) -> Unit
) {
    PodcastLibraryCard(
        modifier = modifier,
        onPlayClick = { onShowOverlay(FeatureCards.PODCAST) }
    )
}

@Composable
fun PodcastSubscribeRoute(
    modifier: Modifier = Modifier
) {
    PodcastSubscribeCard(modifier = modifier)
}
