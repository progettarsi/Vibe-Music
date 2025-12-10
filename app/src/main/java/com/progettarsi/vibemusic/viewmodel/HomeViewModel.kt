package com.progettarsi.vibemusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.progettarsi.vibemusic.model.SongParser
import com.progettarsi.vibemusic.network.YouTubeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Stato della UI tipizzato: molto più pulito di avere variabili separate
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val data: List<Any>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel : ViewModel() {
    private val repository = YouTubeRepository()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchHomeContent()
    }

    fun fetchHomeContent() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val json = repository.getHomeContent()
                if (json != null) {
                    val parsed = SongParser.parseHomeContent(json)
                    _uiState.value = HomeUiState.Success(parsed)
                } else {
                    _uiState.value = HomeUiState.Error("Impossibile caricare la home")
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }
}