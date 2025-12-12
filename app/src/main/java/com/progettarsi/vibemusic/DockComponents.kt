package com.progettarsi.vibemusic

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.progettarsi.vibemusic.model.Song
import com.progettarsi.vibemusic.ui.theme.PurplePrimary
import com.progettarsi.vibemusic.ui.theme.TextGrey
import com.progettarsi.vibemusic.viewmodel.MusicViewModel
import androidx.compose.ui.res.stringResource
import com.progettarsi.vibemusic.R
import com.progettarsi.vibemusic.viewmodel.SearchViewModel // Importante

// --- MINI PLAYER VIEW ---
@Composable
fun MiniPlayerContent(
    musicViewModel: MusicViewModel,
    onPlayerClick: () -> Unit
) {
    val title: String = musicViewModel.currentTitle
    val artist: String = musicViewModel.currentArtist
    val coverUrl: String = musicViewModel.currentCoverUrl

    val isPlaying = musicViewModel.isPlaying
    val isBuffering = musicViewModel.isBuffering

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .clickable(onClick = onPlayerClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Copertina Girevole / Play Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(0.7f))
                .clickable {
                    if (title == "Nessuna Traccia") musicViewModel.playTestTrack()
                    else musicViewModel.togglePlayPause()
                },
            contentAlignment = Alignment.Center
        ) {
            val fp = rememberVectorPainter(Icons.Default.Album)
            if (coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = fp, error = fp, fallback = fp
                )
            }

            if (isBuffering) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = Color.White, strokeWidth = 3.dp)
            }

            Crossfade(targetState = isPlaying, label = "MiniPlay") { playing ->
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = if (musicViewModel.currentTitle.isEmpty()) stringResource(R.string.no_track_title) else musicViewModel.currentTitle,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )
            Text(
                text = if (musicViewModel.currentTitle.isEmpty()) stringResource(R.string.no_track_artist) else musicViewModel.currentArtist,
                color = Color.White.copy(0.7f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

// --- SEARCH BAR CONTENT ---
@Composable
fun DockSearchBarContent(
    isExpanded: Boolean,
    searchViewModel: SearchViewModel, // Usiamo il ViewModel dedicato
    placeholder: String
) {
    Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
        if (!isExpanded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Search, null, tint = Color.White)
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Search, null, tint = TextGrey)
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = searchViewModel.query, // Binding al ViewModel
                    onValueChange = {
                        searchViewModel.updateQuery(it) // Aggiornamento tramite ViewModel
                    },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(PurplePrimary),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (searchViewModel.query.isEmpty()) {
                            Text(placeholder, color = TextGrey.copy(0.7f), maxLines = 1)
                        }
                        inner()
                    }
                )
                if (searchViewModel.query.isNotEmpty()) {
                    IconButton(onClick = {
                        searchViewModel.clearSearch()
                    }) {
                        Icon(Icons.Default.Close, null, tint = TextGrey)
                    }
                }
            }
        }
    }
}

// --- ELEMENTO LISTA RISULTATI ---
@Composable
fun SongResultItem(song: Song, onClick: () -> Unit) {
    val fp = rememberVectorPainter(Icons.Default.MusicNote)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray),
                contentScale = ContentScale.Crop,
                placeholder = fp, error = fp, fallback = fp
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(song.artist, color = TextGrey, fontSize = 14.sp, maxLines = 1)
            }
            Icon(Icons.Default.PlayArrow, null, tint = TextGrey.copy(0.5f), modifier = Modifier.size(24.dp))
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
    }
}