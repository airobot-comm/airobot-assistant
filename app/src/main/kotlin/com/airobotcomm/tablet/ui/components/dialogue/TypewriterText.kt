package com.airobotcomm.tablet.ui.components.dialogue

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 打字机效果文本组件
 * 
 * Web原型对应: VoiceDialoguePanel.tsx 中的 TypewriterText
 * 
 * 功能:
 * - 逐字显示文本，确保与语音同步
 * - 可配置打字速度
 * - 完成回调
 * - 支持动态文本更新
 */
@Composable
fun TypewriterText(
    text: String,
    speed: Long = 50L, // 稍微降低速度，更接近正常语速
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.95f),
        lineHeight = 26.sp
    ),
    onComplete: () -> Unit = {}
) {
    var displayedText by remember { mutableStateOf("") }
    var isComplete by remember { mutableStateOf(false) }
    
    // 使用LaunchedEffect处理文本变化，确保每次文本更新都重新开始打字效果
    LaunchedEffect(text) {
        // 如果新文本以当前显示文本开头，则继续打字，否则重新开始
        if (text.startsWith(displayedText)) {
            // 继续打字
            val startIndex = displayedText.length
            for (i in startIndex until text.length) {
                delay(speed)
                displayedText = text.substring(0, i + 1)
            }
        } else {
            // 重新开始
            displayedText = ""
            isComplete = false
            
            text.forEachIndexed { index, _ ->
                delay(speed)
                displayedText = text.substring(0, index + 1)
            }
        }
        
        isComplete = true
        onComplete()
    }
    
    Text(
        text = displayedText,
        modifier = modifier,
        style = style
    )
}

/**
 * 带光标的打字机效果
 */
@Composable
fun TypewriterTextWithCursor(
    text: String,
    speed: Long = 30L,
    cursorChar: String = "▌",
    showCursorAfterComplete: Boolean = false,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White.copy(alpha = 0.95f),
        lineHeight = 26.sp
    ),
    onComplete: () -> Unit = {}
) {
    var displayedText by remember(text) { mutableStateOf("") }
    var isComplete by remember(text) { mutableStateOf(false) }
    var showCursor by remember { mutableStateOf(true) }
    
    // 光标闪烁
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            showCursor = !showCursor
        }
    }
    
    // 打字效果
    LaunchedEffect(text) {
        displayedText = ""
        isComplete = false
        
        text.forEachIndexed { index, _ ->
            delay(speed)
            displayedText = text.substring(0, index + 1)
        }
        
        isComplete = true
        onComplete()
    }
    
    val finalText = if (!isComplete || showCursorAfterComplete) {
        if (showCursor) displayedText + cursorChar else displayedText + " "
    } else {
        displayedText
    }
    
    Text(
        text = finalText,
        modifier = modifier,
        style = style
    )
}
