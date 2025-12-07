package com.progettarsi.openmusic.model

data class Song(
    val videoId: String,       // L'ID univoco di YouTube (es. "dQw4w9WgXcQ")
    val title: String,         // Titolo canzone
    val artist: String,        // Artista
    val coverUrl: String,      // URL della copertina
    val duration: Long = 0L    // Durata in millisecondi (opzionale per ora)
)