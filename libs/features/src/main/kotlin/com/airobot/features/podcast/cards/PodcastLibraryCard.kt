package com.airobot.features.podcast.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airobot.features.R
import com.airobot.features.podcast.cards.library.EpisodeLibraryCardItem
import com.airobot.features.podcast.cards.library.LibraryFilterPill
import com.airobot.features.podcast.cards.widgets.verticalScrollbar
import com.airobot.features.podcast.viewmodel.PodcastViewModel
import com.airobot.framework.cards.ModuleServiceCard
import com.airobot.framework.theme.PodcastFeaturedBg
import com.airobot.framework.theme.RobotTheme

/**
 * Podcast Library Card — episode history, categorization, and favorites list with a vertical scrollbar.
 *
 * Prototype ref: PodcastLibraryView.tsx
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PodcastLibraryCard(
    modifier: Modifier = Modifier,
    podcastViewModel: PodcastViewModel = hiltViewModel(),
    onPlayClick: () -> Unit = {}
) {
    val isDark = RobotTheme.isDark
    val episodes by podcastViewModel.episodes.collectAsState()
    val latestEpisodes by podcastViewModel.latestEpisodes.collectAsState()

    var activeFilter by remember { mutableStateOf("all") }

    // Unique channel names for subscription channels filtering
    val channels = episodes.map { it.channelName }.distinct().filter { it.isNotEmpty() }

    // Filter episodes list
    val filteredEpisodes = when (activeFilter) {
        "all" -> episodes
        "favorite" -> episodes.filter { it.favorite }
        "video" -> episodes.filter { it.type == "video" }
        "audio" -> episodes.filter { it.type == "audio" }
        "text" -> episodes.filter { it.type == "text" }
        else -> episodes.filter { it.channelName == activeFilter }
    }

    Box(modifier = modifier) {
        ModuleServiceCard(
            title = stringResource(R.string.podcast_library_title),
            subtitle = stringResource(R.string.podcast_library_subtitle),
            icon = Icons.Outlined.History,
            iconColor = PodcastFeaturedBg,
            iconBgColor = PodcastFeaturedBg.copy(alpha = 0.10f),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            showSubtitle = true
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Standard Scrollable Column with custom scrollbar
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .verticalScrollbar(
                        scrollValue = scrollState.value,
                        maxValue = scrollState.maxValue,
                        isDark = isDark,
                        paddingRight = 0.dp
                    )
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- 1. Latest Podcast (最新播客) Section ---
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RssFeed,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.podcast_library_latest),
                                color = RobotTheme.colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (latestEpisodes.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(latestEpisodes) { ep ->
                                Box(modifier = Modifier.width(260.dp)) {
                                    EpisodeLibraryCardItem(
                                        episode = ep,
                                        onCardClick = {
                                            podcastViewModel.playEpisode(ep)
                                            onPlayClick()
                                        },
                                        onFavoriteClick = { podcastViewModel.toggleFavorite(ep.id) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (isDark) RobotTheme.colors.surfaceOverlay.copy(alpha = 0.03f)
                                    else RobotTheme.colors.surfaceOverlay.copy(alpha = 0.10f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = RobotTheme.colors.cardBorder,
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.podcast_library_empty_latest),
                                color = RobotTheme.colors.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // --- 2. All Programs (全部节目) Section ---
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RssFeed,
                            contentDescription = null,
                            tint = PodcastFeaturedBg,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.podcast_library_all_programs),
                            color = RobotTheme.colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    // Filter row of pill buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FilterAlt,
                            contentDescription = "Filter",
                            tint = RobotTheme.colors.textMuted,
                            modifier = Modifier.size(14.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                LibraryFilterPill(
                                    text = stringResource(R.string.podcast_library_filter_all),
                                    selected = activeFilter == "all",
                                    onClick = { activeFilter = "all" }
                                )
                            }
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (activeFilter == "favorite") Color(0xFFFBBF24)
                                            else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                        )
                                        .clickable { activeFilter = "favorite" }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = if (activeFilter == "favorite") Color(0xFF78350F) else RobotTheme.colors.textMuted
                                    )
                                    Text(
                                        text = stringResource(R.string.podcast_library_filter_favorite),
                                        color = if (activeFilter == "favorite") Color(0xFF78350F) else RobotTheme.colors.textSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            item {
                                LibraryFilterPill(
                                    text = stringResource(R.string.podcast_tag_video),
                                    selected = activeFilter == "video",
                                    onClick = { activeFilter = "video" }
                                )
                            }
                            item {
                                LibraryFilterPill(
                                    text = stringResource(R.string.podcast_tag_audio),
                                    selected = activeFilter == "audio",
                                    onClick = { activeFilter = "audio" }
                                )
                            }
                            item {
                                LibraryFilterPill(
                                    text = stringResource(R.string.podcast_tag_text),
                                    selected = activeFilter == "text",
                                    onClick = { activeFilter = "text" }
                                )
                            }

                            // Dynamic channel filters
                            items(channels) { channel ->
                                LibraryFilterPill(
                                    text = channel,
                                    selected = activeFilter == channel,
                                    onClick = { activeFilter = channel }
                                )
                            }
                        }
                    }
                }

                // Grid content laid out as chunked items
                if (filteredEpisodes.isNotEmpty()) {
                    val chunks = filteredEpisodes.chunked(2)
                    chunks.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                EpisodeLibraryCardItem(
                                    episode = rowItems[0],
                                    onCardClick = {
                                        podcastViewModel.playEpisode(rowItems[0])
                                        onPlayClick()
                                    },
                                    onFavoriteClick = { podcastViewModel.toggleFavorite(rowItems[0].id) }
                                )
                            }
                            if (rowItems.size > 1) {
                                Box(modifier = Modifier.weight(1f)) {
                                    EpisodeLibraryCardItem(
                                        episode = rowItems[1],
                                        onCardClick = {
                                            podcastViewModel.playEpisode(rowItems[1])
                                            onPlayClick()
                                        },
                                        onFavoriteClick = { podcastViewModel.toggleFavorite(rowItems[1].id) }
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                if (isDark) RobotTheme.colors.surfaceOverlay.copy(alpha = 0.03f)
                                else RobotTheme.colors.surfaceOverlay.copy(alpha = 0.10f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.podcast_library_no_results),
                            color = RobotTheme.colors.textMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

