package com.airobot.features.podcast.data

import com.airobot.features.podcast.data.model.PodcastEpisode
import com.airobot.features.podcast.data.model.PodcastSubscription
import com.airobot.features.podcast.data.model.QnaItem

/**
 * System-predefined podcast channels and episodes.
 *
 * All built-in content is maintained here as a single source of truth.
 * To add/remove/rename system channels or episodes, only modify this file.
 *
 * ID Convention:
 * - System subscriptions: "sys_sub_<sequential>"
 * - System episodes: "sys_ep_<sequential>"
 * - DIY subscriptions: "diy_<timestamp>"
 * - DIY episodes: "diy_ep_<timestamp>"
 */
object PodcastSystemPresets {

    const val DIY_SUBSCRIPTION_ID = "sys_sub_diy"
    const val DIY_SUBSCRIPTION_TITLE = "DIY创作栏目"

    // ---- System-predefined Subscriptions (Channels) ----

    val SUBSCRIPTIONS: List<PodcastSubscription> = listOf(
        PodcastSubscription(
            id = DIY_SUBSCRIPTION_ID,
            title = DIY_SUBSCRIPTION_TITLE,
            type = "custom",
            time = "随时更新",
            description = "存放用户个人导入的音视频节目内容。",
            isSubscribed = true,
            isDIY = true,
            filesCount = 0
        ),
        PodcastSubscription(
            id = "sys_sub_1",
            title = "每日科技速递",
            type = "text",
            time = "每天 08:00",
            description = "每天早上8点，为你播报最新科技圈动态与深度解析。",
            isSubscribed = false
        ),
        PodcastSubscription(
            id = "sys_sub_2",
            title = "每日声音电台",
            type = "audio",
            time = "每天 22:00",
            description = "高音质立体声配音，让你在通勤和深夜用耳朵领略自然森林白噪音与人声互动。",
            isSubscribed = false
        ),
        PodcastSubscription(
            id = "sys_sub_4",
            title = "商业思维日课",
            type = "text",
            time = "工作日 18:00",
            description = "下班通勤路上的商业认知升级，解析最新商业案例。",
            isSubscribed = false
        ),
        PodcastSubscription(
            id = "sys_sub_5",
            title = "自然白噪音疗愈馆",
            type = "audio",
            time = "每天 23:00",
            description = "用大自然最纯净的声音治愈心灵，伴你入睡。",
            isSubscribed = false
        ),
        PodcastSubscription(
            id = "sys_sub_6",
            title = "未知的深渊",
            type = "video",
            time = "每周五 20:00",
            description = "探索地球上最深处的奇妙生物与地质奇观。",
            isSubscribed = false
        )
    )

    // ---- System-predefined Episodes ----

    val EPISODES: List<PodcastEpisode> = listOf(
        PodcastEpisode(
            id = "sys_ep_1",
            title = "深海探秘：未知的深渊",
            summary = "跟随着深海潜水器的视角，探索地球上最深处的奇妙生物与地质奇观。",
            type = "video",
            channelName = "未知的深渊",
            content = "【画外音：深沉的声呐声与潜水艇加压轰鸣】\n\n" +
                "[主持人: AETHER] 欢迎收听本期科技视频节目。在我们身下，是超过一万米深、" +
                "完全黑暗的马里亚纳海沟。通过我们的前端深海超清摄像机微光夜视系统，" +
                "我们可以看到前方有一只透明的水母闪烁着极其绚丽的生物荧光...\n\n" +
                "[AI助手: 小艾] 是的，AETHER！这些发光粒子是由它体内的发光蛋白质与" +
                "发光素氧化反应产生，用于在永夜中诱捕猎物以及迷惑天敌。在视频画面中，" +
                "右侧掠过的是长达三米的大王地表蠕虫。",
            date = "2026-06-18",
            bgImage = "https://picsum.photos/seed/ocean/1024/1024?blur=2",
            progress = 0f,
            played = false,
            favorite = true,
            createdAt = 1781740800000L
        ),
        PodcastEpisode(
            id = "sys_ep_2",
            title = "商业帝国的崛起：创新法则",
            summary = "深度剖析顶尖科技公司如何在几十年间保持持续创新，成长为全球最具价值的企业。",
            type = "text",
            channelName = "商业思维日课",
            content = "从最早的车库起家，到如今万亿市值，顶尖科技的成功不仅在于技术本身，" +
                "更在于他们对人性、设计以及极简主义的深刻理解。\n\n" +
                "【创新思维核心论点】\n" +
                "1. 极简主义设计：把复杂功能隐藏到最简界面之下。\n" +
                "2. 解决底层痛点：不仅是提供工具，更是重构生活方式。\n" +
                "3. 高频重构迭代：每天进行一小步优化，一年后产生指数级飞跃。\n\n" +
                "优秀的创作者应该通过对行业本质的理解，直击核心，拒绝无意义的复杂功能堆砌，" +
                "正如这款 AETHER 智能工具台的拟物物理卡片设计，让功能回归纯粹。",
            date = "2026-06-17",
            bgImage = "https://picsum.photos/seed/business/1024/1024?blur=2",
            progress = 0f,
            played = false,
            favorite = false,
            createdAt = 1781654400000L
        ),
        PodcastEpisode(
            id = "sys_ep_3",
            title = "极简舒缓氛围乐：森林晨曦",
            summary = "白噪音白绿背景白噪音合成，清晨森林木屋里的原木柴火燃烧、松针小鸟合鸣，静心冥想解压。",
            type = "audio",
            channelName = "自然白噪音疗愈馆",
            content = "【白噪音疗愈音频导聆：轻轻的虫鸣与火土柴柴噼啪响】\n\n" +
                "本专辑由森林原野真实采集的自然声响配以清晨柔和舒缓的合成乐背景混合而成。" +
                "适合作为您阅读、写作、午后小憩、或者沉静心灵的背景音乐合声。\n\n" +
                "请戴上高品质立体声耳机。左耳传来晨鸟从湿润枝头展翅的轻盈拍击，" +
                "右耳则是松风穿过密林的沙沙絮语。",
            date = "2026-06-16",
            bgImage = "https://picsum.photos/seed/forest/1024/1024?blur=4",
            progress = 100f,
            played = true,
            favorite = true,
            createdAt = 1781568000000L
        ),
        PodcastEpisode(
            id = "sys_ep_4",
            title = "今日全球科技速递",
            summary = "快速了解今日最重要的科技新闻，从AI突破到量子计算。",
            type = "text",
            channelName = "每日科技速递",
            content = "今天，科技界迎来了一个重磅消息。某知名实验室宣布在室温超导领域取得了突破性进展。" +
                "该新型超导结构是在全新的合金高压晶格中实现的，一旦其稳定性和常压表现得到长效验证，" +
                "人类将迎来输电零损耗与可控核聚变的爆发性跨越。",
            date = "2026-06-15",
            bgImage = "https://picsum.photos/seed/tech/1024/1024?blur=4",
            progress = 100f,
            played = true,
            favorite = false,
            qnaHistory = listOf(
                QnaItem(
                    "什么是室温超导？",
                    "室温超导是指在接近室温的条件下，材料的电阻降为零的现象."
                )
            ),
            createdAt = 1781481600000L
        )
    )
}
