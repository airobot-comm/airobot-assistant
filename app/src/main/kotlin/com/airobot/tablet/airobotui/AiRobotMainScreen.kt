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
import com.airobot.tablet.airobotui.settings.AiRobotDialog
import com.airobot.character.airobotui.comp.dialogue.DialogueBubble
import com.airobot.character.airobotui.comp.robot.*
import com.airobot.character.airobotui.comp.voice.RobotVoiceInputPanel
import com.airobot.framework.statusbar.RobotTopBar
import com.airobot.framework.comp.drawer.SystemDrawerContent
import com.airobot.framework.comp.drawer.DrawerMenuItemData
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import com.airobot.tablet.airobotui.settings.AiRobotConfig
import com.airobot.tablet.airobotui.settings.RoleConfig
import com.airobot.tablet.airobotui.settings.SystemAuth
import com.airobot.services.compoments.DEFAULT_SERVICE_CARDS
import com.airobot.services.FocusTimerWidget
import com.airobot.services.compoments.ServiceCardCarousel
import com.airobot.services.compoments.getServiceCardIcon
import com.airobot.character.airobotui.state.ConversationSubState
import com.airobot.character.airobotui.state.InteractionType
import com.airobot.character.airobotui.state.RobotEngineState
import com.airobot.character.airobotui.state.RobotUiState
import com.airobot.character.airobotui.state.RobotVisualState
import com.airobot.services.state.ServiceCard
import com.airobot.services.state.ServiceCardType
import com.airobot.services.state.ServiceCardData
import com.airobot.services.state.TimerCardData
import com.airobot.services.state.ServiceSubState
import com.airobot.tablet.airobotui.viewmodel.MainShellViewModel
import com.airobot.character.airobotui.viewmodel.ConversationViewModel
import com.airobot.services.ServiceViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.animation.core.Spring // ADDED IMPORT

/**
 * 鏈哄櫒浜烘湇鍔′富灞忓箷
 * Web鍘熷瀷瀵瑰簲: App.tsx
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AiRobotMainScreen(
    themeMode: RobotThemeMode = RobotThemeMode.DARK,
    onToggleTheme: () -> Unit = {},
    mainShellViewModel: MainShellViewModel = hiltViewModel(),
    conversationViewModel: ConversationViewModel = hiltViewModel(),
    serviceViewModel: ServiceViewModel = hiltViewModel()
) {
    // 鏉冮檺绠＄悊
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
    )

    // 浠?RobotMainViewModel 鏀堕泦涓€绾х姸鎬?
    val robotState by mainShellViewModel.robotState.collectAsState()
    val errorMessage by mainShellViewModel.errorMessage.collectAsState()
    val showActivationDialog by mainShellViewModel.showActivationDialog.collectAsState()
    val activationCode by mainShellViewModel.activationCode.collectAsState()
    val mainVoiceLevel by mainShellViewModel.voiceLevel.collectAsState()

    // 浠?ConversationViewModel 鏀堕泦浜や簰鐘舵€?
    val convAudioLevel by conversationViewModel.audioLevel.collectAsState()
    val currentRoundUserText by conversationViewModel.currentRoundUserText.collectAsState()
    val currentRoundAiText by conversationViewModel.currentRoundAiText.collectAsState()

    // 缁勫悎闊抽噺绛夌骇锛氬璇濇椂鐢ㄥ璇漋M鐨勶紝闈炲璇濇椂鐢ㄤ富VM鐨?
    val audioLevel = if (robotState is RobotEngineState.Conversation) convAudioLevel else mainVoiceLevel

    // 浠?ServiceViewModel 鏀堕泦鍔熻兘鐘舵€?
    val activeCard by serviceViewModel.activeCard.collectAsState()
    val activeServiceData by serviceViewModel.activeServiceData.collectAsState()
    val serviceSubState by serviceViewModel.serviceSubState.collectAsState()
    
    // 鏈湴UI鐘舵€?
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 鏈哄櫒浜篣I鐘舵€?
    var robotUiState by remember { mutableStateOf(RobotUiState()) }
    var currentCardIndex by remember { mutableIntStateOf(0) }
    
    // 鏈嶅姟鍗＄墖瀹氫箟
    val serviceCards = DEFAULT_SERVICE_CARDS
    val currentCard = serviceCards.getOrNull(currentCardIndex) ?: serviceCards.first()

    // 鑷姩杞挱閫昏緫
    LaunchedEffect(serviceCards.size, robotUiState.isInteracting) {
        if (serviceCards.isNotEmpty() && !robotUiState.isInteracting) {
            while (true) {
                kotlinx.coroutines.delay(10000L)
                currentCardIndex = (currentCardIndex + 1) % serviceCards.size
            }
        }
    }

    // 鏈哄櫒浜篣I鐘舵€佹眹鎬诲悓姝?- 鏁村悎澶氫釜鏉ユ簮锛岄伩鍏嶇珵鎬佽鐩?
    LaunchedEffect(
        robotState, 
        currentRoundUserText, 
        currentRoundAiText, 
        activeCard, 
        activeServiceData, 
        serviceSubState,
        currentCard // currentCard 闅?currentCardIndex 鍙樺寲
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

    // 璇锋眰鏉冮檺
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // 鍒濆鍖栭煶棰戠郴缁?(褰撴潈闄愯幏寰楀悗)
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            mainShellViewModel.initAudioService()
        }
    }

    // 鐩戝惉鍞ら啋浜嬩欢
    LaunchedEffect(Unit) {
        mainShellViewModel.wakeupEvent.collect {
            conversationViewModel.startConversation()
        }
    }

    // 鏈哄櫒浜烘按骞充綅绉诲姩鐢?
    // 褰?isCardMode 涓?true (鐐瑰嚮鍗＄墖灞曞紑) 鏃讹紝鏈哄櫒浜烘粦鍚戝乏渚?(bias 0.1f)
    // 鍚﹀垯淇濇寔鍦ㄤ腑闂?(bias 0.5f)
    val robotHorizontalBias by animateFloatAsState(
        targetValue = if (robotUiState.isCardMode) 0.04f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "robotSlide"
    )

    val drawerMenuItems = listOf(
        DrawerMenuItemData(Icons.Default.Lock, "系统认证", "系统认证信息") { SystemAuth() },
        DrawerMenuItemData(Icons.Default.Person, "角色管理", "角色管理") { RoleConfig() },
        DrawerMenuItemData(Icons.Default.Settings, "Ai智能体", "Ai智能体配置") { AiRobotConfig() }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SystemDrawerContent(
                menuItems = drawerMenuItems,
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
                    // 缁夊娅庡銈咁槱鐎佃壈鍩呴弽蹇撳敶鏉堢绐涢敍宀冾唨閼冲本娅欑拹顖溾敍
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
                
                // ErrorBanner 杩佺Щ鍒?TopBar 涓紝姝ゅ绉婚櫎
                
                // 涓績鍐呭鍖哄煙 - 浣跨敤 ConstraintLayout 绮剧‘鎺у埗鐩稿浣嶇疆
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    val (robotRef, voicePanelRef, aiBubbleRef, serviceCardsRef, activeCardRef) = createRefs()

                    // 1. 鏈哄櫒浜鸿鑹?(灞呬腑鍋忎笂)
                    Box(
                        modifier = Modifier
                            .constrainAs(robotRef) {
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom, margin = 200.dp)
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                horizontalBias = robotHorizontalBias
                                verticalBias = 0.35f // 缁х画涓婄Щ锛岀粰涓嬫柟鑵惧嚭閫昏緫绌洪棿
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        RobotCharacter(
                            state = robotUiState.visualState,
                            audioLevel = { audioLevel }, // 浼犲叆闊抽绛夌骇鐢ㄤ簬寰〃鎯?
                            headSize = 400.dp // 420 -> 400 绋嶅井缂╁皬涓€鐐圭偣
                        )
                    }
                    // 2. 璇煶杈撳叆闈㈡澘 (涓嬬Щ 15%锛屽澶у簳閮ㄩ棿璺?
                    Box(
                        modifier = Modifier
                            .constrainAs(voicePanelRef) {
                                bottom.linkTo(parent.bottom, margin = 40.dp) // 80 -> 40 鏄捐憲涓嬬Щ
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

                    // 3. AI 瀵硅瘽姘旀场 (鏈哄櫒浜哄彸涓婅)
                    Box(
                        modifier = Modifier
                            .constrainAs(aiBubbleRef) {
                                start.linkTo(robotRef.end, margin = (-180).dp) // 绋嶅井閲嶅彔涓€鐐圭湅璧锋潵鍍忎粠鏈哄櫒浜哄彂鍑?
                                top.linkTo(robotRef.top, margin = 180.dp)
                            }
                    ) {
                        DialogueBubble(
                            robotState = robotUiState.visualState,
                            aiMsg = currentRoundAiText,
                            onAiSpeechComplete = {
                                // 璇煶鎾姤瀹屾垚
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

                    // 4. 鍙充晶鍔熻兘鎺ㄨ崘鍗＄墖 (闈炰氦浜?鍗＄墖妯″紡鏃舵樉绀?
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
                                statusTip = robotUiState.statusTip, // 浼犻€掔姸鎬佹彁绀哄埌鍗＄墖鍖哄煙
                                onCardClick = { card ->
                                    // 纭繚鐐瑰嚮鐨勬槸褰撳墠鏄剧ず鐨勫崱鐗?
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

                    // 5. 鍔熻兘鍗＄墖璇︽儏 (浜や簰/鍗＄墖妯″紡鏃舵樉绀?
                    if (robotUiState.isCardMode) {
                        Box(
                            modifier = Modifier
                                .constrainAs(activeCardRef) {
                                    end.linkTo(parent.end, margin = 48.dp)
                                    top.linkTo(parent.top)
                                    bottom.linkTo(parent.bottom)
                                }
                                .width(600.dp) // 鏇村鐨勫崱鐗?
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
            
            // 搴曢儴椤佃剼
            BottomFooter(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
            
            // 婵€娲诲脊绐?
            if (showActivationDialog && activationCode != null) {
                AiRobotDialog(
                    activationCode = activationCode!!,
                    onConfirm = { mainShellViewModel.onActivationConfirmed() },
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
                        contentDescription = "鍏抽棴",
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
                                text = card.demoContent ?: "${card.type.name} 鍔熻兘寮€鍙戜腑",
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

