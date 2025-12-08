package com.progettarsi.openmusic

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.progettarsi.openmusic.ui.theme.*
import com.progettarsi.openmusic.viewmodel.MusicViewModel

// --- IMPORTS WAVY SLIDER ---
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider

// 1. COMPONENTE STATEFUL (Connesso al ViewModel)
@Composable
fun MusicPlayerScreen(
    musicViewModel: MusicViewModel,
    onCollapse: () -> Unit
) {
    // Estraiamo i dati dal ViewModel per passarli al componente "puro"
    MusicPlayerContent(
        title = musicViewModel.currentTitle,
        artist = musicViewModel.currentArtist,
        coverUrl = musicViewModel.currentCoverUrl,
        isPlaying = musicViewModel.isPlaying,
        isBuffering = musicViewModel.isBuffering,
        progress = musicViewModel.progress,
        onCollapse = onCollapse,
        onPlayPause = {
            if (musicViewModel.currentTitle == "Nessuna Traccia") musicViewModel.playTestTrack()
            else musicViewModel.togglePlayPause()
        },
        onSeek = { position ->
            musicViewModel.seekTo(position)
        }
    )
}

// 2. COMPONENTE STATELESS (Puro UI - Usato dalla Preview)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerContent(
    title: String,
    artist: String,
    coverUrl: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    progress: Float,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit
) {
    // Gestione locale dello scrubbing per evitare "salti" visivi
    var scrubbingProgress by remember { mutableStateOf<Float?>(null) }
    val effectiveProgress = scrubbingProgress ?: progress

    var isLoopOn by remember { mutableStateOf(false) }
    var isShuffleOn by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY > 150f) {
                            onCollapse()
                            // Non resettiamo offsetY qui per lasciare l'animazione di uscita fluida
                        } else {
                            offsetY = 0f
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = offsetY + dragAmount.y
                        offsetY = newOffset.coerceAtLeast(0f)
                    }
                )
            }
    ) {
        val fallbackPainter = rememberVectorPainter(Icons.Default.Album)

        // SFONDO (Copertina sfocata)
        if (coverUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (offsetY * 0.5f).dp) // Effetto parallasse
                    .alpha(0.6f),
                placeholder = fallbackPainter,
                error = fallbackPainter,
                fallback = fallbackPainter
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray.copy(alpha = 0.3f))
            )
        }

        // SFUMATURE (Overlay scuro per leggibilità)
        Column(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(DarkBackground.copy(0.9f), Color.Transparent))))
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(400.dp).background(Brush.verticalGradient(listOf(Color.Transparent, DarkBackground.copy(0.5f), DarkBackground))))
        }

        // CONTENUTO PLAYER
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .offset(y = offsetY.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // TITOLO E ARTISTA
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(artist, color = TextGrey, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(title, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { isLiked = !isLiked }) {
                    AnimatedContent(targetState = isLiked, label = "Like") { liked ->
                        Icon(if(liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if(liked) PurplePrimary else Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            // Placeholder per i testi (Lyrics)
            Text("Testo della canzone in streaming...", color = TextGrey.copy(0.9f), fontSize = 18.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(48.dp))

            // --- WAVY SLIDER E CONTROLLI ---
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                // 1. Wavy Slider
                Box(Modifier.weight(1f).height(60.dp), contentAlignment = Alignment.CenterStart) {
                    WavySlider(
                        value = effectiveProgress,
                        onValueChange = { scrubbingProgress = it },
                        onValueChangeFinished = {
                            onSeek(scrubbingProgress ?: 0f)
                            scrubbingProgress = null
                        },
                        // Configurazione Onda
                        waveLength = 35.dp,
                        waveHeight = 12.dp,
                        // L'onda si muove solo se sta suonando e non è in buffering
                        waveVelocity = if (isPlaying && !isBuffering) 14.dp to WaveDirection.HEAD else 0.dp to WaveDirection.HEAD,
                        waveThickness = 4.dp,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = PurplePrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }

                // 2. Play/Pause Button
                Box(contentAlignment = Alignment.Center) {
                    if (isBuffering) {
                        CircularProgressIndicator(modifier = Modifier.size(56.dp), color = Color.White, strokeWidth = 3.dp)
                    }
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(56.dp).background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        AnimatedContent(targetState = isPlaying, label = "Play") { playing ->
                            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // CONTROLLI EXTRA (Shuffle/Loop)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { isLoopOn = !isLoopOn }) {
                    Icon(Icons.Default.AllInclusive, null, tint = if(isLoopOn) Color.White else TextGrey.copy(0.5f), modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { isShuffleOn = !isShuffleOn }) {
                    Icon(Icons.Default.Shuffle, null, tint = if(isShuffleOn) Color.White else TextGrey.copy(0.5f), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// 3. PREVIEW (Funziona perché usa MusicPlayerContent con dati finti)
@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun MusicPlayerScreenPreview() {
    MusicPlayerContent(
        title = "Midnight City",
        artist = "M83",
        coverUrl = "", // Vuoto per testare il fallback
        isPlaying = true,
        isBuffering = false,
        progress = 0.6f, // Simuliamo la barra al 60%
        onCollapse = {},
        onPlayPause = {},
        onSeek = {}
    )
}