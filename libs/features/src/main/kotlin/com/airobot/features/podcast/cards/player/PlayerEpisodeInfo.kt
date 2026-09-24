package com.airobot.features.podcast.cards.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.R
import com.airobot.features.podcast.data.model.PodcastEpisode
import com.airobot.framework.theme.RobotTheme

/**
 * Bottom episode info section (category tag + title + fade-out description).
 */
@Composable
fun PlayerEpisodeInfo(
    episode: PodcastEpisode?,
    typeName: String,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark
    val cardBgColor = if (isDark) Color(0xFF1E2026) else Color(0xFFFAF5EB)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Tag and Title in a single row to save vertical space
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isDark) Color(0xFF2D3139) else Color(0xFFE5DEC9).copy(
                            alpha = 0.5f
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = typeName,
                    color = RobotTheme.colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Title
            Text(
                text = episode?.title
                    ?: stringResource(R.string.podcast_no_active_episode),
                color = RobotTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Description/Summary (Limited to 2 lines to prevent content overflow)
        Text(
            text = episode?.summary
                ?: stringResource(R.string.podcast_no_active_episode_summary),
            color = RobotTheme.colors.textMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
