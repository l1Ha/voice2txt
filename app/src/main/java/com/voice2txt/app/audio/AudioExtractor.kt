package com.voice2txt.app.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioExtractor {

    private const val TAG = "AudioExtractor"
    private const val TARGET_SAMPLE_RATE = 16000 // 16kHz standard for ASR models
    private const val TIMEOUT_US = 5000L

    data class ExtractedAudio(
        val pcmSamples: FloatArray,
        val durationMs: Long,
        val tempWavFile: File? = null
    )

    /**
     * Extracts audio track from any audio or video file Uri and decodes it to 16kHz mono FloatArray.
     */
    suspend fun extractAudio(
        context: Context,
        uri: Uri,
        onProgress: (Float) -> Unit = {}
    ): ExtractedAudio = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set data source for extractor", e)
            throw e
        }

        var audioTrackIndex = -1
        var inputFormat: MediaFormat? = null

        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                inputFormat = format
                break
            }
        }

        if (audioTrackIndex < 0 || inputFormat == null) {
            extractor.release()
            throw IllegalArgumentException("No audio track found in the selected media file.")
        }

        extractor.selectTrack(audioTrackIndex)

        val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
            inputFormat.getLong(MediaFormat.KEY_DURATION)
        } else {
            0L
        }
        val durationMs = durationUs / 1000

        val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: ""
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(inputFormat, null, null, 0)
        codec.start()

        val rawPcmStream = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var isEOS = false

        var originalSampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var originalChannelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        while (!isEOS) {
            val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: continue
                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    codec.queueInputBuffer(
                        inputBufferIndex, 0, 0, 0L,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    isEOS = true
                } else {
                    val sampleTime = extractor.sampleTime
                    codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, sampleTime, 0)
                    extractor.advance()

                    if (durationUs > 0) {
                        val progress = (sampleTime.toFloat() / durationUs).coerceIn(0f, 1f)
                        onProgress(progress * 0.4f) // Extraction accounts for first 40%
                    }
                }
            }

            var outputBufferIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            while (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null && info.size > 0) {
                    val chunk = ByteArray(info.size)
                    outputBuffer.position(info.offset)
                    outputBuffer.limit(info.offset + info.size)
                    outputBuffer.get(chunk)
                    rawPcmStream.write(chunk)
                }
                codec.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            }

            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                break
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        val pcmData = rawPcmStream.toByteArray()
        val floatSamples = convertPcmTo16kMono(
            rawBytes = pcmData,
            sourceSampleRate = originalSampleRate,
            sourceChannels = originalChannelCount
        )

        ExtractedAudio(
            pcmSamples = floatSamples,
            durationMs = durationMs
        )
    }

    /**
     * Converts raw 16-bit PCM bytes (possibly multi-channel, arbitrary sample rate) to 16kHz mono FloatArray.
     */
    private fun convertPcmTo16kMono(
        rawBytes: ByteArray,
        sourceSampleRate: Int,
        sourceChannels: Int
    ): FloatArray {
        if (rawBytes.isEmpty()) return FloatArray(0)

        val shortBuffer = ByteBuffer.wrap(rawBytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        val numShorts = shortBuffer.remaining()
        val numFrames = numShorts / sourceChannels

        // Downmix to mono first
        val monoSamples = FloatArray(numFrames)
        for (i in 0 until numFrames) {
            var sum = 0f
            for (c in 0 until sourceChannels) {
                sum += shortBuffer.get(i * sourceChannels + c)
            }
            // Normalize short (-32768..32767) to -1.0f..1.0f
            monoSamples[i] = (sum / sourceChannels) / 32768.0f
        }

        // Resample to 16000Hz if needed
        if (sourceSampleRate == TARGET_SAMPLE_RATE) {
            return monoSamples
        }

        val resampleRatio = TARGET_SAMPLE_RATE.toDouble() / sourceSampleRate.toDouble()
        val targetLength = (numFrames * resampleRatio).toInt()
        val resampled = FloatArray(targetLength)

        for (i in 0 until targetLength) {
            val srcIndex = i / resampleRatio
            val indexFloor = srcIndex.toInt()
            val fraction = (srcIndex - indexFloor).toFloat()

            if (indexFloor + 1 < monoSamples.size) {
                // Linear interpolation
                resampled[i] = monoSamples[indexFloor] * (1 - fraction) + monoSamples[indexFloor + 1] * fraction
            } else if (indexFloor < monoSamples.size) {
                resampled[i] = monoSamples[indexFloor]
            }
        }

        return resampled
    }
}
