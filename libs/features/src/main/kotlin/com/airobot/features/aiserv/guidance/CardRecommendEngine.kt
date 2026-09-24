package com.airobot.features.aiserv.guidance

import com.airobot.features.aiserv.guidance.data.RecommendCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRecommendEngine @Inject constructor(
    private val cardRankingEngine: CardRankingEngine
) {
    companion object {
        const val MAX_CARDS = 3
    }

    // Registry for the models mapped to their tags
    private val modelRegistry = mutableMapOf<String, RecommendCard>()

    init {
        // Seed default service cards
        RecommendCard.defaultCards.forEach { card ->
            modelRegistry[card.tag] = card
        }
    }

    fun registerServiceCard(model: RecommendCard) {
        modelRegistry[model.tag] = model
    }

    fun getRecommendCards(): Flow<List<RecommendCard>> {
        return cardRankingEngine.getRankedTags().map { tags ->
            mapTagsToModels(tags)
        }
    }

    fun getRecommendCards(tags: List<String>): Flow<List<RecommendCard>> {
        return cardRankingEngine.getRankedTags(tags).map { sortedTags ->
            mapTagsToModels(sortedTags)
        }
    }

    private fun mapTagsToModels(tags: List<String>): List<RecommendCard> {
        return tags.mapNotNull { modelRegistry[it] }.take(MAX_CARDS)
    }

    // Proxy the usage metrics down to the ranking engine
    fun recordUsage(tag: String) {
        cardRankingEngine.recordUsage(tag)
    }

    fun markNewContent(tag: String, hasNew: Boolean) {
        cardRankingEngine.markNewContent(tag, hasNew)
    }
}
