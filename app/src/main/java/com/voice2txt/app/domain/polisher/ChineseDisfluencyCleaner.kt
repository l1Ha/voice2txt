package com.voice2txt.app.domain.polisher

object ChineseDisfluencyCleaner {

    // Common Chinese spoken filler words / vocalized pauses
    private val FILLER_WORDS = listOf(
        "就是说", "然后就是", "也就是说", "其实就是", "这个的话", "怎么说呢",
        "大概就是", "基本上来说", "所谓的", "然后呢", "那什么", "我的意思是",
        "呃", "啊", "嗯", "唔", "诶", "噢", "哎", "吧", "哈"
    )

    // Contextual filler patterns that occur at the start of a clause or between pauses
    private val CLAUSE_INITIAL_FILLERS = listOf(
        Regex("^(那个|就是|然后|这这个|那那个)[，,]?"),
        Regex("([，。！？；\n])(那个|就是|然后|这这个|那那个)[，,]?")
    )

    // Consecutive character / word repetition (stuttering: "我我我", "这个这个")
    private val REPEATED_CHAR_REGEX = Regex("([\\u4e00-\\u9fa5a-zA-Z0-9])\\1{1,4}")
    private val REPEATED_TWO_CHAR_REGEX = Regex("([\\u4e00-\\u9fa5]{2})\\1{1,2}")

    // English filler words
    private val ENGLISH_FILLERS_REGEX = Regex("(?i)\\b(um|uh|er|ah|you know|sort of|kind of|like,)\\b[ ,]?")

    /**
     * Clean disfluencies, stuttering, and filler words from raw transcribed text.
     */
    fun clean(text: String, aggressive: Boolean = false): String {
        if (text.isBlank()) return text

        var result = text

        // 1. Remove obvious standalone vocalized pauses (呃、嗯、啊) followed or surrounded by punctuation
        result = result.replace(Regex("[，,]?[\\s]*(呃|啊|嗯|唔|诶)+[\\s]*[，,]?"), "，")

        // 2. Remove English filler words
        result = result.replace(ENGLISH_FILLERS_REGEX, "")

        // 3. Remove clause-initial filler words
        for (pattern in CLAUSE_INITIAL_FILLERS) {
            result = result.replace(pattern) { matchResult ->
                if (matchResult.groupValues.size > 2) {
                    matchResult.groupValues[1] // Keep preceding punctuation
                } else {
                    ""
                }
            }
        }

        // 4. Remove 2-char stutter repetitions (e.g. "我们我们" -> "我们", "这个这个" -> "这个")
        result = result.replace(REPEATED_TWO_CHAR_REGEX) { it.groupValues[1] }

        // 5. Remove 1-char stutter repetitions (e.g. "我我我" -> "我", "你你" -> "你")
        result = result.replace(REPEATED_CHAR_REGEX) { it.groupValues[1] }

        // 6. If aggressive mode enabled, clean remaining filler particles
        if (aggressive) {
            for (filler in FILLER_WORDS) {
                if (filler.length > 1) {
                    result = result.replace("，$filler，", "，")
                    result = result.replace("，$filler", "，")
                }
            }
        }

        // Clean up any double punctuation produced by removals
        result = result.replace(Regex("，{2,}"), "，")
            .replace(Regex("，。"), "。")
            .replace(Regex("^[，,、]"), "")
            .trim()

        return result
    }
}
