package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.BlemishSuggestion
import com.example.model.CropRect
import com.example.model.HistogramData
import com.example.model.RetouchType
import com.example.model.StrokePoint
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun PhotoCanvas(
    displayBitmap: Bitmap?,
    originalBitmap: Bitmap?,
    isRetouchMode: Boolean,
    retouchType: RetouchType,
    brushRadius: Float,
    isComparing: Boolean,
    isSplitView: Boolean,
    splitRatio: Float,
    showGridOverlay: Boolean = false,
    isCropMode: Boolean = false,
    cropRect: CropRect = CropRect(),
    lockedCropRatio: Float? = null,
    suggestedBlemishes: List<BlemishSuggestion> = emptyList(),
    isBlurFocusMode: Boolean = false,
    blurFocusX: Float = 0.50f,
    blurFocusY: Float = 0.42f,
    blurFocusRadius: Float = 0.32f,
    lang: AppLanguage = AppLanguage.SYSTEM,
    onCropRectChanged: (CropRect) -> Unit = {},
    onBlurFocusChanged: (Float, Float) -> Unit = { _, _ -> },
    onSplitRatioChanged: (Float) -> Unit,
    onRetouchStroke: (List<StrokePoint>) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom/pan when image dimensions change or crop mode engages
    LaunchedEffect(displayBitmap?.width, displayBitmap?.height, isCropMode) {
        scale = 1f
        offset = Offset.Zero
    }

    // Brush cursor tracking for Retouch mode
    var activeTouchPos by remember { mutableStateOf<Offset?>(null) }
    val currentPoints = remember { mutableStateListOf<StrokePoint>() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .testTag("photo_editor_canvas_container"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        if (displayBitmap != null && containerWidth > 0f && containerHeight > 0f) {
            val bmpWidth = displayBitmap.width.toFloat()
            val bmpHeight = displayBitmap.height.toFloat()

            // Calculate fitted image dimensions inside container
            val fitScale = min(containerWidth / bmpWidth, containerHeight / bmpHeight)
            val renderW = bmpWidth * fitScale
            val renderH = bmpHeight * fitScale
            val imgLeft = (containerWidth - renderW) / 2f
            val imgTop = (containerHeight - renderH) / 2f

            val latestBlurFocusMode by rememberUpdatedState(isBlurFocusMode)
            val latestOnBlurFocusChanged by rememberUpdatedState(onBlurFocusChanged)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isRetouchMode, isSplitView, isCropMode, renderW, renderH, imgLeft, imgTop) {
                        if (!isRetouchMode && !isSplitView && !isCropMode) {
                            detectTapGestures(
                                onTap = { tapPos ->
                                    if (latestBlurFocusMode) {
                                        val cx = containerWidth / 2f
                                        val cy = containerHeight / 2f
                                        val contentX = (tapPos.x - cx - offset.x) / scale + cx
                                        val contentY = (tapPos.y - cy - offset.y) / scale + cy
                                        val normX = ((contentX - imgLeft) / renderW).coerceIn(0f, 1f)
                                        val normY = ((contentY - imgTop) / renderH).coerceIn(0f, 1f)
                                        latestOnBlurFocusChanged(normX, normY)
                                    }
                                },
                                onDoubleTap = { tapPos ->
                                    if (scale > 1.05f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        val targetScale = 2.5f
                                        val cx = containerWidth / 2f
                                        val cy = containerHeight / 2f
                                        val focalShift = Offset(cx - tapPos.x, cy - tapPos.y) * (targetScale - 1f)
                                        scale = targetScale
                                        offset = clampCanvasOffset(
                                            candidate = focalShift,
                                            targetScale = targetScale,
                                            renderW = renderW,
                                            renderH = renderH,
                                            containerW = containerWidth,
                                            containerH = containerHeight
                                        )
                                    }
                                }
                            )
                        }
                    }
                    .pointerInput(isRetouchMode, isSplitView, isCropMode, renderW, renderH) {
                        if (!isRetouchMode && !isSplitView && !isCropMode) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(MIN_CANVAS_ZOOM, MAX_CANVAS_ZOOM)
                                if (nextScale <= 1.01f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    val ratio = if (scale > 0.01f) nextScale / scale else 1f
                                    scale = nextScale
                                    offset = clampCanvasOffset(
                                        candidate = offset * ratio + pan,
                                        targetScale = nextScale,
                                        renderW = renderW,
                                        renderH = renderH,
                                        containerW = containerWidth,
                                        containerH = containerHeight
                                    )
                                }
                            }
                        }
                    }
                    .pointerInput(isRetouchMode, isSplitView, isCropMode, renderW, renderH, imgLeft, imgTop) {
                        if (isRetouchMode && !isSplitView && !isCropMode) {
                            // Supports 1-finger tap/drag retouching AND 2-finger pinch-to-zoom + pan
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()

                                fun mapScreenToImagePoint(screenPos: Offset): Offset {
                                    val cx = containerWidth / 2f
                                    val cy = containerHeight / 2f
                                    val contentX = (screenPos.x - cx - offset.x) / scale + cx
                                    val contentY = (screenPos.y - cy - offset.y) / scale + cy
                                    return Offset(contentX, contentY)
                                }

                                var isMultiTouchZoom = false
                                val startContent = mapScreenToImagePoint(down.position)
                                activeTouchPos = startContent
                                currentPoints.clear()
                                currentPoints.add(
                                    StrokePoint(
                                        normX = ((startContent.x - imgLeft) / renderW).coerceIn(0f, 1f),
                                        normY = ((startContent.y - imgTop) / renderH).coerceIn(0f, 1f)
                                    )
                                )

                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    val activeChanges = event.changes.filter { it.pressed }
                                    if (activeChanges.isEmpty()) {
                                        event.changes.forEach { it.consume() }
                                        break
                                    }

                                    if (activeChanges.size >= 2) {
                                        if (!isMultiTouchZoom) {
                                            isMultiTouchZoom = true
                                            currentPoints.clear()
                                            activeTouchPos = null
                                        }
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        val nextScale = (scale * zoomChange).coerceIn(MIN_CANVAS_ZOOM, MAX_CANVAS_ZOOM)
                                        if (nextScale <= 1.01f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            val ratio = if (scale > 0.01f) nextScale / scale else 1f
                                            scale = nextScale
                                            offset = clampCanvasOffset(
                                                candidate = offset * ratio + panChange,
                                                targetScale = nextScale,
                                                renderW = renderW,
                                                renderH = renderH,
                                                containerW = containerWidth,
                                                containerH = containerHeight
                                            )
                                        }
                                        event.changes.forEach { it.consume() }
                                    } else if (!isMultiTouchZoom) {
                                        val change = activeChanges.first()
                                        change.consume()
                                        val contentPos = mapScreenToImagePoint(change.position)
                                        activeTouchPos = contentPos
                                        currentPoints.add(
                                            StrokePoint(
                                                normX = ((contentPos.x - imgLeft) / renderW).coerceIn(0f, 1f),
                                                normY = ((contentPos.y - imgTop) / renderH).coerceIn(0f, 1f)
                                            )
                                        )
                                    } else {
                                        activeChanges.forEach { it.consume() }
                                    }
                                }

                                if (!isMultiTouchZoom && currentPoints.isNotEmpty()) {
                                    onRetouchStroke(currentPoints.toList())
                                }
                                currentPoints.clear()
                                activeTouchPos = null
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                val showBmp = if (isComparing && originalBitmap != null) originalBitmap else displayBitmap

                if (!isSplitView || originalBitmap == null) {
                    // Standard Single View
                    Image(
                        bitmap = showBmp.asImageBitmap(),
                        contentDescription = "Edited photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("main_display_photo"),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Split Before/After View
                    val splitX = containerWidth * splitRatio

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasW = size.width
                        val canvasH = size.height

                        // Draw Left Half (Original Before)
                        val leftPath = Path().apply {
                            addRect(Rect(0f, 0f, splitX, canvasH))
                        }
                        clipPath(leftPath) {
                            drawImage(
                                image = originalBitmap.asImageBitmap(),
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(originalBitmap.width, originalBitmap.height),
                                dstOffset = IntOffset(imgLeft.toInt(), imgTop.toInt()),
                                dstSize = IntSize(renderW.toInt(), renderH.toInt())
                            )
                        }

                        // Draw Right Half (Edited After)
                        val rightPath = Path().apply {
                            addRect(Rect(splitX, 0f, canvasW, canvasH))
                        }
                        clipPath(rightPath) {
                            drawImage(
                                image = displayBitmap.asImageBitmap(),
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(displayBitmap.width, displayBitmap.height),
                                dstOffset = IntOffset(imgLeft.toInt(), imgTop.toInt()),
                                dstSize = IntSize(renderW.toInt(), renderH.toInt())
                            )
                        }

                        // Split Line
                        drawLine(
                            color = Color.White,
                            start = Offset(splitX, 0f),
                            end = Offset(splitX, canvasH),
                            strokeWidth = 2.dp.toPx()
                        )
                        // Split Line Handle indicator
                        drawCircle(
                            color = AccentCyan,
                            radius = 12.dp.toPx(),
                            center = Offset(splitX, canvasH / 2f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(splitX, canvasH / 2f)
                        )
                    }
                }

                // Rule-of-Thirds Composition Grid Overlay (when enabled in Settings, outside crop mode)
                if (showGridOverlay && !isCropMode) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val lineColor = Color.White.copy(alpha = 0.35f)
                        val strokeW = 1.dp.toPx()
                        // Vertical 1/3 and 2/3 lines inside image frame
                        for (i in 1..2) {
                            val gx = imgLeft + renderW * (i / 3f)
                            drawLine(
                                color = lineColor,
                                start = Offset(gx, imgTop),
                                end = Offset(gx, imgTop + renderH),
                                strokeWidth = strokeW
                            )
                            val gy = imgTop + renderH * (i / 3f)
                            drawLine(
                                color = lineColor,
                                start = Offset(imgLeft, gy),
                                end = Offset(imgLeft + renderW, gy),
                                strokeWidth = strokeW
                            )
                        }
                    }
                }

                // Automated Blemish Detection suggestion markers
                if (isRetouchMode &&
                    retouchType == RetouchType.BLEMISH &&
                    !isSplitView &&
                    !isComparing &&
                    suggestedBlemishes.isNotEmpty()
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("blemish_suggestions_overlay")
                    ) {
                        val minRenderDim = min(renderW, renderH)
                        val strokeW = 1.8.dp.toPx()
                        val minTargetPx = 10.dp.toPx()
                        val maxTargetPx = 30.dp.toPx()

                        for (spot in suggestedBlemishes) {
                            val cx = imgLeft + spot.normX * renderW
                            val cy = imgTop + spot.normY * renderH
                            val targetR = (spot.normRadius * minRenderDim).coerceIn(minTargetPx, maxTargetPx)

                            drawCircle(
                                color = AccentAmber.copy(alpha = 0.14f),
                                radius = targetR,
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = AccentAmber.copy(alpha = 0.92f),
                                radius = targetR,
                                center = Offset(cx, cy),
                                style = Stroke(width = strokeW)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.75f),
                                radius = (targetR + 2.dp.toPx()),
                                center = Offset(cx, cy),
                                style = Stroke(width = 0.9.dp.toPx())
                            )
                            drawCircle(
                                color = AccentCyan,
                                radius = 2.dp.toPx(),
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }

                // Brush cursor ring indicator for Retouch mode
                if (isRetouchMode && activeTouchPos != null) {
                    val pos = activeTouchPos!!
                    val cursorRadiusPx = (brushRadius * min(renderW, renderH) / 400f).coerceAtLeast(10f)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = AccentCyan.copy(alpha = 0.28f),
                            radius = cursorRadiusPx,
                            center = pos
                        )
                        drawCircle(
                            color = AccentCyan,
                            radius = cursorRadiusPx,
                            center = pos,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = pos
                        )
                    }
                }
                // Interactive Background Blur Focus Ring
                if (isBlurFocusMode && !isRetouchMode && !isSplitView && !isCropMode && !isComparing) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("blur_focus_overlay")
                    ) {
                        val fCx = imgLeft + blurFocusX.coerceIn(0f, 1f) * renderW
                        val fCy = imgTop + blurFocusY.coerceIn(0f, 1f) * renderH
                        val rx = (blurFocusRadius.coerceIn(0.15f, 0.70f) * min(renderW, renderH)).coerceAtLeast(16f)
                        val ry = rx * 1.15f
                        drawOval(
                            color = AccentCyan.copy(alpha = 0.55f),
                            topLeft = Offset(fCx - rx, fCy - ry),
                            size = Size(rx * 2f, ry * 2f),
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = 6.dp.toPx(),
                            center = Offset(fCx, fCy),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        drawCircle(
                            color = AccentCyan,
                            radius = 2.5.dp.toPx(),
                            center = Offset(fCx, fCy)
                        )
                    }
                }
                // Interactive Crop Overlay (when Crop tab is active & not comparing)
                if (isCropMode && !isSplitView && !isComparing) {
                    InteractiveCropOverlay(
                        cropRect = cropRect,
                        lockedCropRatio = lockedCropRatio,
                        imgLeft = imgLeft,
                        imgTop = imgTop,
                        renderW = renderW,
                        renderH = renderH,
                        bmpWidth = displayBitmap.width,
                        bmpHeight = displayBitmap.height,
                        onCropRectChanged = onCropRectChanged
                    )
                }
            }
        }

        // Split view drag gesture overlay on the slider line
        if (isSplitView) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val newRatio = (change.position.x / size.width).coerceIn(0.05f, 0.95f)
                            onSplitRatioChanged(newRatio)
                        }
                    }
            )

            // Before / After labels
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "BEFORE",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    text = "AFTER",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Detected blemish suggestion count pill
        if (isRetouchMode &&
            retouchType == RetouchType.BLEMISH &&
            !isSplitView &&
            !isComparing &&
            suggestedBlemishes.isNotEmpty()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(StudioSurface.copy(alpha = 0.88f))
                    .border(1.dp, AccentAmber.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .testTag("blemish_suggestions_badge")
            ) {
                Text(
                    text = AppStrings.blemishOverlayHint(suggestedBlemishes.size, lang),
                    color = AccentAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Before indicator badge when holding compare
        if (isComparing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(AccentAmber, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ORIGINAL PHOTO",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // Quick Zoom In / Out / Reset pill bar for precision editing & retouching
        if (displayBitmap != null && containerWidth > 0f && containerHeight > 0f && !isSplitView && !isCropMode && !isComparing) {
            val bmpWidth = displayBitmap.width.toFloat()
            val bmpHeight = displayBitmap.height.toFloat()
            val fitScale = min(containerWidth / bmpWidth, containerHeight / bmpHeight)
            val renderW = bmpWidth * fitScale
            val renderH = bmpHeight * fitScale

            ZoomControlsBar(
                scale = scale,
                lang = lang,
                onZoomOut = {
                    val (nextScale, nextOffset) = computeZoomedState(
                        currentScale = scale,
                        currentOffset = offset,
                        targetScale = nextZoomOutStep(scale),
                        renderW = renderW,
                        renderH = renderH,
                        containerW = containerWidth,
                        containerH = containerHeight
                    )
                    scale = nextScale
                    offset = nextOffset
                },
                onZoomIn = {
                    val (nextScale, nextOffset) = computeZoomedState(
                        currentScale = scale,
                        currentOffset = offset,
                        targetScale = nextZoomInStep(scale),
                        renderW = renderW,
                        renderH = renderH,
                        containerW = containerWidth,
                        containerH = containerHeight
                    )
                    scale = nextScale
                    offset = nextOffset
                },
                onToggleReset = {
                    if (scale > 1.05f) {
                        scale = MIN_CANVAS_ZOOM
                        offset = Offset.Zero
                    } else {
                        val (nextScale, nextOffset) = computeZoomedState(
                            currentScale = scale,
                            currentOffset = offset,
                            targetScale = 2.0f,
                            renderW = renderW,
                            renderH = renderH,
                            containerW = containerWidth,
                            containerH = containerHeight
                        )
                        scale = nextScale
                        offset = nextOffset
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 64.dp)
            )
        }
    }
}

@Composable
private fun ZoomControlsBar(
    scale: Float,
    lang: AppLanguage,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onToggleReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canZoomOut = scale > 1.05f
    val canZoomIn = scale < MAX_CANVAS_ZOOM - 0.05f
    val zoomLabel = String.format(Locale.US, "%.1fx", scale)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(StudioSurface.copy(alpha = 0.88f))
            .border(
                width = 1.dp,
                color = if (canZoomOut) AccentCyan.copy(alpha = 0.7f) else StudioBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 2.dp)
            .testTag("canvas_zoom_controls"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onZoomOut,
            enabled = canZoomOut,
            modifier = Modifier
                .size(40.dp)
                .minimumInteractiveComponentSize()
                .testTag("zoom_out_button")
        ) {
            Icon(
                imageVector = Icons.Default.ZoomOut,
                contentDescription = AppStrings.zoomOut(lang),
                tint = if (canZoomOut) TextPrimary else TextDisabled,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onToggleReset)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("zoom_reset_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = zoomLabel,
                color = if (canZoomOut) AccentCyan else TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }

        IconButton(
            onClick = onZoomIn,
            enabled = canZoomIn,
            modifier = Modifier
                .size(40.dp)
                .minimumInteractiveComponentSize()
                .testTag("zoom_in_button")
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = AppStrings.zoomIn(lang),
                tint = if (canZoomIn) AccentCyan else TextDisabled,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private enum class CropDragHandle {
    NONE, TL, TR, BL, BR, LEFT, TOP, RIGHT, BOTTOM, MOVE
}

@Composable
private fun InteractiveCropOverlay(
    cropRect: CropRect,
    lockedCropRatio: Float?,
    imgLeft: Float,
    imgTop: Float,
    renderW: Float,
    renderH: Float,
    bmpWidth: Int,
    bmpHeight: Int,
    onCropRectChanged: (CropRect) -> Unit
) {
    val latestCropRect by rememberUpdatedState(cropRect)
    val latestRatio by rememberUpdatedState(lockedCropRatio)
    val latestOnChanged by rememberUpdatedState(onCropRectChanged)

    val cropWpx = (cropRect.width * bmpWidth).roundToInt().coerceAtLeast(1)
    val cropHpx = (cropRect.height * bmpHeight).roundToInt().coerceAtLeast(1)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("interactive_crop_overlay")
            .pointerInput(imgLeft, imgTop, renderW, renderH) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val r = latestCropRect
                    val cLeft = imgLeft + r.left * renderW
                    val cTop = imgTop + r.top * renderH
                    val cRight = imgLeft + r.right * renderW
                    val cBottom = imgTop + r.bottom * renderH
                    val cornerTol = 38.dp.toPx()
                    val edgeTol = 26.dp.toPx()
                    val p = down.position

                    val handle = when {
                        (p - Offset(cLeft, cTop)).getDistance() <= cornerTol -> CropDragHandle.TL
                        (p - Offset(cRight, cTop)).getDistance() <= cornerTol -> CropDragHandle.TR
                        (p - Offset(cLeft, cBottom)).getDistance() <= cornerTol -> CropDragHandle.BL
                        (p - Offset(cRight, cBottom)).getDistance() <= cornerTol -> CropDragHandle.BR
                        abs(p.x - cLeft) <= edgeTol && p.y in (cTop - edgeTol)..(cBottom + edgeTol) -> CropDragHandle.LEFT
                        abs(p.x - cRight) <= edgeTol && p.y in (cTop - edgeTol)..(cBottom + edgeTol) -> CropDragHandle.RIGHT
                        abs(p.y - cTop) <= edgeTol && p.x in (cLeft - edgeTol)..(cRight + edgeTol) -> CropDragHandle.TOP
                        abs(p.y - cBottom) <= edgeTol && p.x in (cLeft - edgeTol)..(cRight + edgeTol) -> CropDragHandle.BOTTOM
                        p.x in cLeft..cRight && p.y in cTop..cBottom -> CropDragHandle.MOVE
                        else -> CropDragHandle.NONE
                    }

                    if (handle == CropDragHandle.NONE) return@awaitEachGesture
                    down.consume()

                    var prevPos = p
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        val delta = change.position - prevPos
                        if (delta != Offset.Zero) {
                            prevPos = change.position
                            latestOnChanged(
                                applyCropDrag(
                                    rect = latestCropRect,
                                    handle = handle,
                                    dxNorm = delta.x / renderW,
                                    dyNorm = delta.y / renderH,
                                    lockedRatio = latestRatio,
                                    renderW = renderW,
                                    renderH = renderH
                                )
                            )
                        }
                        change.consume()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cLeft = imgLeft + cropRect.left * renderW
            val cTop = imgTop + cropRect.top * renderH
            val cRight = imgLeft + cropRect.right * renderW
            val cBottom = imgTop + cropRect.bottom * renderH
            val cW = (cRight - cLeft).coerceAtLeast(1f)
            val cH = (cBottom - cTop).coerceAtLeast(1f)
            val imgRight = imgLeft + renderW
            val imgBottom = imgTop + renderH

            val maskColor = Color.Black.copy(alpha = 0.62f)
            // Top mask
            if (cTop > imgTop) {
                drawRect(maskColor, topLeft = Offset(imgLeft, imgTop), size = Size(renderW, cTop - imgTop))
            }
            // Bottom mask
            if (cBottom < imgBottom) {
                drawRect(maskColor, topLeft = Offset(imgLeft, cBottom), size = Size(renderW, imgBottom - cBottom))
            }
            // Left mask
            if (cLeft > imgLeft) {
                drawRect(maskColor, topLeft = Offset(imgLeft, cTop), size = Size(cLeft - imgLeft, cH))
            }
            // Right mask
            if (cRight < imgRight) {
                drawRect(maskColor, topLeft = Offset(cRight, cTop), size = Size(imgRight - cRight, cH))
            }

            // Crisp outer border
            drawRect(
                color = Color.White.copy(alpha = 0.92f),
                topLeft = Offset(cLeft, cTop),
                size = Size(cW, cH),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Rule of Thirds interior lines
            val gridColor = Color.White.copy(alpha = 0.42f)
            val gridStroke = 1.dp.toPx()
            for (i in 1..2) {
                val gx = cLeft + cW * (i / 3f)
                drawLine(gridColor, Offset(gx, cTop), Offset(gx, cBottom), strokeWidth = gridStroke)
                val gy = cTop + cH * (i / 3f)
                drawLine(gridColor, Offset(cLeft, gy), Offset(cRight, gy), strokeWidth = gridStroke)
            }

            // Corner L-brackets
            val bracketLen = min(20.dp.toPx(), min(cW, cH) * 0.35f)
            val handleStroke = 3.5.dp.toPx()
            val edgeBarLen = min(22.dp.toPx(), min(cW, cH) * 0.3f)

            // TL
            drawLine(AccentCyan, Offset(cLeft, cTop), Offset(cLeft + bracketLen, cTop), strokeWidth = handleStroke, cap = StrokeCap.Round)
            drawLine(AccentCyan, Offset(cLeft, cTop), Offset(cLeft, cTop + bracketLen), strokeWidth = handleStroke, cap = StrokeCap.Round)
            // TR
            drawLine(AccentCyan, Offset(cRight - bracketLen, cTop), Offset(cRight, cTop), strokeWidth = handleStroke, cap = StrokeCap.Round)
            drawLine(AccentCyan, Offset(cRight, cTop), Offset(cRight, cTop + bracketLen), strokeWidth = handleStroke, cap = StrokeCap.Round)
            // BL
            drawLine(AccentCyan, Offset(cLeft, cBottom - bracketLen), Offset(cLeft, cBottom), strokeWidth = handleStroke, cap = StrokeCap.Round)
            drawLine(AccentCyan, Offset(cLeft, cBottom), Offset(cLeft + bracketLen, cBottom), strokeWidth = handleStroke, cap = StrokeCap.Round)
            // BR
            drawLine(AccentCyan, Offset(cRight - bracketLen, cBottom), Offset(cRight, cBottom), strokeWidth = handleStroke, cap = StrokeCap.Round)
            drawLine(AccentCyan, Offset(cRight, cBottom - bracketLen), Offset(cRight, cBottom), strokeWidth = handleStroke, cap = StrokeCap.Round)

            // Edge mid-handles
            val midX = (cLeft + cRight) / 2f
            val midY = (cTop + cBottom) / 2f
            drawLine(Color.White, Offset(midX - edgeBarLen / 2f, cTop), Offset(midX + edgeBarLen / 2f, cTop), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color.White, Offset(midX - edgeBarLen / 2f, cBottom), Offset(midX + edgeBarLen / 2f, cBottom), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color.White, Offset(cLeft, midY - edgeBarLen / 2f), Offset(cLeft, midY + edgeBarLen / 2f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color.White, Offset(cRight, midY - edgeBarLen / 2f), Offset(cRight, midY + edgeBarLen / 2f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }

        // Live crop dimensions badge (top-center)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(StudioSurface.copy(alpha = 0.88f))
                .border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
                .testTag("crop_dimensions_badge")
        ) {
            Text(
                text = "$cropWpx × $cropHpx px",
                color = AccentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun applyCropDrag(
    rect: CropRect,
    handle: CropDragHandle,
    dxNorm: Float,
    dyNorm: Float,
    lockedRatio: Float?,
    renderW: Float,
    renderH: Float
): CropRect {
    val minNorm = 0.14f
    if (handle == CropDragHandle.MOVE) {
        val w = rect.width
        val h = rect.height
        val nl = (rect.left + dxNorm).coerceIn(0f, (1f - w).coerceAtLeast(0f))
        val nt = (rect.top + dyNorm).coerceIn(0f, (1f - h).coerceAtLeast(0f))
        return CropRect(nl, nt, (nl + w).coerceAtMost(1f), (nt + h).coerceAtMost(1f))
    }

    if (lockedRatio == null || renderW <= 0f || renderH <= 0f) {
        return when (handle) {
            CropDragHandle.TL -> rect.copy(
                left = (rect.left + dxNorm).coerceIn(0f, rect.right - minNorm),
                top = (rect.top + dyNorm).coerceIn(0f, rect.bottom - minNorm)
            )
            CropDragHandle.TR -> rect.copy(
                right = (rect.right + dxNorm).coerceIn(rect.left + minNorm, 1f),
                top = (rect.top + dyNorm).coerceIn(0f, rect.bottom - minNorm)
            )
            CropDragHandle.BL -> rect.copy(
                left = (rect.left + dxNorm).coerceIn(0f, rect.right - minNorm),
                bottom = (rect.bottom + dyNorm).coerceIn(rect.top + minNorm, 1f)
            )
            CropDragHandle.BR -> rect.copy(
                right = (rect.right + dxNorm).coerceIn(rect.left + minNorm, 1f),
                bottom = (rect.bottom + dyNorm).coerceIn(rect.top + minNorm, 1f)
            )
            CropDragHandle.LEFT -> rect.copy(
                left = (rect.left + dxNorm).coerceIn(0f, rect.right - minNorm)
            )
            CropDragHandle.RIGHT -> rect.copy(
                right = (rect.right + dxNorm).coerceIn(rect.left + minNorm, 1f)
            )
            CropDragHandle.TOP -> rect.copy(
                top = (rect.top + dyNorm).coerceIn(0f, rect.bottom - minNorm)
            )
            CropDragHandle.BOTTOM -> rect.copy(
                bottom = (rect.bottom + dyNorm).coerceIn(rect.top + minNorm, 1f)
            )
            else -> rect
        }
    }

    // Locked aspect ratio in normalized [0..1] coordinates:
    // normW / normH = lockedRatio * (renderH / renderW)
    val normRatio = (lockedRatio * renderH / renderW).coerceAtLeast(0.05f)
    val minW = max(minNorm, minNorm * normRatio).coerceAtMost(0.95f)
    val minH = (minW / normRatio).coerceAtMost(0.95f)

    return when (handle) {
        CropDragHandle.BR -> {
            val anchorL = rect.left
            val anchorT = rect.top
            val maxW = min(1f - anchorL, (1f - anchorT) * normRatio)
            val candW = if (abs(dxNorm) >= abs(dyNorm * normRatio)) {
                rect.right + dxNorm - anchorL
            } else {
                (rect.bottom + dyNorm - anchorT) * normRatio
            }
            val newW = candW.coerceIn(minW.coerceAtMost(maxW), maxW)
            val newH = newW / normRatio
            CropRect(anchorL, anchorT, anchorL + newW, anchorT + newH)
        }
        CropDragHandle.TL -> {
            val anchorR = rect.right
            val anchorB = rect.bottom
            val maxW = min(anchorR, anchorB * normRatio)
            val candW = if (abs(dxNorm) >= abs(dyNorm * normRatio)) {
                anchorR - (rect.left + dxNorm)
            } else {
                (anchorB - (rect.top + dyNorm)) * normRatio
            }
            val newW = candW.coerceIn(minW.coerceAtMost(maxW), maxW)
            val newH = newW / normRatio
            CropRect(anchorR - newW, anchorB - newH, anchorR, anchorB)
        }
        CropDragHandle.TR -> {
            val anchorL = rect.left
            val anchorB = rect.bottom
            val maxW = min(1f - anchorL, anchorB * normRatio)
            val candW = if (abs(dxNorm) >= abs(dyNorm * normRatio)) {
                rect.right + dxNorm - anchorL
            } else {
                (anchorB - (rect.top + dyNorm)) * normRatio
            }
            val newW = candW.coerceIn(minW.coerceAtMost(maxW), maxW)
            val newH = newW / normRatio
            CropRect(anchorL, anchorB - newH, anchorL + newW, anchorB)
        }
        CropDragHandle.BL -> {
            val anchorR = rect.right
            val anchorT = rect.top
            val maxW = min(anchorR, (1f - anchorT) * normRatio)
            val candW = if (abs(dxNorm) >= abs(dyNorm * normRatio)) {
                anchorR - (rect.left + dxNorm)
            } else {
                (rect.bottom + dyNorm - anchorT) * normRatio
            }
            val newW = candW.coerceIn(minW.coerceAtMost(maxW), maxW)
            val newH = newW / normRatio
            CropRect(anchorR - newW, anchorT, anchorR, anchorT + newH)
        }
        CropDragHandle.RIGHT -> {
            val anchorL = rect.left
            val cy = (rect.top + rect.bottom) / 2f
            val maxW = min(1f - anchorL, normRatio)
            val newW = (rect.right + dxNorm - anchorL).coerceIn(minW.coerceAtMost(maxW), maxW)
            val newH = newW / normRatio
            val nt = (cy - newH / 2f).coerceIn(0f, (1f - newH).coerceAtLeast(0f))
            CropRect(anchorL, nt, anchorL + newW, (nt + newH).coerceAtMost(1f))
        }
        CropDragHandle.LEFT -> {
            val anchorR = rect.right
            val cy = (rect.top + rect.bottom) / 2f
            val maxW = min(anchorR, normRatio)
            val newW = (anchorR - (rect.left + dxNorm)).coerceIn(minW.coerceAtMost(maxW), maxW)
            val newH = newW / normRatio
            val nt = (cy - newH / 2f).coerceIn(0f, (1f - newH).coerceAtLeast(0f))
            CropRect(anchorR - newW, nt, anchorR, (nt + newH).coerceAtMost(1f))
        }
        CropDragHandle.BOTTOM -> {
            val anchorT = rect.top
            val cx = (rect.left + rect.right) / 2f
            val maxH = min(1f - anchorT, 1f / normRatio)
            val newH = (rect.bottom + dyNorm - anchorT).coerceIn(minH.coerceAtMost(maxH), maxH)
            val newW = newH * normRatio
            val nl = (cx - newW / 2f).coerceIn(0f, (1f - newW).coerceAtLeast(0f))
            CropRect(nl, anchorT, (nl + newW).coerceAtMost(1f), anchorT + newH)
        }
        CropDragHandle.TOP -> {
            val anchorB = rect.bottom
            val cx = (rect.left + rect.right) / 2f
            val maxH = min(anchorB, 1f / normRatio)
            val newH = (anchorB - (rect.top + dyNorm)).coerceIn(minH.coerceAtMost(maxH), maxH)
            val newW = newH * normRatio
            val nl = (cx - newW / 2f).coerceIn(0f, (1f - newW).coerceAtLeast(0f))
            CropRect(nl, anchorB - newH, (nl + newW).coerceAtMost(1f), anchorB)
        }
        else -> rect
    }
}

/**
 * Floating real-time RGB + Luminance Histogram HUD overlay for PhotoEditorScreen (v2.0).
 */
@Composable
fun LiveHistogramHud(
    histogram: HistogramData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(164.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StudioSurface.copy(alpha = 0.86f))
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            .padding(8.dp)
            .testTag("live_histogram_hud")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RGB HISTOGRAM",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF5252), CircleShape))
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF69F0AE), CircleShape))
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF448AFF), CircleShape))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val w = size.width
            val h = size.height
            val maxC = histogram.maxCount.toFloat().coerceAtLeast(1f)

            fun drawChannel(bins: IntArray, fillColor: Color, strokeColor: Color) {
                val path = Path()
                path.moveTo(0f, h)
                val step = 2
                for (i in bins.indices step step) {
                    val x = (i / 255f) * w
                    val norm = (bins[i] / maxC).coerceIn(0f, 1f)
                    val y = h - norm * h
                    path.lineTo(x, y)
                }
                path.lineTo(w, h)
                path.close()
                drawPath(path = path, color = fillColor)
                drawPath(path = path, color = strokeColor, style = Stroke(width = 1.dp.toPx()))
            }

            drawChannel(histogram.luminance, Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.45f))
            drawChannel(histogram.red, Color(0xFFFF5252).copy(alpha = 0.18f), Color(0xFFFF5252).copy(alpha = 0.75f))
            drawChannel(histogram.green, Color(0xFF69F0AE).copy(alpha = 0.18f), Color(0xFF69F0AE).copy(alpha = 0.75f))
            drawChannel(histogram.blue, Color(0xFF448AFF).copy(alpha = 0.18f), Color(0xFF448AFF).copy(alpha = 0.75f))
        }
    }
}

internal const val MIN_CANVAS_ZOOM = 1f
internal const val MAX_CANVAS_ZOOM = 6f

internal fun nextZoomInStep(currentScale: Float): Float {
    return when {
        currentScale < 1.49f -> 1.5f
        currentScale < 1.99f -> 2.0f
        currentScale < 2.99f -> 3.0f
        currentScale < 4.49f -> 4.5f
        else -> MAX_CANVAS_ZOOM
    }
}

internal fun nextZoomOutStep(currentScale: Float): Float {
    return when {
        currentScale > 4.6f -> 4.5f
        currentScale > 3.1f -> 3.0f
        currentScale > 2.1f -> 2.0f
        currentScale > 1.55f -> 1.5f
        else -> MIN_CANVAS_ZOOM
    }
}

internal fun clampCanvasOffset(
    candidate: Offset,
    targetScale: Float,
    renderW: Float,
    renderH: Float,
    containerW: Float,
    containerH: Float
): Offset {
    if (targetScale <= 1.01f) return Offset.Zero
    val maxOffsetX = max((renderW * targetScale - containerW) / 2f, renderW * (targetScale - 1f) / 2f).coerceAtLeast(0f)
    val maxOffsetY = max((renderH * targetScale - containerH) / 2f, renderH * (targetScale - 1f) / 2f).coerceAtLeast(0f)
    return Offset(
        candidate.x.coerceIn(-maxOffsetX, maxOffsetX),
        candidate.y.coerceIn(-maxOffsetY, maxOffsetY)
    )
}

internal fun computeZoomedState(
    currentScale: Float,
    currentOffset: Offset,
    targetScale: Float,
    renderW: Float,
    renderH: Float,
    containerW: Float,
    containerH: Float
): Pair<Float, Offset> {
    val clampedScale = targetScale.coerceIn(MIN_CANVAS_ZOOM, MAX_CANVAS_ZOOM)
    if (clampedScale <= 1.01f) return MIN_CANVAS_ZOOM to Offset.Zero
    val ratio = if (currentScale > 0.01f) clampedScale / currentScale else clampedScale
    val scaledOffset = currentOffset * ratio
    return clampedScale to clampCanvasOffset(
        candidate = scaledOffset,
        targetScale = clampedScale,
        renderW = renderW,
        renderH = renderH,
        containerW = containerW,
        containerH = containerH
    )
}
