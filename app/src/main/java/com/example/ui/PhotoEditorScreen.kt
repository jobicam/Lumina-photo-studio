package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Surface
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.LensBlur
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.BuildConfig
import com.example.ui.components.CameraCaptureView
import com.example.ui.components.ColorGradePanel
import com.example.ui.components.SavePresetDialog
import com.example.ui.components.UpdateCheckerModal
import com.example.ui.components.WhatsNewModal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AdjustPanel
import com.example.ui.components.CropPanel
import com.example.ui.components.CurvesPanel
import com.example.ui.components.FiltersPanel
import com.example.ui.components.HslPanel
import com.example.ui.components.LiveHistogramHud
import com.example.ui.components.PhotoCanvas
import com.example.ui.components.PhotoPickerModal
import com.example.ui.components.ReorderTabsModal
import com.example.ui.components.RetouchPanel
import com.example.ui.components.SaveOptionsModal
import com.example.ui.components.SaveSuccessModal
import com.example.ui.components.StudioMotion
import com.example.ui.components.bounceClickable
import com.example.ui.components.editorTabIcon
import com.example.ui.components.editorTabLabel
import com.example.ui.components.pressBounce
import com.example.util.AppStrings
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.EditorTab
import com.example.viewmodel.PhotoEditorViewModel

@Composable
fun PhotoEditorScreen(
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lang = uiState.settings.appLanguage
    var showPhotoPicker by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showReorderTabsModal by remember { mutableStateOf(false) }
    val orderedTabs = remember(uiState.settings.editorTabOrder) {
        com.example.model.UserSettings.normalizeTabOrder(uiState.settings.editorTabOrder)
    }

    val importPresetLutLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importPresetOrLut(uri)
        }
    }

    val storagePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.openSaveModal()
        } else {
            Toast.makeText(context, AppStrings.storagePermDeniedToast(lang), Toast.LENGTH_SHORT).show()
        }
    }

    val requestSaveAction: () -> Unit = {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.openSaveModal()
        }
    }

    // Intercept back button to return to Home screen
    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---------------------------------------------
            // TOP ACTION BAR
            // ---------------------------------------------
            TopEditorBar(
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                isSplitView = uiState.isSplitView,
                lang = lang,
                onNavigateHome = { viewModel.navigateTo(AppScreen.HOME) },
                onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
                onOpenPicker = { showPhotoPicker = true },
                onOpenCamera = { showCamera = true },
                onOpenReorderTabs = { showReorderTabsModal = true },
                onOpenWhatsNew = { showWhatsNew = true },
                onCheckUpdates = { viewModel.checkForUpdates(openDialog = true) },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleSplit = { viewModel.toggleSplitView() },
                onShare = {
                    viewModel.shareImage { shareIntent ->
                        try {
                            context.startActivity(shareIntent)
                        } catch (_: Exception) {
                            Toast.makeText(context, AppStrings.statusShareFailed(lang, null), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onSave = requestSaveAction
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(StudioBorder.copy(alpha = 0.7f))
            )

            // ---------------------------------------------
            // CENTER CANVAS WORKSPACE
            // ---------------------------------------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val isCropMode = uiState.activeTab == EditorTab.CROP
                val cropSourceBmp = uiState.workingBitmap ?: uiState.displayBitmap
                val lockedCropRatio = cropSourceBmp?.let { bmp ->
                    com.example.model.CropRect.effectiveRatio(
                        bitmapWidth = bmp.width,
                        bitmapHeight = bmp.height,
                        aspectRatio = uiState.selectedCropRatio,
                        invertOrientation = uiState.isCropRatioInverted
                    )
                }

                val isBlurFocusMode = uiState.activeTab == EditorTab.ADJUST &&
                    uiState.activeAdjustCategory == com.example.model.AdjustCategory.EFFECTS &&
                    (
                        uiState.adjustments.backgroundBlur > 0f ||
                            (uiState.adjustments.motionBlurIntensity > 0f &&
                                uiState.adjustments.motionBlurTarget != com.example.model.MotionBlurTarget.SCENE)
                    )

                PhotoCanvas(
                    displayBitmap = uiState.displayBitmap,
                    originalBitmap = uiState.originalBitmap,
                    isRetouchMode = uiState.activeTab == EditorTab.RETOUCH,
                    retouchType = uiState.activeRetouchTool,
                    brushRadius = uiState.brushRadius,
                    brushIntensity = uiState.brushIntensity,
                    brushHardness = uiState.brushHardness,
                    sculptMode = uiState.sculptMode,
                    isEraser = uiState.isEraser,
                    isComparing = uiState.isComparing,
                    isSplitView = uiState.isSplitView,
                    splitRatio = uiState.splitRatio,
                    showGridOverlay = uiState.settings.showGridOverlay,
                    isCropMode = isCropMode,
                    cropRect = uiState.pendingCropRect,
                    lockedCropRatio = lockedCropRatio,
                    straightenDegrees = uiState.pendingStraightenDegrees,
                    cropGridStyle = uiState.cropGridStyle,
                    isBlurFocusMode = isBlurFocusMode,
                    blurFocusX = uiState.adjustments.blurFocusX,
                    blurFocusY = uiState.adjustments.blurFocusY,
                    blurFocusRadius = uiState.adjustments.blurFocusRadius,
                    lang = lang,
                    onCropRectChanged = { viewModel.updatePendingCropRect(it) },
                    onBlurFocusChanged = { nx, ny -> viewModel.setBlurFocusPoint(nx, ny) },
                    onSplitRatioChanged = { viewModel.setSplitRatio(it) },
                    onRetouchStroke = { viewModel.applyRetouchStroke(it) }
                )

                // Live Histogram HUD (Top Left, offset below BEFORE label if Split View is active)
                val hudTopPadding by animateDpAsState(
                    targetValue = if (uiState.isSplitView) 48.dp else 12.dp,
                    animationSpec = StudioMotion.PanelHeightSpring,
                    label = "histogram_hud_top_padding"
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.showHistogramOverlay && uiState.histogram != null,
                    enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f),
                    exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            top = hudTopPadding,
                            start = 12.dp
                        )
                ) {
                    uiState.histogram?.let { hist ->
                        LiveHistogramHud(histogram = hist)
                    }
                }

                // Top-Right Compact Canvas Metadata & Active Look Pill (replaces duplicate floating Save button)
                val activeEditsCount = remember(
                    uiState.activePreset,
                    uiState.activeLut,
                    uiState.hasRetouchEdits,
                    uiState.adjustments,
                    uiState.colorGrade,
                    uiState.curves,
                    uiState.hsl,
                    uiState.activeFrame
                ) {
                    var c = 0
                    if (uiState.activePreset != com.example.model.FilterPresets.NONE || uiState.activeLut != null) c++
                    if (uiState.hasRetouchEdits) c++
                    if (!uiState.adjustments.isDefault) c++
                    if (!uiState.colorGrade.isDefault) c++
                    if (!uiState.curves.isDefault) c++
                    if (!uiState.hsl.isDefault) c++
                    if (uiState.activeFrame != com.example.model.FrameStyle.NONE) c++
                    c
                }
                val activeBmp = uiState.displayBitmap
                androidx.compose.animation.AnimatedVisibility(
                    visible = !uiState.isSplitView && activeBmp != null,
                    enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f),
                    exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    activeBmp?.let { bmp ->
                        val lookName = uiState.activeLut?.name
                            ?: if (uiState.activePreset != com.example.model.FilterPresets.NONE) uiState.activePreset.name else null
                        val infoLabel = buildString {
                            append("${bmp.width}×${bmp.height}")
                            if (lookName != null) {
                                append(" • ")
                                append(lookName)
                            } else if (activeEditsCount > 0) {
                                append(" • ")
                                append(AppStrings.activeEditsCount(activeEditsCount, lang))
                            }
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(StudioSurface.copy(alpha = 0.78f))
                                .border(
                                    1.dp,
                                    if (activeEditsCount > 0) AccentCyan.copy(alpha = 0.45f) else StudioBorder,
                                    RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("canvas_info_badge"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeEditsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(AccentCyan, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = infoLabel,
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Live Histogram HUD Toggle Pill (Bottom Left, hidden in Crop mode to keep bottom handles clear)
                val histPillBg by animateColorAsState(
                    targetValue = if (uiState.showHistogramOverlay) AccentCyan.copy(alpha = 0.92f) else StudioSurface.copy(alpha = 0.85f),
                    animationSpec = tween(180),
                    label = "histogram_pill_bg"
                )
                val histPillBorder by animateColorAsState(
                    targetValue = if (uiState.showHistogramOverlay) AccentCyan else StudioBorder,
                    animationSpec = tween(180),
                    label = "histogram_pill_border"
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.activeTab != EditorTab.CROP,
                    enter = fadeIn(tween(180)) + slideInVertically { it / 3 },
                    exit = fadeOut(tween(140)) + slideOutVertically { it / 3 },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(histPillBg)
                            .border(
                                width = 1.dp,
                                color = histPillBorder,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .bounceClickable(pressedScale = 0.94f) { viewModel.toggleHistogramOverlay() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("toggle_histogram_hud_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = AppStrings.histogramHud(lang),
                                tint = if (uiState.showHistogramOverlay) Color.Black else AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.histogramHud(lang),
                                color = if (uiState.showHistogramOverlay) Color.Black else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Hold to Compare floating button (when not in split view or crop mode)
                val compareScale by animateFloatAsState(
                    targetValue = if (uiState.isComparing) 0.93f else 1f,
                    animationSpec = StudioMotion.PressSpring,
                    label = "compare_pill_scale"
                )
                val compareBorder by animateColorAsState(
                    targetValue = if (uiState.isComparing) AccentAmber else StudioBorder,
                    animationSpec = tween(150),
                    label = "compare_pill_border"
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = !uiState.isSplitView && uiState.activeTab != EditorTab.CROP,
                    enter = fadeIn(tween(180)) + slideInVertically { it / 3 },
                    exit = fadeOut(tween(140)) + slideOutVertically { it / 3 },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = compareScale
                                scaleY = compareScale
                            }
                            .clip(RoundedCornerShape(24.dp))
                            .background(StudioSurface.copy(alpha = 0.85f))
                            .border(1.dp, compareBorder, RoundedCornerShape(24.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        viewModel.setComparing(true)
                                        tryAwaitRelease()
                                        viewModel.setComparing(false)
                                    }
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("hold_to_compare_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = AppStrings.holdOriginal(lang),
                                tint = if (uiState.isComparing) AccentAmber else AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.holdOriginal(lang),
                                color = if (uiState.isComparing) AccentAmber else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Processing Indicator
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.isProcessing,
                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.85f),
                    exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.85f),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(StudioSurface.copy(alpha = 0.85f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AccentCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // ---------------------------------------------
            // BOTTOM TOOL DRAWER
            // ---------------------------------------------
            Surface(
                color = StudioSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(StudioBorder.copy(alpha = 0.7f))
                    )
                    val drawerHeight by animateDpAsState(
                        targetValue = when (uiState.activeTab) {
                            EditorTab.CURVES, EditorTab.COLOR_GRADE -> 296.dp
                            EditorTab.CROP -> 324.dp
                            EditorTab.RETOUCH -> 304.dp
                            EditorTab.ADJUST -> if (
                                uiState.activeAdjustCategory == com.example.model.AdjustCategory.EFFECTS &&
                                (uiState.adjustments.backgroundBlur > 0f || uiState.adjustments.motionBlurIntensity > 0f)
                            ) 292.dp else 226.dp
                            EditorTab.HSL -> 215.dp
                            EditorTab.FILTERS -> when {
                                uiState.filterBrowserMode == com.example.model.FilterBrowserMode.FILTERS &&
                                    uiState.activePreset != com.example.model.FilterPresets.NONE -> 236.dp
                                uiState.filterBrowserMode == com.example.model.FilterBrowserMode.LUTS &&
                                    uiState.activeLut != null -> 228.dp
                                else -> 208.dp
                            }
                        },
                        animationSpec = StudioMotion.PanelHeightSpring,
                        label = "tool_drawer_height"
                    )

                    // Tool Panel Content with smooth directional slide & fade
                    AnimatedContent(
                        targetState = uiState.activeTab,
                        transitionSpec = {
                            val toRight = orderedTabs.indexOf(targetState) > orderedTabs.indexOf(initialState)
                            val slideIn = slideInHorizontally(
                                animationSpec = StudioMotion.SlideSpring
                            ) { width -> if (toRight) width / 8 else -width / 8 }
                            val slideOut = slideOutHorizontally(
                                animationSpec = tween(140)
                            ) { width -> if (toRight) -width / 10 else width / 10 }
                            (fadeIn(tween(200)) + slideIn) togetherWith (fadeOut(tween(140)) + slideOut)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(drawerHeight),
                        label = "editor_tab_content"
                    ) { tab ->
                        val hapticsEnabled = uiState.settings.hapticFeedback
                        when (tab) {
                            EditorTab.FILTERS -> FiltersPanel(
                                activePreset = uiState.activePreset,
                                filterIntensity = uiState.filterIntensity,
                                onSelectPreset = { viewModel.selectFilter(it) },
                                onIntensityChanged = { viewModel.setFilterIntensity(it) },
                                lang = lang,
                                hapticFeedback = hapticsEnabled,
                                onEditFinished = { viewModel.finishContinuousEdit() },
                                browserMode = uiState.filterBrowserMode,
                                onSelectBrowserMode = { viewModel.setFilterBrowserMode(it) },
                                customPresets = uiState.customPresets,
                                availableLuts = uiState.availableLuts,
                                activeLut = uiState.activeLut,
                                lutIntensity = uiState.lutIntensity,
                                onSelectLut = { viewModel.selectLut(it) },
                                onLutIntensityChanged = { viewModel.setLutIntensity(it) },
                                onApplyCustomPreset = { viewModel.applyCustomPreset(it) },
                                onShareCustomPreset = { preset ->
                                    viewModel.shareCustomPreset(preset) { intent ->
                                        runCatching { context.startActivity(intent) }
                                    }
                                },
                                onDeleteCustomPreset = { viewModel.deleteCustomPreset(it) },
                                onShareLut = { lut ->
                                    viewModel.shareLut(lut) { intent ->
                                        runCatching { context.startActivity(intent) }
                                    }
                                },
                                onDeleteLut = { viewModel.deleteCustomLut(it) },
                                onSavePresetClick = { showSavePresetDialog = true },
                                onImportPresetOrLutClick = {
                                    importPresetLutLauncher.launch(arrayOf("*/*"))
                                }
                            )
                            EditorTab.RETOUCH -> RetouchPanel(
                                activeTool = uiState.activeRetouchTool,
                                brushRadius = uiState.brushRadius,
                                brushIntensity = uiState.brushIntensity,
                                onSelectTool = { viewModel.selectRetouchTool(it) },
                                onRadiusChanged = { viewModel.setBrushRadius(it) },
                                onIntensityChanged = { viewModel.setBrushIntensity(it) },
                                isEraser = uiState.isEraser,
                                onToggleEraser = { viewModel.toggleEraser() },
                                texturePreserve = uiState.texturePreserve,
                                onTexturePreserveChanged = { viewModel.setTexturePreserve(it) },
                                brushHardness = uiState.brushHardness,
                                onHardnessChanged = { viewModel.setBrushHardness(it) },
                                sculptMode = uiState.sculptMode,
                                onSelectSculptMode = { viewModel.selectSculptMode(it) },
                                hasRetouchEdits = uiState.hasRetouchEdits,
                                onResetRetouch = { viewModel.resetRetouchLayer() },
                                onAutoSkinSmooth = { viewModel.applyAutoSkinSmoothing() },
                                onOpenBackgroundBlur = { viewModel.openBackgroundBlurTool() },
                                lang = lang,
                                hapticFeedback = hapticsEnabled
                            )
                            EditorTab.ADJUST -> AdjustPanel(
                                adjustments = uiState.adjustments,
                                activeCategory = uiState.activeAdjustCategory,
                                onSelectCategory = { viewModel.selectAdjustCategory(it) },
                                onAdjustmentsChanged = { viewModel.updateAdjustments(it) },
                                onReset = { viewModel.resetAdjustments() },
                                onAutoEnhance = { viewModel.autoEnhance() },
                                lang = lang,
                                hapticFeedback = hapticsEnabled,
                                onEditFinished = { viewModel.finishContinuousEdit() }
                            )
                            EditorTab.COLOR_GRADE -> ColorGradePanel(
                                colorGrade = uiState.colorGrade,
                                onSelectTone = { viewModel.selectColorGradeTone(it) },
                                onWheelChanged = { viewModel.updateActiveColorWheel(it) },
                                onBalanceChanged = { viewModel.updateColorGradeBalance(it) },
                                onResetWheel = { viewModel.resetActiveColorWheel() },
                                onResetAll = { viewModel.resetAllColorGrade() },
                                lang = lang,
                                hapticFeedback = hapticsEnabled,
                                onEditFinished = { viewModel.finishContinuousEdit() }
                            )
                            EditorTab.CURVES -> CurvesPanel(
                                curves = uiState.curves,
                                onSelectChannel = { viewModel.selectCurveChannel(it) },
                                onCurveChanged = { viewModel.updateActiveCurve(it) },
                                onResetChannel = { viewModel.resetActiveCurve() },
                                histogram = uiState.histogram,
                                lang = lang,
                                onEditFinished = { viewModel.finishContinuousEdit() }
                            )
                            EditorTab.HSL -> HslPanel(
                                hsl = uiState.hsl,
                                onSelectChannel = { viewModel.selectHslChannel(it) },
                                onAdjustmentChanged = { viewModel.updateHslAdjustment(it) },
                                onResetChannel = { viewModel.resetActiveHslChannel() },
                                lang = lang,
                                hapticFeedback = hapticsEnabled,
                                onEditFinished = { viewModel.finishContinuousEdit() }
                            )
                            EditorTab.CROP -> CropPanel(
                                onRotate = { viewModel.rotateClockwise() },
                                onRotateLeft = { viewModel.rotateCounterClockwise() },
                                onFlipH = { viewModel.flipHorizontal() },
                                onFlipV = { viewModel.flipVertical() },
                                selectedRatio = uiState.selectedCropRatio,
                                isRatioInverted = uiState.isCropRatioInverted,
                                cropRect = uiState.pendingCropRect,
                                straightenDegrees = uiState.pendingStraightenDegrees,
                                cropGridStyle = uiState.cropGridStyle,
                                onSelectRatio = { viewModel.selectCropRatio(it) },
                                onSwapRatioOrientation = { viewModel.swapCropRatioOrientation() },
                                onStraightenChanged = { viewModel.setPendingStraighten(it) },
                                onCycleGridStyle = { viewModel.cycleCropGridStyle() },
                                onApplyCrop = { viewModel.applyPendingCrop() },
                                onResetCrop = { viewModel.resetPendingCropRect() },
                                activeFrame = uiState.activeFrame,
                                onSelectFrame = { viewModel.selectFrame(it) },
                                lang = lang,
                                hapticFeedback = hapticsEnabled
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(StudioBorder.copy(alpha = 0.6f))
                    )

                    // Main Category Tab Row (Uses customizable user tab order + active-edit indicators)
                    val selectedTabIndex = orderedTabs.indexOf(uiState.activeTab).coerceAtLeast(0)
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = StudioSurfaceVariant,
                        contentColor = AccentCyan,
                        edgePadding = 8.dp,
                        indicator = { tabPositions ->
                            if (selectedTabIndex in tabPositions.indices) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = AccentCyan,
                                    height = 3.dp
                                )
                            }
                        },
                        divider = {}
                    ) {
                        orderedTabs.forEach { tab ->
                            val isSelected = uiState.activeTab == tab
                            val hasTabEdits = when (tab) {
                                EditorTab.FILTERS -> uiState.activePreset != com.example.model.FilterPresets.NONE || uiState.activeLut != null
                                EditorTab.RETOUCH -> uiState.hasRetouchEdits
                                EditorTab.ADJUST -> !uiState.adjustments.isDefault
                                EditorTab.COLOR_GRADE -> !uiState.colorGrade.isDefault
                                EditorTab.CURVES -> !uiState.curves.isDefault
                                EditorTab.HSL -> !uiState.hsl.isDefault
                                EditorTab.CROP -> !uiState.pendingCropRect.isFull ||
                                    kotlin.math.abs(uiState.pendingStraightenDegrees) >= 0.1f ||
                                    uiState.activeFrame != com.example.model.FrameStyle.NONE
                            }
                            val tabInteraction = remember { MutableInteractionSource() }
                            val iconScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.14f else 1f,
                                animationSpec = StudioMotion.PressSpring,
                                label = "tab_icon_scale_${tab.name}"
                            )
                            val tabTint by animateColorAsState(
                                targetValue = if (isSelected) AccentCyan else if (hasTabEdits) TextPrimary else TextSecondary,
                                animationSpec = tween(180),
                                label = "tab_tint_${tab.name}"
                            )
                            val icon = editorTabIcon(tab)
                            val tabLabel = editorTabLabel(tab, lang)

                            Tab(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(tab) },
                                interactionSource = tabInteraction,
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = tabLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected || hasTabEdits) FontWeight.Bold else FontWeight.Normal,
                                            color = tabTint,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        if (hasTabEdits) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(AccentCyan, CircleShape)
                                            )
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = tabLabel,
                                        tint = tabTint,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .graphicsLayer {
                                                scaleX = iconScale
                                                scaleY = iconScale
                                            }
                                    )
                                },
                                modifier = Modifier
                                    .pressBounce(tabInteraction, pressedScale = 0.92f)
                                    .testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Save Custom Preset Dialog
        if (showSavePresetDialog) {
            SavePresetDialog(
                lang = lang,
                onConfirm = { presetName ->
                    showSavePresetDialog = false
                    viewModel.saveCurrentAsCustomPreset(presetName)
                },
                onDismiss = { showSavePresetDialog = false }
            )
        }

        // In-Editor Reorder Tabs Modal
        if (showReorderTabsModal) {
            ReorderTabsModal(
                orderedTabs = orderedTabs,
                lang = lang,
                onMoveTab = { tab, delta -> viewModel.moveEditorTab(tab, delta) },
                onResetOrder = { viewModel.resetEditorTabOrder() },
                onDismiss = { showReorderTabsModal = false }
            )
        }

        // Photo Picker Modal
        if (showPhotoPicker) {
            PhotoPickerModal(
                lang = lang,
                onDismiss = { showPhotoPicker = false },
                onOpenCamera = { showCamera = true },
                onSelectSamplePortrait = { viewModel.loadSamplePhoto(isPortrait = true) },
                onSelectSampleLandscape = { viewModel.loadSamplePhoto(isPortrait = false) },
                onImagePicked = { uri -> viewModel.loadFromUri(uri) }
            )
        }

        // CameraX Live Capture View
        if (showCamera) {
            CameraCaptureView(
                onPhotoCaptured = { bitmap, preset ->
                    viewModel.loadCapturedBitmap(bitmap, preset)
                    showCamera = false
                },
                onClose = { showCamera = false },
                defaultFrontCamera = uiState.settings.defaultFrontCamera,
                lang = lang,
                modifier = Modifier.fillMaxSize()
            )
        }

        // What's New Modal
        if (showWhatsNew) {
            WhatsNewModal(
                lang = lang,
                upgradedFromVersion = uiState.upgradedFromVersion,
                onCheckUpdates = {
                    showWhatsNew = false
                    viewModel.checkForUpdates(openDialog = true)
                },
                onDismiss = { showWhatsNew = false }
            )
        }

        // Update Checker Modal
        if (uiState.showUpdateDialog) {
            UpdateCheckerModal(
                updateStatus = uiState.updateStatus,
                lastCheckedMillis = uiState.lastUpdateCheckMillis,
                lang = lang,
                apkDownloadState = uiState.apkDownloadState,
                onCheckAgain = { viewModel.checkForUpdates(openDialog = true) },
                onOpenWhatsNew = { showWhatsNew = true },
                onDownloadAndInstall = { onReady -> viewModel.downloadAndInstallUpdate(onReady) },
                onCancelDownload = { viewModel.cancelApkDownload() },
                onDismiss = { viewModel.dismissUpdateDialog() }
            )
        }

        // Save to Gallery Options Modal
        if (uiState.showSaveModal && uiState.displayBitmap != null) {
            SaveOptionsModal(
                displayBitmap = uiState.displayBitmap!!,
                initialFormat = uiState.settings.exportFormat,
                initialQuality = uiState.settings.jpegQuality,
                lang = lang,
                onSave = { filename, format, quality, resolution ->
                    viewModel.saveToGallery(
                        customFilename = filename,
                        format = format,
                        quality = quality,
                        targetMaxDimension = resolution
                    )
                },
                onDismiss = { viewModel.closeSaveModal() }
            )
        }

        // Save Success Modal
        if (uiState.showSaveSuccessModal && uiState.savedImageInfo != null) {
            SaveSuccessModal(
                savedInfo = uiState.savedImageInfo!!,
                lang = lang,
                onShare = {
                    viewModel.shareImage { shareIntent ->
                        try {
                            context.startActivity(shareIntent)
                        } catch (_: Exception) {
                            Toast.makeText(context, AppStrings.statusShareFailed(lang, null), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = { viewModel.closeSaveSuccessModal() }
            )
        }
    }
}

@Composable
private fun TopEditorBar(
    canUndo: Boolean,
    canRedo: Boolean,
    isSplitView: Boolean,
    lang: com.example.model.AppLanguage,
    onNavigateHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPicker: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenReorderTabs: () -> Unit,
    onOpenWhatsNew: () -> Unit,
    onCheckUpdates: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleSplit: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Actions: Navigation & Edit History
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            val backInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onNavigateHome,
                interactionSource = backInteraction,
                modifier = Modifier
                    .width(40.dp)
                    .height(48.dp)
                    .pressBounce(backInteraction, pressedScale = 0.88f)
                    .testTag("editor_back_to_home")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = AppStrings.backToHome(lang),
                    tint = TextPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }

            val undoInteraction = remember { MutableInteractionSource() }
            val undoTint by animateColorAsState(
                targetValue = if (canUndo) TextPrimary else TextDisabled,
                animationSpec = tween(160),
                label = "undo_tint"
            )
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                interactionSource = undoInteraction,
                modifier = Modifier
                    .width(38.dp)
                    .height(48.dp)
                    .pressBounce(undoInteraction, pressedScale = 0.88f, enabled = canUndo)
                    .testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = AppStrings.undo(lang),
                    tint = undoTint,
                    modifier = Modifier.size(19.dp)
                )
            }

            val redoInteraction = remember { MutableInteractionSource() }
            val redoTint by animateColorAsState(
                targetValue = if (canRedo) TextPrimary else TextDisabled,
                animationSpec = tween(160),
                label = "redo_tint"
            )
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                interactionSource = redoInteraction,
                modifier = Modifier
                    .width(38.dp)
                    .height(48.dp)
                    .pressBounce(redoInteraction, pressedScale = 0.88f, enabled = canRedo)
                    .testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = AppStrings.redo(lang),
                    tint = redoTint,
                    modifier = Modifier.size(19.dp)
                )
            }

            val splitInteraction = remember { MutableInteractionSource() }
            val splitTint by animateColorAsState(
                targetValue = if (isSplitView) AccentCyan else TextSecondary,
                animationSpec = tween(180),
                label = "split_tint"
            )
            val splitBg by animateColorAsState(
                targetValue = if (isSplitView) AccentCyan.copy(alpha = 0.16f) else Color.Transparent,
                animationSpec = tween(180),
                label = "split_bg"
            )
            IconButton(
                onClick = onToggleSplit,
                interactionSource = splitInteraction,
                modifier = Modifier
                    .width(40.dp)
                    .height(48.dp)
                    .pressBounce(splitInteraction, pressedScale = 0.88f)
                    .testTag("toggle_split_view_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(splitBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerticalSplit,
                        contentDescription = AppStrings.splitView(lang),
                        tint = splitTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // Right Actions: Share, More Overflow Menu, and HERO SAVE BUTTON
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val shareInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onShare,
                interactionSource = shareInteraction,
                modifier = Modifier
                    .width(38.dp)
                    .height(48.dp)
                    .pressBounce(shareInteraction, pressedScale = 0.88f)
                    .testTag("share_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = AppStrings.share(lang),
                    tint = TextPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }

            // More Menu Dropdown
            Box {
                val moreInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = { showMenu = true },
                    interactionSource = moreInteraction,
                    modifier = Modifier
                        .width(38.dp)
                        .height(48.dp)
                        .pressBounce(moreInteraction, pressedScale = 0.88f)
                        .testTag("editor_more_options_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = AppStrings.moreOptions(lang),
                        tint = TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(StudioCard)
                        .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text(AppStrings.importPhoto(lang), color = TextPrimary, fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onOpenPicker()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(AppStrings.cameraCapture(lang), color = TextPrimary, fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onOpenCamera()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(AppStrings.reorderTabsTitle(lang), color = TextPrimary, fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onOpenReorderTabs()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("editor_reorder_tabs_menu_item")
                    )
                    DropdownMenuItem(
                        text = { Text(AppStrings.tabSettings(lang), color = TextPrimary, fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onOpenSettings()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("${AppStrings.whatsNew(lang)} (v${BuildConfig.VERSION_NAME})", color = TextPrimary, fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onOpenWhatsNew()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(AppStrings.checkForUpdates(lang), color = TextPrimary, fontSize = 13.sp) },
                        onClick = {
                            showMenu = false
                            onCheckUpdates()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("editor_check_updates_menu_item")
                    )
                }
            }

            // PRIMARY HERO SAVE BUTTON - Always visible on the far right
            val saveInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onSave,
                interactionSource = saveInteraction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .pressBounce(saveInteraction, pressedScale = 0.93f)
                    .testTag("save_photo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = AppStrings.save(lang),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
