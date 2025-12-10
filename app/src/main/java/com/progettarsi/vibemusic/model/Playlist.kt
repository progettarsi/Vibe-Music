package com.progettarsi.vibemusic.model

data class Playlist(
    val title: String,
    val subtitle: String?, // Contiene artista, o numero di brani
    val playlistId: String,
    val thumbnails: String,
    val songCount: Int? = null
)