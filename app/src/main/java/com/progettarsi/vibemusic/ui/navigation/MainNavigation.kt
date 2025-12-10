package com.progettarsi.vibemusic.ui.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.progettarsi.vibemusic.HomeScreen
import com.progettarsi.vibemusic.MorphingSearchDock
import com.progettarsi.vibemusic.PlayerState
import com.progettarsi.vibemusic.SearchState
import com.progettarsi.vibemusic.ui.theme.DarkBackground
import com.progettarsi.vibemusic.viewmodel.HomeViewModel
import com.progettarsi.vibemusic.viewmodel.MusicViewModel
import com.progettarsi.vibemusic.viewmodel.SearchViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@Composable
fun MainNavigation(
    musicViewModel: MusicViewModel,
    homeViewModel: HomeViewModel,
    searchViewModel: SearchViewModel
) {
    val navController = rememberNavController()
    val hazeState = remember { HazeState() }
    val context = LocalContext.current

    // Stati UI Locali (Gestione visibilità Dock/Profilo)
    val searchState = remember { SearchState() } // Gestisce "isSearching" (UI espansa)
    val playerState = remember { PlayerState() } // Gestisce "isPlayerOpen"
    var isProfileOpen by remember { mutableStateOf(false) }

    // --- GESTIONE TASTO INDIETRO (Priority Stack) ---

    // 0. LIVELLO BASE: Doppio click per uscire
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler(enabled = true) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Premi ancora per uscire", Toast.LENGTH_SHORT).show()
        }
    }

    // 1. Chiudi PROFILO (Priorità Alta)
    BackHandler(enabled = isProfileOpen) { isProfileOpen = false }

    // 2. Chiudi PLAYER (Priorità Alta)
    BackHandler(enabled = playerState.isPlayerOpen) { playerState.isPlayerOpen = false }

    // 3. Chiudi RICERCA (Priorità Alta)
    BackHandler(enabled = searchState.isSearching) { searchState.isSearching = false }

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

            // LIVELLO 1: HOME SCREEN
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState) // Sorgente effetto vetro
            ) {
                composable("home") {
                    HomeScreen(
                        topPadding = innerPadding.calculateTopPadding(),
                        bottomPadding = if (isDockExpanded) 0.dp else 160.dp,
                        homeViewModel = homeViewModel,
                        musicViewModel = musicViewModel
                    )
                }
            }

            // LIVELLO 2: DIMMER RICERCA (Oscura la home quando cerchi)
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
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            searchState.isSearching = false
                        }
                )
            }

            // LIVELLO 3: OMBRA DOCK (Gradiente in basso)
            if (!isDockExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.9f))))
                )
            }

            // LIVELLO 4: SUPER DOCK (Player / Search / Profile)
            MorphingSearchDock(
                searchState = searchState,
                playerState = playerState,
                isProfileOpen = isProfileOpen,
                musicViewModel = musicViewModel,
                searchViewModel = searchViewModel,
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