package com.airobot.features.podcast.cards

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.airobot.features.R
import com.airobot.features.podcast.cards.player.ComicBookPlayer
import com.airobot.features.podcast.cards.player.CrtMonitorPlayer
import com.airobot.features.podcast.cards.player.PlayerEpisodeInfo
import com.airobot.features.podcast.cards.player.PlayerProgressSlider
import com.airobot.features.podcast.cards.player.PlayerToolbar
import com.airobot.features.podcast.cards.player.VinylRecordTurntable
import com.airobot.features.podcast.cards.widgets.OverlayCloseArrowIcon
import com.airobot.features.podcast.cards.widgets.OverlayHangingTab
import com.airobot.features.podcast.cards.widgets.OverlayTabsScaffold
import com.airobot.features.podcast.viewmodel.PodcastViewModel
import com.airobot.framework.comp.TopAlertBanner
import com.airobot.framework.comp.TopAlertSeverity
import com.airobot.framework.theme.PodcastFeaturedBg
import com.airobot.framework.theme.RobotTheme
import kotlinx.coroutines.delay

@Composable
fun PodcastPlayerOverlay(
    onClose: () -> Unit,
    onWakeupAirobot: () -> Unit,
    podcastViewModel: PodcastViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isDark = RobotTheme.isDark

    // Unified alert banner states
    var alertVisible by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf("") }
    var alertSeverity by remember { mutableStateOf(TopAlertSeverity.WARNING) }

    LaunchedEffect(alertVisible) {
        if (alertVisible) {
            delay(2500)
            alertVisible = false
        }
    }

    LaunchedEffect(podcastViewModel) {
        podcastViewModel.playbackError.collect { stringResId ->
            alertMessage = context.getString(stringResId)
            alertSeverity = TopAlertSeverity.ERROR
            alertVisible = true
        }
    }

    val activeEpisode by podcastViewModel.activeEpisode.collectAsState()
    val isPlaying by podcastViewModel.isPlaying.collectAsState()
    val progress by podcastViewModel.progress.collectAsState()
    val realPlayback by podcastViewModel.realPlaybackState.collectAsState()

    // Determine if current episode is DIY (real media)
    val isDiyEpisode = activeEpisode?.isDiy == true && activeEpisode?.mediaUri != null

    // Spinning record animation
    val infiniteTransition = rememberInfiniteTransition(label = "record")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "recordRotation"
    )

    // Tonearm lowering/raising swing angle animation
    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying) 20f else 36f,
        animationSpec = tween(1200, easing = LinearEasing),
        label = "tonearmAngle"
    )

    val coverResId = getEpisodeCoverDrawable(activeEpisode?.bgImage, activeEpisode?.type ?: "video")

    val typeName = when (activeEpisode?.type) {
        "video" -> stringResource(R.string.podcast_tag_video)
        "audio" -> stringResource(R.string.podcast_tag_audio)
        else -> stringResource(R.string.podcast_tag_text)
    }

    // --- Time & Progress Formatting ---
    // DIY episodes: use real duration/position from playback service
    // Demo episodes: use simulated progress with hardcoded 15:00 duration
    val currentFormatted: String
    val totalFormatted: String

    if (isDiyEpisode && realPlayback.durationMs > 0) {
        currentFormatted = formatMillis(realPlayback.currentPositionMs)
        totalFormatted = formatMillis(realPlayback.durationMs)
    } else {
        // Demo episode: assume 15:00 total duration
        val totalSeconds = 900
        val currentSeconds = ((progress / 100f) * totalSeconds).toInt()
        currentFormatted = "${currentSeconds / 60}:${String.format("%02d", currentSeconds % 60)}"
        totalFormatted = "15:00"
    }

    // Script lines for text/demo subtitle display
    val scriptLines = remember(activeEpisode) {
        activeEpisode?.content?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
    }
    val activeScriptIndex = remember(progress, scriptLines) {
        if (scriptLines.isNotEmpty()) {
            ((progress / 100f) * scriptLines.size).toInt().coerceIn(0, scriptLines.size - 1)
        } else 0
    }
    val activeSubtitle = if (scriptLines.isNotEmpty() && activeScriptIndex in scriptLines.indices) {
        scriptLines[activeScriptIndex]
    } else {
        "连接 Airobot 共同解锁更多话题见解..."
    }

    val cardBgColor = if (isDark) Color(0xFF1E2026) else Color(0xFFFAF5EB)
    val cardBorderColor = if (isDark) Color(0xFF2E323E) else Color(0xFFE6DDC4)

    // Get Media3 Player for video surface binding
    val player = if (isDiyEpisode && activeEpisode?.type == "video") {
        null // Player is passed separately below
    } else null

    Box(modifier = Modifier.fillMaxSize()) {
        OverlayTabsScaffold(
            onClose = onClose,
            enabled = true,
            isDark = isDark,
            isKeepAlive = isPlaying,
            tabs = {
                // Tab 1: Play/Pause control
                OverlayHangingTab(
                    onClick = { podcastViewModel.togglePlay() },
                    tabHeight = 102.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause Tab",
                            tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            repeat(5) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 26.dp, height = 2.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDark) Color(0xFF5A6672) else Color(0xFF90A1AC)
                                        )
                                )
                            }
                        }
                    }
                }

                // Tab 2: Close / Collapse
                OverlayHangingTab(
                    onClick = onClose,
                    tabHeight = 70.dp
                ) {
                    OverlayCloseArrowIcon(isDark = isDark)
                }
            },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1) Player container — type-switched
                    Box(
                        modifier = Modifier
                            .weight(1.8f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val currentEpisode = activeEpisode
                        if (currentEpisode != null) {
                            val playerWidth = 440.dp
                            val playerHeight = 330.dp
                            when (currentEpisode.type) {
                                "video" -> {
                                    CrtMonitorPlayer(
                                        episode = currentEpisode,
                                        isPlaying = isPlaying,
                                        progress = progress,
                                        activeSubtitle = activeSubtitle,
                                        width = playerWidth,
                                        exoPlayer = if (isDiyEpisode) {
                                            podcastViewModel.getPlayer()
                                        } else null
                                    )
                                }

                                "audio" -> {
                                    VinylRecordTurntable(
                                        isPlaying = isPlaying,
                                        rotation = rotation,
                                        tonearmAngle = tonearmAngle,
                                        coverResId = coverResId,
                                        cardBgColor = cardBgColor,
                                        onRecordClick = { podcastViewModel.togglePlay() },
                                        playerSize = 380.dp
                                    )
                                }

                                else -> { // text
                                    ComicBookPlayer(
                                        episode = currentEpisode,
                                        activeSubtitle = activeSubtitle,
                                        scriptLines = scriptLines,
                                        activeScriptIndex = activeScriptIndex,
                                        width = playerWidth,
                                        height = playerHeight
                                    )
                                }
                            }
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(if (isDark) Color(0xFF2D3139) else Color(0xFFE5DEC9))
                    )

                    // 2) Bottom: Status Row, Slider, Episode Info
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Status Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Headphones,
                                    contentDescription = null,
                                    tint = PodcastFeaturedBg,
                                    modifier = Modifier.size(18.dp)
                                )
                                val labelText = when (activeEpisode?.type) {
                                    "video" -> "Video Case"
                                    "audio" -> "Audio Station"
                                    else -> "Text Journal"
                                }
                                Text(
                                    text = labelText,
                                    color = PodcastFeaturedBg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }

                            PlayerToolbar(
                                onWakeupAirobot = onWakeupAirobot,
                                onPlaceholderClick = { featureName ->
                                    alertSeverity = TopAlertSeverity.WARNING
                                    alertMessage = "【$featureName】功能后续将根据用户需求开发，当前先作为演示原型。"
                                    alertVisible = true
                                    Log.d("PodcastPlayerOverlay", "Clicked placeholder: $featureName")
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Progress Slider
                        PlayerProgressSlider(
                            currentFormatted = currentFormatted,
                            totalFormatted = totalFormatted,
                            progress = progress,
                            onSeek = { podcastViewModel.seekTo(it) }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Episode Info
                        PlayerEpisodeInfo(
                            episode = activeEpisode,
                            typeName = typeName
                        )
                    }
                }
            }
        )

        // Alert Banner
        TopAlertBanner(
            visible = alertVisible,
            message = alertMessage,
            severity = alertSeverity,
            onDismiss = { alertVisible = false },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(100f)
        )
    }
}

/**
 * Format milliseconds to mm:ss display string.
 */
private fun formatMillis(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${String.format("%02d", seconds)}"
}

private fun getEpisodeCoverDrawable(bgImage: String?, type: String): Int {
    if (bgImage != null) {
        when {
            bgImage.contains("ocean") -> return R.drawable.cover_episode_ocean
            bgImage.contains("business") -> return R.drawable.cover_episode_business
            bgImage.contains("forest") -> return R.drawable.cover_episode_forest
            bgImage.contains("tech") -> return R.drawable.cover_episode_tech
        }
    }
    // Default fallback based on type for DIY/custom episodes
    return when (type) {
        "video" -> R.drawable.cover_episode_ocean
        "audio" -> R.drawable.cover_episode_forest
        else -> R.drawable.cover_episode_business
    }
}
