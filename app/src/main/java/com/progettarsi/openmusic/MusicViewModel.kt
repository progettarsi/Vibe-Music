package com.progettarsi.openmusic.viewmodel

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.progettarsi.openmusic.service.MusicService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {

    // STATI UI (Osservabili da MusicPlayer e Dock)
    var isPlaying by mutableStateOf(false)
    var currentTitle by mutableStateOf("Nessuna Traccia")
    var currentArtist by mutableStateOf("Scegli una canzone")
    var currentCoverUrl by mutableStateOf("") // URL Copertina
    var progress by mutableFloatStateOf(0f)
    var duration by mutableFloatStateOf(1f)

    private var mediaController: MediaController? = null

    // Inizializza la connessione col Service
    fun initPlayer(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            setupPlayerListeners()
        }, MoreExecutors.directExecutor())

        // Loop per aggiornare la barra di progresso
        startProgressUpdater()
    }

    private fun setupPlayerListeners() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                // Qui aggiorneremo titolo/artista dai metadati della canzone
                mediaItem?.mediaMetadata?.let {
                    currentTitle = it.title.toString()
                    currentArtist = it.artist.toString()
                    currentCoverUrl = it.artworkUri.toString()
                }
                duration = mediaController?.duration?.toFloat() ?: 1f
            }
        })
    }

    // COMANDI
    fun togglePlayPause() {
        if (isPlaying) mediaController?.pause() else mediaController?.play()
    }

    fun seekTo(value: Float) {
        val position = (value * duration).toLong()
        mediaController?.seekTo(position)
    }

    fun next() = mediaController?.seekToNext()
    fun prev() = mediaController?.seekToPrevious()

    // SIMULAZIONE CARICAMENTO TRACCIA (Per ora usiamo un MP3 di test)
    fun playTestTrack() {
        val item = MediaItem.fromUri("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
        mediaController?.setMediaItem(item)
        mediaController?.prepare()
        mediaController?.play()

        // Simuliamo metadati per la UI
        currentTitle = "Test Song Online"
        currentArtist = "SoundHelix"
    }

    private fun startProgressUpdater() {
        viewModelScope.launch {
            while (true) {
                if (isPlaying && mediaController != null) {
                    val current = mediaController!!.currentPosition.toFloat()
                    val total = mediaController!!.duration.toFloat().coerceAtLeast(1f)
                    progress = current / total
                }
                delay(1000) // Aggiorna ogni secondo
            }
        }
    }

    override fun onCleared() {
        mediaController?.release()
        super.onCleared()
    }
}