package com.voice2txt.app.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object VadSegmenter {

    data class SpeechChunk(
        val index: Int,
        val startSample: Int,
        val endSample: Int,
        val startTimeMs: Long,
        val endTimeMs: Long,
        val samples: FloatArray
    )

    private const val FRAME_SIZE = 320 // 20ms at 16kHz
    private const val FRAME_SHIFT = 160 // 10ms hop
    private const val MIN_SPEECH_DURATION_MS = 500L
    private const val MAX_SPEECH_DURATION_MS = 15000L
    private const val SILENCE_HANGOVER_MS = 400L // 400ms silence marks end of sentence

    /**
     * Splits continuous 16kHz mono audio into natural speech chunks based on voice activity detection.
     * Prevents cutting words midway and filters out long periods of silence.
     */
    fun segmentSpeech(
        samples: FloatArray,
        sampleRate: Int = 16000
    ): List<SpeechChunk> {
        if (samples.isEmpty()) return emptyList()

        val totalFrames = (samples.size - FRAME_SIZE) / FRAME_SHIFT
        if (totalFrames <= 0) {
            return listOf(
                SpeechChunk(
                    index = 1,
                    startSample = 0,
                    endSample = samples.size,
                    startTimeMs = 0L,
                    endTimeMs = (samples.size * 1000L) / sampleRate,
                    samples = samples
                )
            )
        }

        // 1. Calculate frame energy
        val frameEnergies = FloatArray(totalFrames)
        var sumEnergy = 0.0
        for (f in 0 until totalFrames) {
            val offset = f * FRAME_SHIFT
            var energy = 0f
            for (i in 0 until FRAME_SIZE) {
                val s = samples[offset + i]
                energy += s * s
            }
            frameEnergies[f] = energy
            sumEnergy += energy
        }

        val avgEnergy = (sumEnergy / totalFrames).toFloat()
        // Adaptive threshold based on average energy
        val speechThreshold = max(0.0005f, avgEnergy * 0.25f)

        val chunks = mutableListOf<SpeechChunk>()
        var inSpeech = false
        var speechStartFrame = 0
        var silenceFramesCount = 0

        val maxSilenceFrames = ((SILENCE_HANGOVER_MS * sampleRate / 1000) / FRAME_SHIFT).toInt()
        val maxSpeechFrames = ((MAX_SPEECH_DURATION_MS * sampleRate / 1000) / FRAME_SHIFT).toInt()

        for (f in 0 until totalFrames) {
            val energy = frameEnergies[f]
            val isVoiced = energy > speechThreshold

            if (!inSpeech) {
                if (isVoiced) {
                    inSpeech = true
                    speechStartFrame = max(0, f - 2) // slight lookback
                    silenceFramesCount = 0
                }
            } else {
                val currentSpeechFrames = f - speechStartFrame
                if (!isVoiced) {
                    silenceFramesCount++
                    if (silenceFramesCount >= maxSilenceFrames) {
                        // End of speech sentence detected
                        val speechEndFrame = min(totalFrames, f - silenceFramesCount + 3)
                        emitChunk(chunks, samples, speechStartFrame, speechEndFrame, sampleRate)
                        inSpeech = false
                        silenceFramesCount = 0
                    }
                } else {
                    silenceFramesCount = 0
                }

                // Force cut if sentence is too long (> 15s)
                if (inSpeech && currentSpeechFrames >= maxSpeechFrames) {
                    emitChunk(chunks, samples, speechStartFrame, f, sampleRate)
                    speechStartFrame = f
                    silenceFramesCount = 0
                }
            }
        }

        // Handle trailing speech
        if (inSpeech) {
            emitChunk(chunks, samples, speechStartFrame, totalFrames, sampleRate)
        }

        // If VAD produced nothing (very quiet audio), fall back to uniform 10s chunks
        if (chunks.isEmpty()) {
            val defaultChunkSize = sampleRate * 10
            var s = 0
            var idx = 1
            while (s < samples.size) {
                val e = min(s + defaultChunkSize, samples.size)
                chunks.add(
                    SpeechChunk(
                        index = idx++,
                        startSample = s,
                        endSample = e,
                        startTimeMs = (s * 1000L) / sampleRate,
                        endTimeMs = (e * 1000L) / sampleRate,
                        samples = samples.copyOfRange(s, e)
                    )
                )
                s = e
            }
        }

        return chunks
    }

    private fun emitChunk(
        chunks: MutableList<SpeechChunk>,
        allSamples: FloatArray,
        startFrame: Int,
        endFrame: Int,
        sampleRate: Int
    ) {
        val startSample = max(0, startFrame * FRAME_SHIFT)
        val endSample = min(allSamples.size, endFrame * FRAME_SHIFT + FRAME_SIZE)
        val durationMs = ((endSample - startSample) * 1000L) / sampleRate

        if (durationMs >= MIN_SPEECH_DURATION_MS) {
            chunks.add(
                SpeechChunk(
                    index = chunks.size + 1,
                    startSample = startSample,
                    endSample = endSample,
                    startTimeMs = (startSample * 1000L) / sampleRate,
                    endTimeMs = (endSample * 1000L) / sampleRate,
                    samples = allSamples.copyOfRange(startSample, endSample)
                )
            )
        }
    }
}
