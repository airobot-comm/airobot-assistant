package com.airobot.features.aiserv.guidance

import com.airobot.features.aiserv.guidance.data.TipCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TipEngine @Inject constructor(
    private val cardRankingEngine: CardRankingEngine
) {
    // Registry for tip models mapped to their tags
    private val tipRegistry = mutableMapOf<String, TipCard>()

    init {
        // Seed default tips
        TipCard.defaultCards.forEach { card ->
            tipRegistry[card.tag] = card
        }
    }

    fun registerTipCard(model: TipCard) {
        tipRegistry[model.tag] = model
    }

    fun updateTipContent(tag: String, tipResId: Int) {
        val existing = tipRegistry[tag]
        if (existing != null) {
            tipRegistry[tag] = existing.copy(tipResId = tipResId)
        }
    }

    fun getTipCards(tags: List<String>): Flow<List<TipCard>> {
        return cardRankingEngine.getRankedTags(tags).map { sortedTags ->
            sortedTags.mapNotNull { tipRegistry[it] }
        }
    }
}
