package com.airobot.features

import com.airobot.features.aiserv.notepad.AiNotepadProcessor
import com.airobot.features.aiserv.guidance.CardRankingEngine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FeatureCardType — Categorizes cards into popups (overlays) or static cards.
 */
enum class FeatureCardType {
    POPUP,   // 功能弹出服务卡片
    STATIC   // 功能模块静态卡片
}

/**
 * FeatureCard — Common data class defining a card's basic parameters.
 */
data class FeatureCard(
    val tag: String,
    val type: FeatureCardType,
    val basePriority: Int
)

/**
 * FeatureCards — Unified single source of truth for all cards in the features module.
 */
object FeatureCards {
    // Popup Service Cards
    const val ALARM = "ALARM"
    const val TIMER = "TIMER"
    const val FOCUS = "FOCUS"
    const val HOURLY_CHIME = "HOURLY_CHIME"
    const val CHIME = "CHIME"
    const val PODCAST = "PODCAST"
    const val DIY_PODCAST = "DIY_PODCAST"
    const val SCHEDULE_PLANNER = "SCHEDULE_PLANNER"
    const val LOGBOOK = "LOGBOOK"

    // Static Module Cards
    const val CLOCK_HOME = "CLOCK_HOME"
    const val CLOCK_ALARM = "CLOCK_ALARM"
    const val CLOCK_TIMER = "CLOCK_TIMER"
    const val SCHEDULE_HOME = "SCHEDULE_HOME"
    const val SCHEDULE_BOARD = "SCHEDULE_BOARD"
    const val SCHEDULE_LIST = "SCHEDULE_LIST"
    const val PODCAST_HOME = "PODCAST_HOME"
    const val PODCAST_LIBRARY = "PODCAST_LIBRARY"
    const val PODCAST_SUBSCRIBE = "PODCAST_SUBSCRIBE"

    private val allCards = listOf(
        FeatureCard(ALARM, FeatureCardType.POPUP, 75),
        FeatureCard(TIMER, FeatureCardType.POPUP, 65),
        FeatureCard(FOCUS, FeatureCardType.POPUP, 65),
        FeatureCard(HOURLY_CHIME, FeatureCardType.POPUP, 50),
        FeatureCard(CHIME, FeatureCardType.POPUP, 50),
        FeatureCard(PODCAST, FeatureCardType.POPUP, 70),
        FeatureCard(DIY_PODCAST, FeatureCardType.POPUP, 60),
        FeatureCard(SCHEDULE_PLANNER, FeatureCardType.POPUP, 65),
        FeatureCard(LOGBOOK, FeatureCardType.POPUP, 65),

        FeatureCard(CLOCK_HOME, FeatureCardType.STATIC, 80),
        FeatureCard(CLOCK_ALARM, FeatureCardType.STATIC, 75),
        FeatureCard(CLOCK_TIMER, FeatureCardType.STATIC, 65),
        FeatureCard(SCHEDULE_HOME, FeatureCardType.STATIC, 70),
        FeatureCard(SCHEDULE_BOARD, FeatureCardType.STATIC, 60),
        FeatureCard(SCHEDULE_LIST, FeatureCardType.STATIC, 65),
        FeatureCard(PODCAST_HOME, FeatureCardType.STATIC, 68),
        FeatureCard(PODCAST_LIBRARY, FeatureCardType.STATIC, 58),
        FeatureCard(PODCAST_SUBSCRIBE, FeatureCardType.STATIC, 52)
    )

    private val cardMap = allCards.associateBy { it.tag }

    fun getAll(): List<FeatureCard> = allCards
    fun get(tag: String): FeatureCard? = cardMap[tag]
}

/**
 * FeaturesModule — Unified entry point for eagerly initializing background
 * and application-scoped components inside the features module.
 */
@Singleton
class FeaturesModule @Inject constructor(
    private val aiNotepadProcessor: AiNotepadProcessor,
    private val cardRankingEngine: CardRankingEngine
) {
    /**
     * Backward-compatible initialize method.
     */
    fun initialize() {
        initialize(emptyList())
    }

    /**
     * Initializes all background processors and engines with the host application's supported cards.
     */
    fun initialize(supportedCardTags: List<String>) {
        aiNotepadProcessor.start()
        cardRankingEngine.setSupportedTags(supportedCardTags)
    }
}
