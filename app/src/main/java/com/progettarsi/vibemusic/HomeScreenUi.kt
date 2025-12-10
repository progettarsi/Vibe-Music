package com.progettarsi.vibemusic

import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.progettarsi.vibemusic.model.Playlist
import com.progettarsi.vibemusic.model.Song
import com.progettarsi.vibemusic.model.YTCollection
import com.progettarsi.vibemusic.ui.theme.PurpleDark
import com.progettarsi.vibemusic.ui.theme.PurplePrimary
import com.progettarsi.vibemusic.ui.theme.SurfaceDark
import com.progettarsi.vibemusic.ui.theme.SurfaceHighlight
import com.progettarsi.vibemusic.ui.theme.TextGrey

@Composable
fun YourRadioButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(PurplePrimary, PurpleDark)
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Your Radio",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "YOUR RADIO",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HomeSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.fillMaxHeight(0.01f))
        content()
    }
}

// --- LA TUA FUNZIONE MODIFICATA ---
// Ora accetta List<Song> e mostra i placeholder magenta
@Composable
fun QuickPicksRow(songs: List<Song>, onItemClick: (Song) -> Unit) {
    val spacing = 16.dp // Definisci la spaziatura uguale per altezza e larghezza

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Rende il contenitore un quadrato perfetto
            .padding(16.dp),
        // 1. Spaziatura Orizzontale uguale
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(3) { colIndex ->
            Column(
                modifier = Modifier
                    .weight(1f)      // Prende esattamente 1/3 della larghezza disponibile
                    .fillMaxHeight(), // Si estende per tutta l'altezza
                // 2. Spaziatura Verticale uguale (identica a quella orizzontale)
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                repeat(3) { rowIndex ->
                    // CALCOLO INDICE:
                    // Vogliamo riempire: Riga 0 (0,1,2), Riga 1 (3,4,5)...
                    // Formula: (NumeroRiga * 3) + NumeroColonna
                    val index = (rowIndex * 3) + colIndex

                    // Recuperiamo la canzone in sicurezza
                    val song = songs.getOrNull(index)

                    if (song != null) {
                        QuickPicksSong(
                            song = song,
                            onItemClick = onItemClick,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Se non c'è una canzone (es. lista da 7 brani),
                        // mettiamo un box vuoto invisibile per mantenere la struttura
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPicksCollectionsRow(
    collections: List<YTCollection>,
    onItemClick: (YTCollection) -> Unit
) {
    val spacing = 16.dp // Definisci la spaziatura uguale per altezza e larghezza

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Rende il contenitore un quadrato perfetto
            .padding(16.dp),
        // 1. Spaziatura Orizzontale uguale
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(3) { colIndex ->
            Column(
                modifier = Modifier
                    .weight(1f)      // Prende esattamente 1/3 della larghezza disponibile
                    .fillMaxHeight(), // Si estende per tutta l'altezza
                // 2. Spaziatura Verticale uguale (identica a quella orizzontale)
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                repeat(3) { rowIndex ->
                    // CALCOLO INDICE:
                    // Vogliamo riempire: Riga 0 (0,1,2), Riga 1 (3,4,5)...
                    // Formula: (NumeroRiga * 3) + NumeroColonna
                    val index = (rowIndex * 3) + colIndex

                    // Recuperiamo la canzone in sicurezza
                    val item = collections.getOrNull(index)
                    if (item != null) {
                        // Qui dentro devi usare i dati di YTCollection (title, coverUrl)
                        // Puoi riusare il layout grafico di QuickPicksSong, basta passare i dati giusti
                        QuickPicksCollectionItem(item, onItemClick)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPicksSong(song: Song, onItemClick: (Song) -> Unit, modifier: Modifier)
{
    val placeholder = rememberVectorPainter(Icons.Default.Album)
    BoxWithConstraints(
        modifier = Modifier // Prende esattamente 1/3 dell'altezza della colonna
            .fillMaxWidth()
            .aspectRatio(1f)// Riempie tutta la larghezza della colonna
            .clip(RoundedCornerShape(12.dp)) // Stondatura fissa (consigliata per elementi piccoli)
            .background(SurfaceDark)
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = song.title,
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceDark),
            contentScale = ContentScale.Crop,

            // --- AGGIUNGI QUESTE RIGHE ---
            placeholder = placeholder, // Mostra questo mentre carica
            error = placeholder,       // Mostra questo se fallisce (es. in Preview)
            fallback = placeholder     // Mostra questo se l'URL è nullo
        )
        IconButton(onClick = {TODO()}, modifier = Modifier.fillMaxSize(0.7f).align(Alignment.Center)) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White.copy(0.7f), modifier = Modifier.fillMaxSize(0.7f))
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .fillMaxHeight(0.5f)
            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black)))
        )
        Text(
            text = if(song.title.isNotEmpty()){if (song.title.length > 15) song.title.take(15) + "..." else song.title} else "PlaceHolder",
            color = Color.White,
            fontSize = (maxWidth.value * 0.10f).sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = (maxHeight.value*0.08f).dp)
        )
    }
}

@Composable
fun QuickPicksCollectionItem(
    collection: YTCollection,
    onItemClick: (YTCollection) -> Unit
) {
    val placeholder = rememberVectorPainter(Icons.Default.Album)
    BoxWithConstraints(
        modifier = Modifier // Prende esattamente 1/3 dell'altezza della colonna
            .fillMaxWidth()
            .aspectRatio(1f)// Riempie tutta la larghezza della colonna
            .clip(RoundedCornerShape(12.dp)) // Stondatura fissa (consigliata per elementi piccoli)
            .background(SurfaceDark)
    ) {
        AsyncImage(
            model = collection.coverUrl,
            contentDescription = collection.title,
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceDark),
            contentScale = ContentScale.Crop,

            // --- AGGIUNGI QUESTE RIGHE ---
            placeholder = placeholder, // Mostra questo mentre carica
            error = placeholder,       // Mostra questo se fallisce (es. in Preview)
            fallback = placeholder     // Mostra questo se l'URL è nullo
        )
        IconButton(onClick = {TODO()}, modifier = Modifier.fillMaxSize(0.7f).align(Alignment.Center)) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White.copy(0.7f), modifier = Modifier.fillMaxSize(0.7f))
        }
        Box(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .fillMaxHeight(0.5f)
            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black)))
        )
        Text(
            text = if(collection.title.isNotEmpty()){if (collection.title.length > 15) collection.title.take(15) + "..." else collection.title} else "PlaceHolder",
            color = Color.White,
            fontSize = (maxWidth.value * 0.10f).sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = (maxHeight.value*0.08f).dp)
        )
    }
}

@Preview
@Composable
fun QuickPicksSongsPreview()
{
    QuickPicksSong(song = mockSongs[0], onItemClick = {}, modifier = Modifier)
}

@Composable
fun NewDropsRow(songs: List<Song>, onItemClick: (Song) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(songs) { song ->
            AlbumCard(song = song, onClick = { onItemClick(song) })
        }
    }
}

@Composable
fun ArtistCard(playlist: Playlist, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = playlist.thumbnails,
            contentDescription = playlist.title,
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(SurfaceDark),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AlbumCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = song.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = TextGrey,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

val mockSongs = listOf(
    Song("id1", "Canzone 1", "Artista A", "https://placehold.co/400/1a1a1a/FFFFFF?text=1", 200000),
    Song("id2", "Canzone 2", "Artista B", "https://placehold.co/400/2b2b2b/FFFFFF?text=2", 210000),
    Song("id3", "Canzone 3", "Artista C", "https://placehold.co/400/3c3c3c/FFFFFF?text=3", 220000),
    Song("id4", "Canzone 4", "Artista D", "https://placehold.co/400/4d4d4d/FFFFFF?text=4", 230000),
    Song("id5", "Canzone 5", "Artista E", "https://placehold.co/400/5e5e5e/FFFFFF?text=5", 240000),
    Song("id6", "Canzone 6", "Artista F", "https://placehold.co/400/6f6f6f/FFFFFF?text=6", 250000),
    Song("id7", "Canzone 7", "Artista G", "https://placehold.co/400/808080/FFFFFF?text=7", 260000),
    Song("id8", "Canzone 8", "Artista H", "https://placehold.co/400/919191/FFFFFF?text=8", 270000),
    Song("id9", "Canzone 9", "Artista I", "https://placehold.co/400/a2a2a2/FFFFFF?text=9", 280000)
)

// --- PREVIEW ---
@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun HomeScreenUiPreview() {

    Column(modifier = Modifier.fillMaxSize()) {
        // Simulazione Header
        Text(
            text = "Welcome Back Preview",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        YourRadioButton(onClick = {})

        // Testiamo la tua nuova QuickPicksRow
        HomeSection(title = "Quick Picks") {
            QuickPicksRow(songs = mockSongs, onItemClick = {})
        }

        // Testiamo New Drops
        HomeSection(title = "New Drops") {
            NewDropsRow(songs = mockSongs, onItemClick = {})
        }
    }
}