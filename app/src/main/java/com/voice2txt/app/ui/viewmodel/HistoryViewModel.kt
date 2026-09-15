package com.voice2txt.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voice2txt.app.Voice2TxtApplication
import com.voice2txt.app.domain.model.TranscriptionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as Voice2TxtApplication).repository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val transcriptions: StateFlow<List<TranscriptionResult>> = repository.getAllTranscriptions()
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) list
            else list.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.polishedContent.contains(query, ignoreCase = true) ||
                        it.rawContent.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteTranscription(id: Long) {
        viewModelScope.launch {
            repository.deleteTranscription(id)
        }
    }
}
