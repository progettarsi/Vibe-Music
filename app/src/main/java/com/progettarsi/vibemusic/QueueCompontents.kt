package com.progettarsi.vibemusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.progettarsi.vibemusic.model.Song
import com.progettarsi.vibemusic.ui.theme.DarkBackground
import com.progettarsi.vibemusic.ui.theme.PurplePrimary
import com.progettarsi.vibemusic.ui.theme.SurfaceHighlight
import com.progettarsi.vibemusic.ui.theme.TextGrey
import com.progettarsi.vibemusic.viewmodel.MusicViewModel

@Composable
fun QueueScreen(
    musicViewModel: MusicViewModel,
    onClose: () -> Unit
) {
    val queue = musicViewModel.queue
    val currentIndex = musicViewModel.currentSongIndex

    // Recuperiamo gli stati dal ViewModel
    val isLoopOn = musicViewModel.isLoopMode
    val isShuffleOn = musicViewModel.isShuffleMode

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212).copy(alpha = 0.98f))
    ) {
        // --- LISTA CANZONI ---
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            item { Box(modifier = Modifier.statusBarsPadding()) }
            itemsIndexed(queue) { index, song ->
                QueueItem(
                    song = song,
                    isPlaying = index == currentIndex,
                    onClick = { musicViewModel.playSong(song) }
                )
            }
        }

        Box(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.05f)
            .background(Brush.verticalGradient(listOf(DarkBackground, Color.Transparent)))
            .align(Alignment.TopCenter)
        )
        Box(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.1f)
            .background(Brush.verticalGradient(listOf(Color.Transparent, DarkBackground.copy(0.6f), DarkBackground)))
            .align(Alignment.BottomCenter)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
                .align(Alignment.BottomEnd), // Un po' di spazio prima della lista
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. PILLOLA "FROM: ..." (Funziona come tasto Chiudi/Indietro)
            Box(
                modifier = Modifier
                    .weight(1f) // Occupa lo spazio disponibile a sinistra
                    .height(50.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(SurfaceHighlight)
                    .clickable(onClick = onClose) // <--- Chiude la coda
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.LibraryMusic,
                        null,
                        tint = Color.White, // Colore accento per far capire che è attivo
                        modifier = Modifier.size(20.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "From: ",
                            color = TextGrey,
                            fontSize = 14.sp
                        )
                        Text(
                            text = musicViewModel.currentQueueTitle,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 2. TASTO LOOP
            IconButton(
                onClick = { musicViewModel.toggleLoop() },
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (isLoopOn) PurplePrimary.copy(alpha = 0.2f) else SurfaceHighlight)
            ) {
                Icon(
                    Icons.Default.AllInclusive,
                    null,
                    tint = if (isLoopOn) PurplePrimary else TextGrey,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 3. TASTO SHUFFLE
            IconButton(
                onClick = { musicViewModel.toggleShuffle() },
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (isShuffleOn) PurplePrimary.copy(alpha = 0.2f) else SurfaceHighlight)
            ) {
                Icon(
                    Icons.Rounded.Shuffle,
                    null,
                    tint = if (isShuffleOn) PurplePrimary else TextGrey,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun QueueItem(song: Song, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPlaying) SurfaceHighlight else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover o Equalizzatore
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceHighlight),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                // Icona animata (simulata) per il brano corrente
                Icon(Icons.Rounded.GraphicEq, null, tint = PurplePrimary)
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isPlaying) PurplePrimary else Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = TextGrey,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
fun QueueScreenPreview()
{
    QueueScreen(musicViewModel = MusicViewModel(), onClose = {})
}

@Preview
@Composable
fun QueueItemPreview()
{
    QueueItem(song = Song("ADC","Title", "Artist", "CoverUrl"), true, onClick = {})
}