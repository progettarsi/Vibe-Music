package com.progettarsi.vibemusic.db.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.progettarsi.vibemusic.db.entities.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongsDao {
    // Inserisce o aggiorna una canzone (es. quando la riproduci o metti like)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity)

    // Ottieni tutte le canzoni preferite (Flusso reattivo per la UI)
    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavorites(): Flow<List<SongEntity>>

    // Controlla se una canzone è nei preferiti
    @Query("SELECT isFavorite FROM songs WHERE videoId = :videoId")
    fun isFavorite(videoId: String): Flow<Boolean>

    // Imposta Like/Dislike
    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE videoId = :videoId")
    suspend fun setFavorite(videoId: String, isFavorite: Boolean)

    // Cronologia recente
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT 50")
    fun getHistory(): Flow<List<SongEntity>>
}