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
import androidx.compose.material.icons.rounded.Shuffle
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
import com.progettarsi.vibemusic.model.CollectionType
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
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
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
                imageVector = Icons.Rounded.Shuffle,
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
    Column(modifier = Modifier.padding(bottom = 24.dp)) { // Padding inferiore per staccare le sezioni
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp)) // Spazio fisso tra titolo e griglia
        content()
    }
}

// --- LA TUA FUNZIONE MODIFICATA ---
// Ora accetta List<Song> e mostra i placeholder magenta
@Composable
fun QuickPicksRow(
    collections: List<YTCollection>,
    onItemClick: (YTCollection) -> Unit
) {
    val spacing = 12.dp
    val sidePadding = 16.dp

    // 1. CONTENITORE PRINCIPALE (Il Quadrato)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sidePadding) // Padding esterno
            .aspectRatio(1f), // Forza l'aspetto quadrato SU TUTTO IL BLOCCO
        verticalArrangement = Arrangement.spacedBy(spacing) // Spazio verticale tra le righe
    ) {
        // 2. CICLO PER LE 3 RIGHE
        repeat(3) { rowIndex ->
            Row(
                modifier = Modifier
                    .weight(1f) // Importante: Ogni riga occupa 1/3 dell'altezza totale
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing) // Spazio orizzontale tra gli item
            ) {
                // 3. CICLO PER I 3 ELEMENTI NELLA RIGA
                repeat(3) { colIndex ->
                    val index = (rowIndex * 3) + colIndex
                    val collection = collections.getOrNull(index)

                    if (collection != null) {
                        QuickPicksCollectionItem(
                            collection = collection,
                            onItemClick = onItemClick,
                            // L'item deve riempire la sua "cella" della griglia
                            modifier = Modifier
                                .weight(1f)      // 1/3 della larghezza
                                .fillMaxHeight() // Tutta l'altezza della riga
                        )
                    } else {
                        // Se non c'è la collection, mantieni lo spazio occupato ma vuoto
                        Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
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
    val spacing = 12.dp // Spazio tra i quadratini
    val sidePadding = 16.dp // Spazio laterale dallo schermo

    // BoxWithConstraints ci dà la larghezza esatta dello schermo in questo momento
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sidePadding)
    ) {
        // MATEMATICA:
        // Larghezza disponibile = LarghezzaSchermo - (padding laterali già tolti dal modifier)
        // Spazio occupato dai "buchi" vuoti = spacing * 2 (ci sono 2 spazi tra 3 colonne)
        // Larghezza di UN quadrato = (TuttoIlResto) / 3
        val itemSize = (maxWidth - (spacing * 2)) / 3

        // Costruiamo la griglia: Una Colonna che contiene 3 Righe
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            repeat(3) { rowIndex ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(3) { colIndex ->
                        // Calcolo indice: (Riga * 3) + Colonna
                        // Riga 0 -> 0, 1, 2
                        // Riga 1 -> 3, 4, 5 ...
                        val index = (rowIndex * 3) + colIndex
                        val collection = collections.getOrNull(index)

                        if (collection != null) {
                            QuickPicksCollectionItem(
                                collection = collection,
                                onItemClick = onItemClick,
                                // FORZIAMO la dimensione calcolata. Niente weight, niente aspectRatio.
                                modifier = Modifier.size(itemSize)
                            )
                        } else {
                            // Se manca l'elemento, mettiamo un buco vuoto della stessa misura
                            Spacer(modifier = Modifier.size(itemSize))
                        }
                    }
                }
            }
        }
    }
}

// ... (imports invariati)

@Composable
fun QuickPicksCollectionItem(
    collection: YTCollection,
    onItemClick: (YTCollection) -> Unit,
    modifier: Modifier = Modifier // <--- Importante: Default parameter
) {
    val placeholder = rememberVectorPainter(Icons.Default.Album)

    Box(
        modifier = modifier // Usiamo il modifier (con .size) passato dal padre
            .clip(RoundedCornerShape(8.dp)) // Angoli un po' più eleganti (8dp o 12dp)
            .background(SurfaceDark)
            .clickable { onItemClick(collection) }
    ) {
        AsyncImage(
            model = collection.coverUrl,
            contentDescription = collection.title,
            modifier = Modifier.fillMaxSize(), // Riempie il box di dimensione fissa
            contentScale = ContentScale.Crop,
            placeholder = placeholder,
            error = placeholder,
            fallback = placeholder
        )

        // Sfumatura nera in basso per leggere il testo
        Box(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .fillMaxHeight(0.6f)
            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black)))
        )

        Text(
            text = if (collection.title.isNotEmpty()) collection.title else "Raccolta",
            color = Color.White,
            fontSize = 11.sp, // Font più piccolo e leggibile
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(6.dp)
        )
    }
}

@Composable
fun QuickPicksSong(
    song: Song,
    onItemClick: (Song) -> Unit,
    modifier: Modifier // Questo modifier contiene già il .weight(1f) passato dal padre
) {
    val placeholder = rememberVectorPainter(Icons.Default.Album)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            // Rimuovi .aspectRatio(1f) qui, lascia che sia il layout padre a decidere l'altezza
            .clip(RoundedCornerShape(8.dp)) // 8dp o 12dp è più elegante per griglie piccole
            .background(SurfaceDark)
            .clickable { onItemClick(song) }
    ) {
        AsyncImage(
            model = song.coverUrl,
            contentDescription = song.title,
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceDark),
            contentScale = ContentScale.Crop,
            placeholder = placeholder,
            error = placeholder,
            fallback = placeholder
        )

        // Gradiente e Testo
        Box(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .fillMaxHeight(0.6f) // Gradiente leggermente più alto per leggibilità
            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black)))
        )

        Text(
            text = song.title,
            color = Color.White,
            fontSize = 11.sp, // Font più piccolo per stare nella griglia
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp, start = 4.dp, end = 4.dp)
        )
    }
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
val mockCollections = List(9) { i ->
    YTCollection(
        id = "id$i",
        title = "Mix $i",
        coverUrl = "https://placehold.co/400/1a1a1a/FFFFFF?text=Mix+$i",
        subtitle = "",
        type = CollectionType.PLAYLIST
    )
}

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
            QuickPicksRow(mockCollections, onItemClick = {})
        }

        // Testiamo New Drops
        HomeSection(title = "New Drops") {
            NewDropsRow(songs = mockSongs, onItemClick = {})
        }
    }
}