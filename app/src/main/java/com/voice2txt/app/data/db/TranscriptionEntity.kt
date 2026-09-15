package com.voice2txt.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.voice2txt.app.domain.model.MediaType
import com.voice2txt.app.domain.model.TranscriptionResult
import com.voice2txt.app.domain.model.TranscriptionSegment

@Entity(tableName = "transcriptions")
data class TranscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sourceUri: String,
    val mediaType: String,
    val durationMs: Long,
    val rawContent: String,
    val polishedContent: String,
    val segmentsJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modelName: String = "SenseVoice-Small"
) {
    fun toDomain(): TranscriptionResult {
        val segmentListType = object : TypeToken<List<TranscriptionSegment>>() {}.type
        val segments: List<TranscriptionSegment> = try {
            Gson().fromJson(segmentsJson, segmentListType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return TranscriptionResult(
            id = id,
            title = title,
            sourceUri = sourceUri,
            mediaType = try { MediaType.valueOf(mediaType) } catch (e: Exception) { MediaType.AUDIO },
            durationMs = durationMs,
            rawContent = rawContent,
            polishedContent = polishedContent,
            segments = segments,
            createdAt = createdAt,
            modelName = modelName
        )
    }

    companion object {
        fun fromDomain(domain: TranscriptionResult): TranscriptionEntity {
            val segmentsJson = Gson().toJson(domain.segments)
            return TranscriptionEntity(
                id = if (domain.id > 1000000000000L) 0 else domain.id,
                title = domain.title,
                sourceUri = domain.sourceUri,
                mediaType = domain.mediaType.name,
                durationMs = domain.durationMs,
                rawContent = domain.rawContent,
                polishedContent = domain.polishedContent,
                segmentsJson = segmentsJson,
                createdAt = domain.createdAt,
                modelName = domain.modelName
            )
        }
    }
}
