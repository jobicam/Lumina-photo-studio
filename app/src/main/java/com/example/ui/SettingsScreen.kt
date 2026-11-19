package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.model.AppLanguage
import com.example.model.ExportFormat
import com.example.model.UpdateStatus
import com.example.ui.components.UpdateCheckerModal
import com.example.ui.components.WhatsNewModal
import com.example.ui.components.bounceClickable
import com.example.ui.components.pressBounce
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings
import com.example.viewmodel.AppScreen
import com.example.viewmodel.PhotoEditorViewModel

@Composable
fun SettingsScreen(
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val lang = settings.appLanguage
    var showWhatsNew by remember { mutableStateOf(false) }

    // Android Hardware/System Back Handler returns to Home screen
    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentCyan.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.15f, 0f),
                        radius = size.width * 0.75f
                    ),
                    center = Offset(size.width * 0.15f, 0f),
                    radius = size.width * 0.75f
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("settings_screen_content")
        ) {
            // ----------------------------------------------------
            // 1. TOP BAR
            // ----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val backInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.HOME) },
                    interactionSource = backInteraction,
                    modifier = Modifier
                        .pressBounce(backInteraction, pressedScale = 0.90f)
                        .testTag("settings_back_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(StudioCard)
                            .border(1.dp, StudioBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = AppStrings.backToHome(lang),
                            tint = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = AppStrings.tabSettings(lang),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = AppStrings.preferences(lang),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ----------------------------------------------------
            // 2. LANGUAGE SELECTION
            // ----------------------------------------------------
            SettingsSectionHeader(title = AppStrings.languageSection(lang))

            SettingsGroupCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = AppStrings.appDisplayLanguage(lang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = settings.appLanguage.nativeName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.values().forEach { l ->
                            val isSelected = settings.appLanguage == l
                            val pillBg by animateColorAsState(
                                targetValue = if (isSelected) AccentCyan else StudioSurfaceVariant,
                                animationSpec = tween(180),
                                label = "lang_pill_bg"
                            )
                            val pillBorder by animateColorAsState(
                                targetValue = if (isSelected) AccentCyan else StudioBorder,
                                animationSpec = tween(180),
                                label = "lang_pill_border"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(pillBg)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = pillBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .bounceClickable(pressedScale = 0.95f) {
                                        viewModel.updateSettings { it.copy(appLanguage = l) }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = l.nativeName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else TextPrimary
                                    )
                                    Text(
                                        text = l.displayName,
                                        fontSize = 9.sp,
                                        color = if (isSelected) Color.Black.copy(alpha = 0.7f) else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ----------------------------------------------------
            // 3. EXPORT & IMAGE QUALITY
            // ----------------------------------------------------
            SettingsSectionHeader(title = AppStrings.exportAndFormat(lang))

            SettingsGroupCard {
                // Export Format
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.saveFormat(lang),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = settings.exportFormat.extension.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExportFormat.values().forEach { fmt ->
                            val isSelected = settings.exportFormat == fmt
                            val fmtBg by animateColorAsState(
                                targetValue = if (isSelected) AccentCyan else StudioSurfaceVariant,
                                animationSpec = tween(180),
                                label = "fmt_pill_bg"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(fmtBg)
                                    .bounceClickable(pressedScale = 0.95f) {
                                        viewModel.updateSettings { it.copy(exportFormat = fmt) }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fmt.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                            }
                        }
                    }
                }

                // JPEG Quality Slider (if JPEG format)
                AnimatedVisibility(
                    visible = settings.exportFormat == ExportFormat.JPEG,
                    enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(StudioBorder)
                        )
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = AppStrings.qualityHeader(lang),
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${settings.jpegQuality}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan
                                )
                            }
                            Slider(
                                value = settings.jpegQuality.toFloat(),
                                onValueChange = { q ->
                                    viewModel.updateSettings { it.copy(jpegQuality = q.toInt()) }
                                },
                                valueRange = 70f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentCyan,
                                    activeTrackColor = AccentCyan,
                                    inactiveTrackColor = StudioBorder
                                ),
                                modifier = Modifier.testTag("settings_jpeg_quality_slider")
                            )
                        }
                    }
                }

                // Max Processing Resolution
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StudioBorder)
                )
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = AppStrings.maxResolution(lang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1600 to "1600 px", 2048 to "2048 px (HD)", 3200 to "3200 px (4K)").forEach { (res, label) ->
                            val isSelected = settings.maxResolution == res
                            val resBg by animateColorAsState(
                                targetValue = if (isSelected) StudioSurfaceVariant else StudioBackground,
                                animationSpec = tween(180),
                                label = "res_pill_bg"
                            )
                            val resBorder by animateColorAsState(
                                targetValue = if (isSelected) AccentCyan else StudioBorder,
                                animationSpec = tween(180),
                                label = "res_pill_border"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(resBg)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = resBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .bounceClickable(pressedScale = 0.95f) {
                                        viewModel.updateSettings { it.copy(maxResolution = res) }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AccentCyan else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Destination Album
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StudioBorder)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.galleryAlbumLocation(lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = AppStrings.visibleInGallery(lang),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pictures/Lumina",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ----------------------------------------------------
            // 4. CAMERA & STUDIO PREFERENCES
            // ----------------------------------------------------
            SettingsSectionHeader(title = AppStrings.cameraAndWorkspace(lang))

            SettingsGroupCard {
                SettingsSwitchRow(
                    title = AppStrings.defaultFrontCam(lang),
                    subtitle = AppStrings.defaultFrontCamSub(lang),
                    checked = settings.defaultFrontCamera,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(defaultFrontCamera = checked) }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StudioBorder)
                )

                SettingsSwitchRow(
                    title = AppStrings.gridLines(lang),
                    subtitle = AppStrings.gridLinesSub(lang),
                    checked = settings.showGridOverlay,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(showGridOverlay = checked) }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StudioBorder)
                )

                SettingsSwitchRow(
                    title = AppStrings.hapticFeedback(lang),
                    subtitle = AppStrings.hapticFeedbackSub(lang),
                    checked = settings.hapticFeedback,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(hapticFeedback = checked) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ----------------------------------------------------
            // 5. STORAGE & CACHE
            // ----------------------------------------------------
            SettingsSectionHeader(title = AppStrings.storageAndMemory(lang))

            SettingsGroupCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.tempCache(lang),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${AppStrings.cacheSizePrefix(lang)} ${uiState.cacheSizeDisplay}",
                            fontSize = 12.sp,
                            color = AccentCyan
                        )
                    }

                    val clearCacheInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = { viewModel.clearCache() },
                        interactionSource = clearCacheInteraction,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .pressBounce(clearCacheInteraction, pressedScale = 0.94f)
                            .testTag("clear_cache_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear cache",
                            tint = ErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.clear(lang), color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ----------------------------------------------------
            // 6. ABOUT & VERSION
            // ----------------------------------------------------
            SettingsSectionHeader(title = AppStrings.aboutAndPrivacy(lang))

            SettingsGroupCard {
                // Version Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClickable(pressedScale = 0.98f) { showWhatsNew = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Lumina Photo Studio",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StudioSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = AppStrings.whatsNew(lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StudioBorder)
                )

                // Check for Updates Row
                val updateBadgeText = when (val st = uiState.updateStatus) {
                    is UpdateStatus.Checking -> AppStrings.checkingForUpdates(lang)
                    is UpdateStatus.UpdateAvailable -> "v${st.latestVersionName}"
                    is UpdateStatus.UpToDate -> AppStrings.upToDateTitle(lang)
                    is UpdateStatus.Error -> AppStrings.updateCheckFailedTitle(lang)
                    is UpdateStatus.Idle -> AppStrings.checkForUpdates(lang)
                }
                val updateBadgeColor = when (uiState.updateStatus) {
                    is UpdateStatus.UpdateAvailable -> AccentAmber
                    is UpdateStatus.Error -> ErrorRed
                    else -> AccentCyan
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClickable(pressedScale = 0.98f) { viewModel.checkForUpdates(openDialog = true) }
                        .padding(14.dp)
                        .testTag("settings_check_updates_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(updateBadgeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = AppStrings.checkForUpdates(lang),
                                tint = updateBadgeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = AppStrings.checkForUpdates(lang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (uiState.lastUpdateCheckMillis > 0L) {
                                    "${AppStrings.lastCheckedLabel(lang)}: " +
                                        java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
                                            .format(java.util.Date(uiState.lastUpdateCheckMillis))
                                } else {
                                    "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                                },
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(updateBadgeColor.copy(alpha = 0.16f))
                            .border(1.dp, updateBadgeColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = updateBadgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = updateBadgeColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StudioBorder)
                )

                SettingsSwitchRow(
                    title = AppStrings.autoCheckUpdates(lang),
                    subtitle = AppStrings.autoCheckUpdatesSub(lang),
                    checked = settings.autoCheckUpdates,
                    onCheckedChange = { checked ->
                        viewModel.updateSettings { it.copy(autoCheckUpdates = checked) }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StudioBorder)
                )

                // Privacy guarantee note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = AppStrings.privacyNote(lang),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
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
                onCheckAgain = { viewModel.checkForUpdates(openDialog = true) },
                onOpenWhatsNew = { showWhatsNew = true },
                onDismiss = { viewModel.dismissUpdateDialog() }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StudioCard)
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AccentCyan,
                uncheckedThumbColor = TextDisabled,
                uncheckedTrackColor = StudioSurfaceVariant
            )
        )
    }
}
