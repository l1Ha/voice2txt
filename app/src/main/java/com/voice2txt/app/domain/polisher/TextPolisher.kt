package com.voice2txt.app.domain.polisher

import com.voice2txt.app.domain.model.TranscriptionSegment

object TextPolisher {

    data class PolishOptions(
        val removeFillers: Boolean = true,
        val removeStuttering: Boolean = true,
        val normalizeNumbers: Boolean = true,
        val restorePunctuation: Boolean = true,
        val autoParagraph: Boolean = true,
        val maxSentencesPerParagraph: Int = 3
    )

    /**
     * Polish a single segment's text using offline rules.
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

        if (options.restorePunctuation) {
            text = PunctuationRestorer.restore(text)
        }

        return text
    }

    /**
     * Polish all segments and generate the full coherent polished article/document.
     */
    fun polishSegments(
        segments: List<TranscriptionSegment>,
        options: PolishOptions = PolishOptions()
    ): List<TranscriptionSegment> {
        return segments.map { segment ->
            val polished = polishSegment(segment.rawText, options)
            segment.copy(polishedText = polished)
        }
    }

    /**
     * Combine segments into formatted full text with paragraphs.
     */
    fun buildFormattedDocument(
        segments: List<TranscriptionSegment>,
        usePolished: Boolean = true,
        options: PolishOptions = PolishOptions()
    ): String {
        val stringBuilder = java.lang.StringBuilder()
        var currentParagraphSentenceCount = 0

        for (segment in segments) {
            val text = if (usePolished) segment.polishedText else segment.rawText
            if (text.isBlank()) continue

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
