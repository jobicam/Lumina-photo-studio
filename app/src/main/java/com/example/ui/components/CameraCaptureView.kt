package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Size
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.AppLanguage
import com.example.model.FilterCategory
import com.example.model.FilterPreset
import com.example.model.FilterPresets
import com.example.processing.BitmapProcessor
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings
import java.util.concurrent.Executors

@Composable
fun CameraCaptureView(
    onPhotoCaptured: (Bitmap, FilterPreset) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    defaultFrontCamera: Boolean = false,
    lang: AppLanguage = AppLanguage.SYSTEM
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler {
        onClose()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, AppStrings.cameraPermToast(lang), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        // Permission Request Fallback Screen
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(StudioBackground)
                .statusBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(StudioCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = AppStrings.cameraPermTitle(lang),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = AppStrings.cameraPermDesc(lang),
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("grant_camera_permission_button")
                ) {
                    Text(AppStrings.grantPermission(lang), color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier.testTag("cancel_camera_button")
                ) {
                    Text(AppStrings.cancel(lang), color = TextSecondary)
                }
            }
        }
        return
    }

    // Camera Active
    val initialLens = if (defaultFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
    var lensFacing by remember(defaultFrontCamera) { mutableIntStateOf(initialLens) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    // Live Filter Preview State
    var selectedPreset by remember { mutableStateOf(FilterPresets.NONE) }
    var showFilterStrip by remember { mutableStateOf(true) }
    var livePreviewFrame by remember { mutableStateOf<Bitmap?>(null) }
    var isViewActive by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            isViewActive = false
            livePreviewFrame = null
        }
    }

    val needLiveFrames by rememberUpdatedState(showFilterStrip || selectedPreset != FilterPresets.NONE)
    val activeLensFacing by rememberUpdatedState(lensFacing)

    val presetColorFilters = remember {
        FilterPresets.ALL_PRESETS.associate { preset ->
            val androidMatrix = BitmapProcessor.buildPresetColorMatrix(preset, 1.0f)
            preset.id to ColorFilter.colorMatrix(ColorMatrix(androidMatrix.array))
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(flashMode, imageCapture) {
        imageCapture?.flashMode = flashMode
    }

    DisposableEffect(lensFacing, lifecycleOwner) {
        var activeProvider: ProcessCameraProvider? = null
        var disposed = false
        val analysisExecutor = Executors.newSingleThreadExecutor()
        var lastProcessedNs = 0L

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            if (disposed) return@addListener
            try {
                val cameraProvider = cameraProviderFuture.get()
                activeProvider = cameraProvider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(flashMode)
                    .build()

                imageCapture = capture

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(720, 960),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                                )
                            )
                            .build()
                    )
                    .build()

                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    try {
                        if (!disposed && needLiveFrames) {
                            val now = SystemClock.elapsedRealtimeNanos()
                            if (now - lastProcessedNs >= 33_000_000L) {
                                lastProcessedNs = now
                                val rotation = imageProxy.imageInfo.rotationDegrees
                                val raw = imageProxy.toBitmap()
                                val isFront = activeLensFacing == CameraSelector.LENS_FACING_FRONT
                                val oriented = if (rotation != 0 || isFront) {
                                    val matrix = Matrix()
                                    if (rotation != 0) {
                                        matrix.postRotate(rotation.toFloat())
                                    }
                                    if (isFront) {
                                        matrix.postScale(-1f, 1f)
                                    }
                                    val transformed = Bitmap.createBitmap(
                                        raw, 0, 0, raw.width, raw.height, matrix, true
                                    )
                                    if (transformed !== raw) raw.recycle()
                                    transformed
                                } else {
                                    raw
                                }
                                val compact = BitmapProcessor.downscaleIfNeeded(oriented, 640)
                                if (compact !== oriented) oriented.recycle()
                                if (!disposed) {
                                    livePreviewFrame = compact
                                }
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                try {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        capture,
                        analysis
                    )
                } catch (_: Exception) {
                    // Fallback if a legacy device only supports 2 concurrent camera streams
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        capture
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            activeProvider?.unbindAll()
            analysisExecutor.shutdownNow()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_capture_screen")
    ) {
        // Base hardware CameraX PreviewView
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Real-time GPU ColorMatrix-filtered Live Viewfinder overlay when a filter preset is active
        val frame = livePreviewFrame
        if (selectedPreset != FilterPresets.NONE && frame != null && !frame.isRecycled) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = selectedPreset.name,
                contentScale = ContentScale.Crop,
                colorFilter = presetColorFilters[selectedPreset.id],
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camera_filtered_viewfinder")
            )
        }

        // Top Controls: Close, Live Filter Indicator, Filter Tray Toggle, Flash Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .testTag("close_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = AppStrings.cancel(lang),
                    tint = Color.White
                )
            }

            // Active Filter Badge Pill (toggles filter preview strip)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (selectedPreset != FilterPresets.NONE) AccentCyan.copy(alpha = 0.90f)
                        else Color.Black.copy(alpha = 0.55f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (selectedPreset != FilterPresets.NONE) AccentCyan else Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { showFilterStrip = !showFilterStrip }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("camera_live_filter_badge")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = AppStrings.liveFilters(lang),
                        tint = if (selectedPreset != FilterPresets.NONE) Color.Black else AccentCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (selectedPreset == FilterPresets.NONE) AppStrings.liveFilters(lang) else selectedPreset.name,
                        color = if (selectedPreset != FilterPresets.NONE) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Right Controls: Filter Tray Toggle + Flash Toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showFilterStrip = !showFilterStrip },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (showFilterStrip) AccentCyan.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.55f),
                            CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (showFilterStrip) AccentCyan else Color.Transparent,
                            shape = CircleShape
                        )
                        .testTag("toggle_camera_filters_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = AppStrings.liveFilters(lang),
                        tint = if (showFilterStrip) AccentCyan else Color.White
                    )
                }

                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("toggle_flash_button")
                ) {
                    val (flashIcon, flashColor) = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn to AccentAmber
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto to AccentCyan
                        else -> Icons.Default.FlashOff to Color.White
                    }
                    Icon(
                        imageVector = flashIcon,
                        contentDescription = "Toggle Flash",
                        tint = flashColor
                    )
                }
            }
        }

        // Bottom Stack: Live Filter Preview Strip + Shutter Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = showFilterStrip,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(vertical = 10.dp)
                        .testTag("camera_live_filter_strip")
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(FilterPresets.ALL_PRESETS, key = { it.id }) { preset ->
                            val isSelected = selectedPreset.id == preset.id
                            CameraLiveFilterItem(
                                preset = preset,
                                liveFrame = frame,
                                colorFilter = presetColorFilters[preset.id],
                                isSelected = isSelected,
                                onClick = { selectedPreset = preset }
                            )
                        }
                    }
                }
            }

            // Bottom Controls: Flip Camera & Shutter Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp, start = 32.dp, end = 32.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spacer for balance
                Spacer(modifier = Modifier.size(48.dp))

                // Main Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                        .clickable(enabled = !isCapturing) {
                            val capture = imageCapture ?: return@clickable
                            isCapturing = true
                            val appliedPreset = selectedPreset
                            val isFrontCapture = lensFacing == CameraSelector.LENS_FACING_FRONT

                            capture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                        val rawBitmap = imageProxy.toBitmap()
                                        imageProxy.close()

                                        if (!isViewActive) {
                                            if (!rawBitmap.isRecycled) rawBitmap.recycle()
                                            isCapturing = false
                                            return
                                        }

                                        val finalBitmap = if (rotationDegrees != 0 || isFrontCapture) {
                                            val matrix = Matrix().apply {
                                                if (rotationDegrees != 0) {
                                                    postRotate(rotationDegrees.toFloat())
                                                }
                                                if (isFrontCapture) {
                                                    postScale(-1f, 1f)
                                                }
                                            }
                                            val transformed = Bitmap.createBitmap(
                                                rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                                            )
                                            if (transformed !== rawBitmap) rawBitmap.recycle()
                                            transformed
                                        } else {
                                            rawBitmap
                                        }

                                        isCapturing = false
                                        onPhotoCaptured(finalBitmap, appliedPreset)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isCapturing = false
                                        if (!isViewActive) return
                                        Toast.makeText(
                                            context,
                                            AppStrings.cameraCaptureFailed(lang, exception.localizedMessage),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Switch Camera Lens (Front / Back)
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("switch_camera_lens_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraLiveFilterItem(
    preset: FilterPreset,
    liveFrame: Bitmap?,
    colorFilter: ColorFilter?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(66.dp)
            .clickable(onClick = onClick)
            .testTag("camera_filter_${preset.id}")
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        preset.isSepia -> Color(0xFF704214)
                        preset.isGrayscale -> Color(0xFF424242)
                        preset.id == "high_contrast" -> Color(0xFF1A1A1A)
                        preset.category == FilterCategory.PORTRAIT -> Color(0xFFD49A89)
                        preset.category == FilterCategory.CINEMATIC -> Color(0xFF1E3A5F)
                        preset.category == FilterCategory.VINTAGE -> Color(0xFF8D6E63)
                        preset.category == FilterCategory.BLACK_WHITE -> Color(0xFF424242)
                        preset.category == FilterCategory.AESTHETIC -> Color(0xFF9575CD)
                        preset.category == FilterCategory.LANDSCAPE -> Color(0xFF2E7D32)
                        else -> Color(0xFF37474F)
                    }
                )
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) AccentCyan else StudioBorder,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (liveFrame != null && !liveFrame.isRecycled) {
                Image(
                    bitmap = liveFrame.asImageBitmap(),
                    contentDescription = preset.name,
                    contentScale = ContentScale.Crop,
                    colorFilter = if (preset == FilterPresets.NONE) null else colorFilter,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = preset.name.take(3).uppercase(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                        .background(AccentCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = preset.name,
            color = if (isSelected) AccentCyan else Color.White,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
