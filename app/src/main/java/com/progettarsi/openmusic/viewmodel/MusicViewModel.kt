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
import com.google.common.util.concurrent.MoreExecutors
import com.progettarsi.openmusic.model.Song
import com.progettarsi.openmusic.model.SongParser
import com.progettarsi.openmusic.network.YouTubeClient
import com.progettarsi.openmusic.network.YouTubeRepository
import com.progettarsi.openmusic.service.MusicService
import com.progettarsi.openmusic.utils.CookieManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {
    private val repository = YouTubeRepository()
    private var searchJob: Job? = null
    private var progressJob: Job? = null // Job specifico per la barra di avanzamento

    // Gestione Cookie e Stato Login
    private lateinit var cookieManager: CookieManager
    var isLoggedIn by mutableStateOf(false)
        private set

    // STATI PLAYER
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var currentTitle by mutableStateOf("Nessuna Traccia")
    var currentArtist by mutableStateOf("Cerca una canzone...")
    var currentCoverUrl by mutableStateOf("")
    var progress by mutableFloatStateOf(0f)
    var duration by mutableFloatStateOf(1f)
    var currentSong by mutableStateOf<Song?>(null)

    // STATI RICERCA
    var searchResults = mutableStateListOf<Song>()
    var isSearchingOnline by mutableStateOf(false)
    var searchError by mutableStateOf<String?>(null)

    // Stato Home
    var homeContent by mutableStateOf<List<Any>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var mediaController: MediaController? = null

    fun initPlayer(context: Context) {
        cookieManager = CookieManager(context)
        YouTubeClient.currentCookie = cookieManager.loadCookie()
        isLoggedIn = YouTubeClient.currentCookie.isNotBlank()

        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
                setupPlayerListeners()
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Errore init player: ${e.message}")
            }
        }, MoreExecutors.directExecutor())

        fetchHomeContent()
    }

    private fun setupPlayerListeners() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    startProgressUpdater()
                } else {
                    stopProgressUpdater()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                // Se finisce la canzone o si ferma, aggiorniamo lo stato finale
                if (playbackState == Player.STATE_ENDED) {
                    isPlaying = false
                    progress = 0f
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaMetadata?.let {
                    currentTitle = it.title.toString()
                    currentArtist = it.artist.toString()
                    currentCoverUrl = it.artworkUri.toString()

                    // Aggiorniamo anche l'oggetto Song corrente per coerenza UI
                    if (mediaItem.mediaId != "test") {
                        currentSong = Song(
                            videoId = mediaItem.mediaId,
                            title = currentTitle,
                            artist = currentArtist,
                            coverUrl = currentCoverUrl
                        )
                    }
                }
                updateDuration()
            }
        })
    }

    // --- LOGICA DI UPDATE PROGRESSO OTTIMIZZATA ---

    private fun startProgressUpdater() {
        // Cancella eventuali job precedenti per evitare duplicati
        stopProgressUpdater()

        progressJob = viewModelScope.launch {
            while (isActive && isPlaying) {
                mediaController?.let { controller ->
                    val currentPos = controller.currentPosition.toFloat()
                    val totalDur = controller.duration.toFloat()

                    if (totalDur > 0) {
                        duration = totalDur
                        progress = (currentPos / totalDur).coerceIn(0f, 1f)
                    }
                }
                // Aggiorniamo ogni secondo invece di 500ms, risparmia batteria
                // e l'occhio umano su una barra piccola non nota la differenza
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdater() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateDuration() {
        mediaController?.let {
            val totalDur = it.duration.toFloat()
            if (totalDur > 0) duration = totalDur
        }
    }

    // ---------------------------------------------

    fun fetchHomeContent() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val json = repository.getHomeContent()
                if (json != null) {
                    homeContent = SongParser.parseHomeContent(json)
                } else {
                    Log.e("MusicViewModel", "Home content vuoto o nullo")
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Errore fetch Home: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveLoginCookie(cookie: String) {
        viewModelScope.launch {
            cookieManager.saveCookie(cookie)
            YouTubeClient.currentCookie = cookie
            isLoggedIn = true
            fetchHomeContent()
        }
    }

    fun logout() {
        viewModelScope.launch {
            cookieManager.clearCookie()
            YouTubeClient.currentCookie = ""
            isLoggedIn = false
            searchResults.clear()
            homeContent = emptyList()
            fetchHomeContent()
        }
    }

    fun search(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            searchResults.clear()
            isSearchingOnline = false
            searchError = null
            return
        }

        searchJob = viewModelScope.launch {
            delay(800) // Debounce per non cercare mentre scrivi

            isSearchingOnline = true
            searchError = null
            searchResults.clear()

            try {
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
                isSearchingOnline = false
            }
        }
    }

    fun playSong(song: Song) {
        // UI Optimistic update: mostriamo subito i dati prima che carichi l'audio
        currentTitle = song.title
        currentArtist = song.artist
        currentCoverUrl = song.coverUrl
        currentSong = song
        isBuffering = true

        viewModelScope.launch {
            val streamUrl = repository.getStreamUrl(song.videoId)

            if (streamUrl != null) {
                val item = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaId(song.videoId)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setArtworkUri(Uri.parse(song.coverUrl))
                            .build()
                    )
                    .build()

                mediaController?.let {
                    it.setMediaItem(item)
                    it.prepare()
                    it.play()
                }
            } else {
                Log.e("MusicViewModel", "Impossibile riprodurre: URL non trovato")
                isBuffering = false
                // Qui potresti aggiungere uno stato di errore visibile all'utente
            }
        }
    }

    fun playTestTrack() {
        val testCoverUrl = "https://i.ytimg.com/vi/0h8Z-L0J-PA/maxresdefault.jpg"
        val testTitle = "Jazz in Paris"
        val testArtist = "Google Samples"

        currentCoverUrl = testCoverUrl
        currentTitle = testTitle
        currentArtist = testArtist
        currentSong = Song(videoId = "test", title = testTitle, artist = testArtist, coverUrl = testCoverUrl)

        val item = MediaItem.Builder()
            .setUri("https://storage.googleapis.com/exoplayer-test-media-0/Jazz_In_Paris.mp3")
            .setMediaId("test")
            .setMediaMetadata(MediaMetadata.Builder().setTitle(testTitle).setArtist(testArtist).setArtworkUri(Uri.parse(testCoverUrl)).build())
            .build()

        mediaController?.let {
            it.setMediaItem(item)
            it.prepare()
            it.play()
        }
    }

    fun togglePlayPause() {
        if (isPlaying) mediaController?.pause() else mediaController?.play()
    }

    fun seekTo(value: Float) {
        mediaController?.let {
            it.seekTo((value * duration).toLong())
            // Aggiorniamo subito la progress bar visiva per feedback immediato
            progress = value
        }
    }

    override fun onCleared() {
        stopProgressUpdater()
        mediaController?.release()
        super.onCleared()
    }
}