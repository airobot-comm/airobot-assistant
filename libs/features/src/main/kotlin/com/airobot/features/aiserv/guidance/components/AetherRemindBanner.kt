package com.airobot.features.aiserv.guidance.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.airobot.features.aiserv.guidance.data.RemindCard

/**
 * Aether Remind Banner — a dynamic internal multi-page recommendation component.
 * Encapsulated within the features module under open-closed principles.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AetherRemindBanner(
    pages: List<RemindCard>,
    cardIcon: ImageVector,
    accentColor: Color,
    onPageClick: (RemindCard) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    // Auto scroll every 10 seconds (10000ms)
    LaunchedEffect(pagerState, pages.size) {
        if (pages.size > 1) {
            while (true) {
                delay(10000L)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % pages.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }

    val cardBorder = RobotTheme.colors.cardBorder
    val surfaceOverlay = RobotTheme.colors.surfaceOverlay
    val textPrimary = RobotTheme.colors.textPrimary
    val textSecondary = RobotTheme.colors.textSecondary
    val textMuted = RobotTheme.colors.textMuted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-8).dp)
            .clip(RoundedCornerShape(40.dp))
            .background(surfaceOverlay.copy(alpha = 0.03f))
            .border(
                width = 1.dp,
                color = cardBorder.copy(alpha = 0.40f),
                shape = RoundedCornerShape(40.dp)
            )
            .clickable {
                if (pages.isNotEmpty() && pagerState.currentPage < pages.size) {
                    onPageClick(pages[pagerState.currentPage])
                }
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left brand icon container showing parent card's logo/icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = cardIcon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = accentColor
            )
        }

        // Center pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            if (pageIndex < pages.size) {
                val page = pages[pageIndex]

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Header label: "AETHER 提醒 · Title ✨"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "AETHER 提醒 · ${page.title}",
                            color = textPrimary.copy(alpha = 0.40f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "✨",
                            fontSize = 11.sp
                        )
                    }

                    // Content with bracket parser using parent card's accent color
                    val parsedContent = parseRemindContent(page.content)
                    Text(
                        text = parsedContent,
                        color = textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right side indicators and next button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.width(36.dp)
        ) {
            // Next arrow button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(surfaceOverlay.copy(alpha = 0.10f))
                    .clickable {
                        if (pages.size > 1) {
                            coroutineScope.launch {
                                val nextPage = (pagerState.currentPage + 1) % pages.size
                                pagerState.animateScrollToPage(nextPage)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Page",
                    modifier = Modifier.size(16.dp),
                    tint = accentColor
                )
            }

            // Vertical indicator dots
            if (pages.size > 1) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    pages.indices.forEach { index ->
                        val isSelected = pagerState.currentPage == index
                        val dotColor = if (isSelected) {
                            accentColor
                        } else {
                            textMuted.copy(alpha = 0.40f)
                        }
                        val dotHeight = if (isSelected) 10.dp else 4.dp
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(dotHeight)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Parses bracketed links like "[ai记事本]" into highlighted, underlined annotated text.
 */
private fun parseRemindContent(text: String): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        while (currentIndex < text.length) {
            val startBracket = text.indexOf('[', currentIndex)
            if (startBracket == -1) {
                append(text.substring(currentIndex))
                break
            }
            append(text.substring(currentIndex, startBracket))
            val endBracket = text.indexOf(']', startBracket)
            if (endBracket == -1) {
                append(text.substring(startBracket))
                break
            }
            val linkText = text.substring(startBracket + 1, endBracket)
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Black
                )
            ) {
                append(linkText)
            }
            currentIndex = endBracket + 1
        }
    }
}
