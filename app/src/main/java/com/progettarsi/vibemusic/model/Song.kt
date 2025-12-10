package com.progettarsi.vibemusic.model

// 1. Interfaccia comune
interface MusicItem {
    val title: String
    val subtitle: String
    val coverUrl: String
}

// 2. Canzone (Riproducibile)
data class Song(
    val videoId: String,
    override val title: String,
    val artist: String,
    override val coverUrl: String,
    val duration: Long = 0,
    val albumName: String? = null
) : MusicItem {
    // Mappa 'artist' su 'subtitle' per compatibilità con l'interfaccia
    override val subtitle: String get() = artist
}

// 3. Raccolta (Album, Playlist, Mix)
data class YTCollection(
    val id: String,          // browseId
    override val title: String,
    override val subtitle: String,
    override val coverUrl: String,
    val type: CollectionType
) : MusicItem

enum class CollectionType {
    ALBUM, PLAYLIST, MIX, UNKNOWN
}