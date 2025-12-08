package com.progettarsi.openmusic

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.progettarsi.openmusic.ui.component.ShapeProgressIndicator
import com.progettarsi.openmusic.ui.theme.DarkBackground
import com.progettarsi.openmusic.ui.theme.PurplePrimary
import com.progettarsi.openmusic.ui.theme.SurfaceHighlight
import com.progettarsi.openmusic.ui.theme.TextGrey
import com.progettarsi.openmusic.viewmodel.MusicViewModel
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material.WavySlider

// 1. COMPONENTE STATEFUL (Connesso al ViewModel)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
            )
            {
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
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .offset(y = (offsetY * 0.5f).dp),
                        placeholder = fallbackPainter,
                        error = fallbackPainter,
                        fallback = fallbackPainter
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Magenta)
                    ) { Icon(Icons.Default.Album, null, tint = Color.White.copy(0.6f), modifier = Modifier.fillMaxSize()) }
                }
                // SFUMATURE (Overlay scuro per leggibilità)
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(DarkBackground, Color.Transparent))))
                    Box(Modifier.fillMaxWidth().height(240.dp).background(Brush.verticalGradient(listOf(Color.Transparent, DarkBackground))).align(Alignment.BottomEnd))
                }


                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal =24.dp)
                ) {
                    // TITOLO E ARTISTA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                artist,
                                color = Color.White.copy(0.8f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                title,
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 40.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { isLiked = !isLiked }) {
                            AnimatedContent(targetState = isLiked, label = "Like") { liked ->
                                Icon(
                                    if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    null,
                                    tint = if (liked) PurplePrimary else Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.fillMaxHeight(0.5f))


            // -- WAVY SLIDER E CONTROLLI ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Rounded.ArrowBackIosNew,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                        .combinedClickable(onClick = {}, onDoubleClick = {}).clip(CircleShape)
                )
                // 1. Wavy Slider
                Box(
                    Modifier.weight(1f).height(60.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    WavySlider(
                        value = effectiveProgress,
                        onValueChange = { scrubbingProgress = it },
                        onValueChangeFinished = {
                            onSeek(scrubbingProgress ?: 0f)
                            scrubbingProgress = null
                        },
                        // Configurazione Onda
                        waveLength = 26.dp,
                        waveHeight = 11.dp,
                        // L'onda si muove solo se sta suonando e non è in buffering
                        waveVelocity = if (isPlaying && !isBuffering) 25.dp to WaveDirection.TAIL else 0.dp to WaveDirection.HEAD,
                        waveThickness = 5.dp,
                        trackThickness = 3.dp
                    )
                }

                // 2. Play/Pause Button
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // Importante: clip deve venire PRIMA del clickable per sagomare l'effetto tocco
                            .clip(MaterialShapes.Cookie12Sided.toShape())
                            .background(SurfaceHighlight)
                            .clickable(onClick = onPlayPause)
                    )

                    // 2. L'ICONA (Al centro)
                    AnimatedContent(targetState = isPlaying, label = "Play") { playing ->
                        Icon(
                            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    if (isBuffering) {
                        ShapeProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            shape = MaterialShapes.Cookie12Sided,
                            color = Color.White.copy(0.7f),
                            strokeWidth = 3.dp
                        )
                    }

                }
            }
        }
        // CONTROLLI EXTRA (Shuffle/Loop/Pulsante Coda)
        Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(24.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {/*TODO*/}, modifier = Modifier.clip(RoundedCornerShape(100.dp)).size(50.dp).background(SurfaceHighlight)) {
                        Icon(Icons.Filled.Bedtime, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {/*TODO*/}, modifier = Modifier.clip(RoundedCornerShape(100.dp)).size(50.dp).background(SurfaceHighlight)) {
                        Icon(Icons.Filled.CellTower, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {/*TODO*/}, modifier = Modifier.clip(RoundedCornerShape(100.dp)).size(50.dp).background(SurfaceHighlight)) {
                        Icon(Icons.Default.Downloading, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = {/*TODO*/}, modifier = Modifier.clip(RoundedCornerShape(100.dp)).size(50.dp).background(SurfaceHighlight)) {
                        Icon(Icons.Default.MoreVert, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(SurfaceHighlight)
                            .height(50.dp)
                            .width(200.dp)
                            .padding(8.dp)
                            .combinedClickable(onClick = {/*open QueueScreen*/ }),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("From: ", color = Color.White, fontSize = 16.sp)
                            Text(
                                "Playlist Placeholder",
                                color = Color.White,
                                fontSize = 16.sp,
                                maxLines = 1
                            )

                        }
                    }
                    IconButton(onClick = { isLoopOn = !isLoopOn }) {
                        Icon(
                            Icons.Default.AllInclusive,
                            null,
                            tint = if (isLoopOn) Color.White else TextGrey.copy(0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = { isShuffleOn = !isShuffleOn }) {
                        Icon(
                            Icons.Default.Shuffle,
                            null,
                            tint = if (isShuffleOn) Color.White else TextGrey.copy(0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
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