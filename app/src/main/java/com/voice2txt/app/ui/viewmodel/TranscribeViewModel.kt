package com.voice2txt.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voice2txt.app.Voice2TxtApplication
import com.voice2txt.app.asr.ModelManager
import com.voice2txt.app.asr.SherpaOnnxEngine
import com.voice2txt.app.audio.AudioExtractor
import com.voice2txt.app.audio.AudioRecorder
import com.voice2txt.app.audio.PcmAudioPlayer
import com.voice2txt.app.domain.model.ExportFormat
import com.voice2txt.app.domain.model.ExportTarget
import com.voice2txt.app.domain.model.MediaType
import com.voice2txt.app.domain.model.TranscriptionResult
import com.voice2txt.app.domain.polisher.TextPolisher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class TranscribeUiState {
    object Idle : TranscribeUiState()
    data class Processing(val stage: String, val progress: Float) : TranscribeUiState()
    data class Success(val result: TranscriptionResult) : TranscribeUiState()
    data class Error(val message: String) : TranscribeUiState()
}

class TranscribeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as Voice2TxtApplication).repository
    val audioPlayer = PcmAudioPlayer(application)
    private val audioRecorder = AudioRecorder()

    private val _uiState = MutableStateFlow<TranscribeUiState>(TranscribeUiState.Idle)
    val uiState: StateFlow<TranscribeUiState> = _uiState.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    var polishOptions = TextPolisher.PolishOptions()

    fun transcribeMediaFile(uri: Uri, mediaType: MediaType, title: String) {
        viewModelScope.launch {
            try {
                _uiState.value = TranscribeUiState.Processing("正在从媒体文件中提取音轨...", 0.05f)

                // 1. Extract audio to 16kHz mono PCM FloatArray
                val extracted = AudioExtractor.extractAudio(getApplication(), uri) { progress ->
                    _uiState.value = TranscribeUiState.Processing("正在解码音频...", progress)
                }

                audioPlayer.prepare(uri)

                // 2. Transcribe using offline ASR engine
                _uiState.value = TranscribeUiState.Processing("正在进行离线语音识别...", 0.45f)
                val activeModel = ModelManager.AVAILABLE_MODELS.first()
                val engine = SherpaOnnxEngine(getApplication(), activeModel)

                val rawSegments = engine.transcribe(extracted.pcmSamples) { progress, stage ->
                    _uiState.value = TranscribeUiState.Processing(stage, progress)
                }

                // 3. Polish text
                _uiState.value = TranscribeUiState.Processing("正在生成智能润色与双版本对照...", 0.95f)
                val polishedSegments = TextPolisher.polishSegments(rawSegments, polishOptions)
                val rawDocument = TextPolisher.buildFormattedDocument(rawSegments, usePolished = false, polishOptions)
                val polishedDocument = TextPolisher.buildFormattedDocument(polishedSegments, usePolished = true, polishOptions)

                val result = TranscriptionResult(
                    title = title,
                    sourceUri = uri.toString(),
                    mediaType = mediaType,
                    durationMs = extracted.durationMs,
                    rawContent = rawDocument,
                    polishedContent = polishedDocument,
                    segments = polishedSegments,
                    modelName = engine.engineName
                )

                // 4. Save to local Room database
                val savedId = repository.saveTranscription(result)
                val finalResult = result.copy(id = savedId)

                _uiState.value = TranscribeUiState.Success(finalResult)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = TranscribeUiState.Error(e.localizedMessage ?: "转写失败，请重试")
            }
        }
    }

    fun startLiveRecording() {
        viewModelScope.launch {
            _isRecording.value = true
            _uiState.value = TranscribeUiState.Processing("正在实时录音中...", 0f)
            val pcmFloats = audioRecorder.startRecording()
            // When stopped:
            if (pcmFloats.isNotEmpty()) {
                val activeModel = ModelManager.AVAILABLE_MODELS.first()
                val engine = SherpaOnnxEngine(getApplication(), activeModel)
                val rawSegments = engine.transcribe(pcmFloats)
                val polishedSegments = TextPolisher.polishSegments(rawSegments, polishOptions)
                val rawDocument = TextPolisher.buildFormattedDocument(rawSegments, usePolished = false)
                val polishedDocument = TextPolisher.buildFormattedDocument(polishedSegments, usePolished = true)

                val result = TranscriptionResult(
                    title = "录音速记_${System.currentTimeMillis() / 1000}",
                    sourceUri = "",
                    mediaType = MediaType.RECORDING,
                    durationMs = (pcmFloats.size.toLong() * 1000) / 16000,
                    rawContent = rawDocument,
                    polishedContent = polishedDocument,
                    segments = polishedSegments
                )
                repository.saveTranscription(result)
                _uiState.value = TranscribeUiState.Success(result)
            }
        }
    }

    fun stopLiveRecording() {
        audioRecorder.stopRecording()
        _isRecording.value = false
    }

    fun exportResult(result: TranscriptionResult, format: ExportFormat, target: ExportTarget): File {
        return repository.exportToFile(getApplication(), result, format, target)
    }

    fun resetState() {
        _uiState.value = TranscribeUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
