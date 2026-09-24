package com.airobot.features.aiserv.notepad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.aiserv.notepad.data.AlarmRecord
import com.airobot.features.aiserv.notepad.data.FocusRecord
import com.airobot.features.aiserv.notepad.data.PodcastActivityRecord
import com.airobot.features.aiserv.notepad.data.TimerRecord
import com.airobot.features.aiserv.notepad.widgets.NotepadAlarmCard
import com.airobot.features.aiserv.notepad.widgets.NotepadFocusCard
import com.airobot.features.aiserv.notepad.widgets.NotepadItem
import com.airobot.features.aiserv.notepad.widgets.NotepadPodcastCard
import com.airobot.features.aiserv.notepad.widgets.NotepadSummaryCard
import com.airobot.features.aiserv.notepad.widgets.NotepadTimerCard
import com.airobot.framework.cards.OverlayBackdrop
import com.airobot.framework.theme.RobotTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * AiNotepadOverlay — Cross-app AI agent service card for viewing Notepad logs/notes.
 * Refactored to coordinate modularized child item card components.
 * Fully customized as a high-fidelity retro skeuomorphic double-spiral bound journal.
 */
@Composable
fun AiNotepadOverlay(
    alarmHistory: List<AlarmRecord>,
    timerHistory: List<TimerRecord>,
    focusHistory: List<FocusRecord>,
    podcastHistory: List<PodcastActivityRecord>,
    onClose: () -> Unit
) {
    val isDark = RobotTheme.isDark
    val paperColor = RobotTheme.colors.logbookPaper
    val notebookBorder = RobotTheme.colors.logbookBorder
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 1. Process real history
    val liveRecords = alarmHistory.map {
        NotepadItem.AlarmCard(
            id = it.id,
            timestamp = it.triggerTime,
            label = it.label,
            time = it.time,
            insight = it.insight
        )
    } + timerHistory.map {
        NotepadItem.TimerCard(
            id = it.id,
            timestamp = it.timestamp,
            label = it.label,
            duration = it.duration,
            insight = it.insight
        )
    } + focusHistory.map {
        NotepadItem.FocusCard(
            id = it.id,
            timestamp = it.startTime,
            task = it.task,
            duration = it.duration,
            targetDuration = it.targetDuration,
            insight = it.insight
        )
    } + podcastHistory.map {
        NotepadItem.PodcastCard(
            id = it.id,
            timestamp = it.timestamp,
            record = it
        )
    }

    val allRecords = remember(alarmHistory, timerHistory, focusHistory, podcastHistory) {
        liveRecords.sortedByDescending { it.timestamp }
    }

    // Helper functions
    val formatTimestamp = remember {
        { ts: Long ->
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(ts))
        }
    }
    val formatSeconds = remember {
        { sec: Int ->
            val m = sec / 60
            val s = sec % 60
            String.format("%02d:%02d", m, s)
        }
    }

    // Lever scrolling synchronization states
    var isDraggingLever by remember { mutableStateOf(false) }
    var leverDragOffset by remember { mutableStateOf(0f) }

    val maxOffsetDp = 106.dp
    val density = LocalDensity.current
    val maxOffsetPx = with(density) { maxOffsetDp.toPx() }

    val listScrollFraction by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) 0f
            else {
                val totalItems = layoutInfo.totalItemsCount
                val firstVisible = visibleItems.first().index
                val lastVisible = visibleItems.last().index
                val visibleCount = lastVisible - firstVisible + 1
                if (totalItems <= visibleCount) 0f
                else firstVisible.toFloat() / (totalItems - visibleCount).toFloat()
            }
        }
    }

    val handleOffsetPx = if (isDraggingLever) {
        leverDragOffset
    } else {
        listScrollFraction * maxOffsetPx
    }

    OverlayBackdrop(
        onClose = onClose,
        enabled = true
    ) {
        // Outer Row to place Notebook Sheet + Right Controls side-by-side
        Row(
            modifier = Modifier
                .wrapContentSize()
                .clickable(enabled = false) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ---- Notebook Page Sheet ----
            Box(
                modifier = Modifier
                    .width(420.dp)
                    .height(580.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 24.dp,
                            bottomStart = 24.dp,
                            topEnd = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(paperColor)
                    .border(
                        BorderStroke(
                            2.dp,
                            Brush.horizontalGradient(listOf(notebookBorder, notebookBorder))
                        ),
                        shape = RoundedCornerShape(
                            topStart = 24.dp,
                            bottomStart = 24.dp,
                            topEnd = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
            ) {
                // Paper lined pattern
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val lineSpacing = 28.dp.toPx()
                            val paintColor =
                                if (isDark) Color.White.copy(alpha = 0.03f) else Color(0xFFF1F5F9)
                            var y = 80.dp.toPx()
                            while (y < size.height) {
                                drawLine(
                                    color = paintColor,
                                    start = Offset(44.dp.toPx(), y),
                                    end = Offset(size.width - 16.dp.toPx(), y),
                                    strokeWidth = 1.dp.toPx()
                                )
                                y += lineSpacing
                            }
                        }
                )

                // Crease/Center Fold shading on the left cover edge next to binding
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(16.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = 32.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = if (isDark) 0.35f else 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Spiral Binding Rings (22 repeat double-bound metal rings on the left margin)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(36.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = (-8).dp)
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(22) {
                        Box {
                            // Shadow behind the ring
                            Box(
                                modifier = Modifier
                                    .size(24.dp, 10.dp)
                                    .offset(x = 1.dp, y = 1.dp)
                                    .border(
                                        BorderStroke(2.5.dp, Color.Black.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                            // The silver metal ring itself
                            Box(
                                modifier = Modifier
                                    .size(24.dp, 10.dp)
                                    .background(
                                        color = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isDark) Color(0xFF64748B) else Color.White
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }

                // Notebook Main Contents
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 48.dp, top = 20.dp, end = 16.dp, bottom = 20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF312E81) else Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NoteAlt,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF818CF8) else Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Aether 记事本",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF1E293B)
                                )
                                Text(
                                    text = "记录你的每次行为，协助你更好地利用时间",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RobotTheme.colors.textMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(thickness = 1.dp, color = dividerColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Diary Entries List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (allRecords.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "这页还是空白的...",
                                        fontSize = 12.sp,
                                        color = RobotTheme.colors.textMuted
                                    )
                                }
                            }
                        }

                        items(allRecords.size) { index ->
                            when (val item = allRecords[index]) {
                                is NotepadItem.AiSummary -> {
                                    NotepadSummaryCard(
                                        item = item,
                                        isDark = isDark,
                                        formatTimestamp = formatTimestamp
                                    )
                                }

                                is NotepadItem.FocusCard -> {
                                    NotepadFocusCard(
                                        item = item,
                                        isDark = isDark,
                                        dividerColor = dividerColor,
                                        formatTimestamp = formatTimestamp
                                    )
                                }

                                is NotepadItem.TimerCard -> {
                                    NotepadTimerCard(
                                        item = item,
                                        isDark = isDark,
                                        dividerColor = dividerColor,
                                        formatTimestamp = formatTimestamp,
                                        formatSeconds = formatSeconds
                                    )
                                }

                                is NotepadItem.AlarmCard -> {
                                    NotepadAlarmCard(
                                        item = item,
                                        isDark = isDark,
                                        dividerColor = dividerColor,
                                        formatTimestamp = formatTimestamp
                                    )
                                }

                                is NotepadItem.PodcastCard -> {
                                    NotepadPodcastCard(
                                        item = item,
                                        isDark = isDark,
                                        dividerColor = dividerColor,
                                        formatTimestamp = formatTimestamp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---- Right Side Control Knobs Column ----
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // 1. Draggable Scroll Lever Track
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(180.dp)
                        .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                if (isDark) listOf(
                                    Color(0xFF334155),
                                    Color(0xFF1E293B),
                                    Color(0xFF334155)
                                )
                                else listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFFE2E8F0))
                            )
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(
                                    alpha = 0.5f
                                )
                            ),
                            shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Track Groove Line
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isDark) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(
                                    alpha = 0.15f
                                )
                            )
                    )

                    // Drag lever handle
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, handleOffsetPx.roundToInt()) }
                            .size(40.dp, 50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    if (isDark) listOf(Color(0xFF64748B), Color(0xFF475569))
                                    else listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0))
                                )
                            )
                            .border(
                                BorderStroke(1.dp, if (isDark) Color(0xFF94A3B8) else Color.White),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .pointerInput(maxOffsetPx) {
                                detectDragGestures(
                                    onDragStart = {
                                        isDraggingLever = true
                                        leverDragOffset = listScrollFraction * maxOffsetPx
                                    },
                                    onDragEnd = {
                                        isDraggingLever = false
                                    },
                                    onDragCancel = {
                                        isDraggingLever = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        leverDragOffset = (leverDragOffset + dragAmount.y).coerceIn(
                                            0f,
                                            maxOffsetPx
                                        )

                                        val totalItems = listState.layoutInfo.totalItemsCount
                                        val visibleItems = listState.layoutInfo.visibleItemsInfo
                                        if (visibleItems.isNotEmpty() && totalItems > 0) {
                                            val firstVisible = visibleItems.first().index
                                            val lastVisible = visibleItems.last().index
                                            val visibleCount = lastVisible - firstVisible + 1
                                            val maxScrollIndex = totalItems - visibleCount
                                            if (maxScrollIndex > 0) {
                                                val fraction = leverDragOffset / maxOffsetPx
                                                val targetIndex =
                                                    (fraction * maxScrollIndex).toInt()
                                                        .coerceIn(0, maxScrollIndex)
                                                coroutineScope.launch {
                                                    listState.scrollToItem(targetIndex, 0)
                                                }
                                            }
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // 3 horizontal metallic finger ridges
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp, 2.dp)
                                        .background(
                                            if (isDark) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(
                                                alpha = 0.15f
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                // 2. Physical Exit/Minimize Tab Lever Button
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(90.dp)
                        .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF3730A3), Color(0xFF1E1B4B))
                                else listOf(Color(0xFFEEF2FF), Color(0xFFC7D2FE))
                            )
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(
                                    alpha = 0.5f
                                )
                            ),
                            shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                        )
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // A small vertical grip line on the left side
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.dp)
                                .background(
                                    if (isDark) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(
                                        alpha = 0.15f
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
