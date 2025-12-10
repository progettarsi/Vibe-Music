package com.progettarsi.vibemusic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Classe per gestire lo stato della ricerca
class SearchState {
    var isSearching by mutableStateOf(false) // <-- CORRETTO: tutto attaccato
    var query by mutableStateOf("")          // Testo scritto
}

// Dati finti per i suggerimenti
val searchSuggestions = listOf(
    "Esempio 1", "Esempio 2", "Esempio 3",
    "Esempio 4", "Esempio 5", "Esempio 6"
)