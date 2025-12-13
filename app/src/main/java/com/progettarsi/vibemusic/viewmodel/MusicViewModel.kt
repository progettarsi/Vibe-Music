package com.progettarsi.vibemusic.viewmodel

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
import com.progettarsi.vibemusic.model.Song
import com.progettarsi.vibemusic.model.YTCollection
import com.progettarsi.vibemusic.network.YouTubeClient
import com.progettarsi.vibemusic.network.YouTubeRepository
import com.progettarsi.vibemusic.service.MusicService
import com.progettarsi.vibemusic.utils.CookieManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Wrapper per la coda
data class QueueItem(
    val uniqueId: String = java.util.UUID.randomUUID().toString(),
    val song: Song
)

class MusicViewModel : ViewModel() {
    var currentQueueTitle by mutableStateOf("Coda")
        private set

    var queue = mutableStateListOf<QueueItem>()
        private set

    var currentSongIndex by mutableStateOf(-1)
    private val repository = YouTubeRepository()
    private var progressJob: Job? = null
    private var mediaController: MediaController? = null

    // Job per la radio (per poterlo annullare se cambiamo canzone velocemente)
    private var radioLoadingJob: Job? = null

    // --- GESTIONE COOKIE E LOGIN ---
    private lateinit var cookieManager: CookieManager
    var isLoggedIn by mutableStateOf(false)
        private set

    // --- STATI PLAYER ---
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var currentTitle by mutableStateOf("")
    var currentArtist by mutableStateOf("Cerca una canzone...")
    var currentCoverUrl by mutableStateOf("")
    var progress by mutableFloatStateOf(0f)
    var duration by mutableFloatStateOf(1f)
    var currentSong by mutableStateOf<Song?>(null)

    var isLoopMode by mutableStateOf(false)
    var isShuffleMode by mutableStateOf(false)

    // --- GESTIONE AZIONI UTENTE (Gesture, Click, ecc.) ---

    // 1. Play Song (Click dalla Ricerca): Avvia canzone + Radio automatica
    fun playSong(song: Song) {
        // Resettiamo la coda perché stiamo avviando un nuovo flusso
        queue.clear()

        // Aggiungiamo la canzone cliccata
        val firstItem = QueueItem(song = song)
        queue.add(firstItem)

        // Impostiamo indici e titoli
        currentSongIndex = 0
        currentQueueTitle = "Radio ${song.title}"

        // Avviamo la riproduzione
        loadAndPlay(song)

        // AVVIO RADIO AUTOMATICA
        // Annulliamo eventuali caricamenti radio precedenti
        radioLoadingJob?.cancel()
        radioLoadingJob = viewModelScope.launch {
            try {
                // Recuperiamo canzoni simili (Radio)
                val radioSongs = repository.getRadio(song.videoId)

                // Filtriamo per non riaggiungere quella che sta già suonando
                val newSongs = radioSongs.filter { it.videoId != song.videoId }

                if (newSongs.isNotEmpty()) {
                    // Convertiamo in QueueItem
                    val radioItems = newSongs.map { QueueItem(song = it) }

                    // Aggiungiamo alla coda
                    queue.addAll(radioItems)
                    Log.d("MusicViewModel", "Radio caricata: ${radioItems.size} brani aggiunti")
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Errore caricamento radio: ${e.message}")
            }
        }
    }

    // 2. Play Next (Gesture Destra->Sinistra): Aggiunge SUBITO DOPO la corrente
    fun playNext(song: Song) {
        if (queue.isEmpty()) {
            // Se la coda è vuota, la suoniamo direttamente
            playSong(song)
            return
        }

        // Calcoliamo la posizione corretta: Indice corrente + 1
        // Usiamo coerceAtLeast(0) per sicurezza, ma currentSongIndex dovrebbe essere valido
        val targetIndex = (currentSongIndex + 1).coerceAtMost(queue.size)

        // CONTROLLO DUPLICATI "INTELLIGENTE"
        // Se la canzone che c'è ESATTAMENTE in quella posizione è già lei, evitiamo di aggiungerla di nuovo.
        // Questo previene l'aggiunta multipla se la gesture scatta due volte.
        if (targetIndex < queue.size && queue[targetIndex].song.videoId == song.videoId) {
            Log.d("MusicViewModel", "PlayNext ignorato: canzone già presente nella posizione successiva")
            return
        }

        // Aggiungiamo il wrapper nella posizione calcolata
        queue.add(targetIndex, QueueItem(song = song))
        Log.d("MusicViewModel", "Aggiunta Play Next in posizione $targetIndex: ${song.title}")
    }

    // 3. Add to Queue (Gesture Sinistra->Destra): Aggiunge in FONDO
    fun addQueue(song: Song) {
        // Controllo duplicati sull'ultimo elemento per evitare spam da gesture
        if (queue.isNotEmpty() && queue.last().song.videoId == song.videoId) {
            Log.d("MusicViewModel", "AddQueue ignorato: canzone già presente in fondo")
            return
        }

        if (queue.isEmpty()) {
            playSong(song)
        } else {
            queue.add(QueueItem(song = song))
        }
    }

    // 4. Click dentro la Coda: Cambia solo l'indice, non ricarica la coda
    fun playQueueItem(item: QueueItem) {
        val index = queue.indexOf(item)
        if (index != -1) {
            currentSongIndex = index
            loadAndPlay(item.song)
        }
    }

    // 5. Rimozione elemento (Swipe Delete)
    fun removeQueueItem(item: QueueItem) {
        val index = queue.indexOf(item)
        if (index != -1) {
            // Se rimuoviamo qualcosa prima della canzone corrente, aggiustiamo l'indice
            if (index < currentSongIndex) {
                currentSongIndex--
            }
            queue.removeAt(index)
        }
    }

    // --- ALTRE FUNZIONI (Shuffle, Loop, Playlist, Player) ---

    fun toggleLoop() {
        isLoopMode = !isLoopMode
    }

    fun toggleShuffle() {
        isShuffleMode = !isShuffleMode
    }

    fun optimizeQueue() {
        val historyLimit = 5
        if (currentSongIndex > historyLimit) {
            val songsToRemove = currentSongIndex - historyLimit
            queue.removeRange(0, songsToRemove)
            currentSongIndex -= songsToRemove
        }
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0, sourceName: String = "Playlist") {
        // Quando si avvia una playlist, non vogliamo la logica Radio, quindi puliamo il job
        radioLoadingJob?.cancel()

        queue.clear()
        val newItems = songs.map { QueueItem(song = it) }
        queue.addAll(newItems)

        currentSongIndex = startIndex
        currentQueueTitle = sourceName

        if (startIndex in queue.indices) {
            loadAndPlay(queue[startIndex].song)
        }
    }

    fun playCollection(collection: YTCollection) {
        currentTitle = collection.title
        currentArtist = "Caricamento..."
        isBuffering = true
        currentQueueTitle = collection.title
        radioLoadingJob?.cancel()

        viewModelScope.launch {
            val songs = repository.getPlaylistSongs(collection.id)
            if (songs.isNotEmpty()) {
                playPlaylist(songs, startIndex = 0, sourceName = collection.title)
            } else {
                currentArtist = "Errore caricamento"
                isBuffering = false
            }
        }
    }

    fun skipToNext() {
        if (queue.isNotEmpty() && currentSongIndex < queue.lastIndex) {
            currentSongIndex++
            loadAndPlay(queue[currentSongIndex].song)
        }
    }

    fun skipToPrevious() {
        if (progress > 0.05f) {
            seekTo(0f)
        } else {
            if (queue.isNotEmpty() && currentSongIndex > 0) {
                currentSongIndex--
                loadAndPlay(queue[currentSongIndex].song)
            }
        }
    }

    // --- INIZIALIZZAZIONE E PLAYER ---

    fun initPlayer(context: Context) {
        cookieManager = CookieManager(context)
        val savedCookie = cookieManager.loadCookie()
        YouTubeClient.currentCookie = savedCookie
        isLoggedIn = savedCookie.isNotBlank()

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

    fun saveLoginCookie(cookie: String) {
        viewModelScope.launch {
            cookieManager.saveCookie(cookie)
            YouTubeClient.currentCookie = cookie
            isLoggedIn = true
        }
    }

    fun logout() {
        viewModelScope.launch {
            cookieManager.clearCookie()
            YouTubeClient.currentCookie = ""
            isLoggedIn = false
        }
    }

    private fun setupPlayerListeners() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) startProgressUpdater() else stopProgressUpdater()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)

                if (playbackState == Player.STATE_ENDED) {
                    if (currentSongIndex < queue.lastIndex) {
                        skipToNext()
                    } else {
                        isPlaying = false
                        progress = 0f
                    }
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

    private fun loadAndPlay(song: Song) {
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
                skipToNext()
            }
        }
    }

    fun playTestTrack() {
        // ... codice test esistente ...
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

    companion object
}