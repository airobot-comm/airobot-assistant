package com.airobot.features.aiserv.guidance.data

import com.airobot.features.FeatureCards

/**
 * RemindCard — Data model for AI Remind Banners.
 */
data class RemindCard(
    val tag: String,
    val title: String,
    val content: String,
    val actionTarget: String
) {
    companion object {
        val defaultCards = listOf(
            RemindCard(
                tag = FeatureCards.FOCUS,
                title = "时间管理",
                content = "进入深度专注期后，我会自动为您开启免打扰，并播放适合心流的脑波音乐。开启 [ai专注] 吧！",
                actionTarget = "focus"
            ),
            RemindCard(
                tag = FeatureCards.LOGBOOK,
                title = "成长足迹",
                content = "AETHER 已为您自动生成了今日的时间轨迹与行为分析。点击查看 [ai记事本] 吧。",
                actionTarget = "logbook"
            ),
            RemindCard(
                tag = FeatureCards.TIMER,
                title = "高效计时",
                content = "不管是烹饪、运动还是短暂休息，让我为您分秒不差地倒计时。点击设置 [ai计时] 吧。",
                actionTarget = "timer"
            ),
            RemindCard(
                tag = FeatureCards.ALARM,
                title = "快捷闹钟",
                content = "准时唤醒，开启美好的一天。点击设置 [ai闹钟] 吧。",
                actionTarget = "clock_alarm"
            ),
            RemindCard(
                tag = FeatureCards.DIY_PODCAST,
                title = "播客 DIY",
                content = "本地文档和音视频素材均可一键转化为专属播客节目，打开 [DIY新节目] 开始创作吧！",
                actionTarget = "diy"
            ),
            RemindCard(
                tag = FeatureCards.PODCAST,
                title = "AI播客精选",
                content = "为您准备了今日精选播客，随时可以开始收听！",
                actionTarget = "podcast"
            )
        )
    }
}
