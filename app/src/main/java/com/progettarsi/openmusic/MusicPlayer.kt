package com.progettarsi.openmusic

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.progettarsi.openmusic.ui.theme.* import com.progettarsi.openmusic.viewmodel.MusicViewModel // Import fondamentale
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun MusicPlayerScreen(
    musicViewModel: MusicViewModel, // <-- ORA RICEVE IL VIEWMODEL REALE
    onCollapse: () -> Unit
) {
    // Osserviamo i dati VERI dal ViewModel
    val isPlaying = musicViewModel.isPlaying
    val progress = musicViewModel.progress
    val title = musicViewModel.currentTitle
    val artist = musicViewModel.currentArtist

    // Stati UI locali
    var isLoopOn by remember { mutableStateOf(false) }
    var isShuffleOn by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY > 150f) onCollapse()
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetY += dragAmount.y
                    }
                )
            }
    ) {
        // SFONDO
        Image(
            painter = rememberVectorPainter(Icons.Default.Album),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().offset(y = (offsetY * 0.5f).dp).alpha(0.6f)
        )

        // SFUMATURE
        Column(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(DarkBackground.copy(0.9f), Color.Transparent))))
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(400.dp).background(Brush.verticalGradient(listOf(Color.Transparent, DarkBackground.copy(0.5f), DarkBackground))))
        }

        // CONTENUTO
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp).offset(y = offsetY.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // INFO
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(artist, color = TextGrey, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(title, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { isLiked = !isLiked }) {
                    Icon(if(isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if(isLiked) PurplePrimary else Color.White, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Per ora Lyrics statiche
            Text("Testo della canzone in streaming...", color = TextGrey.copy(0.9f), fontSize = 18.sp, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(48.dp))

            // BARRA + PLAY
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f).height(60.dp), contentAlignment = Alignment.CenterStart) {
                    InteractiveSquigglyBar(
                        progress = progress,
                        // Quando l'utente scorre, diciamo al ViewModel di saltare
                        onProgressChange = { musicViewModel.seekTo(it) }
                    )
                }

                IconButton(
                    onClick = {
                        // LOGICA TEST: Se non c'è nulla, carica la traccia di test. Altrimenti Play/Pause.
                        if (musicViewModel.currentTitle == "Nessuna Traccia") {
                            musicViewModel.playTestTrack()
                        } else {
                            musicViewModel.togglePlayPause()
                        }
                    },
                    modifier = Modifier.size(56.dp).background(Color.White.copy(0.1f), CircleShape)
                ) {
                    AnimatedContent(targetState = isPlaying, label = "PlayAnim") { playing ->
                        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // CONTROLLI EXTRA
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { isLoopOn = !isLoopOn }) {
                    Icon(Icons.Default.AllInclusive, null, tint = if(isLoopOn) Color.White else TextGrey.copy(0.5f), modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { isShuffleOn = !isShuffleOn }) {
                    Icon(Icons.Default.Shuffle, null, tint = if(isShuffleOn) Color.White else TextGrey.copy(0.5f), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// (Copia qui sotto anche la funzione InteractiveSquigglyBar dal vecchio file,
// o assicurati che sia inclusa se l'hai copiata nel file precedente.
// È identica a prima ma serve per compilare.)
@Composable
fun InteractiveSquigglyBar(progress: Float, onProgressChange: (Float) -> Unit) {
    // ... INCOLLA QUI LA FUNZIONE InteractiveSquigglyBar CHE TI HO DATO PRIMA ...
    // Se non ce l'hai sottomano, dimmelo e te la rimetto.
    // (Per brevità non la ripeto se hai già il codice funzionante della barra)

    // ECCOLA PER SICUREZZA:
    var isScrubbing by remember { mutableStateOf(false) }
    val thumbRadius by animateDpAsState(if (isScrubbing) 16.dp else 8.dp, label = "Thumb")

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
        Canvas(Modifier.fillMaxWidth().height(40.dp).pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { isScrubbing = true },
                onDragEnd = { isScrubbing = false },
                onDragCancel = { isScrubbing = false },
                onHorizontalDrag = { change, _ ->
                    change.consume()
                    onProgressChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
            )
        }.pointerInput(Unit) {
            detectTapGestures { offset -> onProgressChange((offset.x / size.width).coerceIn(0f, 1f)) }
        }) {
            val centerY = size.height / 2
            val path = Path().apply {
                moveTo(0f, centerY)
                for (x in 0..size.width.toInt() step 5) {
                    lineTo(x.toFloat(), centerY + 15f * sin((x.toFloat() / size.width) * 40f * PI.toFloat()))
                }
            }
            drawPath(path, Color.White.copy(0.2f), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            clipRect(right = size.width * progress) {
                drawPath(path, PurplePrimary, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            }
            val cx = size.width * progress
            val cy = centerY + 15f * sin((cx / size.width) * 40f * PI.toFloat())
            drawCircle(Color.White, thumbRadius.toPx(), Offset(cx, cy))
        }
    }
}