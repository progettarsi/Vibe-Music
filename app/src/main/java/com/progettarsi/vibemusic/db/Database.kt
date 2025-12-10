package com.progettarsi.vibemusic.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.progettarsi.vibemusic.db.daos.SongsDao
import com.progettarsi.vibemusic.db.entities.SongEntity

@Database(entities = [SongEntity::class], version = 1)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songsDao(): SongsDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "vibemusic_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}