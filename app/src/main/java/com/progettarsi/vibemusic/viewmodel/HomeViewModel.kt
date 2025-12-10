package com.progettarsi.vibemusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.progettarsi.vibemusic.model.MusicItem
import com.progettarsi.vibemusic.network.YouTubeRepository
import com.progettarsi.vibemusic.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    // CORREZIONE: Ora usa List<MusicItem> invece di List<Any>
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

            // Resource<List<MusicItem>>
            val result = repository.getHomeContent()

            _uiState.value = when (result) {
                // Il cast <*> dice a Kotlin di fidarsi del tipo generico
                is Resource.Success<*> -> {
                    // Siccome result.data è List<MusicItem>, il cast è sicuro
                    @Suppress("UNCHECKED_CAST")
                    val items = result.data as? List<MusicItem> ?: emptyList()
                    HomeUiState.Success(items)
                }
                is Resource.Error<*> -> {
                    HomeUiState.Error(result.message ?: "Errore sconosciuto")
                }
                is Resource.Loading<*> -> {
                    HomeUiState.Loading
                }
            }
        }
    }
}