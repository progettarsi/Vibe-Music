package com.progettarsi.vibemusic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.progettarsi.vibemusic.model.Song
import com.progettarsi.vibemusic.ui.theme.DarkBackground
import com.progettarsi.vibemusic.ui.theme.PurplePrimary
import com.progettarsi.vibemusic.ui.theme.SurfaceHighlight
import com.progettarsi.vibemusic.ui.theme.TextGrey
import com.progettarsi.vibemusic.viewmodel.MusicViewModel
import com.progettarsi.vibemusic.viewmodel.QueueItem // <--- IMPORTANTE: Importa la classe QueueItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    musicViewModel: MusicViewModel,
    onClose: () -> Unit
) {
    val queue = musicViewModel.queue
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Configurazione Schermo per animazione uscita
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = remember(configuration, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }

    // Offset per animazione chiusura finestra
    val offsetY = remember { Animatable(0f, Float.VectorConverter) }

    // Gestione Gesture Chiusura Finestra
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (offsetY.value > 0f) {
                    val newOffset = (offsetY.value + available.y).coerceAtLeast(0f)
                    val delta = newOffset - offsetY.value
                    scope.launch { offsetY.snapTo(newOffset) }
                    return if (available.y < 0) available else Offset(0f, delta)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0) {
                    val newOffset = (offsetY.value + available.y).coerceAtLeast(0f)
                    scope.launch { offsetY.snapTo(newOffset) }
                    return available
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (offsetY.value > 150f) {
                    offsetY.animateTo(
                        targetValue = screenHeightPx,
                        animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)
                    )
                    onClose()
                } else {
                    offsetY.animateTo(0f, animationSpec = tween(durationMillis = 300))
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    LaunchedEffect(Unit) {
        musicViewModel.optimizeQueue()
        if (musicViewModel.currentSongIndex >= 0) {
            listState.scrollToItem(musicViewModel.currentSongIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = offsetY.value }
            .background(Color(0xFF121212).copy(alpha = 0.98f))
            .clickable(interactionSource = null, indication = null) {}
            .nestedScroll(nestedScrollConnection)
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            item { Box(modifier = Modifier.statusBarsPadding()) }

            itemsIndexed(
                items = queue,
                // CORREZIONE: Usiamo uniqueId del wrapper QueueItem, non videoId della Song
                key = { _, item -> item.uniqueId }
            ) { index, item ->

                // CORREZIONE: Estraiamo la canzone dal wrapper
                val song = item.song

                val isPlaying = index == musicViewModel.currentSongIndex

                // Animazione fluida per il riposizionamento
                val itemModifier = Modifier.animateItem()

                val canDismiss = !isPlaying

                if (canDismiss) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                // CORREZIONE: Usiamo removeQueueItem invece di removeSong
                                musicViewModel.removeQueueItem(item)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    // Maschera (Clip) per evitare che l'animazione esca dai bordi
                    Box(
                        modifier = itemModifier // Applichiamo animateItem qui
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = false,
                            backgroundContent = {
                                val alpha = dismissState.progress.coerceIn(0f, 1f)
                                val color = Color(0xFFCF6679).copy(alpha = alpha)

                                if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White.copy(alpha = alpha),
                                            modifier = Modifier.scale(0.8f + (0.2f * alpha))
                                        )
                                    }
                                }
                            }
                        ) {
                            QueueItem(
                                song = song,
                                isPlaying = isPlaying,
                                // CORREZIONE: Usiamo playQueueItem per settare l'indice corretto
                                onClick = { musicViewModel.playQueueItem(item) }
                            )
                        }
                    }
                } else {
                    // Item corrente (non swipabile)
                    QueueItem(
                        song = song,
                        isPlaying = isPlaying,
                        onClick = { musicViewModel.playQueueItem(item) },
                        modifier = itemModifier // Applichiamo animateItem anche qui
                    )
                }
            }
        }

        // Sfumature
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.05f)
            .background(Brush.verticalGradient(listOf(DarkBackground, Color.Transparent)))
            .align(Alignment.TopCenter))

        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.1f)
            .background(Brush.verticalGradient(listOf(Color.Transparent, DarkBackground.copy(0.6f), DarkBackground)))
            .align(Alignment.BottomCenter))

        // Controlli inferiori
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(SurfaceHighlight)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.LibraryMusic, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("From: ", color = TextGrey, fontSize = 14.sp)
                        Text(musicViewModel.currentQueueTitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            IconButton(onClick = { musicViewModel.toggleLoop() }, modifier = Modifier.size(50.dp).clip(CircleShape).background(if (musicViewModel.isLoopMode) PurplePrimary.copy(alpha = 0.2f) else SurfaceHighlight)) {
                Icon(Icons.Default.AllInclusive, null, tint = if (musicViewModel.isLoopMode) PurplePrimary else TextGrey, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = { musicViewModel.toggleShuffle() }, modifier = Modifier.size(50.dp).clip(CircleShape).background(if (musicViewModel.isShuffleMode) PurplePrimary.copy(alpha = 0.2f) else SurfaceHighlight)) {
                Icon(Icons.Rounded.Shuffle, null, tint = if (musicViewModel.isShuffleMode) PurplePrimary else TextGrey, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun QueueItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // Parametro per animazione
) {
    Row(
        modifier = modifier // Applichiamo il modificatore esterno (per animateItem)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPlaying) SurfaceHighlight else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceHighlight), contentAlignment = Alignment.Center) {
            AsyncImage(model = song.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (isPlaying) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)))
                Icon(Icons.Rounded.GraphicEq, null, tint = PurplePrimary)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, color = if (isPlaying) PurplePrimary else Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = TextGrey, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}