package com.progettarsi.vibemusic

import androidx.benchmark.traceprocessor.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.progettarsi.vibemusic.model.Playlist
import com.progettarsi.vibemusic.model.Song
import com.progettarsi.vibemusic.ui.theme.PurplePrimary
import com.progettarsi.vibemusic.viewmodel.HomeUiState
import com.progettarsi.vibemusic.viewmodel.HomeViewModel
import com.progettarsi.vibemusic.viewmodel.MusicViewModel
import com.progettarsi.vibemusic.model.YTCollection
import com.progettarsi.vibemusic.model.MusicItem

// 1. COMPONENTE STATEFUL (Logica e ViewModel)
@Composable
fun HomeScreen(
    topPadding: Dp,
    bottomPadding: Dp,
    homeViewModel: HomeViewModel = viewModel(),
    musicViewModel: MusicViewModel
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val isRefreshing = uiState is HomeUiState.Loading
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    HomeScreenContent(
        topPadding = topPadding,
        bottomPadding = bottomPadding,
        uiState = uiState,
        swipeRefreshState = swipeRefreshState,
        onRefresh = { homeViewModel.fetchHomeContent() },
        onRadioClick = { /* TODO */ },
        onPlaylistClick = { /* TODO */ },
        onSongClick = { song -> musicViewModel.playSong(song) }
    )
}

// 2. COMPONENTE STATELESS (Pura UI - Visualizzabile in Preview)
@Composable
fun HomeScreenContent(
    topPadding: Dp,
    bottomPadding: Dp,
    uiState: HomeUiState,
    swipeRefreshState: com.google.accompanist.swiperefresh.SwipeRefreshState,
    onRefresh: () -> Unit,
    onRadioClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onSongClick: (Song) -> Unit
) {
    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        indicatorPadding = PaddingValues(top = topPadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topPadding + 16.dp,
                bottom = bottomPadding + 80.dp
            )
        ) {
            // Header
            item {
                Text(
                    text = "Welcome Back",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Radio
            item {
                YourRadioButton(onClick = onRadioClick)
            }

            // Contenuto Dinamico
            // ... (parte precedente del file uguale)

            // Contenuto Dinamico
            when (uiState) {
                is HomeUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .padding(top = 50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PurplePrimary)
                        }
                    }
                }

                is HomeUiState.Error -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxWidth().padding(top = 50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Errore: ${uiState.message}",
                                    color = Color.Red,
                                    modifier = Modifier.padding(16.dp)
                                )
                                Button(onClick = onRefresh) { Text("Riprova") }
                            }
                        }
                    }
                }

                is HomeUiState.Success -> {
                    val homeContent = uiState.data // Ora è List<MusicItem>

                    // 1. FILTRIAMO: Le Raccolte vanno nei Quick Picks
                    val collections = homeContent.filterIsInstance<YTCollection>()

                    // 2. FILTRIAMO: Le Canzoni vanno nei New Drops
                    val songs = homeContent.filterIsInstance<Song>()

                    // Sezione Quick Picks (Album, Mix, Playlist)
                    if (collections.isNotEmpty()) {
                        item {
                            HomeSection(title = "Quick Picks") {
                                // Per ora la UI di QuickPicksRow vuole 'Song' o 'Playlist'?
                                // Se la tua QuickPicksRow attuale vuole List<Song>, dobbiamo mappare temporaneamente
                                // OPPURE (meglio) aggiorniamo QuickPicksRow per accettare YTCollection.

                                // Soluzione rapida: Passiamo collections alla tua griglia
                                QuickPicksCollectionsRow(collections, onItemClick = { collection ->
                                    // TODO: Qui implementeremo la coda!
                                    // Per ora stampiamo solo il log
                                    println("Hai cliccato la raccolta: ${collection.title}")
                                })
                            }
                        }
                    }

                    // Sezione New Drops (Canzoni singole)
                    if (songs.isNotEmpty()) {
                        item {
                            HomeSection(title = "New Drops") {
                                NewDropsRow(songs, onItemClick = onSongClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun HomeScreenPreview() {
    // Dati Finti
    val mockPlaylists = listOf(
        Playlist("Lo-Fi Study", "Relax", "1", "", 10),
        Playlist("Gym Pump", "Workout", "2", "", 25),
        Playlist("Top 50 IT", "Charts", "3", "", 50)
    )
    val mockSongs = listOf(
        Song("v1", "Blinding Lights", "The Weeknd", ""),
        Song("v2", "Shape of You", "Ed Sheeran", "")
    )

    // Stato simulato: Successo con dati
    val mockState = HomeUiState.Success(mockPlaylists + mockSongs)

    HomeScreenContent(
        topPadding = 24.dp,
        bottomPadding = 80.dp,
        uiState = mockState,
        swipeRefreshState = rememberSwipeRefreshState(isRefreshing = false),
        onRefresh = {},
        onRadioClick = {},
        onPlaylistClick = {},
        onSongClick = {}
    )
}