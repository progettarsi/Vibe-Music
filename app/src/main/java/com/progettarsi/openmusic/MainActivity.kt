package com.progettarsi.openmusic

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.progettarsi.openmusic.ui.theme.DarkBackground
import com.progettarsi.openmusic.ui.theme.OpenMusicTheme
import com.progettarsi.openmusic.viewmodel.MusicViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenMusicTheme(darkTheme = true) {
                // 1. Stati UI
                val searchState = remember { SearchState() }
                val playerState = remember { PlayerState() }

                // 2. AUDIO ENGINE
                val musicViewModel: MusicViewModel = viewModel()
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    musicViewModel.initPlayer(context)
                }

                // 3. UI Principale
                MainAppStructure(
                    searchState = searchState,
                    playerState = playerState,
                    musicViewModel = musicViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppStructure(
    searchState: SearchState,
    playerState: PlayerState,
    musicViewModel: MusicViewModel
) {
    val navController = rememberNavController()
    val hazeState = remember { HazeState() }
    val context = LocalContext.current

    // Stato Profilo
    var isProfileOpen by remember { mutableStateOf(false) }

    // --- GESTIONE TASTO INDIETRO (Priority Stack) ---
    // In Compose, vince l'ultimo BackHandler attivo.
    // Quindi mettiamo quello generico "Esci dall'app" per PRIMO (priorità più bassa),
    // e quelli specifici (chiudi profilo, ecc.) DOPO (priorità più alta).

    // 0. LIVELLO BASE: Doppio click per uscire
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            // Se sono passati meno di 2 secondi dall'ultimo click, esci
            (context as? Activity)?.finish()
        } else {
            // Primo click: aggiorna tempo e mostra messaggio
            lastBackPressTime = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    // 1. Se il PROFILO è aperto, chiudilo (Sovrascrive il livello base)
    BackHandler(enabled = isProfileOpen) {
        isProfileOpen = false
    }

    // 2. Se il PLAYER è aperto, chiudilo (minimizzalo)
    BackHandler(enabled = playerState.isPlayerOpen) {
        playerState.isPlayerOpen = false
    }

    // 3. Se la RICERCA è aperta (espansa), chiudila
    BackHandler(enabled = searchState.isSearching) {
        searchState.isSearching = false
    }

    // -----------------------------------------------

    // --- ANIMAZIONI ---
    val searchTransition = updateTransition(targetState = searchState.isSearching, label = "Search")
    val searchProgress by searchTransition.animateFloat(
        label = "SearchProgress",
        transitionSpec = { tween(300, easing = FastOutSlowInEasing) }
    ) { if (it) 1f else 0f }

    val playerTransition = updateTransition(targetState = playerState.isPlayerOpen, label = "Player")
    val playerProgress by playerTransition.animateFloat(
        label = "PlayerProgress",
        transitionSpec = { tween(400, easing = FastOutSlowInEasing) }
    ) { if (it) 1f else 0f }

    val profileTransition = updateTransition(targetState = isProfileOpen, label = "Profile")
    val profileProgress by profileTransition.animateFloat(
        label = "ProfileProgress",
        transitionSpec = { tween(400, easing = FastOutSlowInEasing) }
    ) { if (it) 1f else 0f }

    val isDockExpanded = searchProgress > 0.1f || playerProgress > 0.1f || profileProgress > 0.1f

    Scaffold(
        containerColor = DarkBackground,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {

            // LIVELLO 1: HOME
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize().haze(state = hazeState)
            ) {
                composable("home") {
                    HomeScreen(
                        topPadding = innerPadding.calculateTopPadding(),
                        bottomPadding = if (isDockExpanded) 0.dp else 160.dp
                    )
                }
            }

            // LIVELLO 2: DIMMER RICERCA
            if (searchProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = searchProgress * 0.8f))
                        .hazeChild(
                            state = hazeState,
                            style = HazeStyle(
                                backgroundColor = DarkBackground,
                                tint = HazeTint(Color.Black.copy(0.5f)),
                                blurRadius = (searchProgress * 20).dp
                            )
                        )
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            searchState.isSearching = false
                        }
                )
            }

            // LIVELLO 3: OMBRA DOCK
            if (!isDockExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f))))
                )
            }

            // LIVELLO 4: SUPER DOCK
            MorphingSearchDock(
                searchState = searchState,
                playerState = playerState,
                isProfileOpen = isProfileOpen,
                musicViewModel = musicViewModel,
                hazeState = hazeState,
                searchProgress = searchProgress,
                playerProgress = playerProgress,
                profileProgress = profileProgress,
                topPadding = innerPadding.calculateTopPadding(),
                onProfileClick = {
                    searchState.isSearching = false
                    playerState.isPlayerOpen = false
                    isProfileOpen = true
                },
                onSearchClick = { searchState.isSearching = true },
                onPlayerClick = { playerState.isPlayerOpen = true },
                onClose = {
                    if (searchState.isSearching) searchState.isSearching = false
                    if (playerState.isPlayerOpen) playerState.isPlayerOpen = false
                    if (isProfileOpen) isProfileOpen = false
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}