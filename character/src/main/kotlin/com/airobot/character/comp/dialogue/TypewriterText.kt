package com.airobot.character.comp.dialogue

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme
import kotlinx.coroutines.delay

/**
 * 鎵撳瓧鏈烘晥鏋滄枃鏈粍浠?
 * 
 * Web鍘熷瀷瀵瑰簲: VoiceDialoguePanel.tsx 涓殑 TypewriterText
 * 
 * 鍔熻兘:
 * - 閫愬瓧鏄剧ず鏂囨湰锛岀‘淇濅笌璇煶鍚屾
 * - 鍙厤缃墦瀛楅€熷害
 * - 瀹屾垚鍥炶皟
 * - 鏀寔鍔ㄦ€佹枃鏈洿鏂?
 */
@Composable
fun TypewriterText(
    text: String,
    speed: Long = 50L, // 绋嶅井闄嶄綆閫熷害锛屾洿鎺ヨ繎姝ｅ父璇€?
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = RobotTheme.colors.textPrimary,
        lineHeight = 26.sp
    ),
    onComplete: () -> Unit = {}
) {
    var displayedText by remember { mutableStateOf("") }
    var isComplete by remember { mutableStateOf(false) }
    
    // 浣跨敤LaunchedEffect澶勭悊鏂囨湰鍙樺寲锛岀‘淇濇瘡娆℃枃鏈洿鏂伴兘閲嶆柊寮€濮嬫墦瀛楁晥鏋?
    LaunchedEffect(text) {
        // 濡傛灉鏂版枃鏈互褰撳墠鏄剧ず鏂囨湰寮€澶达紝鍒欑户缁墦瀛楋紝鍚﹀垯閲嶆柊寮€濮?
        if (text.startsWith(displayedText)) {
            // 缁х画鎵撳瓧
            val startIndex = displayedText.length
            for (i in startIndex until text.length) {
                delay(speed)
                displayedText = text.substring(0, i + 1)
            }
        } else {
            // 閲嶆柊寮€濮?
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
 * 甯﹀厜鏍囩殑鎵撳瓧鏈烘晥鏋?
 */
@Composable
fun TypewriterTextWithCursor(
    text: String,
    speed: Long = 30L,
    cursorChar: String = "█",
    showCursorAfterComplete: Boolean = false,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = RobotTheme.colors.textPrimary,
        lineHeight = 26.sp
    ),
    onComplete: () -> Unit = {}
) {
    var displayedText by remember(text) { mutableStateOf("") }
    var isComplete by remember(text) { mutableStateOf(false) }
    var showCursor by remember { mutableStateOf(true) }
    
    // 鍏夋爣闂儊
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            showCursor = !showCursor
        }
    }
    
    // 鎵撳瓧鏁堟灉
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


