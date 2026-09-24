package com.airobot.features.aiserv.guidance.data

import com.airobot.features.FeatureCards
import com.airobot.features.R

/**
 * TipCard — Data model for AI Tip Banners.
 */
data class TipCard(
    val tag: String,
    val tipResId: Int
) {
    companion object {
        val defaultCards = listOf(
            TipCard(
                tag = FeatureCards.CLOCK_ALARM,
                tipResId = R.string.alarm_prompt_text
            ),
            TipCard(
                tag = FeatureCards.CLOCK_TIMER,
                tipResId = R.string.timer_prompt_text
            )
        )
    }
}
