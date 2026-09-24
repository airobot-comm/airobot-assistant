package com.airobot.features.podcast.cards

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.airobot.features.podcast.cards.creator.DiyFileScanner
import com.airobot.features.podcast.cards.creator.DiyProgressOverlay
import com.airobot.features.podcast.cards.creator.DiyTypeSelector
import com.airobot.features.podcast.cards.creator.ScannedFile
import com.airobot.features.podcast.cards.widgets.OverlayCloseArrowIcon
import com.airobot.features.podcast.cards.widgets.OverlayHangingTab
import com.airobot.features.podcast.cards.widgets.OverlayTabsScaffold
import com.airobot.features.podcast.viewmodel.PodcastViewModel
import com.airobot.framework.theme.PodcastFeaturedBg
import com.airobot.framework.theme.RobotTheme
import kotlinx.coroutines.launch

private const val TAG = "PodcastDiyOverlay"

/**
 * PodcastDiyOverlay — Scans local audio/video files from standard directories,
 * imports selected file to app-private storage, and creates a real podcast episode.
 *
 * Key changes from prototype:
 * - Real MediaStore scanning replaces hardcoded file lists
 * - Real file import replaces fake AI generation
 * - Audio/Video types active; 图文 disabled
 * - Runtime media permission handling for Android 13+
 */
@Composable
fun PodcastDiyOverlay(
    podcastViewModel: PodcastViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Form inputs state — default to "audio" (图文 is disabled)
    var diyType by remember { mutableStateOf("audio") }
    var diyTitle by remember { mutableStateOf("我的DIY创作") }

    // Display directory path from service
    val diyDir = podcastViewModel.getDefaultScanPath(diyType)

    // Scanning state
    var isScanning by remember { mutableStateOf(false) }
    var scanStep by remember { mutableIntStateOf(0) }
    var scannedFiles by remember { mutableStateOf<List<ScannedFile>>(emptyList()) }
    var selectedFile by remember { mutableStateOf<ScannedFile?>(null) }

    // Permission state
    var permissionGranted by remember { mutableStateOf(false) }
    var showPermissionHint by remember { mutableStateOf(false) }

    // Determine required permission based on type and API level
    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        when (diyType) {
            "audio" -> Manifest.permission.READ_MEDIA_AUDIO
            "video" -> Manifest.permission.READ_MEDIA_VIDEO
            else -> Manifest.permission.READ_EXTERNAL_STORAGE
        }
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Native permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (!isGranted) {
            showPermissionHint = true
            Log.d(TAG, "Permission denied for: $requiredPermission")
        } else {
            showPermissionHint = false
            Log.d(TAG, "Permission granted: $requiredPermission")
        }
    }

    // Check and request permission when type changes
    LaunchedEffect(diyType) {
        if (diyType == "text") return@LaunchedEffect

        // Check if already granted
        val granted = ContextCompat.checkSelfPermission(
            context, requiredPermission
        ) == PackageManager.PERMISSION_GRANTED
        permissionGranted = granted

        if (!granted) {
            Log.d(TAG, "Requesting permission: $requiredPermission")
            permissionLauncher.launch(requiredPermission)
            return@LaunchedEffect
        }

        // Permission granted → start scan
        showPermissionHint = false
        isScanning = true
        scanStep = 1
        Log.d(TAG, "Starting real media scan: type=$diyType")

        val files = podcastViewModel.scanMediaFiles(diyType)
        kotlinx.coroutines.delay(800) // smooth refresh animation buffer
        scannedFiles = files
        selectedFile = files.firstOrNull()
        scanStep = if (files.isNotEmpty()) 2 else 0
        isScanning = false
        Log.d(TAG, "Scan complete: ${files.size} files found")
    }

    // Re-scan when permission becomes granted after request
    LaunchedEffect(permissionGranted) {
        if (permissionGranted && diyType != "text" && scannedFiles.isEmpty()) {
            isScanning = true
            scanStep = 1
            val files = podcastViewModel.scanMediaFiles(diyType)
            kotlinx.coroutines.delay(800) // smooth refresh animation buffer
            scannedFiles = files
            selectedFile = files.firstOrNull()
            scanStep = if (files.isNotEmpty()) 2 else 0
            isScanning = false
        }
    }

    // Auto-update title from selected file name
    LaunchedEffect(selectedFile) {
        selectedFile?.let {
            val dotIndex = it.name.lastIndexOf('.')
            diyTitle = if (dotIndex > 0) it.name.substring(0, dotIndex) else it.name
        }
    }

    // Import progress state
    var isGenerating by remember { mutableStateOf(false) }
    var genProgress by remember { mutableIntStateOf(0) }
    var generationStep by remember { mutableStateOf("") }

    // Colors
    val cardBorderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val inputBgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
    val textColor = RobotTheme.colors.textPrimary
    val textMutedColor = RobotTheme.colors.textMuted

    OverlayTabsScaffold(
        onClose = onClose,
        enabled = !isScanning && !isGenerating,
        isDark = isDark,
        tabs = {
            // Tab 1: Execute Import
            val canCreate = scanStep == 2 && selectedFile != null &&
                diyTitle.isNotBlank() && !isScanning && !isGenerating
            val executeTabBg = if (canCreate) {
                PodcastFeaturedBg
            } else {
                if (isDark) Color(0xFF38434F) else Color(0xFFC5D1D8)
            }
            val executeTabBorder = if (canCreate) {
                PodcastFeaturedBg
            } else {
                cardBorderColor
            }

            OverlayHangingTab(
                onClick = {
                    isGenerating = true
                    genProgress = 5
                    generationStep = "正在拷贝媒体文件到应用存储..."
                    scope.launch {
                        val file = selectedFile ?: return@launch
                        Log.d(TAG, "Starting import: file=${file.name}, title=$diyTitle")

                        // Progress: copying
                        genProgress = 30
                        generationStep = "正在拷贝媒体文件到应用存储..."

                        val success = podcastViewModel.importAndCreateEpisode(
                            file = file,
                            title = diyTitle,
                            type = diyType
                        )

                        if (success) {
                            genProgress = 80
                            generationStep = "正在提取媒体元数据..."
                            kotlinx.coroutines.delay(500)

                            genProgress = 95
                            generationStep = "创建播客节目记录..."
                            kotlinx.coroutines.delay(300)

                            genProgress = 100
                            generationStep = "完成！节目已添加到播客库。"
                            Toast.makeText(context, "新节目已创作完成！已添加到播客列表与推荐收听。", Toast.LENGTH_LONG).show()
                            kotlinx.coroutines.delay(800)
                        } else {
                            genProgress = 100
                            generationStep = "导入失败，请重试。"
                            kotlinx.coroutines.delay(1500)
                        }

                        isGenerating = false
                        if (success) onClose()
                    }
                },
                enabled = canCreate,
                tabHeight = 88.dp,
                backgroundColor = executeTabBg,
                borderColor = executeTabBorder
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "Execute Import",
                        tint = if (canCreate) Color.White else textMutedColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(width = 26.dp, height = 2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (canCreate) Color.White.copy(alpha = 0.6f)
                                        else (if (isDark) Color(0xFF5A6672) else Color(0xFF90A1AC))
                                    )
                            )
                        }
                    }
                }
            }

            // Tab 2: Close / Exit
            OverlayHangingTab(
                onClick = onClose,
                enabled = !isScanning && !isGenerating,
                tabHeight = 60.dp
            ) {
                OverlayCloseArrowIcon(isDark = isDark)
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = PodcastFeaturedBg,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "DIY新节目",
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Scrollable form
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Type Selector
                    item {
                        DiyTypeSelector(
                            diyType = diyType,
                            onTypeSelected = { diyType = it },
                            isGenerating = isGenerating,
                            onTextTypeClick = {
                                // Show hint that text type is not yet supported
                                showPermissionHint = true
                            }
                        )
                    }

                    // Permission / unsupported hint
                    if (showPermissionHint) {
                        item {
                            Text(
                                text = if (diyType == "text") {
                                    "📝 图文类暂不支持导入，敬请期待。"
                                } else {
                                    "⚠️ 需要媒体读取权限才能扫描文件，请在系统设置中允许。"
                                },
                                color = PodcastFeaturedBg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // 2. Select Scanned File
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "2. 选择文件",
                                color = textMutedColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "自动扫描默认路径：$diyDir",
                                color = textMutedColor.copy(alpha = 0.7f),
                                fontSize = 8.5.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            DiyFileScanner(
                                isScanning = isScanning,
                                scanStep = scanStep,
                                scannedFiles = scannedFiles,
                                selectedFile = selectedFile,
                                onFileSelected = { selectedFile = it },
                                isGenerating = isGenerating,
                                inputBgColor = inputBgColor,
                                cardBorderColor = cardBorderColor,
                                textColor = textColor,
                                textMutedColor = textMutedColor,
                                diyType = diyType
                            )
                        }
                    }

                    // 3. Custom Title
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "3. 确认节目名称",
                                color = textMutedColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )

                            OutlinedTextField(
                                value = diyTitle,
                                onValueChange = { if (!isGenerating) diyTitle = it },
                                placeholder = {
                                    Text(
                                        "例如：高级系统架构设计分享",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                singleLine = true,
                                enabled = !isGenerating,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = inputBgColor,
                                    focusedContainerColor = inputBgColor,
                                    unfocusedBorderColor = cardBorderColor,
                                    focusedBorderColor = PodcastFeaturedBg,
                                    unfocusedTextColor = textColor,
                                    focusedTextColor = textColor
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "* 创作完成后即立刻呈现在主界面。",
                                color = PodcastFeaturedBg,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            // Import progress overlay
            AnimatedVisibility(
                visible = isGenerating,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
            ) {
                DiyProgressOverlay(
                    genProgress = genProgress,
                    generationStep = generationStep
                )
            }
        }
    )
}
