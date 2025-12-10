package com.progettarsi.vibemusic.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.progettarsi.vibemusic.model.Song
import com.progettarsi.vibemusic.network.YouTubeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val repository = YouTubeRepository()
    private var searchJob: Job? = null

    // Usiamo liste osservabili specifiche per la ricerca
    var searchResults = mutableStateListOf<Song>()
    var isSearchingOnline by mutableStateOf(false)
    var searchError by mutableStateOf<String?>(null)

    // Testo della query (spostato qui da SearchState)
    var query by mutableStateOf("")

    fun updateQuery(newQuery: String) {
        query = newQuery
        performSearch(newQuery)
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            searchResults.clear()
            isSearchingOnline = false
            searchError = null
            return
        }

        searchJob = viewModelScope.launch {
            delay(800) // Debounce
            isSearchingOnline = true
            searchError = null
            searchResults.clear()

            try {
                val results = repository.searchSongs(query)
                if (results.isNotEmpty()) {
                    searchResults.addAll(results)
                } else {
                    searchError = "Nessun risultato"
                }
            } catch (e: Exception) {
                searchError = "Errore connessione"
            } finally {
                isSearchingOnline = false
            }
        }
    }

    fun clearSearch() {
        query = ""
        searchResults.clear()
    }
}