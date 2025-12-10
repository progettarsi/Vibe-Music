package com.progettarsi.vibemusic.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.progettarsi.vibemusic.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val coverUrl: String,
    val albumName: String? = null,

    // Campi aggiuntivi utili
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val playCount: Int = 0
) {
    // Funzione helper per convertire da/a modello UI
    fun toModel(): Song {
        return Song(
            videoId = videoId,
            title = title,
            artist = artist,
            coverUrl = coverUrl,
            duration = duration,
            albumName = albumName
        )
    }
}

// Extension function per mappare il tuo modello Song in Entità Database
fun Song.toEntity(): SongEntity {
    return SongEntity(
        videoId = videoId,
        title = title,
        artist = artist,
        duration = duration,
        coverUrl = coverUrl,
        albumName = albumName
    )
}