package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.BuildConfig
import com.example.ui.components.CameraCaptureView
import com.example.ui.components.UpdateCheckerModal
import com.example.ui.components.WhatsNewModal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.ui.components.AdjustPanel
import com.example.ui.components.CropPanel
import com.example.ui.components.CurvesPanel
import com.example.ui.components.FiltersPanel
import com.example.ui.components.HslPanel
import com.example.ui.components.LiveHistogramHud
import com.example.ui.components.PhotoCanvas
import com.example.ui.components.PhotoPickerModal
import com.example.ui.components.RetouchPanel
import com.example.ui.components.SaveOptionsModal
import com.example.ui.components.SaveSuccessModal
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
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lang = uiState.settings.appLanguage
    var showPhotoPicker by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }

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
                onOpenWhatsNew = { showWhatsNew = true },
                onCheckUpdates = { viewModel.checkForUpdates(openDialog = true) },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleSplit = { viewModel.toggleSplitView() },
                onShare = {
                    viewModel.shareImage { shareIntent ->
                        context.startActivity(shareIntent)
                    }
                },
                onSave = requestSaveAction
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
                        aspectRatio = uiState.selectedCropRatio
                    )
                }

                PhotoCanvas(
                    displayBitmap = uiState.displayBitmap,
                    originalBitmap = uiState.originalBitmap,
                    isRetouchMode = uiState.activeTab == EditorTab.RETOUCH,
                    retouchType = uiState.activeRetouchTool,
                    brushRadius = uiState.brushRadius,
                    isComparing = uiState.isComparing,
                    isSplitView = uiState.isSplitView,
                    splitRatio = uiState.splitRatio,
                    showGridOverlay = uiState.settings.showGridOverlay,
                    isCropMode = isCropMode,
                    cropRect = uiState.pendingCropRect,
                    lockedCropRatio = lockedCropRatio,
                    suggestedBlemishes = uiState.suggestedBlemishes,
                    lang = lang,
                    onCropRectChanged = { viewModel.updatePendingCropRect(it) },
                    onSplitRatioChanged = { viewModel.setSplitRatio(it) },
                    onRetouchStroke = { viewModel.applyRetouchStroke(it) }
                )

                // Live Histogram HUD (Top Left, offset below BEFORE label if Split View is active)
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.showHistogramOverlay && uiState.histogram != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            top = if (uiState.isSplitView) 48.dp else 12.dp,
                            start = 12.dp
                        )
                ) {
                    uiState.histogram?.let { hist ->
                        LiveHistogramHud(histogram = hist)
                    }
                }

                // Floating Canvas Save Button (Top Right, hidden during Split View to keep AFTER badge clear)
                if (!uiState.isSplitView) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(AccentCyan)
                            .clickable(onClick = requestSaveAction)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("canvas_floating_save_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = AppStrings.save(lang),
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = AppStrings.save(lang),
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Live Histogram HUD Toggle Pill (Bottom Left, hidden in Crop mode to keep bottom handles clear)
                if (uiState.activeTab != EditorTab.CROP) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (uiState.showHistogramOverlay) AccentCyan.copy(alpha = 0.92f)
                                else StudioSurface.copy(alpha = 0.85f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (uiState.showHistogramOverlay) AccentCyan else StudioBorder,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { viewModel.toggleHistogramOverlay() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("toggle_histogram_hud_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
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
                if (!uiState.isSplitView && uiState.activeTab != EditorTab.CROP) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(StudioSurface.copy(alpha = 0.85f))
                            .border(1.dp, StudioBorder, RoundedCornerShape(24.dp))
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
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.holdOriginal(lang),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Processing Indicator
                if (uiState.isProcessing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
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
                    val drawerHeight by animateDpAsState(
                        targetValue = when (uiState.activeTab) {
                            EditorTab.CURVES -> 296.dp
                            EditorTab.CROP -> 268.dp
                            EditorTab.RETOUCH -> if (
                                uiState.activeRetouchTool == com.example.model.RetouchType.SKIN_SMOOTH ||
                                uiState.activeRetouchTool == com.example.model.RetouchType.BLEMISH
                            ) 276.dp else 215.dp
                            EditorTab.ADJUST, EditorTab.HSL -> 215.dp
                            EditorTab.FILTERS -> 185.dp
                        },
                        label = "tool_drawer_height"
                    )

                    // Tool Panel Content
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(drawerHeight)
                    ) {
                        val hapticsEnabled = uiState.settings.hapticFeedback
                        when (uiState.activeTab) {
                            EditorTab.FILTERS -> FiltersPanel(
                                activePreset = uiState.activePreset,
                                filterIntensity = uiState.filterIntensity,
                                onSelectPreset = { viewModel.selectFilter(it) },
                                onIntensityChanged = { viewModel.setFilterIntensity(it) },
                                lang = lang,
                                hapticFeedback = hapticsEnabled,
                                onEditFinished = { viewModel.finishContinuousEdit() }
                            )
                            EditorTab.RETOUCH -> RetouchPanel(
                                activeTool = uiState.activeRetouchTool,
                                brushRadius = uiState.brushRadius,
                                brushIntensity = uiState.brushIntensity,
                                onSelectTool = { viewModel.selectRetouchTool(it) },
                                onRadiusChanged = { viewModel.setBrushRadius(it) },
                                onIntensityChanged = { viewModel.setBrushIntensity(it) },
                                texturePreserve = uiState.texturePreserve,
                                onTexturePreserveChanged = { viewModel.setTexturePreserve(it) },
                                onAutoSkinSmooth = { viewModel.applyAutoSkinSmoothing() },
                                suggestedBlemishCount = uiState.suggestedBlemishes.size,
                                blemishSensitivity = uiState.blemishSensitivity,
                                onBlemishSensitivityChanged = { viewModel.setBlemishSensitivity(it) },
                                onDetectBlemishes = { viewModel.detectBlemishes() },
                                onHealAllBlemishes = { viewModel.healAllSuggestedBlemishes() },
                                onClearBlemishSuggestions = { viewModel.clearBlemishSuggestions() },
                                lang = lang,
                                hapticFeedback = hapticsEnabled
                            )
                            EditorTab.ADJUST -> AdjustPanel(
                                adjustments = uiState.adjustments,
                                onAdjustmentsChanged = { viewModel.updateAdjustments(it) },
                                onReset = { viewModel.resetAdjustments() },
                                onAutoEnhance = { viewModel.autoEnhance() },
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
                                onFlipH = { viewModel.flipHorizontal() },
                                onFlipV = { viewModel.flipVertical() },
                                selectedRatio = uiState.selectedCropRatio,
                                cropRect = uiState.pendingCropRect,
                                onSelectRatio = { viewModel.selectCropRatio(it) },
                                onApplyCrop = { viewModel.applyPendingCrop() },
                                onResetCrop = { viewModel.resetPendingCropRect() },
                                activeFrame = uiState.activeFrame,
                                onSelectFrame = { viewModel.selectFrame(it) },
                                lang = lang
                            )
                        }
                    }

                    // Main Category Tab Row
                    TabRow(
                        selectedTabIndex = uiState.activeTab.ordinal,
                        containerColor = StudioSurfaceVariant,
                        contentColor = AccentCyan,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab.ordinal]),
                                color = AccentCyan,
                                height = 3.dp
                            )
                        },
                        divider = {}
                    ) {
                        EditorTab.values().forEach { tab ->
                            val isSelected = uiState.activeTab == tab
                            val icon = when (tab) {
                                EditorTab.FILTERS -> Icons.Default.Filter
                                EditorTab.RETOUCH -> Icons.Default.AutoFixHigh
                                EditorTab.ADJUST -> Icons.Default.Tune
                                EditorTab.CURVES -> Icons.Default.ShowChart
                                EditorTab.HSL -> Icons.Default.Tune
                                EditorTab.CROP -> Icons.Default.Crop
                            }
                            val tabLabel = when (tab) {
                                EditorTab.FILTERS -> com.example.util.AppStrings.tabFiltersLabel(lang)
                                EditorTab.RETOUCH -> com.example.util.AppStrings.tabRetouchLabel(lang)
                                EditorTab.ADJUST -> com.example.util.AppStrings.tabAdjustLabel(lang)
                                EditorTab.CURVES -> com.example.util.AppStrings.tabCurvesLabel(lang)
                                EditorTab.HSL -> com.example.util.AppStrings.tabHslLabel(lang)
                                EditorTab.CROP -> com.example.util.AppStrings.tabCropLabel(lang)
                            }

                            Tab(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(tab) },
                                text = {
                                    Text(
                                        text = tabLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AccentCyan else TextSecondary
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = tabLabel,
                                        tint = if (isSelected) AccentCyan else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
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
                currentFeedUrl = uiState.settings.updateFeedUrl,
                lang = lang,
                onCheckAgain = { viewModel.checkForUpdates(openDialog = true) },
                onSaveFeedUrl = { url -> viewModel.saveUpdateFeedUrlAndCheck(url) },
                onOpenWhatsNew = { showWhatsNew = true },
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
                        context.startActivity(shareIntent)
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
            IconButton(
                onClick = onNavigateHome,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("editor_back_to_home")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = AppStrings.backToHome(lang),
                    tint = TextPrimary,
                    modifier = Modifier.size(21.dp)
                )
            }

            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = AppStrings.undo(lang),
                    tint = if (canUndo) TextPrimary else TextDisabled,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = AppStrings.redo(lang),
                    tint = if (canRedo) TextPrimary else TextDisabled,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = onToggleSplit,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("toggle_split_view_button")
            ) {
                Icon(
                    imageVector = Icons.Default.VerticalSplit,
                    contentDescription = AppStrings.splitView(lang),
                    tint = if (isSplitView) AccentCyan else TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Right Actions: Share, More Overflow Menu, and HERO SAVE BUTTON
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .size(36.dp)
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
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(36.dp)
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
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
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
