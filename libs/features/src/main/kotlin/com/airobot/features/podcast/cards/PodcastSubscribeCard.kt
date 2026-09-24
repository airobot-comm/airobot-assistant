package com.airobot.features.podcast.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.zIndex
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
import com.airobot.features.podcast.cards.subscribe.SubscriptionItemCard
import com.airobot.features.podcast.cards.widgets.verticalScrollbar
import com.airobot.features.podcast.viewmodel.PodcastViewModel
import com.airobot.framework.cards.ModuleServiceCard
import com.airobot.framework.theme.PodcastFeaturedBg
import com.airobot.framework.theme.RobotTheme

/**
 * Podcast Subscribe Card — subscription management page with my subs & recommended.
 *
 * Prototype ref: PodcastSubscribeView.tsx
 */
@Composable
fun PodcastSubscribeCard(
    modifier: Modifier = Modifier,
    podcastViewModel: PodcastViewModel = hiltViewModel()
) {
    val isDark = RobotTheme.isDark
    val subscriptions by podcastViewModel.subscriptions.collectAsState()
 
    val mySubs = subscriptions.filter { it.isSubscribed }
    val recommendedSubs = subscriptions.filter { !it.isSubscribed }

    Box(modifier = modifier) {
        ModuleServiceCard(
            title = stringResource(R.string.podcast_subscribe_title),
            subtitle = stringResource(R.string.podcast_subscribe_subtitle),
            icon = Icons.Outlined.RssFeed,
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
                // --- 1. My Subscriptions (我的订阅) ---
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = RobotTheme.colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.podcast_subscribe_my),
                                color = RobotTheme.colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "${mySubs.size} / 5",
                            color = if (mySubs.size >= 5) RobotTheme.colors.accent else RobotTheme.colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Render subscribed grid (chunked by 2)
                    val subscribedGridList = mutableListOf<@Composable () -> Unit>()
                    mySubs.forEach { sub ->
                        subscribedGridList.add {
                            SubscriptionItemCard(
                                sub = sub,
                                onActionClick = { podcastViewModel.toggleSubscription(sub.id) }
                            )
                        }
                    }

                    val subscribedChunks = subscribedGridList.chunked(2)
                    subscribedChunks.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                rowItems[0]()
                            }
                            if (rowItems.size > 1) {
                                Box(modifier = Modifier.weight(1f)) {
                                    rowItems[1]()
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // --- 2. Recommended Columns (推荐栏目) ---
                if (recommendedSubs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.podcast_subscribe_recommended),
                            color = RobotTheme.colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        val recommendedChunks = recommendedSubs.chunked(2)
                        recommendedChunks.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SubscriptionItemCard(
                                        sub = rowItems[0],
                                        onActionClick = { podcastViewModel.toggleSubscription(rowItems[0].id) }
                                    )
                                }
                                if (rowItems.size > 1) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        SubscriptionItemCard(
                                            sub = rowItems[1],
                                            onActionClick = { podcastViewModel.toggleSubscription(rowItems[1].id) }
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

