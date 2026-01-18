package com.airobotcomm.tablet.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.BatteryManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.airobotcomm.tablet.ui.theme.RobotPrimaryCyan
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.airobotcomm.tablet.R
import com.airobotcomm.tablet.ui.components.dialogue.DialogueBubble
import com.airobotcomm.tablet.ui.components.dialogue.TypewriterText
import com.airobotcomm.tablet.ui.components.dialogue.UserMessageBubble
import com.airobotcomm.tablet.ui.components.robot.*
import com.airobotcomm.tablet.ui.components.service.*
import com.airobotcomm.tablet.ui.components.voice.RobotVoiceInputPanel
import com.airobotcomm.tablet.ui.framework.RobotTopBar
import com.airobotcomm.tablet.ui.framework.RobotDrawerContent
import com.airobotcomm.tablet.ui.theme.RobotBackgroundDark
import com.airobotcomm.tablet.ui.theme.RobotSurface
import com.airobotcomm.tablet.ui.theme.RobotSecondaryIndigo
import com.airobotcomm.tablet.ui.theme.RobotTextPrimary
import com.airobotcomm.tablet.ui.state.ConversationSubState
import com.airobotcomm.tablet.ui.state.InteractionType
import com.airobotcomm.tablet.ui.state.RobotState
import com.airobotcomm.tablet.ui.state.RobotUiState
import com.airobotcomm.tablet.ui.state.RobotVisualState
import com.airobotcomm.tablet.ui.state.ServiceSubState
import com.airobotcomm.tablet.ui.state.TimerStatus
import com.airobotcomm.tablet.ui.viewmodel.MainViewModel
import com.airobotcomm.tablet.ui.viewmodel.ConversationViewModel
import com.airobotcomm.tablet.ui.viewmodel.ServiceViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * 机器人对话主屏幕
 * 
 * Web原型对应: App.tsx
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RobotConversationScreen(
    mainViewModel: MainViewModel = hiltViewModel(),
    conversationViewModel: ConversationViewModel = hiltViewModel(),
    serviceViewModel: ServiceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // 权限管理
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
    )
    
    // 从ViewModel收集状态
    // 从 MainViewModel 收集一级状态
    val robotState by mainViewModel.robotState.collectAsState()
    val errorMessage by mainViewModel.errorMessage.collectAsState()
    val showActivationDialog by mainViewModel.showActivationDialog.collectAsState()
    val activationCode by mainViewModel.activationCode.collectAsState()

    // 从 ConversationViewModel 收集交互状态
    val conversationSubState by conversationViewModel.subState.collectAsState()
    val audioLevel by conversationViewModel.audioLevel.collectAsState()
    val currentRoundUserText by conversationViewModel.currentRoundUserText.collectAsState()
    val currentRoundAiText by conversationViewModel.currentRoundAiText.collectAsState()

    // 从 ServiceViewModel 收集功能状态
    val activeCard by serviceViewModel.activeCard.collectAsState()
    val serviceSubState by serviceViewModel.serviceSubState.collectAsState()
    val timerCommand by serviceViewModel.timerCommand.collectAsState()
    val timerStatus by serviceViewModel.timerStatus.collectAsState()
    
    // 本地UI状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 注意：我们将在此处使用一个简单的配置占位或通过 ViewModel 获取
    var currentConfig by remember { mutableStateOf(com.airobotcomm.tablet.data.DeviceConfig.createDefault()) }
    
    // 机器人UI状态
    var robotUiState by remember { mutableStateOf(RobotUiState()) }
    var currentCardIndex by remember { mutableIntStateOf(0) }
    
    // 同步到 UI 状态
    LaunchedEffect(robotState) {
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
        robotUiState = robotUiState.copy(
            visualState = visualState,
            isConnected = robotState !is RobotState.Offline
        )
    }

    LaunchedEffect(currentRoundUserText, currentRoundAiText) {
        robotUiState = robotUiState.copy(
            currentUserMsg = currentRoundUserText,
            currentAiMsg = currentRoundAiText
        )
    }

    LaunchedEffect(activeCard, timerCommand, timerStatus) {
        robotUiState = robotUiState.copy(
            activeCard = activeCard,
            timerCommand = timerCommand,
            timerStatus = timerStatus,
            interactionType = if (activeCard != null) InteractionType.CARD else InteractionType.CHAT
        )
    }
    
    // 请求权限
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }
    
    // 服务卡片
    val serviceCards = DEFAULT_SERVICE_CARDS
    val currentCard = serviceCards.getOrNull(currentCardIndex) ?: serviceCards.first()
    
    // 更新状态提示
    LaunchedEffect(currentCard, robotUiState.timerStatus, robotUiState.timerCommand) {
        val newStatusTip = when (robotUiState.timerStatus) {
            TimerStatus.RUNNING -> "正在专注: ${robotUiState.timerCommand?.task ?: "未知任务"}..."
            com.airobotcomm.tablet.ui.state.TimerStatus.PAUSED -> "已暂停，休息一下..."
            else -> currentCard.statusTip
        }
        robotUiState = robotUiState.copy(statusTip = newStatusTip)
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            RobotDrawerContent(
                currentConfig = currentConfig,
                onConfigChange = { newConfig ->
                    currentConfig = newConfig
                    conversationViewModel.updateConfig(newConfig)
                    scope.launch { drawerState.close() }
                },
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
                    val (robotRef, voicePanelRef, aiBubbleRef, userBubbleRef, serviceCardsRef) = createRefs()

                    // 1. 机器人角色 (偏上布局)
                    Box(
                        modifier = Modifier
                            .constrainAs(robotRef) {
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom, margin = 300.dp) // 偏上给语音面板留空间
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                // 垂直偏置，让机器人视觉上处于最佳位置
                                verticalBias = 0.5f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        RobotCharacter(
                            state = robotUiState.visualState,
                            statusTip = if (robotUiState.visualState == com.airobotcomm.tablet.ui.state.RobotVisualState.IDLE ||
                                robotUiState.visualState == com.airobotcomm.tablet.ui.state.RobotVisualState.FOCUS
                            )
                                robotUiState.statusTip else null,
                            audioLevel = audioLevel, // 传入音频等级用于微表情
                            headSize = 420.dp
                        )
                    }
                    // 2. 语音输入面板 (底部保持一定距离)
                    Box(
                        modifier = Modifier
                            .constrainAs(voicePanelRef) {
                                bottom.linkTo(parent.bottom, margin = 60.dp)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                            }
                    ) {
                        RobotVoiceInputPanel(
                            robotState = robotUiState.visualState,
                            isConnected = robotUiState.isConnected,
                            timerStatus = robotUiState.timerStatus,
                            audioLevel = audioLevel,
                            onStartListening = {
                                if (permissionsState.allPermissionsGranted) {
                                    robotUiState = robotUiState.copy(
                                        interactionType = com.airobotcomm.tablet.ui.state.InteractionType.CHAT,
                                        currentUserMsg = null,
                                        currentAiMsg = null
                                    )
                                    conversationViewModel.startAutoConversation()
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
                            aiMsg = if (robotUiState.interactionType == com.airobotcomm.tablet.ui.state.InteractionType.CHAT)
                                robotUiState.currentAiMsg else null,
                            onAiSpeechComplete = {
                                if (robotUiState.timerCommand != null &&
                                    robotUiState.timerStatus == com.airobotcomm.tablet.ui.state.TimerStatus.IDLE
                                ) {
                                    robotUiState = robotUiState.copy(
                                        timerStatus = com.airobotcomm.tablet.ui.state.TimerStatus.RUNNING,
                                        visualState = com.airobotcomm.tablet.ui.state.RobotVisualState.FOCUS
                                    )
                                }
                            },
                            onClose = {
                                robotUiState = robotUiState.copy(
                                    visualState = com.airobotcomm.tablet.ui.state.RobotVisualState.IDLE,
                                    currentUserMsg = null,
                                    currentAiMsg = null,
                                    timerStatus = com.airobotcomm.tablet.ui.state.TimerStatus.IDLE,
                                    timerCommand = null,
                                    activeCard = null
                                )
                                conversationViewModel.interrupt()
                            }
                        )
                    }

                    // 4. 用户消息气泡 (位于语音面板左上方)
                    if (robotUiState.currentUserMsg != null &&
                        (robotUiState.visualState == com.airobotcomm.tablet.ui.state.RobotVisualState.LISTENING ||
                                robotUiState.visualState == com.airobotcomm.tablet.ui.state.RobotVisualState.THINKING)
                    ) {
                        Box(
                            modifier = Modifier
                                .constrainAs(userBubbleRef) {
                                    end.linkTo(voicePanelRef.start, margin = 0.dp)
                                    bottom.linkTo(voicePanelRef.top, margin = 20.dp)
                                }
                        ) {
                            UserMessageBubble(
                                message = robotUiState.currentUserMsg ?: ""
                            )
                        }
                    }

                    // 5. 右侧功能推荐卡片 (非交互/卡片模式时显示)
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
                                onCardClick = { card ->
                                    currentCardIndex = serviceCards.indexOf(card)
                                    robotUiState = robotUiState.copy(
                                        interactionType = com.airobotcomm.tablet.ui.state.InteractionType.CARD,
                                        activeCard = card,
                                        visualState = com.airobotcomm.tablet.ui.state.RobotVisualState.LISTENING,
                                        currentUserMsg = null,
                                        currentAiMsg = null
                                    )
                                    serviceViewModel.startService(card)
                                    if (permissionsState.allPermissionsGranted) {
                                        conversationViewModel.startAutoConversation()
                                    }
                                }
                            )
                        }
                    }
                }
                    
                    // 功能卡片模式 - 右侧面板
                    if (robotUiState.isCardMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 64.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(modifier = Modifier.width(420.dp)) {
                                FunctionalModulePanel(
                                    card = robotUiState.activeCard,
                                    aiMsg = robotUiState.currentAiMsg,
                                    robotState = robotUiState.visualState,
                                    timerCommand = robotUiState.timerCommand,
                                    timerStatus = robotUiState.timerStatus,
                                    onAiSpeechComplete = {
                                        if (robotUiState.timerCommand != null && 
                                            robotUiState.timerStatus == com.airobotcomm.tablet.ui.state.TimerStatus.IDLE) {
                                            robotUiState = robotUiState.copy(
                                                timerStatus = com.airobotcomm.tablet.ui.state.TimerStatus.RUNNING,
                                                visualState = com.airobotcomm.tablet.ui.state.RobotVisualState.FOCUS
                                            )
                                        }
                                    },
                                    onTimerComplete = {
                                        robotUiState = robotUiState.copy(
                                            timerStatus = com.airobotcomm.tablet.ui.state.TimerStatus.IDLE,
                                            visualState = com.airobotcomm.tablet.ui.state.RobotVisualState.IDLE,
                                            timerCommand = null,
                                            activeCard = null,
                                            interactionType = com.airobotcomm.tablet.ui.state.InteractionType.CHAT
                                        )
                                    },
                                    onClose = {
                                        robotUiState = robotUiState.copy(
                                            visualState = com.airobotcomm.tablet.ui.state.RobotVisualState.IDLE,
                                            interactionType = com.airobotcomm.tablet.ui.state.InteractionType.CHAT,
                                            activeCard = null,
                                            timerStatus = com.airobotcomm.tablet.ui.state.TimerStatus.IDLE,
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
                ActivationDialog(
                    activationCode = activationCode!!,
                    onConfirm = { mainViewModel.onActivationConfirmed() }
                )
            }
        }
    }
}

@Composable
private fun BackgroundDecorations() {
    val infiniteTransition = rememberInfiniteTransition(label = "bgAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-150).dp)
                .size(400.dp * pulseScale)
                .clip(CircleShape)
                .background(Color(0xFF3B82F6).copy(alpha = 0.05f))
                .blur(120.dp)
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(350.dp)
                .clip(CircleShape)
                .background(Color(0xFF6366F1).copy(alpha = 0.08f))
                .blur(100.dp)
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        color = Color(0xFFEF4444).copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.cloud_disabled),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFFEF4444)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                color = Color(0xFFEF4444),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("关闭", color = Color(0xFFEF4444), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BottomFooter(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(RobotPrimaryCyan.copy(alpha = 0.3f))
            )
            Text(
                text = "OPERATIONAL MODE: SEAMLESS COMPANION",
                color = RobotTextPrimary.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(RobotPrimaryCyan.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
private fun FunctionalModulePanel(
    card: com.airobotcomm.tablet.ui.state.ServiceCard?,
    aiMsg: String?,
    robotState: com.airobotcomm.tablet.ui.state.RobotVisualState,
    timerCommand: com.airobotcomm.tablet.ui.state.TimerCommand?,
    timerStatus: com.airobotcomm.tablet.ui.state.TimerStatus,
    onAiSpeechComplete: () -> Unit,
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
                                    colors = if (card.type == com.airobotcomm.tablet.ui.state.ServiceCardType.TIMER) {
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
            
            AnimatedVisibility(
                visible = aiMsg != null || robotState == com.airobotcomm.tablet.ui.state.RobotVisualState.THINKING,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF22D3EE).copy(alpha = 0.05f))
                        .padding(16.dp)
                ) {
                    if (robotState == com.airobotcomm.tablet.ui.state.RobotVisualState.THINKING) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22D3EE))
                                )
                            }
                            Text(
                                text = "处理中...",
                                color = Color(0xFF22D3EE).copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    } else if (aiMsg != null) {
                        TypewriterText(
                            text = aiMsg,
                            onComplete = onAiSpeechComplete
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (card.type == com.airobotcomm.tablet.ui.state.ServiceCardType.TIMER) {
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

@Composable
private fun ActivationDialog(
    activationCode: String,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = "设备激活",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    text = "激活码：",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = activationCode,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFF22D3EE),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22D3EE)
                )
            ) {
                Text("我已激活", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E293B),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}
