package com.progettarsi.openmusic

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.progettarsi.openmusic.ui.theme.DarkBackground
import com.progettarsi.openmusic.ui.theme.TextGrey
import com.progettarsi.openmusic.viewmodel.MusicViewModel
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
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val focusManager = LocalFocusManager.current

    val activeProgress = maxOf(searchProgress, playerProgress, profileProgress)
    val animatedHeight = lerp(80.dp, screenHeight, activeProgress)
    val fullScreenModeProgress = maxOf(playerProgress, profileProgress)
    val animatedPadding = lerp(16.dp, 0.dp, fullScreenModeProgress)
    val bottomPadding = lerp(16.dp, 0.dp, fullScreenModeProgress)

    val glassColor = Color(0xFF252530).copy(alpha = 0.65f)
    val borderStroke = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    val hazeStyle = HazeStyle(backgroundColor = DarkBackground, tint = HazeTint(Color.Black.copy(alpha = 0.2f)), blurRadius = 24.dp)

    val placeholders = remember { listOf("Cerca artisti...", "Digita per cercare...", "Cosa vuoi ascoltare?") }
    var currentPlaceholder by remember { mutableStateOf(placeholders.first()) }
    LaunchedEffect(searchState.isSearching) { if(searchState.isSearching) currentPlaceholder = placeholders.random() }

    // Calcolo Alpha dinamico per nascondere gli elementi quando il profilo si apre
    val hideWhenProfileOpens = (1f - profileProgress).coerceIn(0f, 1f)

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

                val topSpacerHeight = (topPadding * activeProgress) + (16.dp * searchProgress * (1f - fullScreenModeProgress))
                Spacer(modifier = Modifier.height(topSpacerHeight.coerceAtLeast(0.dp)))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(if (fullScreenModeProgress > 0.01f) animatedHeight else 64.dp)) {
                    val availableWidth = maxWidth
                    val animatedSpacerWidth = lerp(10.dp, 0.dp, fullScreenModeProgress)
                    val standardButtonSize = 64.dp
                    val fixedProfileSize = 64.dp

                    val restPlayerWidth = (availableWidth - fixedProfileSize - standardButtonSize - (10.dp * 2)).coerceAtLeast(0.dp)
                    val restSearchWidth = standardButtonSize

                    val targetPlayerWidth =
                        if(playerProgress > 0) lerp(restPlayerWidth, availableWidth, playerProgress)
                        else if(profileProgress > 0) lerp(restPlayerWidth, 0.dp, profileProgress)
                        else lerp(restPlayerWidth, 64.dp, searchProgress)

                    val targetSearchWidth =
                        if(playerProgress > 0) lerp(restSearchWidth, 0.dp, playerProgress)
                        else if(profileProgress > 0) lerp(restSearchWidth, 0.dp, profileProgress)
                        else lerp(restSearchWidth, (availableWidth - 64.dp - fixedProfileSize - (10.dp * 2)).coerceAtLeast(0.dp), searchProgress)

                    val targetProfileWidth =
                        if(playerProgress > 0) lerp(fixedProfileSize, 0.dp, playerProgress)
                        else if(profileProgress > 0) lerp(fixedProfileSize, availableWidth, profileProgress)
                        else fixedProfileSize

                    val rowArrangement = if (profileProgress > 0) Arrangement.spacedBy(animatedSpacerWidth, Alignment.End) else Arrangement.spacedBy(animatedSpacerWidth, Alignment.Start)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = if(fullScreenModeProgress > 0.1f) Alignment.Bottom else Alignment.CenterVertically,
                        horizontalArrangement = rowArrangement
                    ) {
                        // A. PLAYER
                        if (targetPlayerWidth > 1.dp) {
                            Surface(
                                color = if(playerProgress > 0.1f) Color.Transparent else glassColor,
                                shape = RoundedCornerShape(if(playerProgress > 0.1f) 0.dp else 32.dp),
                                border = if(playerProgress > 0.1f) null else borderStroke,
                                modifier = Modifier
                                    .width(targetPlayerWidth)
                                    .height(if(playerProgress > 0.1f) animatedHeight else 64.dp)
                                    .clip(RoundedCornerShape(if(playerProgress > 0.1f) 0.dp else 32.dp))
                                    .then(if(playerProgress < 0.9f) Modifier.hazeChild(hazeState, style = hazeStyle) else Modifier)
                            ) {
                                Box {
                                    if (playerProgress > 0.01f) {
                                        Box(Modifier.alpha(playerProgress)) {
                                            SwipeToCloseContainer(onClose = onClose) {
                                                MusicPlayerScreen(musicViewModel, onClose)
                                            }
                                        }
                                    }
                                    if (playerProgress < 0.99f) {
                                        // FIX: Applichiamo 'hideWhenProfileOpens' per far svanire il contenuto
                                        // mentre la larghezza si riduce, evitando l'effetto "schiacciamento"
                                        Box(modifier = Modifier.alpha((1f - playerProgress) * hideWhenProfileOpens)) {
                                            MiniPlayerContent(musicViewModel, onPlayerClick)
                                        }
                                    }
                                }
                            }
                        }

                        // B. SEARCH
                        if (targetSearchWidth > 1.dp) {
                            Surface(
                                color = glassColor,
                                shape = if(searchProgress > 0.2f) RoundedCornerShape(32.dp) else CircleShape,
                                border = borderStroke,
                                modifier = Modifier
                                    .width(targetSearchWidth)
                                    .height(64.dp)
                                    .clip(if(searchProgress > 0.2f) RoundedCornerShape(32.dp) else CircleShape)
                                    .then(if(profileProgress > 0) Modifier.alpha(hideWhenProfileOpens) else Modifier) // FIX: Fade out anche qui
                                    .hazeChild(state = hazeState, style = hazeStyle)
                                    .clickable(onClick = onSearchClick)
                            ) {
                                val isExpanded = searchProgress > 0.8f
                                DockSearchBarContent(isExpanded, searchState, musicViewModel, currentPlaceholder)
                            }
                        }

                        // C. PROFILE
                        if (targetProfileWidth > 1.dp) {
                            val containerColor = if(profileProgress > 0.1f) Color.Transparent else lerp(glassColor, DarkBackground, profileProgress)

                            Surface(
                                color = containerColor,
                                shape = RoundedCornerShape(if(profileProgress > 0.1f) 0.dp else 50.dp),
                                border = if(profileProgress > 0.1f) null else borderStroke,
                                modifier = Modifier
                                    .width(targetProfileWidth)
                                    .height(if(profileProgress > 0.1f) animatedHeight else 64.dp)
                                    .clip(RoundedCornerShape(if(profileProgress > 0.1f) 0.dp else 50.dp))
                                    .then(if(profileProgress < 0.9f) Modifier.hazeChild(state = hazeState, style = hazeStyle) else Modifier)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    // 1. Profilo Aperto
                                    if (profileProgress > 0.01f) {
                                        Box(Modifier.fillMaxSize().alpha(profileProgress)) {
                                            ProfileScreenContent(
                                                onClose = onClose,
                                                musicViewModel = musicViewModel,
                                                hazeState = hazeState
                                            )
                                        }
                                    }

                                    // 2. Icona Chiusa
                                    if (profileProgress < 0.99f) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().alpha(1f - profileProgress),
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
                            musicViewModel.isSearchingOnline -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = com.progettarsi.openmusic.ui.theme.PurplePrimary) }
                            }
                            musicViewModel.searchError != null -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(musicViewModel.searchError ?: "", color = TextGrey) }
                            }
                            else -> {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(musicViewModel.searchResults.toList()) { song ->
                                        SongResultItem(song) {
                                            musicViewModel.playSong(song)
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