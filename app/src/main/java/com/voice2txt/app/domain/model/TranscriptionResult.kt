package com.voice2txt.app.domain.model

data class TranscriptionResult(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val sourceUri: String,
    val mediaType: MediaType = MediaType.AUDIO,
    val durationMs: Long = 0L,
    val rawContent: String = "",
    val polishedContent: String = "",
    val segments: List<TranscriptionSegment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val modelName: String = "SenseVoice-Small"
)

enum class MediaType {
    AUDIO,
    VIDEO,
    RECORDING
}

enum class ExportFormat(val extension: String, val displayName: String, val mimeType: String) {
    MARKDOWN("md", "Markdown 文档 (.md)", "text/markdown"),
    TXT("txt", "纯文本 (.txt)", "text/plain"),
    SRT("srt", "SRT 视频字幕 (.srt)", "application/x-subrip"),
    VTT("vtt", "WebVTT 网页字幕 (.vtt)", "text/vtt"),
    LRC("lrc", "LRC 动态歌词/录音轴 (.lrc)", "text/plain"),
    DUAL_COMPARE("txt", "双版本逐句对照 (.txt)", "text/plain"),
    JSON("json", "结构化数据 (.json)", "application/json")
}

enum class ExportTarget {
    RAW,
    POLISHED,
    BOTH_SIDE_BY_SIDE
}
