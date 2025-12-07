package com.progettarsi.openmusic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.progettarsi.openmusic.model.Song
import com.progettarsi.openmusic.ui.theme.*
import com.progettarsi.openmusic.viewmodel.MusicViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val offsetY = remember { Animatable(0f) }

    LaunchedEffect(searchState.isSearching, playerState.isPlayerOpen, isProfileOpen) {
        if (searchState.isSearching || playerState.isPlayerOpen || isProfileOpen) {
            offsetY.snapTo(0f)
        }
    }

    val draggableState = rememberDraggableState { delta ->
        if (searchProgress > 0.9f || playerProgress > 0.9f || profileProgress > 0.9f) {
            coroutineScope.launch { offsetY.snapTo((offsetY.value + delta).coerceAtLeast(0f)) }
        }
    }

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

    Box(
        modifier = modifier
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .padding(horizontal = animatedPadding)
            .padding(bottom = bottomPadding)
            .height(animatedHeight)
            .fillMaxWidth()
            .draggable(state = draggableState, orientation = Orientation.Vertical, onDragStopped = {
                if (offsetY.value > 150f) {
                    onClose()
                    coroutineScope.launch { offsetY.animateTo(0f, tween(300)) }
                } else {
                    coroutineScope.launch { offsetY.animateTo(0f) }
                }
            })
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            val topSpacerHeight = (topPadding * activeProgress) + (16.dp * searchProgress * (1f - fullScreenModeProgress))
            Spacer(modifier = Modifier.height(topSpacerHeight.coerceAtLeast(0.dp)))

            // --- HEADER ROW ---
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(if (fullScreenModeProgress > 0.01f) animatedHeight else 64.dp)) {
                val availableWidth = maxWidth
                val spacerWidth = 10.dp
                val standardButtonSize = 64.dp
                val fixedProfileSize = 64.dp
                val targetPlayerWidth = if(playerProgress > 0) lerp((availableWidth - fixedProfileSize - standardButtonSize - (spacerWidth*2)).coerceAtLeast(0.dp), availableWidth, playerProgress)
                else if(profileProgress > 0) 0.dp
                else lerp((availableWidth - fixedProfileSize - standardButtonSize - (spacerWidth*2)).coerceAtLeast(0.dp), 64.dp, searchProgress)
                val targetSearchWidth = if(playerProgress > 0) 0.dp else if(profileProgress > 0) 0.dp else lerp(standardButtonSize, (availableWidth - 64.dp - fixedProfileSize - (spacerWidth*2)).coerceAtLeast(0.dp), searchProgress)
                val targetProfileWidth = if(playerProgress > 0) 0.dp else if(profileProgress > 0) lerp(fixedProfileSize, availableWidth, profileProgress) else fixedProfileSize
                val targetProfileHeight = lerp(64.dp, screenHeight, profileProgress)
                val profileCornerSize = lerp(50.dp, 0.dp, profileProgress)
                val playerCornerSize = lerp(32.dp, 0.dp, playerProgress)

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = if(fullScreenModeProgress > 0.1f) Alignment.Top else Alignment.CenterVertically, horizontalArrangement = if(fullScreenModeProgress > 0.9f) Arrangement.Start else Arrangement.spacedBy(spacerWidth)) {

                    // A. PLAYER
                    if (targetPlayerWidth > 1.dp) {
                        Surface(
                            color = if(playerProgress > 0.1f) Color.Transparent else glassColor,
                            shape = RoundedCornerShape(if(playerProgress > 0.1f) 0.dp else 32.dp),
                            border = if(playerProgress > 0.1f) null else borderStroke,
                            modifier = Modifier.width(targetPlayerWidth).height(if(playerProgress > 0.1f) animatedHeight else 64.dp).clip(RoundedCornerShape(if(playerProgress > 0.1f) 0.dp else 32.dp)).then(if(playerProgress < 0.9f) Modifier.hazeChild(hazeState, style = hazeStyle) else Modifier)
                        ) {
                            Box {
                                if (playerProgress > 0.01f) Box(Modifier.alpha(playerProgress)) { MusicPlayerScreen(musicViewModel, onClose) }
                                if (playerProgress < 0.99f) {
                                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).alpha(1f - playerProgress).clickable(onClick = onPlayerClick), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Gray.copy(0.7f)).clickable { if (musicViewModel.currentTitle == "Nessuna Traccia") musicViewModel.playTestTrack() else musicViewModel.togglePlayPause() }, contentAlignment = Alignment.Center) {
                                            val fallbackPainter = rememberVectorPainter(Icons.Default.Album)
                                            if (musicViewModel.currentCoverUrl.isNotEmpty()) {
                                                AsyncImage(model = musicViewModel.currentCoverUrl, contentDescription = null, modifier = Modifier.fillMaxSize().alpha(0.5f), placeholder = fallbackPainter, error = fallbackPainter, fallback = fallbackPainter)
                                            }
                                            if (musicViewModel.isBuffering) { CircularProgressIndicator(modifier = Modifier.size(48.dp), color = Color.White, strokeWidth = 3.dp) }
                                            AnimatedContent(targetState = musicViewModel.isPlaying, label = "MiniPlay") { playing -> Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                                        }
                                        if (searchProgress < 0.5f) { Spacer(Modifier.width(12.dp)); Column(verticalArrangement = Arrangement.Center) { Text(musicViewModel.currentTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1); Text(musicViewModel.currentArtist, color = Color.Gray, fontSize = 12.sp, maxLines = 1) } }
                                    }
                                }
                            }
                        }
                    }

                    // B. SEARCH (Modificato per ricerca dinamica)
                    if (targetSearchWidth > 1.dp) {
                        Surface(color = glassColor, shape = if(searchProgress > 0.2f) RoundedCornerShape(32.dp) else CircleShape, border = borderStroke, modifier = Modifier.width(targetSearchWidth).height(64.dp).clip(if(searchProgress > 0.2f) RoundedCornerShape(32.dp) else CircleShape).hazeChild(state = hazeState, style = hazeStyle)) {
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchProgress < 0.8f) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, null, tint = Color.White) } }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Icon(Icons.Default.Search, null, tint = TextGrey)
                                        Spacer(Modifier.width(12.dp))
                                        BasicTextField(
                                            value = searchState.query,
                                            // FIX: Ricerca dinamica su cambio testo
                                            onValueChange = {
                                                searchState.query = it
                                                musicViewModel.search(it)
                                            },
                                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                                            cursorBrush = SolidColor(PurplePrimary),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            decorationBox = { inner -> if(searchState.query.isEmpty()) Text(currentPlaceholder, color = TextGrey.copy(0.7f), maxLines = 1); inner() }
                                        )
                                        if (searchState.query.isNotEmpty()) { IconButton(onClick = { searchState.query = ""; musicViewModel.search("") }) { Icon(Icons.Default.Close, null, tint = TextGrey) } }
                                    }
                                }
                            }
                        }
                    }

                    // C. PROFILE
                    if (targetProfileWidth > 1.dp) {
                        Surface(color = if(profileProgress > 0.1f) Color.Transparent else glassColor, shape = RoundedCornerShape(if(profileProgress > 0.1f) 0.dp else 50.dp), border = if(profileProgress > 0.1f) null else borderStroke, modifier = Modifier.width(targetProfileWidth).height(if(profileProgress > 0.1f) animatedHeight else 64.dp).clip(RoundedCornerShape(if(profileProgress > 0.1f) 0.dp else 50.dp)).then(if(profileProgress < 0.9f) Modifier.hazeChild(state = hazeState, style = hazeStyle) else Modifier)) {
                            Box(contentAlignment = Alignment.Center) {
                                if (profileProgress > 0.01f) { Box(Modifier.fillMaxSize().alpha(profileProgress)) { ProfileScreenContent(onClose = onClose) } }
                                if (profileProgress < 0.99f) { Box(Modifier.fillMaxSize().alpha(1f - profileProgress), contentAlignment = Alignment.Center) { IconButton(onClick = onProfileClick) { Icon(Icons.Default.Person, null, tint = Color.White) } } }
                            }
                        }
                    }
                }
            }

            // LISTA RISULTATI (Fix Loading e Messaggi)
            if (searchProgress > 0.1f && playerProgress < 0.1f && profileProgress < 0.1f) {
                Spacer(modifier = Modifier.height(16.dp))

                if (musicViewModel.isSearchingOnline) {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PurplePrimary)
                    }
                } else if (musicViewModel.searchError != null) {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(musicViewModel.searchError ?: "", color = TextGrey)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).alpha(searchProgress).clip(RoundedCornerShape(24.dp)).background(Color(0xFF252530).copy(alpha = 0.55f)).border(borderStroke, RoundedCornerShape(24.dp))) {
                        items(musicViewModel.searchResults) { song ->
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

@Composable
fun SongResultItem(song: Song, onClick: () -> Unit) {
    val fallbackPainter = rememberVectorPainter(Icons.Default.MusicNote)
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = song.coverUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray), contentScale = androidx.compose.ui.layout.ContentScale.Crop, placeholder = fallbackPainter, error = fallbackPainter)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(song.artist, color = TextGrey, fontSize = 14.sp, maxLines = 1)
        }
        Icon(Icons.Default.PlayArrow, null, tint = TextGrey.copy(0.5f), modifier = Modifier.size(24.dp))
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
}