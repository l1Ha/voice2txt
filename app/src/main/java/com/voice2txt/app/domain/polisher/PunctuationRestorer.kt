package com.voice2txt.app.domain.polisher

object PunctuationRestorer {

    private val QUESTION_PARTICLES = listOf(
        "吗", "呢", "么", "吧", "对不对", "好不好", "是不是", "可不可以", "行不行"
    )

    private val QUESTION_WORDS = listOf(
        "为什么", "怎么", "哪里", "如何", "怎样", "多少", "谁"
    )

    /**
     * Inspects sentence endings and content to restore appropriate punctuation.
     */
    fun restore(sentence: String): String {
        var trimmed = sentence.trim()
        if (trimmed.isEmpty()) return ""

        // Check if sentence ends with punctuation
        val lastChar = trimmed.last()
        val hasEndingPunctuation = lastChar in listOf('。', '？', '！', '!', '?', '.', ';', '；')

        if (!hasEndingPunctuation) {
            // Determine if it's an interrogative sentence
            val isQuestion = QUESTION_PARTICLES.any { trimmed.endsWith(it) } ||
                    QUESTION_WORDS.any { trimmed.contains(it) }

            trimmed = if (isQuestion) {
                "$trimmed？"
            } else {
                "$trimmed。"
            }
        } else {
            // Normalize English punctuation to full-width Chinese punctuation if text has Chinese characters
            val containsChinese = trimmed.any { it in '\u4e00'..'\u9fa5' }
            if (containsChinese) {
                trimmed = trimmed.replace("?", "？")
                    .replace("!", "！")
                    .replace(",", "，")
                    .replace(":", "：")
                    .replace(";", "；")
                if (trimmed.endsWith(".")) {
                    trimmed = trimmed.dropLast(1) + "。"
                }
            }
        }

        return trimmed
    }
}
