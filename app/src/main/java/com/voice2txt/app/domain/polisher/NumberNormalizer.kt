package com.voice2txt.app.domain.polisher

object NumberNormalizer {

    private val DIGITS_MAP = mapOf(
        '零' to 0L, '一' to 1L, '二' to 2L, '两' to 2L, '三' to 3L,
        '四' to 4L, '五' to 5L, '六' to 6L, '七' to 7L, '八' to 8L, '九' to 9L
    )

    // Normalize years e.g. 二零二六年 -> 2026年
    private val YEAR_REGEX = Regex("([零一二两三四五六七八九]{4})年")

    // Normalize percentages e.g. 百分之八十 -> 80%
    private val PERCENT_REGEX = Regex("百分之([零一二两三四五六七八九十百]+)")

    // Normalize ordinal numbers e.g. 第一百二十三 -> 第123
    private val ORDINAL_REGEX = Regex("第([零一二两三四五六七八九十百千万]+)")

    /**
     * Normalizes spoken numbers into standardized Arabic numerals.
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return text

        var result = text

        // 1. Year conversion
        result = result.replace(YEAR_REGEX) { match ->
            val yearStr = match.groupValues[1].map { DIGITS_MAP[it]?.toString() ?: it.toString() }.joinToString("")
            "${yearStr}年"
        }

        // 2. Percentage conversion
        result = result.replace(PERCENT_REGEX) { match ->
            val num = parseChineseNumber(match.groupValues[1])
            "${num}%"
        }

        // 3. Ordinal conversion
        result = result.replace(ORDINAL_REGEX) { match ->
            val num = parseChineseNumber(match.groupValues[1])
            "第${num}"
        }

        return result
    }

    private fun parseChineseNumber(chinese: String): Long {
        var total = 0L
        var currentSection = 0L
        var currentNumber = 0L

        for (ch in chinese) {
            when (ch) {
                in DIGITS_MAP.keys -> {
                    currentNumber = DIGITS_MAP[ch] ?: 0L
                }
                '十' -> {
                    if (currentNumber == 0L) currentNumber = 1L
                    currentSection += currentNumber * 10L
                    currentNumber = 0L
                }
                '百' -> {
                    currentSection += currentNumber * 100L
                    currentNumber = 0L
                }
                '千' -> {
                    currentSection += currentNumber * 1000L
                    currentNumber = 0L
                }
                '万' -> {
                    total += (currentSection + currentNumber) * 10000L
                    currentSection = 0L
                    currentNumber = 0L
                }
                '亿' -> {
                    total += (currentSection + currentNumber) * 100000000L
                    currentSection = 0L
                    currentNumber = 0L
                }
            }
        }
        return total + currentSection + currentNumber
    }
}
