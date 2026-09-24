package com.airobot.features.aiserv.guidance.data

import com.airobot.features.FeatureCards
import com.airobot.features.R

/**
 * RecommendCard — Data model for AI Recommend Cards.
 */
data class RecommendCard(
    val tag: String,
    val titleResId: Int,
    val contentResId: Int,
    val statusTipResId: Int,
    val iconResId: Int
) {
    companion object {
        val defaultCards = listOf(
            // Podcast Overlays
            RecommendCard(
                tag = FeatureCards.PODCAST,
                titleResId = R.string.card_podcast_title,
                contentResId = R.string.card_podcast_content,
                statusTipResId = R.string.card_podcast_tip,
                iconResId = com.airobot.framework.R.drawable.music
            ),
            RecommendCard(
                tag = FeatureCards.DIY_PODCAST,
                titleResId = R.string.card_podcast_diy_title,
                contentResId = R.string.card_podcast_diy_content,
                statusTipResId = R.string.card_podcast_diy_tip,
                iconResId = com.airobot.framework.R.drawable.palette
            ),

            // AiServ / Notepad Overlays
            RecommendCard(
                tag = FeatureCards.LOGBOOK,
                titleResId = R.string.card_notepad_title,
                contentResId = R.string.card_notepad_content,
                statusTipResId = R.string.card_notepad_tip,
                iconResId = com.airobot.framework.R.drawable.book
            ),

            // Clock Overlays
            RecommendCard(
                tag = FeatureCards.ALARM,
                titleResId = R.string.card_alarm_title,
                contentResId = R.string.card_alarm_content,
                statusTipResId = R.string.card_alarm_tip,
                iconResId = com.airobot.framework.R.drawable.alarm
            ),
            RecommendCard(
                tag = FeatureCards.TIMER,
                titleResId = R.string.card_timer_title,
                contentResId = R.string.card_timer_content,
                statusTipResId = R.string.card_timer_tip,
                iconResId = com.airobot.framework.R.drawable.timer
            ),
            RecommendCard(
                tag = FeatureCards.FOCUS,
                titleResId = R.string.card_focus_title_rec,
                contentResId = R.string.card_focus_content_rec,
                statusTipResId = R.string.card_focus_tip_rec,
                iconResId = com.airobot.framework.R.drawable.timer
            ),

            // Schedule Overlays
            RecommendCard(
                tag = FeatureCards.SCHEDULE_PLANNER,
                titleResId = R.string.card_schedule_title,
                contentResId = R.string.card_schedule_content,
                statusTipResId = R.string.card_schedule_tip,
                iconResId = com.airobot.framework.R.drawable.book
            ),

            // Static Clock Cards
            RecommendCard(
                tag = FeatureCards.CLOCK_HOME,
                titleResId = R.string.category_clock,
                contentResId = R.string.category_clock_subtitle,
                statusTipResId = R.string.clock_title,
                iconResId = com.airobot.framework.R.drawable.alarm
            ),
            RecommendCard(
                tag = FeatureCards.CLOCK_ALARM,
                titleResId = R.string.alarm_title,
                contentResId = R.string.alarm_subtitle,
                statusTipResId = R.string.alarm_default_message,
                iconResId = com.airobot.framework.R.drawable.alarm
            ),
            RecommendCard(
                tag = FeatureCards.CLOCK_TIMER,
                titleResId = R.string.timer_title,
                contentResId = R.string.timer_subtitle,
                statusTipResId = R.string.timer_msg,
                iconResId = com.airobot.framework.R.drawable.timer
            ),

            // Static Schedule Cards
            RecommendCard(
                tag = FeatureCards.SCHEDULE_HOME,
                titleResId = R.string.category_schedule,
                contentResId = R.string.category_schedule_subtitle,
                statusTipResId = R.string.schedule_today,
                iconResId = com.airobot.framework.R.drawable.book
            ),
            RecommendCard(
                tag = FeatureCards.SCHEDULE_BOARD,
                titleResId = R.string.schedule_board,
                contentResId = R.string.category_schedule_subtitle,
                statusTipResId = R.string.schedule_board,
                iconResId = com.airobot.framework.R.drawable.book
            ),
            RecommendCard(
                tag = FeatureCards.SCHEDULE_LIST,
                titleResId = R.string.schedule_list,
                contentResId = R.string.category_schedule_subtitle,
                statusTipResId = R.string.schedule_list,
                iconResId = com.airobot.framework.R.drawable.book
            ),

            // Static Podcast Cards
            RecommendCard(
                tag = FeatureCards.PODCAST_HOME,
                titleResId = R.string.category_podcast,
                contentResId = R.string.category_podcast_subtitle,
                statusTipResId = R.string.podcast_home,
                iconResId = com.airobot.framework.R.drawable.music
            ),
            RecommendCard(
                tag = FeatureCards.PODCAST_LIBRARY,
                titleResId = R.string.podcast_library,
                contentResId = R.string.category_podcast_subtitle,
                statusTipResId = R.string.podcast_library,
                iconResId = com.airobot.framework.R.drawable.music
            ),
            RecommendCard(
                tag = FeatureCards.PODCAST_SUBSCRIBE,
                titleResId = R.string.podcast_subscribe,
                contentResId = R.string.category_podcast_subtitle,
                statusTipResId = R.string.podcast_subscribe,
                iconResId = com.airobot.framework.R.drawable.palette
            )
        )
    }
}
