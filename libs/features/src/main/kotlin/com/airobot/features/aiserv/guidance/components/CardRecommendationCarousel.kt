package com.airobot.features.aiserv.guidance.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.aiserv.guidance.data.RecommendCard
import com.airobot.framework.theme.RobotTheme

/**
 * CardRecommendationCarousel — Auto-rotating carousel of recommended cards.
 * Decoupled from App module.
 */
@Composable
fun CardRecommendationCarousel(
    cards: List<RecommendCard>,
    onCardClick: (RecommendCard) -> Unit,
    currentIndex: Int,
    onPageChanged: (Int) -> Unit,
    statusTip: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Status indicator header
        AnimatedVisibility(
            visible = statusTip != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            StatusTipHeader(tip = statusTip ?: "")
        }

        // Carousel container
        if (cards.isNotEmpty()) {
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    slideInVertically { it } + fadeIn() togetherWith
                    slideOutVertically { -it } + fadeOut()
                },
                label = "cardCarousel"
            ) { index ->
                val card = cards.getOrNull(index) ?: cards.first()
                CardRecommendationItem(
                    card = card,
                    onClick = { onCardClick(card) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatusTipHeader(
    tip: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(bottom = 8.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(RobotTheme.colors.accent)
        )

        Text(
            text = tip,
            color = RobotTheme.colors.textPrimary.copy(alpha = 0.9f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * CardRecommendationList — Simple static list of recommended cards (no rotation)
 */
@Composable
fun CardRecommendationList(
    cards: List<RecommendCard>,
    onCardClick: (RecommendCard) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        cards.forEach { card ->
            CardRecommendationItem(
                card = card,
                onClick = { onCardClick(card) },
                showProgress = false
            )
        }
    }
}
