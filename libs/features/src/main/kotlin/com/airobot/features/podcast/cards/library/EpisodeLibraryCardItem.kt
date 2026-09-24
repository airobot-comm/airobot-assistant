package com.airobot.features.podcast.cards.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.R
import com.airobot.features.podcast.data.model.PodcastEpisode
import com.airobot.framework.theme.PodcastFeaturedBg
import com.airobot.framework.theme.RobotTheme

/**
 * High-fidelity Episode Card Item used in Library grid/row
 */
@Composable
fun EpisodeLibraryCardItem(
    episode: PodcastEpisode,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark

    val categoryColor = when (episode.type) {
        "video" -> Color(0xFFEC4899)
        "audio" -> Color(0xFF0EA5E9)
        else -> Color(0xFF10B981) // text
    }

    val categoryIcon = when (episode.type) {
        "video" -> Icons.Outlined.Videocam
        "audio" -> Icons.AutoMirrored.Outlined.VolumeUp
        else -> Icons.AutoMirrored.Outlined.Article // text
    }

    val categoryName = when (episode.type) {
        "video" -> stringResource(R.string.podcast_tag_video)
        "audio" -> stringResource(R.string.podcast_tag_audio)
        else -> stringResource(R.string.podcast_tag_text) // text
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 3.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color.Black.copy(alpha = 0.12f),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (isDark) Color(0xFF1E293B)
                else Color.White
            )
            .border(
                width = 1.dp,
                color = RobotTheme.colors.cardBorder,
                shape = RoundedCornerShape(28.dp)
            )
            .clickable { onCardClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Program Title (max 1 line, truncate) at the very top
        Text(
            text = episode.title,
            color = RobotTheme.colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )

        // 2. Metadata: type, DIY status, channel name and played status on the second line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category tag & Channel name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = categoryName,
                    color = categoryColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
                if (episode.channelName.isNotEmpty()) {
                    Text(
                        text = " · ${episode.channelName}",
                        color = RobotTheme.colors.textSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Played status (Right side of metadata row)
            if (episode.played) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = stringResource(R.string.podcast_episode_played),
                        color = Color(0xFF10B981),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (episode.progress > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "${episode.progress.toInt()}%",
                        color = Color(0xFF3B82F6),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6))
                    )
                    Text(
                        text = stringResource(R.string.podcast_episode_unplayed),
                        color = Color(0xFF3B82F6),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Summary (up to 2 lines)
        Text(
            text = episode.summary,
            color = RobotTheme.colors.textMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )

        // 4. Progress bar if active but unplayed
        if (episode.progress > 0 && !episode.played) {
            LinearProgressIndicator(
                progress = { episode.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape),
                color = Color(0xFF3B82F6),
                trackColor = RobotTheme.colors.cardBorder
            )
        }

        // 5. Divider
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RobotTheme.colors.cardBorder.copy(alpha = 0.2f))
        )

        // 6. Bottom control row (date, favorite, play button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = episode.date,
                color = RobotTheme.colors.textMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Favorite Icon Button
                Icon(
                    imageVector = if (episode.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (episode.favorite) Color(0xFFF59E0B) else RobotTheme.colors.textMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onFavoriteClick() }
                )

                // Play Button Icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
