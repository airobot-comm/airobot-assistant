package com.airobot.features.podcast.cards.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.podcast.data.model.PodcastEpisode
import com.airobot.framework.theme.RobotTheme
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import kotlin.math.sin

/**
 * Skeuomorphic Retro CRT TV Monitor for video/visual podcast playback.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun CrtMonitorPlayer(
    episode: PodcastEpisode,
    isPlaying: Boolean,
    progress: Float,
    activeSubtitle: String,
    modifier: Modifier = Modifier,
    width: Dp = 380.dp, // Default size scaled up from 320.dp
    exoPlayer: Player? = null // Media3 Player for DIY video episodes
) {
    val isDark = RobotTheme.isDark

    val categoryGrad = when (episode.type) {
        "video" -> Brush.verticalGradient(listOf(Color(0xFFF472B6), Color(0xFFEC4899)))
        "audio" -> Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9)))
        else -> Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF10B981)))
    }

    val typeIcon = when (episode.type) {
        "video" -> Icons.Outlined.Videocam
        "audio" -> Icons.AutoMirrored.Outlined.VolumeUp
        else -> Icons.AutoMirrored.Outlined.Article
    }

    // Phase animation for the oscilloscope sine wave
    val infiniteTransition = rememberInfiniteTransition(label = "crtPhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Ripples expansion animations
    val rippleScale1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1"
    )
    val rippleScale2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2"
    )

    Column(
        modifier = modifier
            .width(width)
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF292524)) // stone-800 cabinet
            .border(5.dp, Color(0xFF44403C), RoundedCornerShape(32.dp)) // stone-700 bezel
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // CRT Screen inside bezel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A)) // slate-900 glass
                .border(2.dp, Color(0xFF0A0F1D), RoundedCornerShape(20.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            // For DIY video with real media: render PlayerView
            if (exoPlayer != null && episode.isDiy) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }
                    },
                    update = { view -> view.player = exoPlayer },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )

                // Scan line overlay on top of real video
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineSpacing = 6.dp.toPx()
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.08f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        y += lineSpacing
                    }
                }
            } else {
            // Demo fallback: VHS overlay scan lines + sine wave visualization
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lineSpacing = 6.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5f.dp.toPx()
                    )
                    y += lineSpacing
                }
            }

            // Top Status Info: PLAY CH1 + Timer
            val totalSeconds = 900 // 15:00
            val currentSeconds = ((progress / 100f) * totalSeconds).toInt()
            val timeString = "TR ${currentSeconds / 60}:${String.format("%02d", currentSeconds % 60)}"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color(0xFFEF4444) else Color(0xFF64748B))
                    )
                    Text(
                        text = "PLAY CH1",
                        color = Color(0xFF2DD4BF),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = timeString,
                    color = Color(0xFF2DD4BF),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
            }

            // Expanding sound waves and center image
            val visualizerSize = width * 0.375f
            val rippleBaseSize = width * 0.3125f
            Box(
                modifier = Modifier.size(visualizerSize),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    // Ripple 1
                    Box(
                        modifier = Modifier
                            .size(rippleBaseSize * rippleScale1)
                            .border(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.25f), CircleShape)
                    )
                    // Ripple 2
                    Box(
                        modifier = Modifier
                            .size(rippleBaseSize * rippleScale2)
                            .border(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.4f), CircleShape)
                    )
                }

                // Oscilloscope Oscillating Sine wave
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val path = Path()
                    val amplitude = if (isPlaying) (width * 0.05625f).toPx() else (width * 0.00625f).toPx()
                    val frequency = 0.04f
                    for (x in 0..w.toInt()) {
                        val y = h / 2f + amplitude * sin(x * frequency + phase)
                        if (x == 0) path.moveTo(x.toFloat(), y) else path.lineTo(x.toFloat(), y)
                    }
                    val strokeW = (width * 0.0078125f).toPx()
                    drawPath(
                        path = path,
                        color = Color(0xFF2DD4BF).copy(alpha = 0.7f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // Center Thumbnail Cover (Skeuomorphic Gradient + Vector Icon placeholder)
                val thumbnailSize = width * 0.16875f
                Box(
                    modifier = Modifier
                        .size(thumbnailSize)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryGrad)
                        .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Dialog subtitles / Captions box at the bottom
            val captionHeight = width * 0.13125f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(captionHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.dp, Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeSubtitle,
                    color = Color(0xFF5EEAD4), // teal-300
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp
                )
            }
            } // close else (demo fallback)
        }

        // Bottom cabinet details row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = "VIDEO SIGNAL OK",
                    color = Color(0xFF78716C),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "CRT_MODE_ON",
                color = Color(0xFF78716C),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Custom speech bubble shape pointing bottom-right
private val SpeechBubbleShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = 12.dp.value
    // Draw bubble box
    moveTo(r, 0f)
    lineTo(w - r, 0f)
    quadraticTo(w, 0f, w, r)
    lineTo(w, h - r - 10f)
    quadraticTo(w, h - 10f, w - r, h - 10f)
    // Draw tail pointing down-right
    lineTo(w - 20f, h - 10f)
    lineTo(w - 15f, h)
    lineTo(w - 32f, h - 10f)
    lineTo(r, h - 10f)
    quadraticTo(0f, h - 10f, 0f, h - r - 10f)
    lineTo(0f, r)
    quadraticTo(0f, 0f, r, 0f)
    close()
}

/**
 * Skeuomorphic Comic Panel Book for text/visual journal podcast playback.
 */
@Composable
fun ComicBookPlayer(
    episode: PodcastEpisode,
    activeSubtitle: String,
    scriptLines: List<String>,
    activeScriptIndex: Int,
    modifier: Modifier = Modifier,
    width: Dp = 380.dp, // Default size scaled up from 320.dp
    height: Dp = width * 0.96875f
) {
    val isDark = RobotTheme.isDark
    val listState = rememberLazyListState()

    val categoryGrad = when (episode.type) {
        "video" -> Brush.verticalGradient(listOf(Color(0xFFF472B6), Color(0xFFEC4899)))
        "audio" -> Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9)))
        else -> Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF10B981)))
    }

    val typeIcon = when (episode.type) {
        "video" -> Icons.Outlined.Videocam
        "audio" -> Icons.AutoMirrored.Outlined.VolumeUp
        else -> Icons.AutoMirrored.Outlined.Article
    }

    // Smooth auto scroll storyboard list when subtitle index shifts
    LaunchedEffect(activeScriptIndex) {
        if (scriptLines.isNotEmpty() && activeScriptIndex in scriptLines.indices) {
            listState.animateScrollToItem(activeScriptIndex)
        }
    }

    val comicPanelHeight = width * 0.40625f
    val speechBubbleWidth = width * 0.5625f
    val speechBubbleHeight = width * 0.18125f

    Column(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFFCF9F2)) // Cream paper book background
            .border(4.dp, Color(0xFF422006), RoundedCornerShape(24.dp)) // Thick book border
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Comic panel visual scene (top half)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(comicPanelHeight)
                .clip(RoundedCornerShape(16.dp))
                .border(2.5.dp, Color(0xFF422006), RoundedCornerShape(16.dp))
                .background(categoryGrad),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = typeIcon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(36.dp)
            )

            // Panel Number Stamp badge (Top-left corner)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF422006))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "PANEL 0${(activeScriptIndex % 4) + 1}",
                    color = Color(0xFFFCF9F2),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            // Dialogue speech bubble (Bottom corner)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .width(speechBubbleWidth)
                    .height(speechBubbleHeight)
                    .clip(SpeechBubbleShape)
                    .background(Color.White)
                    .border(2.dp, Color(0xFF422006), SpeechBubbleShape)
                    .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = activeSubtitle,
                    color = Color(0xFF1C1917),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Perforated page divider line in the middle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFF422006).copy(alpha = 0.15f)))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    tint = Color(0xFF422006).copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "绘本台本",
                    color = Color(0xFF422006).copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFF422006).copy(alpha = 0.15f)))
        }

        // 2. Interactive scrollable storyboard/captions block (bottom half)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFAF7F0).copy(alpha = 0.6f))
                .border(1.dp, Color(0xFF422006).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(6.dp)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(scriptLines) { idx, line ->
                    val isCurrent = idx == activeScriptIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isCurrent) Color(0xFFFDE047).copy(alpha = 0.4f) // Yellow highlighter
                                else Color.Transparent
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 6.dp else 4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) Color(0xFFCA8A04)
                                        else Color(0xFFD6D3D1)
                                    )
                            )
                            Text(
                                text = line,
                                color = if (isCurrent) Color(0xFF422006) else Color(0xFF78716C),
                                fontSize = 9.sp,
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Medium,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
