package com.airobot.character.airobotui.comp.robot

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airobot.character.airobotui.state.RobotVisualState
import com.airobot.framework.theme.RobotAntennaStemDark
import com.airobot.framework.theme.RobotAntennaStemLight
import com.airobot.framework.theme.RobotBlush
import com.airobot.framework.theme.RobotCollarColor
import com.airobot.framework.theme.RobotEyeDefault
import com.airobot.framework.theme.RobotFaceColor
import com.airobot.framework.theme.RobotHeadBorder
import com.airobot.framework.theme.RobotHeadColor
import com.airobot.framework.theme.RobotNeckColor
import com.airobot.framework.theme.RobotTheme


/**
 * 鏈哄櫒浜鸿鑹蹭富缁勪欢 - 澧炲己鐗?
 * 
 * 瀵瑰簲鍘熷瀷: IPCharacter.tsx
 */
@Composable
fun RobotCharacter(
    state: com.airobot.character.airobotui.state.RobotVisualState,
    ttsProgressNormalized: Float = 0f,
    audioLevel: () -> Float = { 0f }, // 浼犲叆闊抽绛夌骇 (Lambda)
    headSize: Dp = 320.dp, // 澧炲ぇ榛樿灏哄浠ュ尮閰?420px 姣斾緥
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "robotAnimation")
    
    // 鎮诞鍔ㄧ敾 (Floating) - 鑼冨洿鍔犲ぇ锛屾洿鐢熷姩
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 18f, // 12 -> 18
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    
    // 鐪ㄧ溂鐘舵€侀€昏緫 (浣跨敤 Random 瀹炵幇闅忔満鎬?
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        while (true) {
            // 闅忔満闂撮殧 2s - 7s
            val delayTime = kotlin.random.Random.nextLong(2000, 7000)
            kotlinx.coroutines.delay(delayTime)
            
            if (state == RobotVisualState.IDLE || state == RobotVisualState.HAPPY || state == RobotVisualState.LISTENING) {
                isBlinking = true
                kotlinx.coroutines.delay(150)
                isBlinking = false
                
                // 鍋跺皵鍙岀湪鐪?(15% 姒傜巼)
                if (kotlin.random.Random.nextFloat() < 0.15f) {
                    kotlinx.coroutines.delay(100)
                    isBlinking = true
                    kotlinx.coroutines.delay(150)
                    isBlinking = false
                }
            }
        }
    }

    Box(
        modifier = modifier
            .width(headSize * 2.0f) 
            .height(headSize * 1.8f),
        contentAlignment = Alignment.Center
    ) {
        // 澧炲己鍨嬭儗鏅幆澧冨厜 (鍙屽眰鍏夋檿 + 鍛煎惛鎰?
        val auraColor = RobotTheme.colors.robotAuraStart
        val auraAlpha = if (RobotTheme.isDark) 0.55f else 0.7f // 鎻愰珮涓嶉€忔槑搴?
        
        val auraScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "auraScale"
        )

        // 鏍稿績鍐呭眰鍏?
        Box(
            modifier = Modifier
                .size(headSize * 1.5f)
                .graphicsLayer { 
                    alpha = auraAlpha
                    scaleX = auraScale
                    scaleY = auraScale
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            auraColor,
                            Color.Transparent
                        )
                    )
                )
                .blur(60.dp)
        )
        
        // 澶栧眰澶у厜鏅?(鎵╂暎鎰?
        Box(
            modifier = Modifier
                .size(headSize * 2.1f)
                .graphicsLayer { 
                    alpha = auraAlpha * 0.5f
                    scaleX = auraScale * 1.3f
                    scaleY = auraScale * 1.3f
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            auraColor.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
                .blur(100.dp)
        )
        
        // 涓讳綋缁撴瀯
        Column(
            modifier = Modifier
                .offset(y = if (state == RobotVisualState.IDLE) floatOffset.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 鐘舵€佹彁绀烘皵娉″凡绉婚櫎锛岃縼绉诲埌鍔熻兘鍗＄墖缁勪欢
            
            // 鏈哄櫒浜哄ご閮?
            RobotHead(
                state = state,
                isBlinking = isBlinking,
                ttsProgressNormalized = ttsProgressNormalized,
                audioLevel = audioLevel,
                headSize = headSize
            )
            
            // 鍦伴潰闃村奖 - 璺熼殢娴姩杞诲井缂╂斁
            Box(
                modifier = Modifier
                    .offset(y = (-10).dp)
                    .width(headSize * 0.6f)
                    .height(20.dp)
                    .graphicsLayer { 
                        alpha = if (state == RobotVisualState.IDLE) 0.2f else 0.4f
                        scaleX = if (state == RobotVisualState.IDLE) 0.85f + (floatOffset / 120f) else 1f
                    }
                    .clip(CircleShape)
                    .background(Color.Black)
                    .blur(20.dp)
            )
            
            // 搴曢儴鑴栧瓙鍜岄瀛?
            RobotNeck(headSize = headSize)
        }
    }
}

/**
 * 鏈哄櫒浜哄ご閮ㄧ粍浠?
 */
@Composable
private fun RobotHead(
    state: com.airobot.character.airobotui.state.RobotVisualState,
    isBlinking: Boolean,
    ttsProgressNormalized: Float,
    audioLevel: () -> Float,
    headSize: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "headAnimation")
    
    Box(
        modifier = modifier
            .width(headSize * 1.1f) // 鎵╁瀹瑰櫒锛屽绾崇獊鍑虹殑鑰虫湹
            .height(headSize * 0.74f),
        contentAlignment = Alignment.Center
    ) {
        // 澶╃嚎 (鏀剧疆鍦ㄥご閮ㄥ悗闈?
        RobotAntennas(
            state = state,
            headSize = headSize,
            infiniteTransition = infiniteTransition
        )
        
        // 鑰虫湹 (绉诲嚭澶撮儴瑁佸壀 Box锛屽苟娣诲姞 3D 杞粨灞?
        Box(
            modifier = Modifier.width(headSize * 1.08f),
            contentAlignment = Alignment.Center
        ) {
            // 宸﹁€?(涓ゅ眰鍙犲姞瀹炵幇绐佽捣鐨勮竟妗嗘劅)
            Box(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                // 澶栬竟妗?鑳屾澘
                Box(
                    modifier = Modifier
                        .size(width = headSize * 0.08f, height = headSize * 0.19f)
                        .clip(RoundedCornerShape(topStart = headSize * 0.1f, bottomStart = headSize * 0.1f))
                        .background(RobotHeadBorder.copy(alpha = 0.4f))
                        .blur(0.5.dp)
                )
                // 鍐呬富浣?
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp, top = 2.dp, bottom = 2.dp)
                        .size(width = headSize * 0.065f, height = headSize * 0.165f)
                        .clip(RoundedCornerShape(topStart = headSize * 0.08f, bottomStart = headSize * 0.08f))
                        .background(RobotHeadColor)
                )
            }
            
            // 鍙宠€?
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                // 澶栬竟妗?鑳屾澘
                Box(
                    modifier = Modifier
                        .size(width = headSize * 0.08f, height = headSize * 0.19f)
                        .clip(RoundedCornerShape(topEnd = headSize * 0.1f, bottomEnd = headSize * 0.1f))
                        .background(RobotHeadBorder.copy(alpha = 0.4f))
                        .blur(0.5.dp)
                )
                // 鍐呬富浣?
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp, top = 2.dp, bottom = 2.dp)
                        .size(width = headSize * 0.065f, height = headSize * 0.165f)
                        .clip(RoundedCornerShape(topEnd = headSize * 0.08f, bottomEnd = headSize * 0.08f))
                        .background(RobotHeadColor)
                )
            }
        }
        
        // 澶撮儴澶栧３ - 鏀逛负娴呰摑鑹?(sky-200)
        Box(
            modifier = Modifier
                .width(headSize)
                .height(headSize * 0.74f)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(headSize * 0.28f),
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                    spotColor = RobotTheme.colors.robotAuraStart.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(headSize * 0.28f))
                .background(RobotHeadColor) // 鍥哄畾娴呰摑鑹?
        ) {

            // 楂樺厜 (Glossy effect)
            Box(
                modifier = Modifier
                    .padding(top = headSize * 0.05f, start = headSize * 0.15f)
                    .size(width = headSize * 0.3f, height = headSize * 0.08f)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
                    .blur(2.dp)
            )

            // 鍐呴儴鏄剧ず灞忓尯鍩?(Inset Face)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(headSize * 0.085f) // 鍖归厤 inset-9 姣斾緥
                    .clip(RoundedCornerShape(headSize * 0.24f))
                    .background(RobotFaceColor) // 鍥哄畾娴呰壊闈㈤儴
                    .border(
                        width = 1.dp,
                        color = Color(0xFF7DD3FC).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(headSize * 0.24f)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = headSize * 0.12f)
                        .offset(y = headSize * 0.1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(headSize * 0.08f, headSize * 0.04f).blur(6.dp).background(
                        RobotBlush.copy(alpha = 0.35f), CircleShape))
                    Box(modifier = Modifier.size(headSize * 0.08f, headSize * 0.04f).blur(6.dp).background(
                        RobotBlush.copy(alpha = 0.35f), CircleShape))
                }

                // 鐪肩潧鍜屽槾宸?
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                if (isBlinking) {
                    // 鐪ㄧ溂鍔ㄧ敾 (淇濇寔闀挎き鍦?
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(headSize * 0.2f)
                    ) {
                        BlinkingEye(size = headSize * 0.17f)
                        BlinkingEye(size = headSize * 0.17f)
                    }
                } else {
                    DynamicEyes(
                        state = state,
                        ttsProgressNormalized = ttsProgressNormalized,
                        audioLevel = audioLevel,
                        eyeSize = headSize * 0.17f,
                        eyeGap = headSize * 0.2f
                    )
                
                // 鍢村反鍔ㄧ敾
                val isSpeaking = state == com.airobot.character.airobotui.state.RobotVisualState.SPEAKING
                val isIdle = state == com.airobot.character.airobotui.state.RobotVisualState.IDLE || state == com.airobot.character.airobotui.state.RobotVisualState.LISTENING

                Box(modifier = Modifier.padding(top = headSize * 0.1f)) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = state,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "mouthTransition"
                    ) { targetState ->
                        when (targetState) {
                            com.airobot.character.airobotui.state.RobotVisualState.SPEAKING -> {
                                SpeakingMouth(
                                    infiniteTransition = infiniteTransition,
                                    modifier = Modifier
                                )
                            }
                            com.airobot.character.airobotui.state.RobotVisualState.IDLE, 
                            com.airobot.character.airobotui.state.RobotVisualState.LISTENING -> {
                                StaticMouth(size = 32.dp)
                            }
                            else -> {
                                // Other states might hide mouth or use static
                                Spacer(modifier = Modifier.size(1.dp))
                            }
                        }
                    }
                }
            }
                }
            }
        }
    }
}

/**
 * 鏈哄櫒浜哄ぉ绾?- 澧炲己鍙戝厜鏁堟灉
 */
@Composable
private fun RobotAntennas(
    state: com.airobot.character.airobotui.state.RobotVisualState,
    headSize: Dp,
    infiniteTransition: InfiniteTransition,
    modifier: Modifier = Modifier
) {
    // 鏃嬭浆鍔ㄧ敾 - 鍔犲ぇ瑙掑害
    val antennaRotation by infiniteTransition.animateFloat(
        initialValue = -12f, // -5 -> -12
        targetValue = 12f,   // 5 -> 12
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine), // 3000 -> 2000 speed up
            repeatMode = RepeatMode.Reverse
        ),
        label = "antennaRotation"
    )

    val isFocus = state == com.airobot.character.airobotui.state.RobotVisualState.FOCUS
    val antennaColor1 = if (isFocus) Color(0xFFF87171) else Color(0xFF7DD3FC) // sky-300 for neutral
    val antennaColor2 = if (isFocus) Color(0xFFF87171) else Color(0xFF7DD3FC) // same for neutral as per prototype

    Row(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = (-headSize * 0.35f)), // 鍚戜笂绉诲姩鏇村锛屽洜涓哄ぉ绾垮彉闀夸簡
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 宸﹀ぉ绾?
        AntennaItem(color = antennaColor1, rotation = antennaRotation, headSize = headSize)
        // 鍙冲ぉ绾?
        AntennaItem(color = antennaColor2, rotation = -antennaRotation, headSize = headSize)
    }
}

@Composable
private fun AntennaItem(color: Color, rotation: Float, headSize: Dp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { rotationZ = rotation }
    ) {
        // 鐏ご + 鍙戝厜 (淇濇寔涓嶅彉)
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
                    .blur(10.dp)
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .shadow(elevation = 8.dp, shape = CircleShape, clip = false, spotColor = color)
            )
        }
        // 澶╃嚎鏉?- 娓愬彉鐏拌壊
        Box(
            modifier = Modifier
                .width(headSize * 0.025f) // 绾?8-10dp
                .height(headSize * 0.35f)
                .clip(RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(RobotAntennaStemLight, RobotAntennaStemDark)
                    )
                )
        )
    }
}

/**
 * 璇磋瘽鍢村反鍔ㄧ敾
 */
@Composable
private fun SpeakingMouth(
    infiniteTransition: InfiniteTransition,
    modifier: Modifier = Modifier
) {
    val mouthScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouthScale"
    )
    
    Box(
        modifier = modifier
            .width(32.dp * mouthScale)
            .height(10.dp) // 绋嶅井鍔犲帤涓€鐐?
            .clip(CircleShape)
            .background(Color(0xFF334155)) // 鏀逛负娣辩伆鑹诧紝鍖归厤鍘熷瀷椋庢牸 (slate-700)
            .blur(0.3.dp)
    )
}

/**
 * 闈欐€佸槾宸?(IDLE 鐘舵€?
 */
@Composable
private fun StaticMouth(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(size)
            .height(5.dp)
            .clip(CircleShape)
            .background(Color(0xFF334155).copy(alpha = 0.8f)) // 娣辩伆鑹叉í绾?
    )
}

// StatusTipBubble宸插垹闄わ紝杩佺Щ鍒板姛鑳藉崱鐗囩粍浠?

/**
 * 鐪ㄧ溂鏃剁殑鐪肩潧褰㈢姸
 */
@Composable
private fun BlinkingEye(size: Dp) {
    Box(
        modifier = Modifier
            .width(size * 1.2f)
            .height(size * 0.15f)
            .clip(RoundedCornerShape(50))
            .background(RobotEyeDefault.copy(alpha = 0.7f)) // 浣跨敤鍥哄畾鐨勭溂鐫涙繁鑹?
    )
}

/**
 * 鑴栧瓙鍜岄瀛愮粍浠?
 */
@Composable
private fun RobotNeck(
    headSize: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.offset(y = (-15).dp), // 鍚戜笂绋嶅井缂╄繘锛屾樉寰楄剸瀛愭洿鐭洿绋冲浐
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 鑴栧瓙绔嬬 - 閰嶅悎鏂伴鑹?
        Box(
            modifier = Modifier
                .width(headSize * 0.14f)
                .height(headSize * 0.08f) // 缂╃煭
                .background(RobotNeckColor)
        )
        // 棰嗗瓙/搴曞骇 - 鏋侀珮鍦嗚妯′豢鍘熷瀷绮樺湡鎰?
        Box(
            modifier = Modifier
                .width(headSize * 0.55f) // 鍔犲
                .height(headSize * 0.14f)
                .clip(RoundedCornerShape(topStart = 100.dp, topEnd = 100.dp)) // 瀹岀編鍗婂渾椤?
                .background(RobotCollarColor)
        )
    }
}


