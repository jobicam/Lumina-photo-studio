package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.ui.components.CameraCaptureView
import com.example.ui.components.SaveOptionsModal
import com.example.ui.components.SaveSuccessModal
import com.example.ui.components.UpdateCheckerModal
import com.example.ui.components.WhatsNewModal
import com.example.ui.components.bounceClickable
import com.example.ui.components.pressBounce
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.EditorTab
import com.example.viewmodel.PhotoEditorViewModel

@Composable
fun HomeScreen(
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lang = uiState.settings.appLanguage
    var showCamera by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(false) }

    // Automatically show What's New modal whenever the app is updated to a new version or build
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("lumina_version_prefs", android.content.Context.MODE_PRIVATE)
        val currentVer = "${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
        val lastSeenVer = prefs.getString("last_seen_version", "")
        if (lastSeenVer != currentVer) {
            showWhatsNew = true
            prefs.edit().putString("last_seen_version", currentVer).apply()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadFromUri(uri)
        }
    }

    val storagePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.openSaveModal()
        } else {
            Toast.makeText(context, com.example.util.AppStrings.storagePermDeniedToast(lang), Toast.LENGTH_SHORT).show()
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentCyan.copy(alpha = 0.09f), Color.Transparent),
                        center = Offset(size.width * 0.18f, 0f),
                        radius = size.width * 0.72f
                    ),
                    center = Offset(size.width * 0.18f, 0f),
                    radius = size.width * 0.72f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentPurple.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.08f),
                        radius = size.width * 0.58f
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.08f),
                    radius = size.width * 0.58f
                )
            }
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("home_screen_content")
        ) {
            // ----------------------------------------------------
            // 1. STUDIO HEADER
            // ----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(AccentCyan, AccentPurple))
                            )
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(StudioBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Lumina Logo",
                            tint = AccentCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lumina",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Photo",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Light,
                                color = AccentCyan
                            )
                        }
                        Text(
                            text = com.example.util.AppStrings.appSubtitle(lang),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Version & What's New pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioCard)
                            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                            .bounceClickable(pressedScale = 0.94f) { showWhatsNew = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("home_version_badge")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "What's New",
                                tint = AccentCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Direct Check for Updates button
                    val hasAvailableUpdate = uiState.updateStatus is com.example.model.UpdateStatus.UpdateAvailable
                    val updatesInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = { viewModel.checkForUpdates(openDialog = true) },
                        interactionSource = updatesInteraction,
                        modifier = Modifier
                            .pressBounce(updatesInteraction, pressedScale = 0.90f)
                            .testTag("home_check_updates_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (hasAvailableUpdate) AccentAmber.copy(alpha = 0.18f) else StudioCard)
                                .border(
                                    width = 1.dp,
                                    color = if (hasAvailableUpdate) AccentAmber else StudioBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = com.example.util.AppStrings.checkForUpdates(lang),
                                tint = if (hasAvailableUpdate) AccentAmber else AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Settings Button
                    val settingsInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        interactionSource = settingsInteraction,
                        modifier = Modifier
                            .pressBounce(settingsInteraction, pressedScale = 0.90f)
                            .testTag("home_settings_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(StudioCard)
                                .border(1.dp, StudioBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = com.example.util.AppStrings.tabSettings(lang),
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ----------------------------------------------------
            // 2. ACTIVE SESSION / RESUME CARD (if photo loaded)
            // ----------------------------------------------------
            AnimatedVisibility(
                visible = uiState.workingBitmap != null,
                enter = fadeIn(tween(220)) + expandVertically(tween(240)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(200))
            ) {
                uiState.workingBitmap?.let { bmp ->
                    Column {
                        ActiveSessionCard(
                            workingBitmap = bmp,
                            activeEditsCount = activeEditsCount,
                            lang = lang,
                            onResume = { viewModel.navigateTo(AppScreen.EDITOR) },
                            onSave = requestSaveAction
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            // ----------------------------------------------------
            // 3. MAIN ACTIONS GRID
            // ----------------------------------------------------
            Text(
                text = com.example.util.AppStrings.createAndEdit(lang),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Action: Camera Studio
                StudioActionCard(
                    title = com.example.util.AppStrings.cameraCapture(lang),
                    subtitle = com.example.util.AppStrings.cameraSubtitle(lang),
                    icon = Icons.Default.PhotoCamera,
                    accentColor = AccentCyan,
                    onClick = { showCamera = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_camera_card")
                )

                // Action: Gallery Import
                StudioActionCard(
                    title = com.example.util.AppStrings.importPhoto(lang),
                    subtitle = com.example.util.AppStrings.importSubtitle(lang),
                    icon = Icons.Default.AddPhotoAlternate,
                    accentColor = AccentAmber,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_gallery_card")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Action: Face Retouch
                StudioActionCard(
                    title = com.example.util.AppStrings.faceRetouch(lang),
                    subtitle = com.example.util.AppStrings.faceRetouchSubtitle(lang),
                    icon = Icons.Default.Face,
                    accentColor = AccentPink,
                    onClick = {
                        viewModel.loadSamplePhoto(
                            isPortrait = true,
                            autoNavigate = true,
                            initialTab = EditorTab.RETOUCH
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_retouch_card")
                )

                // Action: Color & Curves Lab
                StudioActionCard(
                    title = com.example.util.AppStrings.curvesAndFilters(lang),
                    subtitle = com.example.util.AppStrings.curvesSubtitle(lang),
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    accentColor = AccentPurple,
                    onClick = {
                        viewModel.loadSamplePhoto(
                            isPortrait = false,
                            autoNavigate = true,
                            initialTab = EditorTab.CURVES
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_curves_card")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ----------------------------------------------------
            // 4. QUICK-START SAMPLES SECTION
            // ----------------------------------------------------
            Text(
                text = com.example.util.AppStrings.trySamplePhotos(lang),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SamplePhotoHomeCard(
                    title = com.example.util.AppStrings.naturalPortrait(lang),
                    badge = com.example.util.AppStrings.portraitBadge(lang),
                    imageRes = R.drawable.sample_portrait,
                    icon = Icons.Default.Face,
                    onClick = {
                        viewModel.loadSamplePhoto(
                            isPortrait = true,
                            autoNavigate = true,
                            initialTab = EditorTab.RETOUCH
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_sample_portrait")
                )

                SamplePhotoHomeCard(
                    title = com.example.util.AppStrings.mountainSunset(lang),
                    badge = com.example.util.AppStrings.sunsetBadge(lang),
                    imageRes = R.drawable.sample_landscape,
                    icon = Icons.Default.Landscape,
                    onClick = {
                        viewModel.loadSamplePhoto(
                            isPortrait = false,
                            autoNavigate = true,
                            initialTab = EditorTab.FILTERS
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_sample_landscape")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ----------------------------------------------------
            // 5. PRO FEATURES HIGHLIGHT CAROUSEL
            // ----------------------------------------------------
            Text(
                text = com.example.util.AppStrings.studioCapabilities(lang),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureBanner(
                    title = com.example.util.AppStrings.skinSmoothBannerTitle(lang),
                    description = com.example.util.AppStrings.skinSmoothBannerDesc(lang),
                    icon = Icons.Default.Face,
                    accent = AccentPink
                )

                FeatureBanner(
                    title = com.example.util.AppStrings.updateBannerTitle(lang),
                    description = com.example.util.AppStrings.updateBannerDesc(lang),
                    icon = Icons.Default.SystemUpdate,
                    accent = AccentCyan
                )

                FeatureBanner(
                    title = com.example.util.AppStrings.cropBannerTitle(lang),
                    description = com.example.util.AppStrings.cropBannerDesc(lang),
                    icon = Icons.Default.Crop,
                    accent = AccentCyan
                )

                FeatureBanner(
                    title = com.example.util.AppStrings.cameraFilterBannerTitle(lang),
                    description = com.example.util.AppStrings.cameraFilterBannerDesc(lang),
                    icon = Icons.Default.PhotoCamera,
                    accent = AccentCyan
                )

                FeatureBanner(
                    title = com.example.util.AppStrings.autoEnhanceBannerTitle(lang),
                    description = com.example.util.AppStrings.autoEnhanceBannerDesc(lang),
                    icon = Icons.Default.AutoAwesome,
                    accent = AccentCyan
                )

                FeatureBanner(
                    title = com.example.util.AppStrings.studioFramesBannerTitle(lang),
                    description = com.example.util.AppStrings.studioFramesBannerDesc(lang),
                    icon = Icons.Default.Filter,
                    accent = AccentAmber
                )

                FeatureBanner(
                    title = com.example.util.AppStrings.histogramBannerTitle(lang),
                    description = com.example.util.AppStrings.histogramBannerDesc(lang),
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    accent = AccentCyan
                )

                FeatureBanner(
                    title = com.example.util.AppStrings.retouchStudioTitle(lang),
                    description = com.example.util.AppStrings.retouchStudioDesc(lang),
                    icon = Icons.Default.AutoFixHigh,
                    accent = AccentCyan
                )

                FeatureBanner(
                    title = com.example.util.AppStrings.curvesTitle(lang),
                    description = com.example.util.AppStrings.curvesDesc(lang),
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    accent = Color(0xFF69F0AE)
                )

                FeatureBanner(
                    title = com.example.util.AppStrings.filtersTitle(lang),
                    description = com.example.util.AppStrings.filtersDesc(lang),
                    icon = Icons.Default.Filter,
                    accent = AccentAmber
                )
            }

            Spacer(modifier = Modifier.height(96.dp))
        }

        // Bottom Navigation Bar with subtle top studio hairline divider
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(StudioBorder)
            )
            NavigationBar(
                containerColor = StudioSurface,
                contentColor = TextPrimary,
                tonalElevation = 0.dp,
                modifier = Modifier.testTag("home_bottom_navigation")
            ) {
            NavigationBarItem(
                selected = true,
                onClick = { /* Already on Studio Home */ },
                icon = { Icon(Icons.Default.Home, contentDescription = com.example.util.AppStrings.tabStudio(lang)) },
                label = { Text(com.example.util.AppStrings.tabStudio(lang), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = AccentCyan,
                    indicatorColor = AccentCyan,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )

            NavigationBarItem(
                selected = false,
                onClick = {
                    if (uiState.workingBitmap != null) {
                        viewModel.navigateTo(AppScreen.EDITOR)
                    } else {
                        viewModel.loadSamplePhoto(isPortrait = true, autoNavigate = true)
                    }
                },
                icon = { Icon(Icons.Default.Tune, contentDescription = com.example.util.AppStrings.tabEditor(lang)) },
                label = { Text(com.example.util.AppStrings.tabEditor(lang), fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = AccentCyan,
                    indicatorColor = AccentCyan,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )

            NavigationBarItem(
                selected = false,
                onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                icon = { Icon(Icons.Default.Settings, contentDescription = com.example.util.AppStrings.tabSettings(lang)) },
                label = { Text(com.example.util.AppStrings.tabSettings(lang), fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = AccentCyan,
                    indicatorColor = AccentCyan,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
        }

        // Live Camera Capture Fullscreen View
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

        // Save Options Modal
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
                            Toast.makeText(
                                context,
                                com.example.util.AppStrings.statusShareFailed(lang, null),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onDismiss = { viewModel.closeSaveSuccessModal() }
            )
        }
    }
}

@Composable
private fun ActiveSessionCard(
    workingBitmap: Bitmap,
    activeEditsCount: Int,
    lang: com.example.model.AppLanguage,
    onResume: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(StudioCard, StudioSurfaceVariant)
                )
            )
            .border(1.dp, AccentCyan.copy(alpha = 0.42f), RoundedCornerShape(18.dp))
            .bounceClickable(pressedScale = 0.97f, onClick = onResume)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, AccentCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
        ) {
            Image(
                bitmap = workingBitmap.asImageBitmap(),
                contentDescription = "Active session",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(AccentCyan, CircleShape)
                )
                Text(
                    text = com.example.util.AppStrings.activeSession(lang),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
                    letterSpacing = 0.7.sp
                )
                if (activeEditsCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentCyan.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = com.example.util.AppStrings.activeEditsCount(activeEditsCount, lang),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = com.example.util.AppStrings.continueEditing(lang),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "${workingBitmap.width} × ${workingBitmap.height} px",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Direct Save to Gallery button
            val saveInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onSave,
                interactionSource = saveInteraction,
                modifier = Modifier
                    .pressBounce(saveInteraction, pressedScale = 0.90f)
                    .testTag("home_active_session_save")
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StudioSurfaceVariant)
                        .border(1.dp, StudioBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Save to Gallery",
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Resume Edit button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AccentCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Resume",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StudioActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(StudioCard)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(36.dp.toPx(), 36.dp.toPx()),
                        radius = 84.dp.toPx()
                    ),
                    center = Offset(36.dp.toPx(), 36.dp.toPx()),
                    radius = 84.dp.toPx()
                )
            }
            .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
            .bounceClickable(pressedScale = 0.95f, onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.16f))
                .border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun SamplePhotoHomeCard(
    title: String,
    badge: String,
    imageRes: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(StudioCard)
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
            .bounceClickable(pressedScale = 0.95f, onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle bottom gradient vignette for contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    )
            )
            // Badge tag
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun FeatureBanner(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color
) {
    Row(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StudioCard)
            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
