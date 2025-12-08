package com.progettarsi.openmusic

import android.annotation.SuppressLint
import android.graphics.Paint
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.progettarsi.openmusic.ui.theme.*
import com.progettarsi.openmusic.viewmodel.MusicViewModel
import kotlin.math.PI
import kotlin.math.sin
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider as WavySlider3
import ir.mahozad.multiplatform.wavyslider.material.WavySlider as WavySlider2
import ir.mahozad.multiplatform.wavyslider.WaveDirection.*



@Composable
fun MusicPlayerScreen(
    musicViewModel: MusicViewModel,
    onCollapse: () -> Unit
) {
    val isPlaying = musicViewModel.isPlaying
    val isBuffering = musicViewModel.isBuffering
    val realProgress = musicViewModel.progress
    val title = musicViewModel.currentTitle
    val artist = musicViewModel.currentArtist

    // URL dal ViewModel
    val coverUrl = musicViewModel.currentCoverUrl

    var scrubbingProgress by remember { mutableStateOf<Float?>(null) }
    val effectiveProgress = scrubbingProgress ?: realProgress

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
                        if (offsetY > 150f) {
                            onCollapse()
                            // FIX ANIMAZIONE: NON resettiamo offsetY a 0f qui!
                            // Lasciamo che il player rimanga spostato in basso mentre svanisce.
                            // Verrà ricreato a 0 alla prossima apertura.
                        } else {
                            offsetY = 0f // Resetta solo se l'utente annulla lo swipe
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Impediamo di trascinare verso l'alto (valori negativi)
                        val newOffset = offsetY + dragAmount.y
                        offsetY = newOffset.coerceAtLeast(0f)
                    }
                )
            }
    ) {
        // --- LOGICA DI DEBUG IMMAGINE ---
        val fallbackPainter = rememberVectorPainter(Icons.Default.Album)

        // Se c'è un URL, stampalo nel Logcat (utile per debug)
        LaunchedEffect(coverUrl) {
            if (coverUrl.isNotEmpty()) Log.d("MusicPlayerDebug", "URL Immagine: '$coverUrl'")
        }

        Box(modifier = Modifier
            .width(400.dp)
            .height(400.dp)
            .background(DarkBackground)
        )
        {
            if (coverUrl.isNotEmpty()) {

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (offsetY * 0.5f).dp),
                    placeholder = fallbackPainter,
                    error = fallbackPainter,
                    fallback = fallbackPainter
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Magenta)
                )
                {
                    Icon(Icons.Default.Album, null, tint = Color.White.copy(0.6f), modifier = Modifier.fillMaxSize().align(Alignment.Center))
                }
            }
        }

        // SFUMATURE
        Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(DarkBackground.copy(1f), Color.Transparent))))
        Box(Modifier.padding(top=280.dp).fillMaxWidth().height(120.dp).background(Brush.verticalGradient(listOf(Color.Transparent, DarkBackground))))

        // CONTENUTO
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 32.dp)
                .offset(y = offsetY.dp), // Il contenuto segue il dito
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(350.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(artist, color = TextGrey, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(title, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { isLiked = !isLiked }) {
                    AnimatedContent(targetState = isLiked, label = "Like") { liked ->
                        Icon(if(liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if(liked) PurplePrimary else Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Testo della canzone in streaming...", color = TextGrey.copy(0.9f), fontSize = 18.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))

            // BARRA + PLAY
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f).height(60.dp), contentAlignment = Alignment.CenterStart) {
                    InteractiveSquigglyBar(
                        progress = effectiveProgress,
                        onProgressChange = { scrubbingProgress = it },
                        onCommit = {
                            musicViewModel.seekTo(it)
                            scrubbingProgress = null
                        }
                    )
                }

                Box(contentAlignment = Alignment.Center) {
                    if (isBuffering) {
                        CircularProgressIndicator(modifier = Modifier.size(56.dp), color = Color.White, strokeWidth = 3.dp)
                    }
                    IconButton(
                        onClick = {
                            if (musicViewModel.currentTitle == "Nessuna Traccia") musicViewModel.playTestTrack()
                            else musicViewModel.togglePlayPause()
                        },
                        modifier = Modifier.size(56.dp).background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        AnimatedContent(targetState = isPlaying, label = "Play") { playing ->
                            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

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

// BARRA INTERATTIVA (Lineare Fix)
@Composable
fun InteractiveSquigglyBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    onCommit: (Float) -> Unit
) {
    var isScrubbing by remember { mutableStateOf(false) }
    val thumbRadius by animateDpAsState(if (isScrubbing) 16.dp else 8.dp, label = "Thumb")
    val totalSeconds = 180 // Durata finta per visualizzazione (quella reale è gestita dal ViewModel)
    val currentSeconds = (totalSeconds * progress).toInt()
    val timeString = String.format("%d:%02d", currentSeconds / 60, currentSeconds % 60)

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
        val width = maxWidth
        Canvas(Modifier.fillMaxWidth().height(40.dp).pointerInput(Unit) {
            var currentDragProgress = 0f
            detectHorizontalDragGestures(
                onDragStart = { offset ->
                    isScrubbing = true
                    currentDragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                },
                onDragEnd = { isScrubbing = false; onCommit(currentDragProgress) },
                onDragCancel = { isScrubbing = false; onCommit(currentDragProgress) },
                onHorizontalDrag = { change, _ ->
                    change.consume()
                    currentDragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    onProgressChange(currentDragProgress)
                }
            )
        }.pointerInput(Unit) {
            detectTapGestures { offset ->
                val tapProgress = (offset.x / size.width).coerceIn(0f, 1f)
                onProgressChange(tapProgress); onCommit(tapProgress)
            }
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
            val cy = centerY
            drawCircle(Color.White, thumbRadius.toPx(), Offset(cx, cy))
        }
        AnimatedVisibility(isScrubbing, enter = fadeIn()+scaleIn(), exit = fadeOut()+scaleOut(), modifier = Modifier.align(Alignment.TopStart)) {
            val offsetDp = (width * progress) - 15.dp
            Text(timeString, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(x = offsetDp, y = (-10).dp).background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}


val x: MusicViewModel = MusicViewModel()
@Preview
@Composable
fun k()
{
    MusicPlayerScreen(x, onCollapse = {})
}