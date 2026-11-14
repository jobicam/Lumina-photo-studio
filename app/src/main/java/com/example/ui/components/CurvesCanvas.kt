package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.CurveChannel
import com.example.model.CurvePoint
import com.example.model.HistogramData
import com.example.model.SingleCurve
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import kotlin.math.sqrt

@Composable
fun CurvesCanvas(
    curve: SingleCurve,
    channel: CurveChannel,
    onCurveChanged: (SingleCurve) -> Unit,
    modifier: Modifier = Modifier,
    histogram: HistogramData? = null,
    onEditFinished: () -> Unit = {}
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }

    val latestCurve by rememberUpdatedState(curve)
    val latestOnCurveChanged by rememberUpdatedState(onCurveChanged)
    val latestOnEditFinished by rememberUpdatedState(onEditFinished)

    val curveColor = when (channel) {
        CurveChannel.RGB -> Color.White
        CurveChannel.RED -> Color(0xFFFF5252)
        CurveChannel.GREEN -> Color(0xFF69F0AE)
        CurveChannel.BLUE -> Color(0xFF448AFF)
    }

    Box(
        modifier = modifier
            .size(212.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCard)
            .testTag("curves_canvas_container")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .testTag("curves_interactive_canvas")
                .pointerInput(channel) {
                    detectTapGestures { offset ->
                        val activeCurve = latestCurve
                        val normX = (offset.x / size.width).coerceIn(0f, 1f)
                        val normY = (1f - offset.y / size.height).coerceIn(0f, 1f)

                        val existingIndex = activeCurve.points.indexOfFirst { pt ->
                            val px = pt.x * size.width
                            val py = (1f - pt.y) * size.height
                            val dist = sqrt((px - offset.x) * (px - offset.x) + (py - offset.y) * (py - offset.y))
                            dist < 36f
                        }

                        if (existingIndex > 0 && existingIndex < activeCurve.points.size - 1) {
                            val updated = activeCurve.points.toMutableList()
                            updated.removeAt(existingIndex)
                            latestOnCurveChanged(SingleCurve(updated.sortedBy { it.x }))
                            latestOnEditFinished()
                        } else if (existingIndex == -1) {
                            val updated = activeCurve.points.toMutableList()
                            updated.add(CurvePoint(normX, normY))
                            latestOnCurveChanged(SingleCurve(updated.sortedBy { it.x }))
                            latestOnEditFinished()
                        }
                    }
                }
                .pointerInput(channel) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val activeCurve = latestCurve
                            val closest = activeCurve.points.indices.minByOrNull { i ->
                                val pt = activeCurve.points[i]
                                val px = pt.x * size.width
                                val py = (1f - pt.y) * size.height
                                (px - offset.x) * (px - offset.x) + (py - offset.y) * (py - offset.y)
                            }
                            if (closest != null) {
                                val pt = activeCurve.points[closest]
                                val px = pt.x * size.width
                                val py = (1f - pt.y) * size.height
                                val dist = sqrt((px - offset.x) * (px - offset.x) + (py - offset.y) * (py - offset.y))
                                if (dist < 56f) {
                                    draggedIndex = closest
                                }
                            }
                        },
                        onDragEnd = {
                            if (draggedIndex != null) {
                                draggedIndex = null
                                latestOnEditFinished()
                            }
                        },
                        onDragCancel = {
                            if (draggedIndex != null) {
                                draggedIndex = null
                                latestOnEditFinished()
                            }
                        },
                        onDrag = { change, _ ->
                            val idx = draggedIndex ?: return@detectDragGestures
                            val activeCurve = latestCurve
                            if (idx !in activeCurve.points.indices) return@detectDragGestures
                            change.consume()
                            val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                            val normY = (1f - change.position.y / size.height).coerceIn(0f, 1f)

                            val updated = activeCurve.points.toMutableList()
                            val newX = when (idx) {
                                0 -> 0f
                                updated.size - 1 -> 1f
                                else -> normX.coerceIn(0.01f, 0.99f)
                            }

                            val newPoint = CurvePoint(newX, normY)
                            updated[idx] = newPoint
                            val sorted = updated.sortedBy { it.x }
                            draggedIndex = sorted.indexOf(newPoint)
                            latestOnCurveChanged(SingleCurve(sorted))
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height

            // 0. Draw Real-Time Histogram Underlay (v2.0 Pro)
            if (histogram != null && histogram.maxCount > 0) {
                val bins = when (channel) {
                    CurveChannel.RGB -> histogram.luminance
                    CurveChannel.RED -> histogram.red
                    CurveChannel.GREEN -> histogram.green
                    CurveChannel.BLUE -> histogram.blue
                }
                val histPath = Path()
                histPath.moveTo(0f, height)
                val maxC = histogram.maxCount.toFloat().coerceAtLeast(1f)
                val binCount = bins.size
                for (b in 0 until binCount) {
                    val x = (b.toFloat() / (binCount - 1)) * width
                    val ratio = (bins[b] / maxC).coerceIn(0f, 1f)
                    val y = height - (ratio * height * 0.85f)
                    histPath.lineTo(x, y)
                }
                histPath.lineTo(width, height)
                histPath.close()

                drawPath(
                    path = histPath,
                    color = curveColor.copy(alpha = 0.22f)
                )
            }

            // 1. Draw Grid
            for (i in 1..3) {
                val frac = i / 4f
                // Vertical
                drawLine(
                    color = StudioBorder.copy(alpha = 0.5f),
                    start = Offset(width * frac, 0f),
                    end = Offset(width * frac, height),
                    strokeWidth = 1.dp.toPx()
                )
                // Horizontal
                drawLine(
                    color = StudioBorder.copy(alpha = 0.5f),
                    start = Offset(0f, height * frac),
                    end = Offset(width, height * frac),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Diagonal reference
            drawLine(
                color = StudioBorder.copy(alpha = 0.3f),
                start = Offset(0f, height),
                end = Offset(width, 0f),
                strokeWidth = 1.dp.toPx()
            )

            // 2. Draw Curve Path
            val path = Path()
            val stepCount = 60
            for (step in 0..stepCount) {
                val normX = step.toFloat() / stepCount
                val normY = curve.evaluate(normX)
                val px = normX * width
                val py = (1f - normY) * height
                if (step == 0) {
                    path.moveTo(px, py)
                } else {
                    path.lineTo(px, py)
                }
            }

            drawPath(
                path = path,
                color = curveColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Draw Points
            curve.points.forEachIndexed { index, pt ->
                val px = pt.x * width
                val py = (1f - pt.y) * height
                val isSelected = (index == draggedIndex)

                // Outer halo
                drawCircle(
                    color = if (isSelected) AccentCyan.copy(alpha = 0.4f) else curveColor.copy(alpha = 0.25f),
                    radius = if (isSelected) 12.dp.toPx() else 8.dp.toPx(),
                    center = Offset(px, py)
                )
                // Inner circle
                drawCircle(
                    color = if (isSelected) AccentCyan else curveColor,
                    radius = 5.dp.toPx(),
                    center = Offset(px, py)
                )
                // Center white dot
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}
