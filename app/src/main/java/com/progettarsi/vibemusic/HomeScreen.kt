package com.progettarsi.vibemusic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
        // --- MODIFICA QUI: Colleghiamo la UI alla funzione playPlaylist del ViewModel ---
        onCollectionClick = { collection ->
            musicViewModel.playCollection(collection)
        },

        onPlayTrack = { list, index ->
            musicViewModel.playPlaylist(list, index)
        }
    )
}

// 2. COMPONENTE STATELESS (Pura UI)
@Composable
fun HomeScreenContent(
    topPadding: Dp,
    bottomPadding: Dp,
    uiState: HomeUiState,
    swipeRefreshState: com.google.accompanist.swiperefresh.SwipeRefreshState,
    onRefresh: () -> Unit,
    onRadioClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    // --- MODIFICA QUI: La callback ora accetta Lista e Indice ---
    onPlayTrack: (List<Song>, Int) -> Unit,
    // Aggiungi questo:
    onCollectionClick: (YTCollection) -> Unit
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
                                    "${uiState.message}",
                                    color = Color.Red,
                                    modifier = Modifier.padding(16.dp)
                                )
                                Button(onClick = onRefresh) { Text("Riprova") }
                            }
                        }
                    }
                }

                is HomeUiState.Success -> {
                    val homeContent = uiState.data

                    val collections = homeContent.filterIsInstance<YTCollection>()
                    val songs = homeContent.filterIsInstance<Song>()

                    if (collections.isNotEmpty()) {
                        item {
                            HomeSection(title = "Quick Picks") {
                                QuickPicksCollectionsRow(collections, onItemClick = { collection ->
                                    // Chiama la callback che scarica e suona
                                    onCollectionClick(collection)
                                })
                            }
                        }
                    }

                    // Sezione New Drops
                    if (songs.isNotEmpty()) {
                        item {
                            HomeSection(title = "New Drops") {
                                // --- MODIFICA QUI: Logica per passare la lista ---
                                NewDropsRow(songs, onItemClick = { song ->
                                    // 1. Troviamo l'indice della canzone cliccata
                                    val index = songs.indexOf(song)
                                    // 2. Chiamiamo la callback passando TUTTA la lista e l'indice
                                    if (index != -1) {
                                        onPlayTrack(songs, index)
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}