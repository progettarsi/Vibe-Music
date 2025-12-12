package com.progettarsi.vibemusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

    // --- WRAPPER FONDAMENTALE ---
    // Avvolge tutto il contenuto. Gestisce lo swipe sulla lista e sullo sfondo.
    SwipeToCloseContainer(
        onClose = onClose
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212).copy(alpha = 0.98f))
                // Blocca i click "dietro" la coda, ma non interferisce con lo scroll
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        ) {
            // --- LISTA ---
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // Spazio per la status bar (così la lista scorre "sotto" l'orologio ma parte giusta)
                item {
                    Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()+8.dp))
                }

                itemsIndexed(queue) { index, song ->
                    QueueItem(
                        song = song,
                        isPlaying = index == currentIndex,
                        onClick = { musicViewModel.playSong(song) }
                    )
                }
            }

            // Sfumature (Opzionali)
            Box(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.05f)
                .background(Brush.verticalGradient(listOf(DarkBackground, Color.Transparent)))
                .align(Alignment.TopCenter)
            )
            Box(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.1f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, DarkBackground.copy(0.7f), DarkBackground)))
                .align(Alignment.BottomCenter)
            )

            // --- DOCK IN BASSO (Rimane uguale) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp)
                    .align(Alignment.BottomEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ... (Il tuo codice dei pulsanti From, Loop, Shuffle) ...
                // Nota: Incolla qui il codice dei pulsanti che avevi prima
                // Tasto FROM (Chiudi)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(SurfaceHighlight)
                        .clickable(onClick = onClose)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.LibraryMusic, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("From: ", color = TextGrey, fontSize = 14.sp)
                            Text(musicViewModel.currentQueueTitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Tasti Loop/Shuffle (usa i viewModel.toggle...)
                IconButton(onClick = { musicViewModel.toggleLoop() }, modifier = Modifier.clip(CircleShape).size(50.dp).background(if(musicViewModel.isLoopMode) PurplePrimary.copy(0.2f) else SurfaceHighlight)) {
                    Icon(Icons.Default.AllInclusive, null, tint = if(musicViewModel.isLoopMode) PurplePrimary else TextGrey)
                }
                IconButton(onClick = { musicViewModel.toggleShuffle() }, modifier = Modifier.clip(CircleShape).size(50.dp).background(if(musicViewModel.isShuffleMode) PurplePrimary.copy(0.2f) else SurfaceHighlight)) {
                    Icon(Icons.Rounded.Shuffle, null, tint = if(musicViewModel.isShuffleMode) PurplePrimary else TextGrey)
                }
            }
        }
    }
}

// ... (QueueItem e Preview rimangono invariati)
@Composable
fun QueueItem(song: Song, isPlaying: Boolean, onClick: () -> Unit) {
    // ... (Il tuo codice QueueItem precedente) ...
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPlaying) SurfaceHighlight else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceHighlight),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                Icon(Icons.Rounded.GraphicEq, null, tint = PurplePrimary)
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(0.7f),
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
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, color = if (isPlaying) PurplePrimary else Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = TextGrey, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}