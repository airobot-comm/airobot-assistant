package com.airobot.features.aiserv.notepad.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.aiserv.notepad.data.PodcastActivityRecord
import com.airobot.framework.theme.PodcastFeaturedBg
import com.airobot.framework.theme.RobotTheme

@Composable
fun NotepadPodcastCard(
    item: NotepadItem.PodcastCard,
    isDark: Boolean,
    dividerColor: Color,
    formatTimestamp: (Long) -> String,
    modifier: Modifier = Modifier
) {
    val record = item.record
    val accentColor = PodcastFeaturedBg

    val (icon, titleText, subtitleText) = when (record) {
        is PodcastActivityRecord.PlaybackRecord -> {
            Triple(
                Icons.Outlined.Headphones,
                "播客收听：${record.title}",
                "${formatTimestamp(record.timestamp)} · ${if (record.isCompleted) "已完结" else "已暂停"}"
            )
        }

        is PodcastActivityRecord.CreationRecord -> {
            Triple(
                Icons.Outlined.AutoAwesome,
                "播客创作：${record.title}",
                "${formatTimestamp(record.timestamp)} · ${if (record.isDiy) "本地导入" else "系统生成"}"
            )
        }

        is PodcastActivityRecord.FavoriteRecord -> {
            Triple(
                Icons.Outlined.Star,
                "播客收藏：${record.title}",
                "${formatTimestamp(record.timestamp)} · ${if (record.favorite) "添加收藏" else "移出收藏"}"
            )
        }

        is PodcastActivityRecord.SubscriptionRecord -> {
            Triple(
                Icons.Outlined.RssFeed,
                "播客订阅：${record.channelTitle}",
                "${formatTimestamp(record.timestamp)} · ${if (record.isSubscribed) "添加订阅" else "取消订阅"}"
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.02f) else Color.White)
            .border(
                BorderStroke(1.dp, if (isDark) Color.Transparent else Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBehind {
                // Left-border purple accent
                drawLine(
                    color = accentColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 4.dp.toPx()
                )
            }
            .padding(start = 14.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = titleText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFFEEF2FF) else Color(0xFF312E81),
                        maxLines = 1
                    )
                }
            }

            Text(
                text = subtitleText,
                fontSize = 9.sp,
                color = RobotTheme.colors.textMuted,
                fontWeight = FontWeight.Bold
            )

            // Progress Bar for active playback records
            if (record is PodcastActivityRecord.PlaybackRecord && !record.isCompleted && record.currentProgressPercent > 0f) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { record.currentProgressPercent / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = accentColor,
                        trackColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color(
                            0xFFEEF2FF
                        )
                    )
                    Text(
                        text = "${record.currentProgressPercent.toInt()}%",
                        fontSize = 8.sp,
                        color = RobotTheme.colors.textMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (record.insight.isNotEmpty()) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = dividerColor.copy(alpha = 0.5f)
                )
                Text(
                    text = "Aether 洞察：\"${record.insight}\"",
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
