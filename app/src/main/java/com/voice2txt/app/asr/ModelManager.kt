package com.voice2txt.app.asr

import android.content.Context
import com.voice2txt.app.domain.model.ModelArchitecture
import com.voice2txt.app.domain.model.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object ModelManager {

    private val httpClient = OkHttpClient()

    val AVAILABLE_MODELS = listOf(
        ModelConfig(
            id = "sense_voice_small",
            name = "SenseVoice-Small (推荐)",
            description = "阿里巴巴 FunASR 优秀多语言端侧模型，中文、英文、粤语高精度，自带标点和情感标记，低延迟低功耗。",
            language = "中/英/粤/日/韩",
            modelType = ModelArchitecture.SENSE_VOICE,
            downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17.tar.bz2",
            expectedSizeMB = 145
        ),
        ModelConfig(
            id = "whisper_tiny",
            name = "Whisper-Tiny",
            description = "OpenAI Whisper 极速轻量化端侧模型，极小内存占用，适合低配设备及快速语音速记。",
            language = "多语言通用",
            modelType = ModelArchitecture.WHISPER,
            downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-tiny.tar.bz2",
            expectedSizeMB = 75
        ),
        ModelConfig(
            id = "whisper_base",
            name = "Whisper-Base",
            description = "OpenAI Whisper 均衡版，更高识别准确率，适合清晰长会议与演讲音频录音。",
            language = "多语言通用",
            modelType = ModelArchitecture.WHISPER,
            downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-base.tar.bz2",
            expectedSizeMB = 140
        )
    )

    fun getModelsDir(context: Context): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun isModelDownloaded(context: Context, modelId: String): Boolean {
        val modelFolder = File(getModelsDir(context), modelId)
        return modelFolder.exists() && (modelFolder.listFiles()?.isNotEmpty() == true)
    }

    fun getModelPath(context: Context, modelId: String): File {
        return File(getModelsDir(context), modelId)
    }

    /**
     * Downloads and extracts the model archive to the app internal models directory.
     */
    suspend fun downloadModel(
        context: Context,
        modelConfig: ModelConfig,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(getModelsDir(context), modelConfig.id)
            if (!targetDir.exists()) targetDir.mkdirs()

            val request = Request.Builder().url(modelConfig.downloadUrl).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false

                val body = response.body ?: return@withContext false
                val contentLength = body.contentLength()
                val tempFile = File(context.cacheDir, "${modelConfig.id}.download")

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                onProgress((totalRead.toFloat() / contentLength).coerceIn(0f, 1f))
                            }
                        }
                        output.flush()
                    }
                }

                // If zip, extract zip
                if (modelConfig.downloadUrl.endsWith(".zip")) {
                    extractZip(tempFile, targetDir)
                    tempFile.delete()
                } else {
                    // For archive format, move to target directory
                    val dest = File(targetDir, "model.onnx")
                    tempFile.renameTo(dest)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun extractZip(zipFile: File, destDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }
}
