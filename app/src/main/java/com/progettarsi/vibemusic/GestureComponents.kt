package com.progettarsi.vibemusic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToCloseContainer(
    enabled: Boolean = true,
    resistanceFactor: Float = 0.7f,
    maxDragOffset: Float = 600f,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    var rawDragOffset by remember { mutableFloatStateOf(0f) }
    val visualOffset = rawDragOffset * resistanceFactor

    val resetAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val performFastReset = {
        scope.launch {
            resetAnim.snapTo(rawDragOffset)
            resetAnim.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing)) {
                rawDragOffset = value
            }
            rawDragOffset = 0f
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled) {
            rawDragOffset = 0f
            resetAnim.snapTo(0f)
        }
    }

    // Questa connessione gestisce lo scroll proveniente dalle liste (LazyColumn)
    val nestedScrollConnection = remember(enabled) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled) return Offset.Zero
                // Se stiamo già trascinando la finestra (offset > 0) e l'utente spinge SU,
                // consumiamo l'evento per riportare la finestra a 0 prima di scrollare la lista.
                if (rawDragOffset > 0f && available.y < 0) {
                    val newOffset = (rawDragOffset + available.y).coerceAtLeast(0f)
                    val consumed = rawDragOffset - newOffset
                    rawDragOffset = newOffset
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!enabled) return Offset.Zero
                // Se la lista è finita (overscroll) e l'utente tira GIÙ (available.y > 0)
                if (available.y > 0) {
                    rawDragOffset = (rawDragOffset + available.y).coerceIn(0f, maxDragOffset)
                    return Offset(0f, available.y) // Consumiamo l'evento
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!enabled) return super.onPostFling(consumed, available)
                if (rawDragOffset > 200f) { // Soglia chiusura
                    onClose()
                    performFastReset()
                } else if (rawDragOffset > 0f) {
                    performFastReset()
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, visualOffset.roundToInt()) }
            .nestedScroll(nestedScrollConnection)
            // Aggiungiamo ANCHE un rilevatore di drag generico per le parti della UI che non sono liste
            // (es. header della ricerca, spazi vuoti).
            .pointerInput(enabled) {
                if (enabled) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (rawDragOffset > 200f) {
                                onClose()
                                performFastReset()
                            } else {
                                performFastReset()
                            }
                        },
                        onVerticalDrag = { _, dragAmount ->
                            // Attiviamo solo se stiamo tirando giù o se la finestra è già mossa
                            if (dragAmount > 0 || rawDragOffset > 0f) {
                                rawDragOffset = (rawDragOffset + dragAmount).coerceIn(0f, maxDragOffset)
                            }
                        }
                    )
                }
            }
    ) {
        content()
    }
}