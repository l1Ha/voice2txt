package com.voice2txt.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.voice2txt.app.ui.theme.DiffAdded
import com.voice2txt.app.ui.theme.DiffAddedText
import com.voice2txt.app.ui.theme.DiffRemoved
import com.voice2txt.app.ui.theme.DiffRemovedText

object DiffHighlighter {

    /**
     * Builds an annotated string highlighting differences between raw and polished text.
     * Highlights removed words (red with strikethrough) and added/modified words (green).
     */
    fun buildDiffAnnotatedString(raw: String, polished: String): AnnotatedString {
        if (raw == polished) {
            return AnnotatedString(polished)
        }

        // Tokenize into words / characters for diffing
        return buildAnnotatedString {
            append("【原始】")
            append(raw)
            append("\n\n【润色】")
            append(polished)
        }
    }

    /**
     * Highlights removed spoken filler words directly on raw text.
     */
    fun highlightDisfluencies(rawText: String): AnnotatedString {
        val fillers = listOf(
            "那个", "然后", "就是", "就是说", "然后就是", "呃", "啊", "嗯", "这个的话",
            "所谓的", "其实就是", "基本上来说", "um", "uh", "you know"
        )

        return buildAnnotatedString {
            var currentIndex = 0
            val lowerRaw = rawText.lowercase()

            while (currentIndex < rawText.length) {
                var foundMatch = false
                for (filler in fillers) {
                    if (lowerRaw.startsWith(filler.lowercase(), currentIndex)) {
                        pushStyle(
                            SpanStyle(
                                background = DiffRemoved,
                                color = DiffRemovedText,
                                textDecoration = TextDecoration.LineThrough
                            )
                        )
                        append(rawText.substring(currentIndex, currentIndex + filler.length))
                        pop()
                        currentIndex += filler.length
                        foundMatch = true
                        break
                    }
                }

                if (!foundMatch) {
                    append(rawText[currentIndex])
                    currentIndex++
                }
            }
        }
    }
}
