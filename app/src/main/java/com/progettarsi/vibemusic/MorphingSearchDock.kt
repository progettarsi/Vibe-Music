package com.progettarsi.vibemusic

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor // Alias per evitare conflitti
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.progettarsi.vibemusic.ui.theme.DarkBackground
import com.progettarsi.vibemusic.ui.theme.TextGrey
import com.progettarsi.vibemusic.viewmodel.MusicViewModel
import com.progettarsi.vibemusic.viewmodel.SearchViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MorphingSearchDock(
    searchState: SearchState,
    playerState: PlayerState,
    isProfileOpen: Boolean,
    musicViewModel: MusicViewModel,
    searchViewModel: SearchViewModel,
    hazeState: HazeState,
    searchProgress: Float,
    playerProgress: Float,
    profileProgress: Float,
    topPadding: Dp,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPlayerClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showBlur = playerProgress == 0f
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val focusManager = LocalFocusManager.current

    // OTTIMIZZAZIONE 1: Calcoli di base memorizzati o derivati
    val activeProgress by remember(searchProgress, playerProgress, profileProgress) {
        derivedStateOf { maxOf(searchProgress, playerProgress, profileProgress) }
    }

    val fullScreenModeProgress by remember(playerProgress, profileProgress) {
        derivedStateOf { maxOf(playerProgress, profileProgress) }
    }

    // Usiamo derivedStateOf per evitare ricalcoli inutili durante l'animazione
    val animatedHeight by remember(activeProgress, screenHeight) {
        derivedStateOf { lerp(80.dp, screenHeight, activeProgress) }
    }

    val animatedPadding by remember(fullScreenModeProgress) {
        derivedStateOf { lerp(16.dp, 0.dp, fullScreenModeProgress) }
    }

    val bottomPadding by remember(fullScreenModeProgress) {
        derivedStateOf { lerp(16.dp, 0.dp, fullScreenModeProgress) }
    }

    val glassColor = remember { Color(0xFF252530).copy(alpha = 0.65f) }
    // Colore di fallback più coprente per quando Haze è spento durante l'animazione
    val fastGlassColor = remember { Color(0xFF252530).copy(alpha = 0.95f) }
    val borderStroke = remember { BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)) }
    val hazeStyle = remember {
        HazeStyle(backgroundColor = DarkBackground, tint = HazeTint(Color.Black.copy(alpha = 0.2f)), blurRadius = 24.dp)
    }

    val context = LocalContext.current
    val placeholders = listOf(
        stringResource(R.string.search_placeholder_1),
        stringResource(R.string.search_placeholder_2),
        stringResource(R.string.search_placeholder_3)
    )
    var currentPlaceholder by remember { mutableStateOf(placeholders.first()) }

    LaunchedEffect(searchState.isSearching) {
        if(searchState.isSearching) currentPlaceholder = placeholders.random()
    }

    val hideWhenProfileOpens by remember(profileProgress) {
        derivedStateOf { (1f - profileProgress).coerceIn(0f, 1f) }
    }

    Box(
        modifier = modifier
            .padding(horizontal = animatedPadding)
            .padding(bottom = bottomPadding)
            .height(animatedHeight)
            .fillMaxWidth()
    ) {
        val isSearchSwipeEnabled = searchProgress > 0.1f && playerProgress < 0.1f && profileProgress < 0.1f

        SwipeToCloseContainer(
            enabled = isSearchSwipeEnabled,
            resistanceFactor = 1.0f,
            onClose = onClose
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                val topSpacerHeight by remember(activeProgress, topPadding, searchProgress, fullScreenModeProgress) {
                    derivedStateOf {
                        (topPadding * activeProgress) + (16.dp * searchProgress * (1f - fullScreenModeProgress))
                        val totalPadding = topPadding + 16.dp
                        totalPadding * searchProgress * (1f - fullScreenModeProgress)
                    }
                }

                Spacer(modifier = Modifier.height(topSpacerHeight.coerceAtLeast(0.dp)))

                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().height(if (fullScreenModeProgress > 0.01f) animatedHeight else 64.dp)
                ) {
                    val availableWidth = maxWidth

                    // OTTIMIZZAZIONE 3: Tutta la matematica pesante delle larghezze è "cachata" qui
                    // Ricalcola SOLO se le progress bar cambiano.
                    val widthState by remember(activeProgress, availableWidth) {
                        derivedStateOf {
                            val animatedSpacerWidth = lerp(10.dp, 0.dp, fullScreenModeProgress)
                            val standardButtonSize = 64.dp
                            val fixedProfileSize = 64.dp

                            val restPlayerWidth = (availableWidth - fixedProfileSize - standardButtonSize - (10.dp * 2)).coerceAtLeast(0.dp)
                            val restSearchWidth = standardButtonSize

                            val pWidth = if(playerProgress > 0) lerp(restPlayerWidth, availableWidth, playerProgress)
                            else if(profileProgress > 0) lerp(restPlayerWidth, 0.dp, profileProgress)
                            else lerp(restPlayerWidth, 64.dp, searchProgress)

                            val sWidth = if(playerProgress > 0) lerp(restSearchWidth, 0.dp, playerProgress)
                            else if(profileProgress > 0) lerp(restSearchWidth, 0.dp, profileProgress)
                            else lerp(restSearchWidth, (availableWidth - 64.dp - fixedProfileSize - (10.dp * 2)).coerceAtLeast(0.dp), searchProgress)

                            val prWidth = if(playerProgress > 0) lerp(fixedProfileSize, 0.dp, playerProgress)
                            else if(profileProgress > 0) lerp(fixedProfileSize, availableWidth, profileProgress)
                            else fixedProfileSize

                            val spacer = animatedSpacerWidth

                            Triple(pWidth, sWidth, prWidth) to spacer
                        }
                    }

                    val (widths, animatedSpacerWidth) = widthState
                    val (targetPlayerWidth, targetSearchWidth, targetProfileWidth) = widths

                    val rowArrangement = if (profileProgress > 0) Arrangement.spacedBy(animatedSpacerWidth, Alignment.End) else Arrangement.spacedBy(animatedSpacerWidth, Alignment.Start)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = if(fullScreenModeProgress > 0.1f) Alignment.Bottom else Alignment.CenterVertically,
                        horizontalArrangement = rowArrangement
                    ) {
                        // A. PLAYER
                        if (targetPlayerWidth > 1.dp) {
                            val playerShape = if(playerProgress > 0.1f) RoundedCornerShape(0.dp) else RoundedCornerShape(32.dp)

                            val isStaticMiniPlayer = playerProgress < 0.01f

                            Surface(
                                // Se non c'è blur, usa un colore leggermente più solido
                                color = if (isStaticMiniPlayer) glassColor else fastGlassColor,
                                shape = playerShape,
                                border = if(playerProgress > 0.1f) null else borderStroke,
                                modifier = Modifier
                                    .width(targetPlayerWidth)
                                    .height(if(playerProgress > 0.1f) animatedHeight else 64.dp)
                                    .clip(playerShape)
                                    // APPLICAZIONE CONDIZIONALE OTTIMIZZATA
                                    .then(if(isStaticMiniPlayer) Modifier.hazeChild(hazeState, style = hazeStyle) else Modifier)
                            ) {
                                Box {
                                    if (playerProgress > 0.01f) {
                                        // OTTIMIZZAZIONE 4: graphicsLayer invece di alpha
                                        Box(Modifier.graphicsLayer { alpha = playerProgress }) {
                                            SwipeToCloseContainer(onClose = onClose) {
                                                MusicPlayerScreen(musicViewModel, onClose)
                                            }
                                        }
                                    }
                                    if (playerProgress < 0.99f) {
                                        Box(modifier = Modifier.graphicsLayer { alpha = (1f - playerProgress) * hideWhenProfileOpens }) {
                                            MiniPlayerContent(musicViewModel, onPlayerClick)
                                        }
                                    }
                                }
                            }
                        }

                        // B. SEARCH
                        if (targetSearchWidth > 1.dp) {
                            val searchShape = if(searchProgress > 0.2f) RoundedCornerShape(32.dp) else CircleShape

                            Surface(
                                color = glassColor,
                                shape = searchShape,
                                border = borderStroke,
                                modifier = Modifier
                                    .width(targetSearchWidth)
                                    .height(64.dp)
                                    .clip(searchShape)
                                    .graphicsLayer { alpha = hideWhenProfileOpens }
                                    .hazeChild(state = hazeState, style = hazeStyle)
                                    .clickable(onClick = onSearchClick)
                            ) {
                                val isExpanded = searchProgress > 0.8f
                                DockSearchBarContent(isExpanded, searchViewModel, currentPlaceholder)
                            }
                        }

                        // C. PROFILE
                        if (targetProfileWidth > 1.dp) {
                            val profileShape = if(profileProgress > 0.1f) RoundedCornerShape(0.dp) else RoundedCornerShape(50.dp)
                            val containerColor = if(profileProgress > 0.1f) Color.Transparent else lerpColor(glassColor, DarkBackground, profileProgress)

                            Surface(
                                color = containerColor,
                                shape = profileShape,
                                border = if(profileProgress > 0.1f) null else borderStroke,
                                modifier = Modifier
                                    .width(targetProfileWidth)
                                    .height(if(profileProgress > 0.1f) animatedHeight else 64.dp)
                                    .clip(profileShape)
                                    .then(if(profileProgress < 0.9f) Modifier.hazeChild(state = hazeState, style = hazeStyle) else Modifier)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (profileProgress > 0.01f) {
                                        Box(Modifier.fillMaxSize().graphicsLayer { alpha = profileProgress }) {
                                            ProfileScreenContent(
                                                onClose = onClose,
                                                musicViewModel = musicViewModel,
                                                hazeState = hazeState
                                            )
                                        }
                                    }
                                    if (profileProgress < 0.99f) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 1f - profileProgress },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize().clickable(onClick = onProfileClick), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Person, null, tint = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // LISTA RISULTATI RICERCA
                if (searchProgress > 0.1f && playerProgress < 0.1f && profileProgress < 0.1f) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF252530).copy(alpha = 0.55f))
                            .border(borderStroke, RoundedCornerShape(24.dp))
                    ) {
                        when {
                            searchViewModel.isSearchingOnline -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = com.progettarsi.vibemusic.ui.theme.PurplePrimary) }
                            }
                            searchViewModel.searchError != null -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(searchViewModel.searchError ?: "", color = TextGrey) }
                            }
                            else -> {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(searchViewModel.searchResults.toList()) { song ->
                                        SongResultItem(song) {
                                            // PRIMA: musicViewModel.playSong(song)
                                            // ORA:
                                            musicViewModel.startRadio(song)

                                            focusManager.clearFocus()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}