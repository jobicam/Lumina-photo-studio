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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.material.icons.filled.ShowChart
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.BuildConfig
import com.example.R
import com.example.ui.components.CameraCaptureView
import com.example.ui.components.SaveOptionsModal
import com.example.ui.components.SaveSuccessModal
import com.example.ui.components.UpdateCheckerModal
import com.example.ui.components.WhatsNewModal
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
    val uiState by viewModel.uiState.collectAsState()
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
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
                            .clickable { showWhatsNew = true }
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
                    IconButton(
                        onClick = { viewModel.checkForUpdates(openDialog = true) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (hasAvailableUpdate) AccentAmber.copy(alpha = 0.18f) else StudioCard)
                            .border(
                                width = 1.dp,
                                color = if (hasAvailableUpdate) AccentAmber else StudioBorder,
                                shape = CircleShape
                            )
                            .testTag("home_check_updates_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = com.example.util.AppStrings.checkForUpdates(lang),
                            tint = if (hasAvailableUpdate) AccentAmber else AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Settings Button
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(StudioCard)
                            .border(1.dp, StudioBorder, CircleShape)
                            .testTag("home_settings_button")
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

            Spacer(modifier = Modifier.height(16.dp))

            // ----------------------------------------------------
            // 2. ACTIVE SESSION / RESUME CARD (if photo loaded)
            // ----------------------------------------------------
            if (uiState.workingBitmap != null) {
                ActiveSessionCard(
                    workingBitmap = uiState.workingBitmap!!,
                    lang = lang,
                    onResume = { viewModel.navigateTo(AppScreen.EDITOR) },
                    onSave = requestSaveAction
                )
                Spacer(modifier = Modifier.height(20.dp))
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
                        viewModel.selectTab(EditorTab.RETOUCH)
                        viewModel.loadSamplePhoto(isPortrait = true, autoNavigate = true)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_retouch_card")
                )

                // Action: Color & Curves Lab
                StudioActionCard(
                    title = com.example.util.AppStrings.curvesAndFilters(lang),
                    subtitle = com.example.util.AppStrings.curvesSubtitle(lang),
                    icon = Icons.Default.ShowChart,
                    accentColor = AccentPurple,
                    onClick = {
                        viewModel.selectTab(EditorTab.CURVES)
                        viewModel.loadSamplePhoto(isPortrait = false, autoNavigate = true)
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
                        viewModel.selectTab(EditorTab.RETOUCH)
                        viewModel.loadSamplePhoto(isPortrait = true, autoNavigate = true)
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
                        viewModel.selectTab(EditorTab.FILTERS)
                        viewModel.loadSamplePhoto(isPortrait = false, autoNavigate = true)
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
                    icon = Icons.Default.ShowChart,
                    accent = AccentCyan
                )

                FeatureBanner(
                    title = com.example.util.AppStrings.blemishTitle(lang),
                    description = com.example.util.AppStrings.blemishDesc(lang),
                    icon = Icons.Default.AutoFixHigh,
                    accent = AccentCyan
                )

                FeatureBanner(
                    title = com.example.util.AppStrings.curvesTitle(lang),
                    description = com.example.util.AppStrings.curvesDesc(lang),
                    icon = Icons.Default.ShowChart,
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

        // Bottom Navigation Bar
        NavigationBar(
            containerColor = StudioCard,
            contentColor = TextPrimary,
            tonalElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .testTag("home_bottom_navigation")
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
                currentFeedUrl = uiState.settings.updateFeedUrl,
                lang = lang,
                onCheckAgain = { viewModel.checkForUpdates(openDialog = true) },
                onSaveFeedUrl = { url -> viewModel.saveUpdateFeedUrlAndCheck(url) },
                onOpenWhatsNew = { showWhatsNew = true },
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
                        context.startActivity(shareIntent)
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
    lang: com.example.model.AppLanguage,
    onResume: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(StudioCard, StudioSurfaceVariant)
                )
            )
            .border(1.dp, AccentCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onResume)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AccentCyan, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = com.example.util.AppStrings.activeSession(lang),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = com.example.util.AppStrings.continueEditing(lang),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "${workingBitmap.width} × ${workingBitmap.height} px",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Direct Save to Gallery button
            IconButton(
                onClick = onSave,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(StudioSurfaceVariant)
                    .border(1.dp, StudioBorder, CircleShape)
                    .testTag("home_active_session_save")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Save to Gallery",
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Resume Edit button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
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
            .clip(RoundedCornerShape(16.dp))
            .background(StudioCard)
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.15f)),
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
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = TextSecondary
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
            .clickable(onClick = onClick)
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
            // Badge tag
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
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
