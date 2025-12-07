package com.progettarsi.openmusic

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.progettarsi.openmusic.model.Playlist
import com.progettarsi.openmusic.model.Song
import com.progettarsi.openmusic.viewmodel.MusicViewModel

@Composable
fun HomeScreen(
    topPadding: Dp,
    bottomPadding: Dp,
    viewModel: MusicViewModel = viewModel()
) {
    val homeContent = viewModel.homeContent
    val isLoading by viewModel.isLoading.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = { viewModel.fetchHomeContent() },
        modifier = Modifier.fillMaxSize(),
        indicatorPadding = PaddingValues(top = topPadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topPadding + 16.dp,
                bottom = bottomPadding + 80.dp // Spazio per il player
            )
        ) {
            // Welcome back
            item {
                Text(
                    text = "Welcome Back, SHAdow", // TODO: Nome utente dinamico
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Your Radio
            item {
                YourRadioButton(onClick = { /* TODO */ })
            }

            // Quick Picks (usiamo le playlist)
            val playlists = homeContent.filterIsInstance<Playlist>()
            if (playlists.isNotEmpty()) {
                item {
                    HomeSection(title = "Quick Picks") {
                        QuickPicksRow(playlists = playlists, onItemClick = { /* TODO */ })
                    }
                }
            }

            // New Drops (usiamo le canzoni)
            val songs = homeContent.filterIsInstance<Song>()
            if (songs.isNotEmpty()) {
                item {
                    HomeSection(title = "New Drops") {
                        NewDropsRow(songs = songs, onItemClick = { viewModel.playSong(it) })
                    }
                }
            }

            // Placeholder se non ci sono contenuti
            if (homeContent.isEmpty() && !isLoading) {
                item {
                    Text(
                        "No content available. Swipe down to refresh.",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun HomeScreenPreview() {
    HomeScreen(topPadding = 0.dp, bottomPadding = 0.dp)
}