package com.voice2txt.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorder {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    @SuppressLint("MissingPermission")
    suspend fun startRecording(
        onSamplesRecorded: (FloatArray) -> Unit = {}
    ): FloatArray = withContext(Dispatchers.IO) {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize, 3200) // 100ms chunks

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        val byteStream = ByteArrayOutputStream()
        val audioBuffer = ShortArray(bufferSize / 2)
        audioRecord?.startRecording()
        isRecording = true

        while (isRecording && isActive) {
            val readCount = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1
            if (readCount > 0) {
                val byteBuf = ByteBuffer.allocate(readCount * 2).order(ByteOrder.LITTLE_ENDIAN)
                val floatChunk = FloatArray(readCount)
                for (i in 0 until readCount) {
                    byteBuf.putShort(audioBuffer[i])
                    floatChunk[i] = audioBuffer[i] / 32768.0f
                }
                byteStream.write(byteBuf.array())
                onSamplesRecorded(floatChunk)
            }
        }

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val allBytes = byteStream.toByteArray()
        val shortBuffer = ByteBuffer.wrap(allBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val totalFloats = FloatArray(shortBuffer.remaining())
        for (i in totalFloats.indices) {
            totalFloats[i] = shortBuffer.get() / 32768.0f
        }
        totalFloats
    }

    fun stopRecording() {
        isRecording = false
    }
}
