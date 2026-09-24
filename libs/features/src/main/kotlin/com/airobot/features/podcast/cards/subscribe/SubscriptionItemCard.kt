package com.airobot.features.podcast.cards.subscribe

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
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
import com.airobot.features.podcast.data.model.PodcastSubscription
import com.airobot.framework.theme.PodcastFeaturedBg
import com.airobot.framework.theme.RobotTheme

/**
 * Standard subscription list item card.
 */
@Composable
fun SubscriptionItemCard(
    sub: PodcastSubscription,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark

    val categoryColor = when (sub.type) {
        "video" -> Color(0xFFEC4899)
        "audio" -> Color(0xFF0EA5E9)
        "custom" -> Color(0xFF8B5CF6) // Purple for Custom
        else -> Color(0xFF10B981) // text
    }

    val categoryIcon = when (sub.type) {
        "video" -> Icons.Outlined.Videocam
        "audio" -> Icons.AutoMirrored.Outlined.VolumeUp
        "custom" -> Icons.Outlined.AutoAwesome
        else -> Icons.AutoMirrored.Outlined.Article // text
    }

    val categoryName = when (sub.type) {
        "video" -> stringResource(R.string.podcast_tag_video)
        "audio" -> stringResource(R.string.podcast_tag_audio)
        "custom" -> stringResource(R.string.podcast_tag_custom)
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
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Column Title (column name) at the top
        Text(
            text = sub.title,
            color = RobotTheme.colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 2. Metadata row: Type Tag & Time Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left category tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
            }

            // Right scheduled time tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color(0xFF334155).copy(alpha = 0.5f)
                        else Color(0xFFF1F5F9)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = RobotTheme.colors.textSecondary,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = sub.time,
                    color = RobotTheme.colors.textSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. Description
        Text(
            text = sub.description,
            color = RobotTheme.colors.textMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 4. Subscription Action Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (sub.isSubscribed) {
                        if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                    } else {
                        PodcastFeaturedBg
                    }
                )
                .clickable { onActionClick() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (sub.isSubscribed) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Subscribed",
                        tint = RobotTheme.colors.textSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = stringResource(R.string.podcast_subscribed),
                        color = RobotTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Subscribe",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = stringResource(R.string.podcast_subscribe_action),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
