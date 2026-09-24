package com.airobot.features.podcast.cards

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airobot.features.R
import com.airobot.features.aiserv.guidance.components.AetherRemindBanner
import com.airobot.features.FeatureCards
import com.airobot.features.podcast.cards.home.NowPlayingWidget
import com.airobot.features.podcast.cards.home.PodcastHomeEpisodeListItem
import com.airobot.features.podcast.cards.home.PodcastStudioMic
import com.airobot.features.podcast.viewmodel.PodcastViewModel
import com.airobot.framework.cards.ModuleServiceCard
import com.airobot.framework.theme.PodcastFeaturedBg
import com.airobot.framework.theme.RobotTheme

/**
 * Podcast Home Card — Left interactive column and right recommendations.
 *
 * Prototype ref: PodcastHomeView.tsx + docs/design/aipodcast_home.png
 */
@Composable
fun PodcastHomeCard(
    modifier: Modifier = Modifier,
    podcastViewModel: PodcastViewModel = hiltViewModel(),
    onPlayClick: () -> Unit = {},
    onRemindClick: (String) -> Unit = {},
    onNavigateToLibrary: () -> Unit = {}
) {
    val isDark = RobotTheme.isDark

    val recommendedEpisodes by podcastViewModel.recommendedEpisodes.collectAsState()
    val activeEpisode by podcastViewModel.activeEpisode.collectAsState()
    val isPlaying by podcastViewModel.isPlaying.collectAsState()
    val progress by podcastViewModel.progress.collectAsState()
    val recommendation by podcastViewModel.recommendation.collectAsState()

    // Use recommendation engine strategy list (show up to 2 if active is present, else 3 to prevent UI overlap)
    val displayEpisodes = recommendedEpisodes
        .filter { it.id != activeEpisode?.id }
        .take(if (activeEpisode != null) 2 else 3)

    val remindCards by podcastViewModel.remindCards.collectAsState(initial = emptyList())
    
    val mappedRemindCards = remindCards.map { card ->
        if (card.tag == FeatureCards.PODCAST) {
            card.copy(content = recommendation)
        } else {
            card
        }
    }

    // Rotation animation for now playing disc
    val infiniteTransition = rememberInfiniteTransition(label = "discRotation")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "discAngle"
    )

    ModuleServiceCard(
        title = stringResource(R.string.category_podcast),
        subtitle = stringResource(R.string.category_podcast_subtitle),
        icon = Icons.Outlined.Headphones,
        iconColor = PodcastFeaturedBg,
        iconBgColor = PodcastFeaturedBg.copy(alpha = 0.10f),
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        showSubtitle = true
    ) {
        // Main content area - Scrollable on overflow, weight(1f) dynamically pushes the remind banner to the bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // --- Left Interactive AI Podcast Card ---
                PodcastStudioMic()

                // --- Right Recommended Episodes Card with Hanging Button ---
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // The main card container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp) // half of button height
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) // bg-slate-800/60
                                else Color(0xFFF8FAFC) // bg-slate-50
                            )
                            .border(
                                width = 1.dp,
                                color = RobotTheme.colors.cardBorder,
                                shape = RoundedCornerShape(32.dp)
                            )
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 14.dp,
                                bottom = 26.dp
                            ), // extra padding for hanging button
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Label Header
                        Text(
                            text = stringResource(R.string.podcast_home_recommended_title),
                            color = RobotTheme.colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        // 1) Now Playing Widget
                        activeEpisode?.let { active ->
                            NowPlayingWidget(
                                activeEpisode = active,
                                isPlaying = isPlaying,
                                progress = progress,
                                discRotation = discRotation,
                                onCardClick = onPlayClick,
                                onTogglePlay = { podcastViewModel.togglePlay() }
                            )
                        }

                        // 2) List of other recommendation episodes
                        displayEpisodes.forEach { ep ->
                            PodcastHomeEpisodeListItem(
                                episode = ep,
                                onEpisodeClick = {
                                    podcastViewModel.playEpisode(ep)
                                    onPlayClick() // Pop up retro player overlay card directly!
                                }
                            )
                        }
                    }

                    // 3) Hanging Add Subscriptions Pill Button, overlapping the bottom border of the card
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isDark) Color(0xFF334155).copy(alpha = 0.3f) // bg-slate-700/30
                                else Color(0xFFF8FAFC) // bg-slate-50
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color.Transparent else Color(0xFFE2E8F0), // border-slate-200
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onNavigateToLibrary() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val elementColor =
                                if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B) // text-slate-400 / text-slate-500
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = elementColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(R.string.podcast_home_view_more),
                                color = elementColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // Bottom Aether dynamic remind card
        AetherRemindBanner(
            pages = mappedRemindCards,
            cardIcon = Icons.Outlined.Headphones,
            accentColor = PodcastFeaturedBg,
            onPageClick = { page ->
                onRemindClick(page.actionTarget)
            }
        )
    }
}
