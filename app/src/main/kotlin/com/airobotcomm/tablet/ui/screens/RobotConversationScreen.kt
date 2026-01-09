package com.airobotcomm.tablet.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.BatteryManager
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.airobotcomm.tablet.R
import com.airobotcomm.tablet.data.ConfigManager
import com.airobotcomm.tablet.ui.SettingsScreen
import com.airobotcomm.tablet.ui.components.dialogue.DialogueBubble
import com.airobotcomm.tablet.ui.components.dialogue.UserMessageBubble
import com.airobotcomm.tablet.ui.components.robot.*
import com.airobotcomm.tablet.ui.components.service.*
import com.airobotcomm.tablet.ui.components.voice.RobotVoiceInputPanel
import com.airobotcomm.tablet.ui.theme.RobotPrimaryCyan
import com.airobotcomm.tablet.ui.theme.RobotSecondaryIndigo
import com.airobotcomm.tablet.ui.theme.RobotTextPrimary
import com.airobotcomm.tablet.viewmodel.ConversationState
import com.airobotcomm.tablet.viewmodel.ConversationViewModel

/**
 * 机器人对话主屏幕
 * 
 * Web原型对应: App.tsx
 * 
 * 整合所有组件:
 * - 机器人角色 (RobotCharacter)
 * - 对话气泡 (DialogueBubble)
 * - 语音输入面板 (RobotVoiceInputPanel)
 * - 服务卡片轮播 (ServiceCardCarousel)
 * - 专注计时器 (FocusTimerWidget)
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RobotConversationScreen(
    viewModel: ConversationViewModel = viewModel()
) {
    val context = LocalContext.current
    val configManager = remember { ConfigManager(context) }
    
    // 权限管理
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
    )
    
    // 从ViewModel收集状态
    val conversationState by viewModel.state.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val showActivationDialog by viewModel.showActivationDialog.collectAsState()
    val activationCode by viewModel.activationCode.collectAsState()
    
    // 本地UI状态
    var showSettings by remember { mutableStateOf(false) }
    var currentConfig by remember { mutableStateOf(configManager.loadConfig()) }
    
    // 机器人UI状态
    var robotUiState by remember { mutableStateOf(RobotUiState()) }
    var currentCardIndex by remember { mutableStateOf(0) }
    
    // 同步ViewModel状态到UI状态
    LaunchedEffect(conversationState, isConnected) {
        robotUiState = robotUiState.copy(
            visualState = conversationState.toRobotVisualState(),
            isConnected = isConnected
        )
    }
    
    // 获取最新消息
    val latestUserMsg = messages.lastOrNull { it.role == com.airobotcomm.tablet.data.MessageRole.USER }?.content
    val latestAiMsg = messages.lastOrNull { it.role == com.airobotcomm.tablet.data.MessageRole.ASSISTANT }?.content
    
    LaunchedEffect(latestUserMsg, latestAiMsg) {
        robotUiState = robotUiState.copy(
            currentUserMsg = latestUserMsg,
            currentAiMsg = latestAiMsg
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
    
    // 更新状态提示 - 动态跟随卡片变化
    LaunchedEffect(currentCard, robotUiState.timerStatus, robotUiState.timerCommand) {
        val newStatusTip = when {
            robotUiState.timerStatus == TimerStatus.RUNNING -> 
                "正在专注: ${robotUiState.timerCommand?.task ?: "未知任务"}..."
            robotUiState.timerStatus == TimerStatus.PAUSED -> 
                "已暂停，休息一下..."
            else -> currentCard.statusTip
        }
        robotUiState = robotUiState.copy(statusTip = newStatusTip)
    }
    
    if (showSettings) {
        SettingsScreen(
            config = currentConfig,
            onConfigChange = { newConfig ->
                currentConfig = newConfig
                configManager.saveConfig(newConfig)
                viewModel.updateConfig(newConfig)
                showSettings = false
            },
            onBack = { showSettings = false }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A), // slate-900
                            Color(0xFF020617)  // slate-950
                        )
                    )
                )
        ) {
            // 背景装饰
            BackgroundDecorations()
            
            // 主内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 顶部栏
                TopBar(
                    isConnected = isConnected,
                    onShowSettings = { showSettings = true }
                )
                
                // 错误提示
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    ErrorBanner(
                        message = errorMessage ?: "",
                        onDismiss = { viewModel.clearError() }
                    )
                }
                
                // 主内容区域 - 使用约束布局，避免重叠
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // AI角色区域 - 固定在左侧50%区域
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // 机器人角色（居中显示）
                            Box(contentAlignment = Alignment.Center) {
                                RobotCharacter(
                                    state = robotUiState.visualState,
                                    statusTip = if (robotUiState.visualState == RobotVisualState.IDLE || 
                                                   robotUiState.visualState == RobotVisualState.FOCUS) 
                                               robotUiState.statusTip else null,
                                    headSize = 240.dp
                                )
                                
                                // AI对话气泡
                                DialogueBubble(
                                    robotState = robotUiState.visualState,
                                    aiMsg = if (robotUiState.interactionType == InteractionType.CHAT) 
                                            robotUiState.currentAiMsg else null,
                                    onAiSpeechComplete = {
                                        if (robotUiState.timerCommand != null && 
                                            robotUiState.timerStatus == TimerStatus.IDLE) {
                                            robotUiState = robotUiState.copy(
                                                timerStatus = TimerStatus.RUNNING,
                                                visualState = RobotVisualState.FOCUS
                                            )
                                        }
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
                                        viewModel.interrupt()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 180.dp, y = 40.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // 用户消息气泡
                            androidx.compose.animation.AnimatedVisibility(
                                visible = robotUiState.currentUserMsg != null && robotUiState.isInteracting,
                                enter = slideInHorizontally() + fadeIn(),
                                exit = slideOutHorizontally() + fadeOut()
                            ) {
                                UserMessageBubble(
                                    message = robotUiState.currentUserMsg ?: "",
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            // 语音输入面板
                            RobotVoiceInputPanel(
                                robotState = robotUiState.visualState,
                                isConnected = isConnected,
                                timerStatus = robotUiState.timerStatus,
                                onStartListening = {
                                    if (permissionsState.allPermissionsGranted) {
                                        robotUiState = robotUiState.copy(
                                            interactionType = InteractionType.CHAT,
                                            currentUserMsg = null,
                                            currentAiMsg = null
                                        )
                                        viewModel.startListening()
                                    }
                                },
                                onStopListening = {
                                    viewModel.stopListening()
                                },
                                onTimerControl = { action ->
                                    when (action) {
                                        "PAUSE" -> robotUiState = robotUiState.copy(
                                            timerStatus = TimerStatus.PAUSED,
                                            visualState = RobotVisualState.IDLE
                                        )
                                        "RESUME" -> robotUiState = robotUiState.copy(
                                            timerStatus = TimerStatus.RUNNING,
                                            visualState = RobotVisualState.FOCUS
                                        )
                                        "STOP" -> {
                                            robotUiState = robotUiState.copy(
                                                timerStatus = TimerStatus.IDLE,
                                                timerCommand = null,
                                                visualState = RobotVisualState.IDLE,
                                                activeCard = null,
                                                interactionType = InteractionType.CHAT
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                    
                    // 右侧 - 功能区域（在右侧区域居中，使用绝对定位防止重叠）
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !robotUiState.isInteracting,
                            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(320.dp), // 减小卡片宽度
                                contentAlignment = Alignment.Center
                            ) {
                                ServiceCardCarousel(
                                    cards = serviceCards,
                                    onCardClick = { card ->
                                        currentCardIndex = serviceCards.indexOf(card)
                                        robotUiState = robotUiState.copy(
                                            interactionType = InteractionType.CARD,
                                            activeCard = card,
                                            visualState = RobotVisualState.LISTENING,
                                            currentUserMsg = null,
                                            currentAiMsg = null
                                        )
                                        if (permissionsState.allPermissionsGranted) {
                                            viewModel.startListening()
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    // 功能卡片模式 - 右侧面板（在右侧区域居中，使用绝对定位防止重叠）
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = robotUiState.isCardMode,
                            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(420.dp)
                            ) {
                                FunctionalModulePanel(
                                    card = robotUiState.activeCard,
                                    aiMsg = robotUiState.currentAiMsg,
                                    robotState = robotUiState.visualState,
                                    timerCommand = robotUiState.timerCommand,
                                    timerStatus = robotUiState.timerStatus,
                                    onAiSpeechComplete = {
                                        if (robotUiState.timerCommand != null && 
                                            robotUiState.timerStatus == TimerStatus.IDLE) {
                                            robotUiState = robotUiState.copy(
                                                timerStatus = TimerStatus.RUNNING,
                                                visualState = RobotVisualState.FOCUS
                                            )
                                        }
                                    },
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
                                        viewModel.interrupt()
                                    }
                                )
                            }
                        }
                    }
                    
                    // 底部信息
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        BottomFooter()
                    }
                }
                
                // 激活弹窗
                if (showActivationDialog && activationCode != null) {
                    ActivationDialog(
                        activationCode = activationCode!!,
                        onConfirm = { viewModel.onActivationConfirmed() }
                    )
                }
            }
            
        } // Box
    } // else
} // RobotConversationScreen

/**
 * 背景装饰
 */
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
        // 左上光晕
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-150).dp)
                .size(400.dp * pulseScale)
                .clip(CircleShape)
                .background(Color(0xFF3B82F6).copy(alpha = 0.05f))
                .blur(120.dp)
        )
        
        // 右下光晕
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

/**
 * 顶部栏 - 模仿原型：LOGO + 网络/电量/设置
 */
@Composable
private fun TopBar(
    isConnected: Boolean,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                RobotPrimaryCyan, // cyan-500
                                RobotSecondaryIndigo  // indigo-600
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.cloud_on),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = RobotTextPrimary
                )
            }
            
            Text(
                text = "AETHER",
                color = RobotTextPrimary.copy(alpha = 0.9f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
        }
        
        // 右侧按钮 - 网络/电量/设置
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 网络/电量状态图标
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // WiFi图标
                NetworkStatusIcon()
                
                // 电量图标
                BatteryLevelIcon()
            }
            
            // 设置按钮
            IconButton(
                onClick = onShowSettings,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(RobotTextPrimary.copy(alpha = 0.05f))
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "设置",
                    modifier = Modifier.size(20.dp),
                    tint = RobotTextPrimary.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/**
 * 网络状态图标组件
 */
@Composable
private fun NetworkStatusIcon() {
    val context = LocalContext.current
    var wifiConnected by remember { mutableStateOf(false) }
    
    // 监听网络状态变化
    LaunchedEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // 获取初始网络状态
        val activeNetwork = connectivityManager.activeNetworkInfo
        wifiConnected = activeNetwork?.isConnectedOrConnecting == true
        
        // 监听网络变化
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    wifiConnected = true
                }
                
                override fun onLost(network: Network) {
                    wifiConnected = false
                }
            })
        }
    }
    
    Icon(
        painter = painterResource(id = if (wifiConnected) R.drawable.wifi else R.drawable.wifi_off),
        contentDescription = if (wifiConnected) "WiFi已连接" else "WiFi未连接",
        modifier = Modifier.size(16.dp),
        tint = if (wifiConnected) RobotTextPrimary.copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f)
    )
}

/**
 * 电池电量图标组件
 */
@Composable
private fun BatteryLevelIcon() {
    val context = LocalContext.current
    var batteryLevel by remember { mutableStateOf(100) }
    var batteryCharging by remember { mutableStateOf(false) }
    
    // 监听电池状态变化
    LaunchedEffect(Unit) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)
        
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        
        batteryLevel = (level.toFloat() / scale.toFloat() * 100).toInt()
        batteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                          status == BatteryManager.BATTERY_STATUS_FULL
        
        // 注册广播接收器监听电池变化
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                
                batteryLevel = (level.toFloat() / scale.toFloat() * 100).toInt()
                batteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                                  status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }
    
    // 根据电量选择不同图标和颜色
    val batteryIcon = when {
        batteryCharging -> R.drawable.battery_charging
        batteryLevel >= 80 -> R.drawable.battery_full
        batteryLevel >= 50 -> R.drawable.battery_medium
        batteryLevel >= 20 -> R.drawable.battery_low
        else -> R.drawable.battery_alert
    }
    
    val batteryColor = when {
        batteryCharging -> Color(0xFF34D399) // green-400
        batteryLevel >= 50 -> RobotTextPrimary.copy(alpha = 0.3f)
        else -> Color(0xFFEF4444).copy(alpha = 0.3f) // red-500
    }
    
    Icon(
        painter = painterResource(id = batteryIcon),
        contentDescription = "电池电量: ${batteryLevel}%",
        modifier = Modifier.size(16.dp),
        tint = batteryColor
    )
}

/**
 * 错误提示条
 */
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

/**
 * 底部信息
 */
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

/**
 * 功能模块面板
 */
@Composable
private fun FunctionalModulePanel(
    card: ServiceCard?,
    aiMsg: String?,
    robotState: RobotVisualState,
    timerCommand: TimerCommand?,
    timerStatus: TimerStatus,
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
            // 头部
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
            
            // AI消息区域
            AnimatedVisibility(
                visible = aiMsg != null || robotState == RobotVisualState.THINKING,
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
                    if (robotState == RobotVisualState.THINKING) {
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
                        com.airobotcomm.tablet.ui.components.dialogue.TypewriterText(
                            text = aiMsg,
                            onComplete = onAiSpeechComplete
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 内容区域
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
                    // 其他功能模块占位
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

/**
 * 激活弹窗
 */
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
