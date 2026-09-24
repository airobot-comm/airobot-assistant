package com.airobot.features.clock.cards


/**
 * Clock Home Card — main time display page with skeuomorphic clock face.
 *
 * Prototype ref: HomeMenu.tsx (time/home mode)
 *
 * Layout:
 *   - Centered SkeuomorphicClockFace (takes most space)
 *   - AetherRemindCard at bottom with dynamic multi-page recommendations
 */
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airobot.features.R
import com.airobot.features.aiserv.guidance.components.AetherRemindBanner
import com.airobot.features.FeatureCards
import com.airobot.features.aiserv.popup.PopupServiceItem
import com.airobot.features.clock.cards.home.SkeuomorphicClockFace
import com.airobot.framework.cards.ModuleServiceCard
import com.airobot.framework.theme.RobotTheme

@Composable
fun ClockHomeCard(
    modifier: Modifier = Modifier,
    queueItems: List<PopupServiceItem> = emptyList(),
    clockViewModel: com.airobot.features.clock.viewmodel.ClockViewModel = hiltViewModel(),
    onAlarmClick: () -> Unit = {},
    onFocusClick: () -> Unit = {},
    onTimerClick: () -> Unit = {},
    onRemindClick: (String) -> Unit = {},
    onChimeClick: () -> Unit = {}
) {
    val isDark = RobotTheme.isDark
    val chimeConfig by clockViewModel.chimeConfig.collectAsState()
    val remindCards by clockViewModel.remindCards.collectAsState(initial = emptyList())

    ModuleServiceCard(
        title = stringResource(R.string.clock_title),
        subtitle = stringResource(R.string.clock_subtitle),
        icon = Icons.Outlined.AccessTime,
        iconColor = if (isDark) Color(0xFF22D3EE) else Color(0xFF0891B2),
        iconBgColor = if (isDark) Color(0xFF22D3EE).copy(alpha = 0.20f) else Color(0xFFCFFAFE),
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween,
        showSubtitle = false
    ) {
        // Centered clock face (Moved up slightly with negative top padding or alignment)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .offset(y = (-40).dp),
            contentAlignment = Alignment.Center
        ) {
            val clockSize = minOf(maxWidth, maxHeight) * 1.06f
            SkeuomorphicClockFace(
                size = clockSize,
                queueItems = queueItems,
                hourlyChimeEnabled = chimeConfig.enabled,
                onAlarmClick = onAlarmClick,
                onFocusClick = onFocusClick,
                onTimerClick = onTimerClick,
                onChimeClick = onChimeClick
            )
        }

        // Bottom Aether dynamic remind banner
        AetherRemindBanner(
            pages = remindCards,
            cardIcon = Icons.Outlined.AccessTime,
            accentColor = if (isDark) Color(0xFF22D3EE) else Color(0xFF0891B2),
            onPageClick = { page ->
                onRemindClick(page.actionTarget)
            }
        )
    }
}
