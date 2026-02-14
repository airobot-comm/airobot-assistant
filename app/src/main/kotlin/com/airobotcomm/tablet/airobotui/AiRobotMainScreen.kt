package com.airobotcomm.tablet.airobotui

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
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
import com.airobotcomm.tablet.R
import com.airobotcomm.tablet.airobotui.framework.comp.BackgroundDecorations
import com.airobotcomm.tablet.airobotui.framework.comp.BottomFooter
import com.airobotcomm.tablet.airobotui.framework.subpage.AiRobotDialog
import com.airobotcomm.tablet.airobotui.robotcomp.dialogue.DialogueBubble
import com.airobotcomm.tablet.airobotui.robotcomp.robot.*
import com.airobotcomm.tablet.airobotui.robotcomp.voice.RobotVoiceInputPanel
import com.airobotcomm.tablet.airobotui.framework.statusbar.RobotTopBar
import com.airobotcomm.tablet.airobotui.framework.drawer.RobotDrawerContent
import com.airobotcomm.tablet.service.compoments.DEFAULT_SERVICE_CARDS
import com.airobotcomm.tablet.service.FocusTimerWidget
import com.airobotcomm.tablet.service.compoments.ServiceCardCarousel
import com.airobotcomm.tablet.service.compoments.getServiceCardIcon
import com.airobotcomm.tablet.airobotui.state.ConversationSubState
import com.airobotcomm.tablet.airobotui.state.InteractionType
import com.airobotcomm.tablet.airobotui.state.RobotState
import com.airobotcomm.tablet.airobotui.state.RobotUiState
import com.airobotcomm.tablet.airobotui.state.RobotVisualState
import com.airobotcomm.tablet.airobotui.state.ServiceCard
import com.airobotcomm.tablet.airobotui.state.ServiceCardType
import com.airobotcomm.tablet.airobotui.state.ServiceSubState
import com.airobotcomm.tablet.airobotui.state.TimerCommand
import com.airobotcomm.tablet.airobotui.state.TimerStatus
import com.airobotcomm.tablet.airobotui.viewmodel.RobotMainViewModel
import com.airobotcomm.tablet.airobotui.viewmodel.ConversationViewModel
import com.airobotcomm.tablet.airobotui.viewmodel.ServiceViewModel
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
    val audioLevel = if (robotState is RobotState.Conversation) convAudioLevel else mainVoiceLevel

    // 从 ServiceViewModel 收集功能状态
    val activeCard by serviceViewModel.activeCard.collectAsState()
    val timerCommand by serviceViewModel.timerCommand.collectAsState()
    val timerStatus by serviceViewModel.timerStatus.collectAsState()
    
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
        timerCommand, 
        timerStatus,
        currentCard // currentCard 随 currentCardIndex 变化
    ) {
        val visualState = when (val s = robotState) {
            is RobotState.Offline -> RobotVisualState.SLEEPING
            is RobotState.Initializing -> RobotVisualState.THINKING
            is RobotState.Connecting -> RobotVisualState.THINKING
            is RobotState.Unauthorized -> RobotVisualState.IDLE
            is RobotState.Ready -> RobotVisualState.IDLE
            is RobotState.Conversation -> when (s.subState) {
                ConversationSubState.LISTENING -> RobotVisualState.LISTENING
                ConversationSubState.THINKING -> RobotVisualState.THINKING
                ConversationSubState.SPEAKING -> RobotVisualState.SPEAKING
            }
            is RobotState.FunctionService -> when (s.subState) {
                ServiceSubState.IDLE -> RobotVisualState.IDLE
                ServiceSubState.RUNNING -> RobotVisualState.FOCUS
                ServiceSubState.PAUSED -> RobotVisualState.IDLE
            }
        }

        val newStatusTip = when (timerStatus) {
            TimerStatus.RUNNING -> "正在专注: ${timerCommand?.task ?: "未知任务"}..."
            TimerStatus.PAUSED -> "已暂停，休息一下..."
            else -> currentCard.statusTip // 跟随轮播卡片变化
        }

        robotUiState = robotUiState.copy(
            visualState = visualState,
            isConnected = robotState !is RobotState.Offline,
            currentUserMsg = currentRoundUserText,
            currentAiMsg = currentRoundAiText,
            activeCard = activeCard,
            timerCommand = timerCommand,
            timerStatus = timerStatus,
            interactionType = if (activeCard != null) InteractionType.CARD else InteractionType.CHAT,
            statusTip = newStatusTip
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
                onClose = { scope.launch { drawerState.close() } }
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
                            Color(0xFF0F172A),
                            Color(0xFF020617)
                        )
                    )
                )
        ) {
            BackgroundDecorations()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                RobotTopBar(
                    robotState = robotState,
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

                    // 1. 机器人角色 (偏上布局，可滑动)
                    Box(
                        modifier = Modifier
                            .constrainAs(robotRef) {
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom, margin = 300.dp) // 偏上给语音面板留空间
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                // 动态水平偏置实现滑动
                                horizontalBias = robotHorizontalBias
                                // 垂直偏置，让机器人视觉上处于最佳位置
                                verticalBias = 0.5f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        RobotCharacter(
                            state = robotUiState.visualState,
                            statusTip = if (robotUiState.visualState == RobotVisualState.IDLE ||
                                robotUiState.visualState == RobotVisualState.FOCUS
                            )
                                robotUiState.statusTip else null,
                            audioLevel = { audioLevel }, // 传入音频等级用于微表情
                            headSize = 420.dp
                        )
                    }
                    // 2. 语音输入面板 (底部保持一定距离)
                    Box(
                        modifier = Modifier
                            .constrainAs(voicePanelRef) {
                                bottom.linkTo(parent.bottom, margin = 65.dp)
                                start.linkTo(robotRef.start)
                                end.linkTo(robotRef.end)
                            }
                    ) {
                        RobotVoiceInputPanel(
                            robotState = robotUiState.visualState,
                            isConnected = robotUiState.isConnected,
                            timerStatus = robotUiState.timerStatus,
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
                                    timerStatus = TimerStatus.IDLE,
                                    timerCommand = null,
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
                                timerCommand = robotUiState.timerCommand,
                                timerStatus = robotUiState.timerStatus,
                                onTimerComplete = {
                                    robotUiState = robotUiState.copy(
                                        timerStatus = TimerStatus.IDLE,
                                        visualState = RobotVisualState.IDLE,
                                        timerCommand = null,
                                        activeCard = null,
                                        interactionType = InteractionType.CHAT
                                    )
                                },
                                onClose = {
                                    robotUiState = robotUiState.copy(
                                        visualState = RobotVisualState.IDLE,
                                        interactionType = InteractionType.CHAT,
                                        activeCard = null,
                                        timerStatus = TimerStatus.IDLE,
                                        timerCommand = null
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
    timerCommand: TimerCommand?,
    timerStatus: TimerStatus,
    onTimerComplete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (card == null) return
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.5f))
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
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AETHER SYSTEM MODULE",
                            color = Color(0xFF22D3EE).copy(alpha = 0.6f),
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
                    .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "关闭",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.6f)
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
                    FocusTimerWidget(
                        command = timerCommand,
                        timerStatus = timerStatus,
                        onTimerComplete = onTimerComplete
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = getServiceCardIcon(card.type)),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.White.copy(alpha = 0.2f)
                            )
                            Text(
                                text = "${card.type.name} 功能开发中",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 14.sp,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}