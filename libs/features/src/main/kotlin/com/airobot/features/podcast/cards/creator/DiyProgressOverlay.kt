package com.airobot.features.podcast.cards.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.PodcastFeaturedBg
import com.airobot.framework.theme.RobotTheme

/**
 * Full-card overlay showing AI podcast generation progress.
 */
@Composable
fun DiyProgressOverlay(
    genProgress: Int,
    generationStep: String,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark
    val cardBgColor = if (isDark) Color(0xFF0F172A) else Color.White
    val cardBorderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val textColor = RobotTheme.colors.textPrimary
    val textMutedColor = RobotTheme.colors.textMuted

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(cardBgColor.copy(alpha = 0.95f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = PodcastFeaturedBg,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "AI 智能体专属创作中...",
                color = PodcastFeaturedBg,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$genProgress%",
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { genProgress / 100f },
                color = PodcastFeaturedBg,
                trackColor = cardBorderColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = generationStep,
                color = textMutedColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
