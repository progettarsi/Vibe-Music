package com.progettarsi.openmusic.viewmodel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.progettarsi.openmusic.model.Song
import com.progettarsi.openmusic.network.YouTubeRepository
import com.progettarsi.openmusic.service.MusicService
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {
    private val repository = YouTubeRepository()

    // Job per gestire la ricerca dinamica (Debouncing)
    private var searchJob: Job? = null

    // STATI
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var currentTitle by mutableStateOf("Nessuna Traccia")
    var currentArtist by mutableStateOf("Cerca una canzone...")
    var currentCoverUrl by mutableStateOf("")

    var progress by mutableFloatStateOf(0f)
    var duration by mutableFloatStateOf(1f)

    var searchResults = mutableStateListOf<Song>()
    var isSearchingOnline by mutableStateOf(false)
    // Nuovo stato per gestire errori di ricerca visivi
    var searchError by mutableStateOf<String?>(null)

    private var mediaController: MediaController? = null

    fun initPlayer(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
                mediaController?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                    override fun onPlaybackStateChanged(playbackState: Int) { isBuffering = (playbackState == Player.STATE_BUFFERING) }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        mediaItem?.mediaMetadata?.let {
                            if (!it.title.isNullOrEmpty()) currentTitle = it.title.toString()
                            if (!it.artist.isNullOrEmpty()) currentArtist = it.artist.toString()
                            if (it.artworkUri != null) currentCoverUrl = it.artworkUri.toString()
                        }
                        duration = mediaController?.duration?.toFloat()?.coerceAtLeast(1f) ?: 1f
                    }
                })
                startProgressUpdater()
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Errore init player: ${e.message}")
            }
        }, MoreExecutors.directExecutor())
    }

    // --- RICERCA DINAMICA ---
    fun search(query: String) {
        // 1. Cancella la ricerca precedente se l'utente sta ancora scrivendo
        searchJob?.cancel()

        if (query.isBlank()) {
            searchResults.clear()
            isSearchingOnline = false
            searchError = null
            return
        }

        // 2. Avvia una nuova ricerca con ritardo (Debounce)
        searchJob = viewModelScope.launch {
            delay(800) // Aspetta 800ms che l'utente finisca di scrivere

            isSearchingOnline = true
            searchError = null
            searchResults.clear() // Pulisci i vecchi risultati mentre carichi

            try {
                Log.d("MusicViewModel", "Avvio ricerca per: $query")
                val results = repository.searchSongs(query)

                if (results.isNotEmpty()) {
                    searchResults.addAll(results)
                } else {
                    searchError = "Nessun risultato trovato"
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Errore ricerca: ${e.message}")
                searchError = "Errore di connessione"
            } finally {
                // 3. Importante: Spegni SEMPRE il caricamento, anche se c'è errore
                isSearchingOnline = false
            }
        }
    }

    // ... dentro MusicViewModel ...

    fun playSong(song: Song) {
        // 1. Imposta subito i metadati visivi (così l'utente vede che hai cliccato)
        currentTitle = song.title
        currentArtist = song.artist
        currentCoverUrl = song.coverUrl
        isBuffering = true // Mostra caricamento

        viewModelScope.launch {
            // 2. Chiedi a YouTube l'URL vero
            val streamUrl = repository.getStreamUrl(song.videoId)

            if (streamUrl != null) {
                // 3. Se trovato, suona quello!
                val item = MediaItem.Builder()
                    .setUri(streamUrl) // URL REALE
                    .setMediaId(song.videoId)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setArtworkUri(Uri.parse(song.coverUrl))
                            .build()
                    )
                    .build()

                mediaController?.setMediaItem(item)
                mediaController?.prepare()
                mediaController?.play()
            } else {
                // Gestione Errore (es. canzone protetta/vevo)
                Log.e("MusicViewModel", "Impossibile riprodurre: URL non trovato")
                isBuffering = false // Togli caricamento
                // Qui potresti mostrare un Toast "Errore riproduzione"
            }
        }
    }

    fun playTestTrack() {
        val testCoverUrl = "https://i.ytimg.com/vi/0h8Z-L0J-PA/maxresdefault.jpg"
        val item = MediaItem.Builder()
            .setUri("https://storage.googleapis.com/exoplayer-test-media-0/Jazz_In_Paris.mp3")
            .setMediaMetadata(MediaMetadata.Builder().setTitle("Jazz in Paris").setArtist("Google Samples").setArtworkUri(Uri.parse(testCoverUrl)).build())
            .build()
        currentCoverUrl = testCoverUrl
        mediaController?.setMediaItem(item)
        mediaController?.prepare()
        mediaController?.play()
    }

    fun togglePlayPause() { if (isPlaying) mediaController?.pause() else mediaController?.play() }
    fun seekTo(value: Float) { mediaController?.seekTo((value * duration).toLong()) }

    private fun startProgressUpdater() {
        viewModelScope.launch {
            while (true) {
                if (mediaController != null) {
                    val currentPos = mediaController!!.currentPosition.toFloat()
                    val totalDur = mediaController!!.duration.toFloat()
                    if (totalDur > 0) { duration = totalDur; progress = currentPos / totalDur }
                }
                delay(500)
            }
        }
    }

    override fun onCleared() { mediaController?.release(); super.onCleared() }
}