package com.progettarsi.vibemusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.progettarsi.vibemusic.ui.navigation.MainNavigation
import com.progettarsi.vibemusic.ui.theme.OpenMusicTheme
import com.progettarsi.vibemusic.viewmodel.HomeViewModel
import com.progettarsi.vibemusic.viewmodel.MusicViewModel
import com.progettarsi.vibemusic.viewmodel.SearchViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OpenMusicTheme(darkTheme = true) {
                // --- 1. INIZIALIZZAZIONE VIEWMODELS ---
                // Li creiamo qui a livello di Activity così sono condivisi e persistenti
                val musicViewModel: MusicViewModel = viewModel()
                val homeViewModel: HomeViewModel = viewModel()
                val searchViewModel: SearchViewModel = viewModel()

                // --- 2. AUDIO ENGINE INIT ---
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    // Inizializza il player (cookie, sessione) solo una volta all'avvio
                    musicViewModel.initPlayer(context)
                }

                // --- 3. UI PRINCIPALE ---
                // Passiamo i ViewModel alla navigazione che gestirà le schermate
                MainNavigation(
                    musicViewModel = musicViewModel,
                    homeViewModel = homeViewModel,
                    searchViewModel = searchViewModel
                )
            }
        }
    }
}