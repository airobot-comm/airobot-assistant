package com.airobot.features.podcast.cards.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DiscFull
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.R
import com.airobot.features.podcast.data.model.PodcastEpisode
import com.airobot.framework.theme.PodcastFeaturedBg
import com.airobot.framework.theme.RobotTheme

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

/**
 * Compact now playing widget displayed on the Podcast Home card when an episode is active.
 */
@Composable
fun NowPlayingWidget(
    activeEpisode: PodcastEpisode,
    isPlaying: Boolean,
    progress: Float,
    discRotation: Float,
    onCardClick: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 3.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.10f),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isDark) Color(0xFF312E81).copy(alpha = 0.3f)
                else Color(0xFFEEF2FF)
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color(0xFF6366F1).copy(alpha = 0.2f)
                else Color(0xFFC7D2FE),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onCardClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Rotating vinyl representation with cover image background
        val coverRes = getEpisodeCoverDrawable(activeEpisode.bgImage, activeEpisode.type)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(coverRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.40f))
                    .rotate(if (isPlaying) discRotation else 0f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DiscFull,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = activeEpisode.title,
                    color = if (isDark) Color(0xFFEEF2FF) else Color(0xFF312E81),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${progress.toInt()}%",
                    color = PodcastFeaturedBg,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            
            val typeName = when (activeEpisode.type) {
                "video" -> stringResource(R.string.podcast_tag_video)
                "audio" -> stringResource(R.string.podcast_tag_audio)
                else -> stringResource(R.string.podcast_tag_text)
            }
            
            Text(
                text = "${activeEpisode.date} · $typeName",
                color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4F46E5),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape),
                color = PodcastFeaturedBg,
                trackColor = RobotTheme.colors.cardBorder
            )
        }

        // Play/pause toggle trigger button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PodcastFeaturedBg)
                .clickable { onTogglePlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
        }
    }
}
