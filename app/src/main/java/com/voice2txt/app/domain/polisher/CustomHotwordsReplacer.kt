package com.voice2txt.app.domain.polisher

object CustomHotwordsReplacer {

    data class ReplacementRule(
        val pattern: String,
        val replacement: String,
        val isRegex: Boolean = false
    )

    // Default common speech-to-text corrections for tech & names
    val DEFAULT_RULES = listOf(
        ReplacementRule("基哈布", "GitHub"),
        ReplacementRule("安卓", "Android"),
        ReplacementRule("声温", "声文"),
        ReplacementRule("派森", "Python"),
        ReplacementRule("考特林", "Kotlin"),
        ReplacementRule("端侧大模型", "端侧大模型"),
        ReplacementRule("自然语言处理", "NLP"),
        ReplacementRule("人工智能", "AI")
    )

    /**
     * Applies replacement rules to fix specialized domain terms, names, and homophones.
     */
    fun applyRules(text: String, rules: List<ReplacementRule>): String {
        if (text.isBlank() || rules.isEmpty()) return text

        var result = text
        for (rule in rules) {
            result = if (rule.isRegex) {
                try {
                    result.replace(Regex(rule.pattern), rule.replacement)
                } catch (e: Exception) {
                    result
                }
            } else {
                result.replace(rule.pattern, rule.replacement)
            }
        }
        return result
    }
}
