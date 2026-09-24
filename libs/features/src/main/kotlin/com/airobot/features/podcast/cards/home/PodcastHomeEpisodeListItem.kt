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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Standard list item for recommended episodes on the Podcast Home card.
 */
@Composable
fun PodcastHomeEpisodeListItem(
    episode: PodcastEpisode,
    onEpisodeClick: () -> Unit,
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
                if (isDark) Color(0xFF1E293B)
                else Color.White
            )
            .border(
                width = 1.dp,
                color = RobotTheme.colors.cardBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onEpisodeClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cover shape representation with local cover images
        val coverRes = getEpisodeCoverDrawable(episode.bgImage, episode.type)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(coverRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.9f
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                color = RobotTheme.colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            
            val typeName = when (episode.type) {
                "video" -> stringResource(R.string.podcast_tag_video)
                "audio" -> stringResource(R.string.podcast_tag_audio)
                else -> stringResource(R.string.podcast_tag_text)
            }
            
            Text(
                text = "${episode.date} · $typeName",
                color = RobotTheme.colors.textMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // QnA dialogues count badge
        if (episode.qnaHistory.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFEC4899).copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = Color(0xFFEC4899)
                )
                Text(
                    text = "${episode.qnaHistory.size}",
                    color = Color(0xFFEC4899),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
