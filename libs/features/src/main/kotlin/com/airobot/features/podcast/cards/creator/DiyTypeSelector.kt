package com.airobot.features.podcast.cards.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.PodcastFeaturedBg
import com.airobot.framework.theme.RobotTheme

/**
 * Composable for selecting the DIY Podcast generation type.
 *
 * Order: Audio → Video → Text (图文 disabled, moved last).
 * Audio and video are clickable; text shows "暂不支持" and does not change type.
 */
@Composable
fun DiyTypeSelector(
    diyType: String,
    onTypeSelected: (String) -> Unit,
    isGenerating: Boolean,
    onTextTypeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark
    val textColor = RobotTheme.colors.textPrimary
    val textMutedColor = RobotTheme.colors.textMuted
    val inputBgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
    val cardBorderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "1. 选择节目类型",
            color = textMutedColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Audio and Video are active; Text (图文) is disabled and last
            val types = listOf(
                Triple("audio", "🎙️ 音频", "单播说书朗读"),
                Triple("video", "🎥 视频", "章节视频合成"),
                Triple("text", "📝 图文", "暂不支持")
            )

            types.forEach { (typeVal, label, desc) ->
                val isDisabled = typeVal == "text"
                val selected = diyType == typeVal && !isDisabled
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .alpha(if (isDisabled) 0.4f else 1f)
                        .background(
                            if (selected) PodcastFeaturedBg.copy(alpha = 0.12f)
                            else inputBgColor
                        )
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) PodcastFeaturedBg else cardBorderColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = !isGenerating) {
                            if (isDisabled) {
                                onTextTypeClick?.invoke()
                            } else {
                                onTypeSelected(typeVal)
                            }
                        }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selected) PodcastFeaturedBg
                               else if (isDisabled) textMutedColor
                               else textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = desc,
                        color = textMutedColor,
                        fontSize = 8.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
