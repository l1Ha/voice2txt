package com.voice2txt.app.data.repository

import android.content.Context
import com.voice2txt.app.data.db.TranscriptionDao
import com.voice2txt.app.data.db.TranscriptionEntity
import com.voice2txt.app.domain.model.ExportFormat
import com.voice2txt.app.domain.model.ExportTarget
import com.voice2txt.app.domain.model.TranscriptionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TranscriptionRepository(private val dao: TranscriptionDao) {

    fun getAllTranscriptions(): Flow<List<TranscriptionResult>> {
        return dao.getAllTranscriptions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getTranscriptionById(id: Long): TranscriptionResult? {
        return dao.getTranscriptionById(id)?.toDomain()
    }

    suspend fun saveTranscription(result: TranscriptionResult): Long {
        val entity = TranscriptionEntity.fromDomain(result)
        return dao.insertTranscription(entity)
    }

    suspend fun deleteTranscription(id: Long) {
        dao.deleteById(id)
    }

    /**
     * Exports transcription result into user-requested format and target text.
     */
    fun generateExportContent(
        result: TranscriptionResult,
        format: ExportFormat,
        target: ExportTarget
    ): String {
        return when (format) {
            ExportFormat.TXT -> {
                when (target) {
                    ExportTarget.RAW -> result.rawContent
                    ExportTarget.POLISHED -> result.polishedContent
                    ExportTarget.BOTH_SIDE_BY_SIDE -> buildDualTextComparison(result)
                }
            }
            ExportFormat.MARKDOWN -> {
                buildMarkdownDocument(result, target)
            }
            ExportFormat.SRT -> {
                buildSrtSubtitles(result, target)
            }
            ExportFormat.DUAL_COMPARE -> {
                buildDualTextComparison(result)
            }
            ExportFormat.JSON -> {
                com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result)
            }
        }
    }

    private fun buildDualTextComparison(result: TranscriptionResult): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sb.append("=========================================\n")
        sb.append("标题：${result.title}\n")
        sb.append("转写时间：${dateFormat.format(Date(result.createdAt))}\n")
        sb.append("时长：${result.durationMs / 1000} 秒 | 模型：${result.modelName}\n")
        sb.append("=========================================\n\n")

        sb.append("【智能润色版】\n")
        sb.append(result.polishedContent.ifBlank { "（无润色内容）" })
        sb.append("\n\n-----------------------------------------\n\n")

        sb.append("【原始转录版】\n")
        sb.append(result.rawContent.ifBlank { "（无原始内容）" })
        sb.append("\n\n=========================================\n")
        sb.append("【分句时间戳对照】\n\n")

        for (seg in result.segments) {
            sb.append("[${seg.formattedTimestamp}]\n")
            sb.append("  [原] ${seg.rawText}\n")
            sb.append("  [润] ${seg.polishedText}\n\n")
        }

        return sb.toString()
    }

    private fun buildMarkdownDocument(result: TranscriptionResult, target: ExportTarget): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sb.append("# ${result.title}\n\n")
        sb.append("> **转写时间**: ${dateFormat.format(Date(result.createdAt))}  \n")
        sb.append("> **音频时长**: ${result.durationMs / 1000}s | **ASR模型**: ${result.modelName}\n\n")

        when (target) {
            ExportTarget.POLISHED -> {
                sb.append("## 智能润色文本\n\n")
                sb.append(result.polishedContent)
            }
            ExportTarget.RAW -> {
                sb.append("## 原始转录文本\n\n")
                sb.append(result.rawContent)
            }
            ExportTarget.BOTH_SIDE_BY_SIDE -> {
                sb.append("## 智能润色版\n\n")
                sb.append(result.polishedContent)
                sb.append("\n\n---\n\n## 原始转录版\n\n")
                sb.append(result.rawContent)
                sb.append("\n\n---\n\n## 逐句双版本对照\n\n")
                sb.append("| 时间区间 | 原始转录 | 智能润色 |\n")
                sb.append("| :--- | :--- | :--- |\n")
                for (seg in result.segments) {
                    val rawClean = seg.rawText.replace("|", "\\|")
                    val polishedClean = seg.polishedText.replace("|", "\\|")
                    sb.append("| `${seg.formattedTimestamp}` | $rawClean | $polishedClean |\n")
                }
            }
        }

        return sb.toString()
    }

    private fun buildSrtSubtitles(result: TranscriptionResult, target: ExportTarget): String {
        val sb = StringBuilder()
        result.segments.forEachIndexed { index, seg ->
            sb.append("${index + 1}\n")
            sb.append("${seg.srtTimestamp}\n")
            val text = when (target) {
                ExportTarget.RAW -> seg.rawText
                ExportTarget.POLISHED -> seg.polishedText
                ExportTarget.BOTH_SIDE_BY_SIDE -> "${seg.polishedText}\n(${seg.rawText})"
            }
            sb.append("$text\n\n")
        }
        return sb.toString()
    }

    fun exportToFile(
        context: Context,
        result: TranscriptionResult,
        format: ExportFormat,
        target: ExportTarget
    ): File {
        val content = generateExportContent(result, format, target)
        val exportsDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportsDir.exists()) exportsDir.mkdirs()

        val safeTitle = result.title.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_")
        val fileName = "${safeTitle}_${System.currentTimeMillis()}.${format.extension}"
        val file = File(exportsDir, fileName)
        file.writeText(content)
        return file
    }
}
