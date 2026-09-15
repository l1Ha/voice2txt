package com.voice2txt.app.domain.polisher

import com.voice2txt.app.domain.model.TranscriptionSegment

object TextPolisher {

    data class PolishOptions(
        val removeFillers: Boolean = true,
        val removeStuttering: Boolean = true,
        val normalizeNumbers: Boolean = true,
        val restorePunctuation: Boolean = true,
        val autoParagraph: Boolean = true,
        val maxSentencesPerParagraph: Int = 3,
        val applyCustomRules: Boolean = true,
        val customRules: List<CustomHotwordsReplacer.ReplacementRule> = CustomHotwordsReplacer.DEFAULT_RULES,
        val autoSpeakerDiarization: Boolean = true
    )

    /**
     * Polish a single segment's text using offline rules and custom hotwords.
     */
    fun polishSegment(rawText: String, options: PolishOptions = PolishOptions()): String {
        var text = rawText.trim()
        if (text.isEmpty()) return ""

        if (options.removeFillers || options.removeStuttering) {
            text = ChineseDisfluencyCleaner.clean(text)
        }

        if (options.normalizeNumbers) {
            text = NumberNormalizer.normalize(text)
        }

        if (options.applyCustomRules && options.customRules.isNotEmpty()) {
            text = CustomHotwordsReplacer.applyRules(text, options.customRules)
        }

        if (options.restorePunctuation) {
            text = PunctuationRestorer.restore(text)
        }

        return text
    }

    /**
     * Polish all segments and generate speaker tags and full coherent polished document.
     */
    fun polishSegments(
        segments: List<TranscriptionSegment>,
        options: PolishOptions = PolishOptions()
    ): List<TranscriptionSegment> {
        var currentSpeaker = "发言人 A"
        var lastEndTimeMs = 0L

        return segments.mapIndexed { index, segment ->
            // If pause between last segment and this segment > 1.8 seconds, toggle speaker
            if (options.autoSpeakerDiarization && index > 0) {
                val pauseDuration = segment.startTimeMs - lastEndTimeMs
                if (pauseDuration >= 1800) {
                    currentSpeaker = if (currentSpeaker == "发言人 A") "发言人 B" else "发言人 A"
                }
            }
            lastEndTimeMs = segment.endTimeMs

            val polished = polishSegment(segment.rawText, options)
            segment.copy(
                polishedText = polished,
                speakerId = currentSpeaker
            )
        }
    }

    /**
     * Combine segments into formatted full text with paragraphs and optional speaker tags.
     */
    fun buildFormattedDocument(
        segments: List<TranscriptionSegment>,
        usePolished: Boolean = true,
        includeSpeakerNames: Boolean = false,
        options: PolishOptions = PolishOptions()
    ): String {
        val stringBuilder = java.lang.StringBuilder()
        var currentParagraphSentenceCount = 0
        var currentSpeaker = ""

        for (segment in segments) {
            val text = if (usePolished) segment.polishedText else segment.rawText
            if (text.isBlank()) continue

            if (includeSpeakerNames && segment.speakerId != currentSpeaker) {
                if (stringBuilder.isNotEmpty()) stringBuilder.append("\n\n")
                stringBuilder.append("【${segment.speakerId}】\n")
                currentSpeaker = segment.speakerId
                currentParagraphSentenceCount = 0
            }

            stringBuilder.append(text)

            // Auto-paragraph logic
            if (options.autoParagraph) {
                if (text.endsWith("。") || text.endsWith("！") || text.endsWith("？") || text.endsWith(".")) {
                    currentParagraphSentenceCount++
                    if (currentParagraphSentenceCount >= options.maxSentencesPerParagraph) {
                        stringBuilder.append("\n\n")
                        currentParagraphSentenceCount = 0
                    } else {
                        stringBuilder.append(" ")
                    }
                } else {
                    stringBuilder.append(" ")
                }
            } else {
                stringBuilder.append("\n")
            }
        }

        return stringBuilder.toString().trim()
    }
}
