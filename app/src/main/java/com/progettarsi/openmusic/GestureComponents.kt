package com.progettarsi.openmusic

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
    resistanceFactor: Float = 1.0f,
    maxDragOffset: Float = 400f, // NUOVO: Limite massimo di trascinamento (pixel)
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
            resetAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) {
                rawDragOffset = value
            }
            rawDragOffset = 0f
        }
    }

    LaunchedEffect(enabled) {
        if (!enabled && rawDragOffset > 0f) {
            performFastReset()
        }
    }

    val nestedScrollConnection = remember(enabled) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled) return Offset.Zero
                if (rawDragOffset > 0f) {
                    // Applichiamo il limite massimo (maxDragOffset)
                    val newOffset = (rawDragOffset + available.y).coerceIn(0f, maxDragOffset)
                    rawDragOffset = newOffset
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!enabled) return Offset.Zero
                if (available.y > 0) {
                    // Applichiamo il limite anche qui
                    rawDragOffset = (rawDragOffset + available.y).coerceIn(0f, maxDragOffset)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!enabled) return super.onPostFling(consumed, available)

                val isFlingDown = available.y > 1000f
                val isPastThreshold = rawDragOffset > 150f

                if (isPastThreshold || isFlingDown) {
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
            .pointerInput(enabled) {
                if (enabled) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (rawDragOffset > 150f) {
                                onClose()
                                performFastReset()
                            } else if (rawDragOffset > 0f) {
                                performFastReset()
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (rawDragOffset > 0f || dragAmount > 0f) {
                                change.consume()
                                // Applichiamo il limite anche al drag diretto
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