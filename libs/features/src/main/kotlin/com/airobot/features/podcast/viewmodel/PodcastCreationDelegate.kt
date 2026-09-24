package com.airobot.features.podcast.viewmodel

import android.util.Log
import com.airobot.features.aiserv.event.AiEvent
import com.airobot.features.aiserv.event.AiEventDispatcher
import com.airobot.features.podcast.cards.creator.ScannedFile
import com.airobot.features.podcast.data.PodcastRepository
import com.airobot.features.podcast.data.PodcastSystemPresets
import com.airobot.features.podcast.data.model.PodcastEpisode
import com.airobot.features.podcast.data.model.PodcastSubscription
import com.airobot.features.podcast.service.MediaImportService
import com.airobot.features.podcast.service.MediaScannerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Delegate responsible for episode creation (DIY import from MediaStore, custom topic text generation),
 * media scanning, and dispatching episode created AI Notepad events.
 */
class PodcastCreationDelegate @Inject constructor(
    private val repository: PodcastRepository,
    private val mediaScannerService: MediaScannerService,
    private val mediaImportService: MediaImportService,
    private val eventDispatcher: AiEventDispatcher,
    private val dataDelegate: PodcastDataDelegate,
    private val playbackDelegate: PodcastPlaybackDelegate
) {
    companion object {
        private const val TAG = "PodcastCreationDelegate"
    }

    private val _recommendation = MutableStateFlow(
        "想听听今天的新闻资讯或者有趣的故事吗？AETHER为你准备了专属播客。"
    )
    val recommendation: StateFlow<String> = _recommendation.asStateFlow()

    private val delegateScope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

    suspend fun scanMediaFiles(type: String): List<ScannedFile> {
        Log.d(TAG, "scanMediaFiles: type=$type")
        return when (type) {
            "audio" -> mediaScannerService.scanAudioFiles()
            "video" -> mediaScannerService.scanVideoFiles()
            else -> emptyList()
        }
    }

    fun getDefaultScanPath(type: String): String {
        return mediaScannerService.getDefaultDirectoryPath(type)
    }

    suspend fun importAndCreateEpisode(
        file: ScannedFile,
        title: String,
        type: String
    ): Boolean {
        Log.d(TAG, "importAndCreateEpisode: file=${file.name}, title=$title, type=$type")

        val importResult = mediaImportService.importFile(file.uri, file.name)
        if (importResult.isFailure) {
            Log.e(TAG, "File import failed", importResult.exceptionOrNull())
            return false
        }

        val result = importResult.getOrThrow()
        val now = System.currentTimeMillis()
        val dateStr = java.text.SimpleDateFormat(
            "yyyy-MM-dd", java.util.Locale.getDefault()
        ).format(java.util.Date())

        // Create episode belonging to the fixed predefined DIY channel
        val episode = PodcastEpisode(
            id = "diy_ep_$now",
            title = title,
            summary = "导入自本地${if (type == "audio") "音频" else "视频"}文件：${file.name}",
            type = type,
            channelName = PodcastSystemPresets.DIY_SUBSCRIPTION_TITLE,
            content = "",
            date = dateStr,
            mediaUri = result.internalUri,
            durationMs = result.durationMs,
            fileSizeBytes = result.fileSizeBytes,
            mimeType = result.mimeType,
            originalFileName = file.name,
            isDiy = true,
            progress = 0f,
            played = false,
            favorite = false,
            createdAt = now
        )

        // Find existing or create/update the DIY subscription
        val currentSubscriptions = dataDelegate.subscriptions.value
        val existingDiySubIndex = currentSubscriptions.indexOfFirst { it.id == PodcastSystemPresets.DIY_SUBSCRIPTION_ID }
        
        val updatedSubscriptions = if (existingDiySubIndex != -1) {
            val existingDiySub = currentSubscriptions[existingDiySubIndex]
            val updatedDiySub = existingDiySub.copy(
                filesCount = existingDiySub.filesCount + 1,
                time = dateStr
            )
            currentSubscriptions.toMutableList().apply {
                set(existingDiySubIndex, updatedDiySub)
            }
        } else {
            val newDiySub = PodcastSubscription(
                id = PodcastSystemPresets.DIY_SUBSCRIPTION_ID,
                title = PodcastSystemPresets.DIY_SUBSCRIPTION_TITLE,
                type = "custom",
                time = dateStr,
                description = "存放用户个人导入的音视频节目内容。",
                isSubscribed = true,
                isDIY = true,
                sourceDir = mediaScannerService.getDefaultDirectoryPath(type),
                filesCount = 1,
                createdAt = now
            )
            listOf(newDiySub) + currentSubscriptions
        }

        // Persist
        repository.addEpisode(episode)
        repository.saveSubscriptions(updatedSubscriptions)

        // Update in-memory state
        dataDelegate.updateEpisodesState(listOf(episode) + dataDelegate.episodes.value)
        dataDelegate.updateSubscriptionsState(updatedSubscriptions)

        Log.d(TAG, "DIY episode created: id=${episode.id}, duration=${result.durationMs}ms")

        // Dispatch AI event
        eventDispatcher.dispatch(
            AiEvent.PodcastEpisodeCreated(
                episodeId = episode.id,
                title = episode.title,
                type = episode.type,
                channelName = episode.channelName,
                isDiy = true,
                timestamp = now
            )
        )

        // Update recommendation text to showcase the newly created episode
        _recommendation.value = "您已成功创作了新节目《${title}》，快去播客库收听吧！"

        return true
    }

    fun generateEpisode(type: String, topic: String? = null) {
        val topicName = topic ?: when (type) {
            "video" -> "深海探秘"
            "audio" -> "极简舒缓氛围乐"
            "text" -> "商业帝国的崛起"
            else -> "随机科技漫谈"
        }

        val channelMap = mapOf(
            "video" to "我的DIY视频栏目",
            "audio" to "每日声音电台",
            "text" to "每日科技速递"
        )
        val channelName = channelMap[type] ?: "未分类栏目"
        val now = System.currentTimeMillis()

        val newEp = PodcastEpisode(
            id = now.toString(),
            title = "关于 $topicName 的特别生成探索",
            summary = "这是一期基于 Aether 生成的专属 $type 播客，探讨了关于 $topicName 的核心概念...",
            type = type,
            channelName = channelName,
            content = "欢迎收听本期特别生成的播客内容。今天，我们将聚焦在 $topicName 的领域，" +
                "探讨它的发展历史以及在未来的无限可能性。我们将为您讲述从无到有的创新历程，" +
                "以及如何运用AI解决现实问题...",
            date = "2026-06-16",
            progress = 0f,
            played = false,
            favorite = false,
            createdAt = now
        )

        dataDelegate.updateEpisodesState(listOf(newEp) + dataDelegate.episodes.value)
        delegateScope.launch { repository.saveEpisodes(dataDelegate.episodes.value) }

        // Dispatch AI event
        eventDispatcher.dispatch(
            AiEvent.PodcastEpisodeCreated(
                episodeId = newEp.id,
                title = newEp.title,
                type = newEp.type,
                channelName = newEp.channelName,
                isDiy = false,
                timestamp = now
            )
        )

        playbackDelegate.playEpisode(newEp)
    }
}
