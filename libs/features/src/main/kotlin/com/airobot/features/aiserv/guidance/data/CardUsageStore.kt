package com.airobot.features.aiserv.guidance.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class CardUsageRecord(
    val usageCount: Int = 0,
    val lastUsedTimestamp: Long = 0L,
    val hasNewContent: Boolean = false
)

@Singleton
class CardUsageStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        "airobot_card_recommendation_prefs",
        Context.MODE_PRIVATE
    )
    private val json = Json { ignoreUnknownKeys = true }

    fun getUsageRecord(tag: String): CardUsageRecord {
        val jsonStr = sharedPrefs.getString("record_$tag", null) ?: return CardUsageRecord()
        return try {
            json.decodeFromString<CardUsageRecord>(jsonStr)
        } catch (e: Exception) {
            CardUsageRecord()
        }
    }

    fun saveUsageRecord(tag: String, record: CardUsageRecord) {
        val jsonStr = json.encodeToString(record)
        sharedPrefs.edit().putString("record_$tag", jsonStr).apply()
    }

    fun getAllRecords(tags: List<String>): Map<String, CardUsageRecord> {
        return tags.associateWith { getUsageRecord(it) }
    }
}
