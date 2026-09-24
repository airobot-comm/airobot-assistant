package com.airobot.agent.audio.tools.kws

import android.util.Log
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination

/**
 * Text2Token converter.
 * Used to convert text into the token sequence (pinyin or phonemes) required by the model.
 * This implementation combines predefined multi-tone dictionary and rule-based fallback conversion, with strong validation functionality.
 */
object Text2Token {
    private const val TAG = "Text2Token"

    private val initials = listOf(
        "zh", "ch", "sh", "b", "p", "m", "f", "d", "t", "n", "l",
        "g", "k", "h", "j", "q", "x", "r", "z", "c", "s", "y", "w"
    )

    /**
     * Converts text into one or more token variations.
     * @param text Input text
     * @param validTokensSet Set of valid tokens loaded from tokens.txt
     * @return List of validated token sequences
     */
    fun convertToTokensVariations(text: String, validTokensSet: Set<String>): List<String> {
        val variations = mutableListOf<String>()

        // Dynamically convert using pinyin4j
        val dynamicSequence = convertDynamically(text)
        if (dynamicSequence.isNotEmpty() && isValidTokenSequence(dynamicSequence, validTokensSet)) {
            variations.add(dynamicSequence)

            // Add a light tone variant without tone marks to improve KWS recognition rates
            val lightToneSequence =
                dynamicSequence.replace(Regex("[āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜ]")) { matchResult ->
                    when (matchResult.value) {
                        "ā", "á", "ǎ", "à" -> "a"
                        "ē", "é", "ě", "è" -> "e"
                        "ī", "í", "ǐ", "ì" -> "i"
                        "ō", "ó", "ǒ", "ò" -> "o"
                        "ū", "ú", "ǔ", "ù" -> "u"
                        "ǖ", "ǘ", "ǚ", "ǜ" -> "ü"
                        else -> matchResult.value
                    }
                }
            if (lightToneSequence != dynamicSequence && isValidTokenSequence(
                    lightToneSequence,
                    validTokensSet
                )
            ) {
                variations.add(lightToneSequence)
            }
        }

        if (variations.isEmpty()) {
            Log.w(TAG, "Warning: Failed to generate valid tokens for text: $text")
        }

        return variations
    }

    private fun isValidTokenSequence(sequence: String, validTokensSet: Set<String>): Boolean {
        if (validTokensSet.isEmpty()) return true // Skip validation if the validation set is empty
        val tokens = sequence.split(" ").filter { it.isNotBlank() }
        for (token in tokens) {
            if (!validTokensSet.contains(token)) {
                Log.w(TAG, "Invalid token detected: '$token'. Sequence rejected.")
                return false
            }
        }
        return true
    }

    private fun convertDynamically(chineseText: String): String {
        val format = HanyuPinyinOutputFormat().apply {
            caseType = HanyuPinyinCaseType.LOWERCASE
            toneType = HanyuPinyinToneType.WITH_TONE_MARK
            vCharType = HanyuPinyinVCharType.WITH_U_UNICODE // "ü"
        }

        val tokens = mutableListOf<String>()

        for (c in chineseText) {
            if (c.toString().matches(Regex("[\\u4E00-\\u9FA5]+"))) {
                try {
                    val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format)
                    if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                        val pinyin = pinyinArray[0]
                        splitPinyin(pinyin, tokens)
                    }
                } catch (e: BadHanyuPinyinOutputFormatCombination) {
                    e.printStackTrace()
                }
            } else {
                // Ignore or handle English if needed
            }
        }

        return tokens.joinToString(" ")
    }

    private fun splitPinyin(pinyin: String, tokens: MutableList<String>) {
        var matchedInitial: String? = null
        for (initial in initials) {
            if (pinyin.startsWith(initial)) {
                matchedInitial = initial
                break
            }
        }

        if (matchedInitial != null) {
            tokens.add(matchedInitial)
            val finalPart = pinyin.substring(matchedInitial.length)
            if (finalPart.isNotEmpty()) {
                tokens.add(finalPart)
            }
        } else {
            if (pinyin.isNotEmpty()) {
                tokens.add(pinyin)
            }
        }
    }
}
