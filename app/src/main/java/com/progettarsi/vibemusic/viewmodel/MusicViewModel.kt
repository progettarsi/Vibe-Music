package com.progettarsi.vibemusic.viewmodel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.progettarsi.vibemusic.model.Song
import com.progettarsi.vibemusic.network.YouTubeClient
import com.progettarsi.vibemusic.network.YouTubeRepository
import com.progettarsi.vibemusic.service.MusicService
import com.progettarsi.vibemusic.utils.CookieManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {
    private val repository = YouTubeRepository()
    private var progressJob: Job? = null
    private var mediaController: MediaController? = null

    // --- GESTIONE COOKIE E LOGIN ---
    private lateinit var cookieManager: CookieManager
    var isLoggedIn by mutableStateOf(false)
        private set

    // --- STATI PLAYER ---
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var currentTitle by mutableStateOf("Nessuna Traccia")
    var currentArtist by mutableStateOf("Cerca una canzone...")
    var currentCoverUrl by mutableStateOf("")
    var progress by mutableFloatStateOf(0f)
    var duration by mutableFloatStateOf(1f)
    var currentSong by mutableStateOf<Song?>(null)

    fun initPlayer(context: Context) {
        // 1. Inizializza Cookie
        cookieManager = CookieManager(context)
        val savedCookie = cookieManager.loadCookie()
        YouTubeClient.currentCookie = savedCookie
        isLoggedIn = savedCookie.isNotBlank()

        // 2. Connettiti al MusicService
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
    }

    // --- FUNZIONI DI LOGIN (Quelle che mancavano) ---

    fun saveLoginCookie(cookie: String) {
        viewModelScope.launch {
            cookieManager.saveCookie(cookie)
            YouTubeClient.currentCookie = cookie
            isLoggedIn = true
            // Nota: Non ricarichiamo la Home qui perché ora è gestita da HomeViewModel.
            // L'utente vedrà i contenuti personalizzati al prossimo riavvio o refresh manuale.
        }
    }

    fun logout() {
        viewModelScope.launch {
            cookieManager.clearCookie()
            YouTubeClient.currentCookie = ""
            isLoggedIn = false
        }
    }

    // --- LOGICA PLAYER ---

    private fun setupPlayerListeners() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) startProgressUpdater() else stopProgressUpdater()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
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

    fun playSong(song: Song) {
        // UI Optimistic update
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
                Log.e("MusicViewModel", "URL Stream non trovato")
                isBuffering = false
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
        currentSong = Song("test", testTitle, testArtist, testCoverUrl)

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
            progress = value
        }
    }

    private fun startProgressUpdater() {
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

    override fun onCleared() {
        stopProgressUpdater()
        mediaController?.release()
        super.onCleared()
    }
}