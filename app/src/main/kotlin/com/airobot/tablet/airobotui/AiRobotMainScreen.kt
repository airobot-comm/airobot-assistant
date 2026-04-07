package com.airobot.tablet.airobotui

import com.airobot.framework.theme.RobotTheme
import com.airobot.framework.theme.RobotThemeMode

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.airobot.framework.R
import com.airobot.framework.comp.BackgroundDecorations
import com.airobot.framework.comp.BottomFooter
import com.airobot.tablet.airobotui.subpage.AiRobotDialog
import com.airobot.tablet.airobotui.comp.dialogue.DialogueBubble
import com.airobot.tablet.airobotui.comp.robot.*
import com.airobot.tablet.airobotui.comp.voice.RobotVoiceInputPanel
import com.airobot.framework.statusbar.RobotTopBar
import com.airobot.tablet.airobotui.drawer.RobotDrawerContent
import com.airobot.services.compoments.DEFAULT_SERVICE_CARDS
import com.airobot.services.FocusTimerWidget
import com.airobot.services.compoments.ServiceCardCarousel
import com.airobot.services.compoments.getServiceCardIcon
import com.airobot.tablet.airobotui.state.ConversationSubState
import com.airobot.tablet.airobotui.state.InteractionType
import com.airobot.tablet.airobotui.state.RobotEngineState
import com.airobot.tablet.airobotui.state.RobotUiState
import com.airobot.tablet.airobotui.state.RobotVisualState
import com.airobot.services.state.ServiceCard
import com.airobot.services.state.ServiceCardType
import com.airobot.services.state.ServiceCardData
import com.airobot.services.state.TimerCardData
import com.airobot.services.state.ServiceSubState
import com.airobot.tablet.airobotui.viewmodel.RobotMainViewModel
import com.airobot.tablet.airobotui.viewmodel.ConversationViewModel
import com.airobot.services.ServiceViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.animation.core.Spring // ADDED IMPORT

/**
 * 机器人服务主屏幕
 * Web原型对应: App.tsx
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AiRobotMainScreen(
    themeMode: RobotThemeMode = RobotThemeMode.DARK,
    onToggleTheme: () -> Unit = {},
    robotMainViewModel: RobotMainViewModel = hiltViewModel(),
    conversationViewModel: ConversationViewModel = hiltViewModel(),
    serviceViewModel: ServiceViewModel = hiltViewModel()
) {
    // 权限管理
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
    )

    // 从 RobotMainViewModel 收集一级状态
    val robotState by robotMainViewModel.robotState.collectAsState()
    val errorMessage by robotMainViewModel.errorMessage.collectAsState()
    val showActivationDialog by robotMainViewModel.showActivationDialog.collectAsState()
    val activationCode by robotMainViewModel.activationCode.collectAsState()
    val mainVoiceLevel by robotMainViewModel.voiceLevel.collectAsState()

    // 从 ConversationViewModel 收集交互状态
    val convAudioLevel by conversationViewModel.audioLevel.collectAsState()
    val currentRoundUserText by conversationViewModel.currentRoundUserText.collectAsState()
    val currentRoundAiText by conversationViewModel.currentRoundAiText.collectAsState()

    // 组合音量等级：对话时用对话VM的，非对话时用主VM的
    val audioLevel = if (robotState is RobotEngineState.Conversation) convAudioLevel else mainVoiceLevel

    // 从 ServiceViewModel 收集功能状态
    val activeCard by serviceViewModel.activeCard.collectAsState()
    val activeServiceData by serviceViewModel.activeServiceData.collectAsState()
    val serviceSubState by serviceViewModel.serviceSubState.collectAsState()
    
    // 本地UI状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 机器人UI状态
    var robotUiState by remember { mutableStateOf(RobotUiState()) }
    var currentCardIndex by remember { mutableIntStateOf(0) }
    
    // 服务卡片定义
    val serviceCards = DEFAULT_SERVICE_CARDS
    val currentCard = serviceCards.getOrNull(currentCardIndex) ?: serviceCards.first()

    // 自动轮播逻辑
    LaunchedEffect(serviceCards.size, robotUiState.isInteracting) {
        if (serviceCards.isNotEmpty() && !robotUiState.isInteracting) {
            while (true) {
                kotlinx.coroutines.delay(10000L)
                currentCardIndex = (currentCardIndex + 1) % serviceCards.size
            }
        }
    }

    // 机器人UI状态汇总同步 - 整合多个来源，避免竞态覆盖
    LaunchedEffect(
        robotState, 
        currentRoundUserText, 
        currentRoundAiText, 
        activeCard, 
        activeServiceData, 
        serviceSubState,
        currentCard // currentCard 随 currentCardIndex 变化
    ) {
        val visualState = when (val s = robotState) {
            is RobotEngineState.Offline -> RobotVisualState.SLEEPING
            is RobotEngineState.Initializing -> RobotVisualState.THINKING
            is RobotEngineState.Connecting -> RobotVisualState.THINKING
            is RobotEngineState.Unauthorized -> RobotVisualState.IDLE
            is RobotEngineState.Ready -> RobotVisualState.IDLE
            is RobotEngineState.Conversation -> when (s.subState) {
                ConversationSubState.LISTENING -> RobotVisualState.LISTENING
                ConversationSubState.THINKING -> RobotVisualState.THINKING
                ConversationSubState.SPEAKING -> RobotVisualState.SPEAKING
            }
            is RobotEngineState.FunctionService -> when (s.subState) {
                ServiceSubState.IDLE -> RobotVisualState.IDLE
                ServiceSubState.RUNNING -> RobotVisualState.FOCUS
                ServiceSubState.PAUSED -> RobotVisualState.IDLE
                ServiceSubState.COMPLETED -> RobotVisualState.HAPPY
                ServiceSubState.CANCELLED -> RobotVisualState.IDLE
            }
        }

        robotUiState = robotUiState.copy(
            visualState = visualState,
            isConnected = robotState !is RobotEngineState.Offline,
            currentUserMsg = currentRoundUserText,
            currentAiMsg = currentRoundAiText,
            activeCard = activeCard,
            activeServiceData = activeServiceData,
            serviceSubState = serviceSubState,
            interactionType = if (activeCard != null) InteractionType.CARD else InteractionType.CHAT,
            statusTip = currentCard.statusTip
        )
    }

    // 请求权限
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // 初始化音频系统 (当权限获得后)
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            robotMainViewModel.initAudioService()
        }
    }

    // 监听唤醒事件
    LaunchedEffect(Unit) {
        robotMainViewModel.wakeupEvent.collect {
            conversationViewModel.startConversation()
        }
    }

    // 机器人水平位移动画
    // 当 isCardMode 为 true (点击卡片展开) 时，机器人滑向左侧 (bias 0.1f)
    // 否则保持在中间 (bias 0.5f)
    val robotHorizontalBias by animateFloatAsState(
        targetValue = if (robotUiState.isCardMode) 0.04f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "robotSlide"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            RobotDrawerContent(
                onClose = { scope.launch { drawerState.close() } },
                onToggleTheme = onToggleTheme
            )
        },
        gesturesEnabled = drawerState.isOpen
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            RobotTheme.colors.backgroundGradientStart,
                            RobotTheme.colors.backgroundGradientEnd
                        )
                    )
                )
        ) {
            BackgroundDecorations()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    // 绉婚櫎姝ゅ瀵艰埅鏍忓唴杈硅窛锛岃鑳屾櫙璐┛
            ) {
                val stateText = when (robotState) {
                    is RobotEngineState.Offline -> "OFFLINE"
                    is RobotEngineState.Initializing -> "INITIALIZING"
                    is RobotEngineState.Unauthorized -> "UNAUTHORIZED"
                    is RobotEngineState.Connecting -> "CONNECTING"
                    is RobotEngineState.Ready -> "READY"
                    is RobotEngineState.Conversation -> "CONVERSATION"
                    is RobotEngineState.FunctionService -> "SERVICE MODE"
                }

                val stateColor = when (robotState) {
                    is RobotEngineState.Offline -> com.airobot.framework.theme.StatusRed
                    is RobotEngineState.Initializing -> com.airobot.framework.theme.StatusAmber
                    is RobotEngineState.Unauthorized -> com.airobot.framework.theme.StatusRed
                    is RobotEngineState.Connecting -> com.airobot.framework.theme.StatusCyan
                    is RobotEngineState.Ready -> com.airobot.framework.theme.StatusCyan
                    else -> com.airobot.framework.theme.StatusEmerald
                }

                RobotTopBar(
                    stateText = stateText,
                    stateColor = stateColor,
                    errorMessage = errorMessage,
                    onLogoClick = { scope.launch { drawerState.open() } }
                )
                
                // ErrorBanner 迁移到 TopBar 中，此处移除
                
                // 中心内容区域 - 使用 ConstraintLayout 精确控制相对位置
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    val (robotRef, voicePanelRef, aiBubbleRef, serviceCardsRef, activeCardRef) = createRefs()

                    // 1. 机器人角色 (居中偏上)
                    Box(
                        modifier = Modifier
                            .constrainAs(robotRef) {
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom, margin = 200.dp)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                horizontalBias = robotHorizontalBias
                                verticalBias = 0.35f // 继续上移，给下方腾出逻辑空间
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        RobotCharacter(
                            state = robotUiState.visualState,
                            audioLevel = { audioLevel }, // 传入音频等级用于微表情
                            headSize = 400.dp // 420 -> 400 稍微缩小一点点
                        )
                    }
                    // 2. 语音输入面板 (下移 15%，增大底部间距)
                    Box(
                        modifier = Modifier
                            .constrainAs(voicePanelRef) {
                                bottom.linkTo(parent.bottom, margin = 40.dp) // 80 -> 40 显著下移
                                start.linkTo(robotRef.start)
                                end.linkTo(robotRef.end)
                            }
                    ) {
                        RobotVoiceInputPanel(
                            robotState = robotUiState.visualState,
                            isConnected = robotUiState.isConnected,
                            serviceSubState = robotUiState.serviceSubState,
                            userMessage = currentRoundUserText,
                            audioLevel = audioLevel,
                            onStartListening = {
                                if (permissionsState.allPermissionsGranted) {
                                    robotUiState = robotUiState.copy(
                                        interactionType = InteractionType.CHAT,
                                        currentUserMsg = null,
                                        currentAiMsg = null
                                    )
                                    conversationViewModel.startConversation()
                                }
                            },
                            onStopListening = {
                                conversationViewModel.stopAutoConversation()
                            },
                            onTimerControl = { action ->
                                serviceViewModel.handleTimerAction(action)
                            },
                            onCommandClick = { command -> 
                                if (permissionsState.allPermissionsGranted) {
                                    robotUiState = robotUiState.copy(
                                        interactionType = InteractionType.CHAT,
                                        currentUserMsg = command,
                                        currentAiMsg = null
                                    )
                                    conversationViewModel.startConversation()
                                }
                            }
                        )
                    }

                    // 3. AI 对话气泡 (机器人右上角)
                    Box(
                        modifier = Modifier
                            .constrainAs(aiBubbleRef) {
                                start.linkTo(robotRef.end, margin = (-180).dp) // 稍微重叠一点看起来像从机器人发出
                                top.linkTo(robotRef.top, margin = 180.dp)
                            }
                    ) {
                        DialogueBubble(
                            robotState = robotUiState.visualState,
                            aiMsg = currentRoundAiText,
                            onAiSpeechComplete = {
                                // 语音播报完成
                            },
                            onClose = {
                                robotUiState = robotUiState.copy(
                                    visualState = RobotVisualState.IDLE,
                                    currentUserMsg = null,
                                    currentAiMsg = null,
                                    serviceSubState = ServiceSubState.IDLE,
                                    activeServiceData = null,
                                    activeCard = null
                                )
                                conversationViewModel.interrupt()
                            }
                        )
                    }

                    // 4. 右侧功能推荐卡片 (非交互/卡片模式时显示)
                    if (!robotUiState.isInteracting) {
                        Box(
                            modifier = Modifier
                                .constrainAs(serviceCardsRef) {
                                    end.linkTo(parent.end, margin = 64.dp)
                                    top.linkTo(parent.top)
                                    bottom.linkTo(parent.bottom)
                                }
                                .width(260.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            ServiceCardCarousel(
                                cards = serviceCards,
                                currentIndex = currentCardIndex,
                                onPageChanged = { currentCardIndex = it },
                                statusTip = robotUiState.statusTip, // 传递状态提示到卡片区域
                                onCardClick = { card ->
                                    // 确保点击的是当前显示的卡片
                                    val targetCard = if (serviceCards.contains(card)) card else serviceCards[currentCardIndex]
                                    
                                    robotUiState = robotUiState.copy(
                                        interactionType = InteractionType.CARD,
                                        activeCard = targetCard,
                                        visualState = RobotVisualState.LISTENING,
                                        currentUserMsg = null,
                                        currentAiMsg = null
                                    )
                                    serviceViewModel.startService(targetCard)
                                    if (permissionsState.allPermissionsGranted) {
                                        conversationViewModel.startConversation()
                                    }
                                }
                            )
                        }
                    }

                    // 5. 功能卡片详情 (交互/卡片模式时显示)
                    if (robotUiState.isCardMode) {
                        Box(
                            modifier = Modifier
                                .constrainAs(activeCardRef) {
                                    end.linkTo(parent.end, margin = 48.dp)
                                    top.linkTo(parent.top)
                                    bottom.linkTo(parent.bottom)
                                }
                                .width(600.dp) // 更宽的卡片
                        ) {
                            FunctionalModulePanel(
                                card = robotUiState.activeCard,
                                activeServiceData = robotUiState.activeServiceData,
                                serviceSubState = robotUiState.serviceSubState,
                                onTimerComplete = {
                                    robotUiState = robotUiState.copy(
                                        serviceSubState = ServiceSubState.IDLE,
                                        visualState = RobotVisualState.IDLE,
                                        activeServiceData = null,
                                        activeCard = null,
                                        interactionType = InteractionType.CHAT
                                    )
                                },
                                onClose = {
                                    robotUiState = robotUiState.copy(
                                        visualState = RobotVisualState.IDLE,
                                        interactionType = InteractionType.CHAT,
                                        activeCard = null,
                                        serviceSubState = ServiceSubState.IDLE,
                                        activeServiceData = null
                                    )
                                    serviceViewModel.closeService()
                                    conversationViewModel.interrupt()
                                }
                            )
                        }
                    }
                }
            }
            
            // 底部页脚
            BottomFooter(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
            
            // 激活弹窗
            if (showActivationDialog && activationCode != null) {
                AiRobotDialog(
                    activationCode = activationCode!!,
                    onConfirm = { robotMainViewModel.onActivationConfirmed() },
                    onDismiss = { /* Optionally handle dismissal, but usually activation is required */ }
                )
            }
        }
    }
}

@Composable
private fun FunctionalModulePanel(
    card: ServiceCard?,
    activeServiceData: ServiceCardData?,
    serviceSubState: ServiceSubState,
    onTimerComplete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (card == null) return
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(RobotTheme.colors.cardBg.copy(alpha = 0.5f))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = if (card.type == ServiceCardType.TIMER) {
                                        listOf(Color(0xFFEF4444), Color(0xFFF97316))
                                    } else {
                                        listOf(Color(0xFF22D3EE), Color(0xFF6366F1))
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = getServiceCardIcon(card.type)),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                    
                    Column {
                        Text(
                            text = card.title,
                            color = RobotTheme.colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AETHER SYSTEM MODULE",
                            color = RobotTheme.colors.accent.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RobotTheme.colors.surfaceOverlay.copy(alpha = 0.05f))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "关闭",
                        modifier = Modifier.size(16.dp),
                        tint = RobotTheme.colors.textMuted
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Removed text display block
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                ) {
                if (card.type == ServiceCardType.TIMER) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            val duration = (activeServiceData as? TimerCardData)?.duration ?: 0
                            val task = (activeServiceData as? TimerCardData)?.task ?: ""
                            FocusTimerWidget(
                                duration = duration,
                                task = task,
                                serviceSubState = serviceSubState,
                                onTimerComplete = onTimerComplete
                            )
                        }
                        card.demoContent?.let {
                            Text(
                                text = it,
                                color = RobotTheme.colors.textSecondary,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp, start = 32.dp, end = 32.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = getServiceCardIcon(card.type)),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = RobotTheme.colors.textMuted.copy(alpha = 0.3f)
                            )
                            Text(
                                text = card.demoContent ?: "${card.type.name} 功能开发中",
                                color = RobotTheme.colors.textSecondary,
                                fontSize = 18.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

