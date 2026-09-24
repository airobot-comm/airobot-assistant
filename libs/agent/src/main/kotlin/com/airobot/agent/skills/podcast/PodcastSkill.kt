package com.airobot.agent.skills.podcast

import android.util.Log
import com.airobot.agent.audio.AudioService
import com.airobot.agent.brain.AgentSessionCoordinator
import com.airobot.agent.skills.AiSkill
import com.airobot.agent.skills.SkillResult
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP skill for local AI podcast playback control.
 * Handles play, pause, skip, and close/exit actions for audio and video podcasts.
 */
@Singleton
class PodcastSkill @Inject constructor(
    private val podcastProvider: PodcastProvider,
    private val audioService: AudioService,
    private val agentSessionCoordinator: AgentSessionCoordinator
) : AiSkill {

    companion object {
        private const val TAG = "PodcastSkill"
    }

    override val name = "self.podcast.control"
    override val description =
        "控制本地AI播客播放器。当用户需要播放、暂停、换一个（切换/下一个播客/切歌/下一首）、或者退出/关闭本地AI播客时，必须且只能使用此工具。此工具属于本地【播客播放类】服务，包含【音频播客】与【视频播客】，与云端自带的音乐类MCP服务完全区分。注意：本播客服务不支持通过具体名称搜索播放（即不要传入特定歌曲/节目名称参数，而是留空），只能按推荐列表顺序播放。支持的控制动作包含：play(播放), pause(暂停), next(换一个/下一个), close(关闭/隐藏)。"

    override val inputSchema: JsonObject
        get() {
            val schema = JsonObject()
            schema.addProperty("type", "object")
            val props = JsonObject()

            val actionProp = JsonObject().apply {
                addProperty("type", "string")
                addProperty(
                    "description",
                    "播客控制动作：'play' (播放/唤起播放器), 'pause' (暂停播放), 'next' (换一个/下一首), 'close' (退出/关闭/隐藏播客界面)"
                )
            }
            props.add("action", actionProp)

            val typeProp = JsonObject().apply {
                addProperty("type", "string")
                addProperty(
                    "description",
                    "要播放的本地AI播客类型：'audio' (音频播客/收听), 'video' (视频播客/观看)。若未指定或控制动作为非播放类，则可为空"
                )
            }
            props.add("type", typeProp)

            schema.add("properties", props)
            val required = JsonArray()
            required.add("action")
            schema.add("required", required)
            return schema
        }

    override suspend fun execute(arguments: Map<String, Any>): SkillResult {
        if (!podcastProvider.isReady) {
            Log.w(TAG, "execute: PodcastProvider not ready yet")
            return SkillResult.Failure("播客服务尚未准备就绪")
        }

        val action = arguments["action"]?.toString()?.lowercase()
            ?: return SkillResult.Failure("缺少必要参数: action")
        val type = arguments["type"]?.toString()

        Log.d(TAG, "execute: action=$action, type=$type")

        return when (action) {
            "play", "resume" -> {
                // Wait for any active speech confirmation TTS to finish playing before starting music/video
                audioService.waitForPlaybackCompletion()
                // Mute capture temporarily to avoid voice feedback during transitions
                agentSessionCoordinator.closeEarsTemporarily(2000L)
                val success = podcastProvider.playRecommended(type)
                if (success) {
                    SkillResult.Success("已为您开始播放推荐的${if (type == "video") "视频" else "音频"}播客")
                } else {
                    SkillResult.Failure("未找到可用的推荐${if (type == "video") "视频" else "音频"}播客")
                }
            }

            "pause" -> {
                val success = podcastProvider.pause()
                if (success) {
                    SkillResult.Success("已暂停播客播放")
                } else {
                    SkillResult.Failure("暂停播放失败")
                }
            }

            "next", "change", "skip" -> {
                // Wait for any active speech confirmation TTS to finish playing before skipping
                audioService.waitForPlaybackCompletion()
                // Mute capture temporarily to avoid voice feedback during transitions
                agentSessionCoordinator.closeEarsTemporarily(2000L)
                val success = podcastProvider.playNext(type)
                if (success) {
                    SkillResult.Success("已为您切换至下一个推荐的${if (type == "video") "视频" else "音频"}播客")
                } else {
                    SkillResult.Failure("切换失败，未找到可播放的推荐播客")
                }
            }

            "close", "stop", "exit" -> {
                val success = podcastProvider.closePlayer()
                if (success) {
                    SkillResult.Success("已退出并关闭播客播放器")
                } else {
                    SkillResult.Failure("关闭播放器失败")
                }
            }

            else -> SkillResult.Failure("不支持的播客控制动作: $action")
        }
    }
}
