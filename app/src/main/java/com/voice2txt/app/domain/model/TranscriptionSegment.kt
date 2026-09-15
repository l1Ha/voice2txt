package com.voice2txt.app.domain.model

data class TranscriptionSegment(
    val id: Long = System.currentTimeMillis(),
    val index: Int = 0,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val rawText: String,
    val polishedText: String,
    val speakerId: String = "发言人 A",
    val isEdited: Boolean = false
) {
    val formattedTimestamp: String
        get() {
            val startMin = (startTimeMs / 1000) / 60
            val startSec = (startTimeMs / 1000) % 60
            val endMin = (endTimeMs / 1000) / 60
            val endSec = (endTimeMs / 1000) % 60
            return String.format("%02d:%02d -> %02d:%02d", startMin, startSec, endMin, endSec)
        }

    val srtTimestamp: String
        get() {
            fun formatTime(ms: Long): String {
                val hours = ms / 3600000
                val minutes = (ms % 3600000) / 60000
                val seconds = (ms % 60000) / 1000
                val millis = ms % 1000
                return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
            }
            return "${formatTime(startTimeMs)} --> ${formatTime(endTimeMs)}"
        }

    val vttTimestamp: String
        get() {
            fun formatTime(ms: Long): String {
                val hours = ms / 3600000
                val minutes = (ms % 3600000) / 60000
                val seconds = (ms % 60000) / 1000
                val millis = ms % 1000
                return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
            }
            return "${formatTime(startTimeMs)} --> ${formatTime(endTimeMs)}"
        }

    val lrcTimestamp: String
        get() {
            val minutes = (startTimeMs / 60000)
            val seconds = (startTimeMs % 60000) / 1000
            val hundredths = (startTimeMs % 1000) / 10
            return String.format("[%02d:%02d.%02d]", minutes, seconds, hundredths)
        }
}
