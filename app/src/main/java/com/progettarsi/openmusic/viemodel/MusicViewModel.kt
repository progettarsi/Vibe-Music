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
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {
    var isPlaying by mutableStateOf(false)
    var currentTitle by mutableStateOf("Nessuna Traccia")
    var currentArtist by mutableStateOf("Tocca Play per testare")
    var progress by mutableFloatStateOf(0f)
    var duration by mutableFloatStateOf(1f)

    private var mediaController: MediaController? = null

    fun initPlayer(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Aggiorna metadati se presenti
                    mediaItem?.mediaMetadata?.title?.let { currentTitle = it.toString() }
                    mediaItem?.mediaMetadata?.artist?.let { currentArtist = it.toString() }
                    duration = mediaController?.duration?.toFloat()?.coerceAtLeast(1f) ?: 1f
                }
            })
            startProgressUpdater()
        }, MoreExecutors.directExecutor())
    }

    fun playTestTrack() {
        // Un MP3 gratuito e sicuro per testare lo streaming
        val item = MediaItem.Builder()
            .setUri("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle("Test Song Helix")
                    .setArtist("SoundHelix Library")
                    .build()
            )
            .build()

        mediaController?.setMediaItem(item)
        mediaController?.prepare()
        mediaController?.play()
    }

    fun togglePlayPause() {
        if (isPlaying) mediaController?.pause() else mediaController?.play()
    }

    fun seekTo(value: Float) {
        mediaController?.seekTo((value * duration).toLong())
    }

    private fun startProgressUpdater() {
        viewModelScope.launch {
            while (true) {
                if (isPlaying && mediaController != null) {
                    progress = mediaController!!.currentPosition.toFloat() / mediaController!!.duration.toFloat().coerceAtLeast(1f)
                }
                delay(500)
            }
        }
    }
}