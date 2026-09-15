package com.voice2txt.app.domain.model

data class ModelConfig(
    val id: String,
    val name: String,
    val description: String,
    val language: String,
    val modelType: ModelArchitecture,
    val downloadUrl: String,
    val expectedSizeMB: Int,
    val isDownloaded: Boolean = false,
    val localDir: String = "",
    val isSelected: Boolean = false
)

enum class ModelArchitecture {
    SENSE_VOICE,
    WHISPER,
    PARAKEET
}
