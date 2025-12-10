package com.progettarsi.vibemusic.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath // Importante
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath // Importante

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShapeProgressIndicator(
    modifier: Modifier = Modifier,
    shape: RoundedPolygon = MaterialShapes.Cookie12Sided,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }

    // Animazioni
    val infiniteTransition = rememberInfiniteTransition(label = "shape_loader")

    val head by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = CubicBezierEasing(0.2f, 0f, 0.4f, 1f))
        ),
        label = "head"
    )

    val tail by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
            initialStartOffset = StartOffset(300)
        ),
        label = "tail"
    )

    Box(
        modifier = modifier.drawWithCache {
            if (size.minDimension <= 0f) {
                return@drawWithCache onDrawBehind {}
            }

            // 1. Creiamo il Path e la Matrice
            val androidPath = android.graphics.Path()
            val matrix = android.graphics.Matrix()

            // Scriviamo la forma grezza nel path per misurarla
            shape.toPath(androidPath)

            // 2. Calcoliamo i bordi esatti (Bounds) della forma
            val bounds = android.graphics.RectF()
            androidPath.computeBounds(bounds, true)

            // 3. Calcolo Automatico della Scala
            // "Quanto devo ingrandire la forma per riempire il Box?"
            // Usiamo bounds.width() così si adatta a qualsiasi poligono (raggio 1, raggio 0.5, ecc.)
            val scaleFactor = size.minDimension / bounds.width()

            // 4. Applichiamo le trasformazioni nell'ordine corretto
            // A. Porta il centro esatto della forma a (0,0)
            matrix.postTranslate(-bounds.centerX(), -bounds.centerY())

            // B. Scala la forma
            matrix.postScale(scaleFactor, scaleFactor)

            // C. Sposta tutto al centro del nostro Box
            matrix.postTranslate(size.width / 2f, size.height / 2f)

            // Applica la matrice al path
            androidPath.transform(matrix)

            // --- DA QUI IN POI È UGUALE A PRIMA ---

            val pathMeasure = android.graphics.PathMeasure(androidPath, false)
            val length = pathMeasure.length

            val segmentPathAndroid = android.graphics.Path()
            val startDist = (tail * length) % length
            val endDist = (head * length) % length

            if (startDist > endDist) {
                pathMeasure.getSegment(startDist, length, segmentPathAndroid, true)
                pathMeasure.getSegment(0f, endDist, segmentPathAndroid, true)
            } else {
                pathMeasure.getSegment(startDist, endDist, segmentPathAndroid, true)
            }

            val composePath = segmentPathAndroid.asComposePath()

            onDrawBehind {
                drawPath(
                    path = composePath,
                    color = color,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    )
}