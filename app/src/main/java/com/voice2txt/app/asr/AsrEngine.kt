package com.voice2txt.app.asr

import com.voice2txt.app.domain.model.TranscriptionSegment

interface AsrEngine {
    val engineName: String
    val isReady: Boolean

    suspend fun transcribe(
        samples: FloatArray,
        sampleRate: Int = 16000,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): List<TranscriptionSegment>

    fun release()
}
