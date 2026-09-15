package com.voice2txt.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voice2txt.app.asr.ModelManager
import com.voice2txt.app.domain.model.ModelConfig
import com.voice2txt.app.domain.polisher.TextPolisher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _models = MutableStateFlow<List<ModelConfig>>(emptyList())
    val models: StateFlow<List<ModelConfig>> = _models.asStateFlow()

    private val _downloadingModelId = MutableStateFlow<String?>(null)
    val downloadingModelId: StateFlow<String?> = _downloadingModelId.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _polishOptions = MutableStateFlow(TextPolisher.PolishOptions())
    val polishOptions: StateFlow<TextPolisher.PolishOptions> = _polishOptions.asStateFlow()

    init {
        refreshModels()
    }

    fun refreshModels() {
        val context = getApplication<Application>()
        _models.value = ModelManager.AVAILABLE_MODELS.map { model ->
            val isDownloaded = ModelManager.isModelDownloaded(context, model.id)
            model.copy(
                isDownloaded = isDownloaded,
                isSelected = model.id == "sense_voice_small"
            )
        }
    }

    fun downloadModel(model: ModelConfig) {
        viewModelScope.launch {
            _downloadingModelId.value = model.id
            _downloadProgress.value = 0f

            val success = ModelManager.downloadModel(
                context = getApplication(),
                modelConfig = model
            ) { progress ->
                _downloadProgress.value = progress
            }

            _downloadingModelId.value = null
            if (success) {
                refreshModels()
            }
        }
    }

    fun updatePolishOptions(options: TextPolisher.PolishOptions) {
        _polishOptions.value = options
    }
}
