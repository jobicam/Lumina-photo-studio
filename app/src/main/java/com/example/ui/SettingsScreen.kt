package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.model.AppLanguage
import com.example.model.CropGridStyle
import com.example.model.ExportFormat
import com.example.model.UpdateStatus
import com.example.model.UserSettings
import com.example.ui.components.EditorTabOrderList
import com.example.ui.components.ResetPreferencesConfirmDialog
import com.example.ui.components.SettingsCategoryChip
import com.example.ui.components.SettingsGroupCard
import com.example.ui.components.SettingsHairlineDivider
import com.example.ui.components.SettingsIconBadge
import com.example.ui.components.SettingsSectionHeader
import com.example.ui.components.SettingsSwitchRow
import com.example.ui.components.StudioProfileHeroCard
import com.example.ui.components.UpdateCheckerModal
import com.example.ui.components.WhatsNewModal
import com.example.ui.components.bounceClickable
import com.example.ui.components.editorTabIcon
import com.example.ui.components.editorTabLabel
import com.example.ui.components.pressBounce
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings
import com.example.viewmodel.AppScreen
import com.example.viewmodel.PhotoEditorViewModel

private enum class SettingsCategory {
    ALL,
    EXPORT,
    STUDIO,
    SYSTEM
}

@Composable
fun SettingsScreen(
    viewModel: PhotoEditorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val lang = settings.appLanguage
    var showWhatsNew by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }
    var isTabOrderExpanded by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()
    val isHeaderElevated by remember {
        derivedStateOf { scrollState.value > 12 }
    }
    val headerBgAlpha by animateFloatAsState(
        targetValue = if (isHeaderElevated) 0.94f else 0f,
        animationSpec = tween(200),
        label = "settings_header_bg_alpha"
    )
    val headerBorderDp by animateDpAsState(
        targetValue = if (isHeaderElevated) 1.dp else 0.dp,
        animationSpec = tween(200),
        label = "settings_header_border"
    )

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
                        colors = listOf(AccentCyan.copy(alpha = 0.09f), Color.Transparent),
                        center = Offset(size.width * 0.14f, size.height * 0.04f),
                        radius = size.width * 0.72f
                    ),
                    center = Offset(size.width * 0.14f, size.height * 0.04f),
                    radius = size.width * 0.72f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(AccentPurple.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.55f),
                        radius = size.width * 0.65f
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.55f),
                    radius = size.width * 0.65f
                )
            }
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            // ----------------------------------------------------
            // 1. STICKY FROSTED HEADER + QUICK CATEGORY CHIPS
            // ----------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioSurface.copy(alpha = headerBgAlpha))
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 10.dp),
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

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.tabSettings(lang),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = AppStrings.preferences(lang),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentCyan.copy(alpha = 0.14f))
                            .border(1.dp, AccentCyan.copy(alpha = 0.42f), RoundedCornerShape(10.dp))
                            .bounceClickable(pressedScale = 0.94f) { showWhatsNew = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("settings_version_pill")
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }
                }

                // Category filter chips bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp)
                        .testTag("settings_category_tabs"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsCategoryChip(
                        label = AppStrings.settingsCategoryAll(lang),
                        selected = selectedCategory == SettingsCategory.ALL,
                        onClick = { selectedCategory = SettingsCategory.ALL },
                        testTag = "settings_category_all"
                    )
                    SettingsCategoryChip(
                        label = AppStrings.settingsCategoryExport(lang),
                        selected = selectedCategory == SettingsCategory.EXPORT,
                        onClick = { selectedCategory = SettingsCategory.EXPORT },
                        testTag = "settings_category_export"
                    )
                    SettingsCategoryChip(
                        label = AppStrings.settingsCategoryStudio(lang),
                        selected = selectedCategory == SettingsCategory.STUDIO,
                        onClick = { selectedCategory = SettingsCategory.STUDIO },
                        testTag = "settings_category_studio"
                    )
                    SettingsCategoryChip(
                        label = AppStrings.settingsCategorySystem(lang),
                        selected = selectedCategory == SettingsCategory.SYSTEM,
                        onClick = { selectedCategory = SettingsCategory.SYSTEM },
                        testTag = "settings_category_system"
                    )
                }
            }

            if (headerBorderDp > 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerBorderDp)
                        .background(StudioBorder.copy(alpha = 0.8f))
                )
            }

            // ----------------------------------------------------
            // 2. SCROLLABLE SETTINGS CONTENT
            // ----------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("settings_screen_content")
            ) {
                // Overview Hero Card (shown in ALL)
                AnimatedVisibility(
                    visible = selectedCategory == SettingsCategory.ALL,
                    enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                ) {
                    Column {
                        StudioProfileHeroCard(
                            settings = settings,
                            updateStatus = uiState.updateStatus,
                            lang = lang,
                            onOpenWhatsNew = { showWhatsNew = true },
                            onCheckUpdates = { viewModel.checkForUpdates(openDialog = true) }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // ----------------------------------------------------
                // LANGUAGE & EXPORT SECTION
                // ----------------------------------------------------
                AnimatedVisibility(
                    visible = selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.EXPORT,
                    enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                ) {
                    Column {
                        SettingsSectionHeader(
                            title = AppStrings.languageSection(lang),
                            accentColor = AccentCyan,
                            trailingBadge = settings.appLanguage.nativeName
                        )

                        SettingsGroupCard {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SettingsIconBadge(icon = Icons.Default.Language, tint = AccentCyan)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = AppStrings.appDisplayLanguage(lang),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
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
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(pillBg)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = pillBorder,
                                                    shape = RoundedCornerShape(12.dp)
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
                                                    color = if (isSelected) Color.Black.copy(alpha = 0.72f) else TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        SettingsSectionHeader(
                            title = AppStrings.exportAndFormat(lang),
                            accentColor = AccentPurple,
                            trailingBadge = "${settings.exportFormat.extension.uppercase()} · ${settings.maxResolution}px"
                        )

                        SettingsGroupCard {
                            // Export Format
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SettingsIconBadge(icon = Icons.Default.SaveAlt, tint = AccentPurple)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = AppStrings.saveFormat(lang),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }
                                    Text(
                                        text = settings.exportFormat.extension.uppercase(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

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
                                        val fmtBorder by animateColorAsState(
                                            targetValue = if (isSelected) AccentCyan else StudioBorder,
                                            animationSpec = tween(180),
                                            label = "fmt_pill_border"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(fmtBg)
                                                .border(1.dp, fmtBorder, RoundedCornerShape(11.dp))
                                                .bounceClickable(pressedScale = 0.95f) {
                                                    viewModel.updateSettings { it.copy(exportFormat = fmt) }
                                                }
                                                .padding(vertical = 10.dp, horizontal = 6.dp),
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

                            // JPEG Quality Slider + Quick Preset Chips
                            AnimatedVisibility(
                                visible = settings.exportFormat == ExportFormat.JPEG,
                                enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                                exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                            ) {
                                Column {
                                    SettingsHairlineDivider()
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                SettingsIconBadge(icon = Icons.Default.Tune, tint = AccentCyan)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = AppStrings.qualityHeader(lang),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = AppStrings.qualityTierLabel(settings.jpegQuality, lang),
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(AccentCyan.copy(alpha = 0.14f))
                                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${settings.jpegQuality}%",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentCyan
                                                )
                                            }
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
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(80, 90, 95, 100).forEach { preset ->
                                                val isPresetActive = settings.jpegQuality == preset
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isPresetActive) AccentCyan.copy(alpha = 0.18f)
                                                            else StudioSurfaceVariant.copy(alpha = 0.65f)
                                                        )
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isPresetActive) AccentCyan else StudioBorder,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .bounceClickable(pressedScale = 0.94f) {
                                                            viewModel.updateSettings { it.copy(jpegQuality = preset) }
                                                        }
                                                        .padding(vertical = 6.dp)
                                                        .testTag("settings_quality_preset_$preset"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$preset%",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isPresetActive) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isPresetActive) AccentCyan else TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsHairlineDivider()

                            // Max Processing Resolution Cards
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SettingsIconBadge(icon = Icons.Default.PhotoSizeSelectLarge, tint = AccentPurple)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = AppStrings.maxResolution(lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        Triple(1600, "1600 px", AppStrings.resSubFast(lang)),
                                        Triple(2048, "2048 px", AppStrings.resSubBalanced(lang)),
                                        Triple(3200, "3200 px", AppStrings.resSubUltra(lang))
                                    ).forEach { (res, title, sub) ->
                                        val isSelected = settings.maxResolution == res
                                        val resBg by animateColorAsState(
                                            targetValue = if (isSelected) AccentCyan.copy(alpha = 0.15f) else StudioBackground,
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
                                                .heightIn(min = 48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(resBg)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = resBorder,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .bounceClickable(pressedScale = 0.95f) {
                                                    viewModel.updateSettings { it.copy(maxResolution = res) }
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = title,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSelected) AccentCyan else TextPrimary
                                                )
                                                Text(
                                                    text = sub,
                                                    fontSize = 9.sp,
                                                    color = if (isSelected) AccentCyan.copy(alpha = 0.85f) else TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsHairlineDivider()

                            // Destination Album Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SettingsIconBadge(icon = Icons.Default.FolderOpen, tint = AccentAmber)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
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
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StudioSurfaceVariant)
                                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Pictures/Lumina",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // ----------------------------------------------------
                // CAMERA, WORKSPACE & EDITOR TAB ORDER SECTION
                // ----------------------------------------------------
                AnimatedVisibility(
                    visible = selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.STUDIO,
                    enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                ) {
                    Column {
                        SettingsSectionHeader(
                            title = AppStrings.cameraAndWorkspace(lang),
                            accentColor = AccentCyan
                        )

                        SettingsGroupCard {
                            SettingsSwitchRow(
                                icon = Icons.Default.CameraFront,
                                iconTint = AccentPurple,
                                title = AppStrings.defaultFrontCam(lang),
                                subtitle = AppStrings.defaultFrontCamSub(lang),
                                checked = settings.defaultFrontCamera,
                                onCheckedChange = { checked ->
                                    viewModel.updateSettings { it.copy(defaultFrontCamera = checked) }
                                }
                            )

                            SettingsHairlineDivider()

                            SettingsSwitchRow(
                                icon = Icons.Default.Grid4x4,
                                iconTint = AccentCyan,
                                title = AppStrings.gridLines(lang),
                                subtitle = AppStrings.gridLinesSub(lang),
                                checked = settings.showGridOverlay,
                                onCheckedChange = { checked ->
                                    viewModel.updateSettings { it.copy(showGridOverlay = checked) }
                                }
                            )

                            SettingsHairlineDivider()

                            SettingsSwitchRow(
                                icon = Icons.Default.Vibration,
                                iconTint = AccentAmber,
                                title = AppStrings.hapticFeedback(lang),
                                subtitle = AppStrings.hapticFeedbackSub(lang),
                                checked = settings.hapticFeedback,
                                onCheckedChange = { checked ->
                                    viewModel.updateSettings { it.copy(hapticFeedback = checked) }
                                }
                            )

                            SettingsHairlineDivider()

                            SettingsSwitchRow(
                                icon = Icons.Default.History,
                                iconTint = AccentCyan,
                                title = AppStrings.autoSaveDraftsLabel(lang),
                                subtitle = AppStrings.autoSaveDraftsSub(lang),
                                checked = settings.autoSaveDrafts,
                                onCheckedChange = { checked ->
                                    viewModel.updateSettings { it.copy(autoSaveDrafts = checked) }
                                }
                            )

                            SettingsHairlineDivider()

                            // Default Crop Guide Style selector
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SettingsIconBadge(icon = Icons.Default.GridOn, tint = AccentCyan)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = AppStrings.defaultCropGridSetting(lang),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = AppStrings.defaultCropGridSettingSub(lang),
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CropGridStyle.values().forEach { style ->
                                        val isSelected = settings.defaultCropGridStyle == style
                                        val pillBg by animateColorAsState(
                                            targetValue = if (isSelected) AccentCyan else StudioSurfaceVariant,
                                            animationSpec = tween(180),
                                            label = "crop_grid_pill_bg_${style.name}"
                                        )
                                        val pillBorder by animateColorAsState(
                                            targetValue = if (isSelected) AccentCyan else StudioBorder,
                                            animationSpec = tween(180),
                                            label = "crop_grid_pill_border_${style.name}"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 42.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(pillBg)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = pillBorder,
                                                    shape = RoundedCornerShape(9.dp)
                                                )
                                                .bounceClickable(pressedScale = 0.95f) {
                                                    viewModel.updateSettings { it.copy(defaultCropGridStyle = style) }
                                                }
                                                .padding(vertical = 8.dp)
                                                .testTag("settings_crop_grid_${style.name.lowercase()}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = AppStrings.cropGridStyleLabel(style, lang),
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.Black else TextPrimary,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Editor Tab Order Section with Live Preview Strip + Expand/Collapse
                        val normalizedTabs = remember(settings.editorTabOrder) {
                            UserSettings.normalizeTabOrder(settings.editorTabOrder)
                        }
                        val isCustomOrder = normalizedTabs != UserSettings.DEFAULT_EDITOR_TAB_ORDER
                        SettingsSectionHeader(
                            title = AppStrings.editorToolbarSection(lang),
                            accentColor = AccentPurple,
                            trailingBadge = if (isCustomOrder) {
                                AppStrings.customTabOrderBadge(lang)
                            } else {
                                AppStrings.defaultTabOrderBadge(lang)
                            }
                        )

                        SettingsGroupCard {
                            val tabChevronRotation by animateFloatAsState(
                                targetValue = if (isTabOrderExpanded) 180f else 0f,
                                animationSpec = tween(220),
                                label = "tab_order_chevron"
                            )

                            // Compact live preview strip + toggle header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClickable(pressedScale = 0.99f) {
                                        isTabOrderExpanded = !isTabOrderExpanded
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    normalizedTabs.forEachIndexed { idx, tab ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (idx < 4) AccentCyan.copy(alpha = 0.12f)
                                                    else StudioSurfaceVariant.copy(alpha = 0.65f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (idx < 4) AccentCyan.copy(alpha = 0.30f) else StudioBorder,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = editorTabIcon(tab),
                                                contentDescription = null,
                                                tint = if (idx < 4) AccentCyan else TextSecondary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = editorTabLabel(tab, lang),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (idx < 4) TextPrimary else TextSecondary,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StudioSurfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = AppStrings.tapToExpandOrder(lang, isTabOrderExpanded),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .graphicsLayer { rotationZ = tabChevronRotation }
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = isTabOrderExpanded,
                                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                                exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                            ) {
                                Column {
                                    SettingsHairlineDivider()
                                    EditorTabOrderList(
                                        orderedTabs = normalizedTabs,
                                        lang = lang,
                                        onMoveTab = { tab, delta -> viewModel.moveEditorTab(tab, delta) },
                                        onResetOrder = { viewModel.resetEditorTabOrder() }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // ----------------------------------------------------
                // STORAGE, UPDATES & PRIVACY SECTION
                // ----------------------------------------------------
                AnimatedVisibility(
                    visible = selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.SYSTEM,
                    enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                ) {
                    Column {
                        SettingsSectionHeader(
                            title = AppStrings.storageAndMemory(lang),
                            accentColor = AccentAmber,
                            trailingBadge = uiState.cacheSizeDisplay
                        )

                        SettingsGroupCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SettingsIconBadge(icon = Icons.Default.DeleteSweep, tint = ErrorRed)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
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
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                val clearCacheInteraction = remember { MutableInteractionSource() }
                                Button(
                                    onClick = { viewModel.clearCache() },
                                    interactionSource = clearCacheInteraction,
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
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

                            SettingsHairlineDivider()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SettingsIconBadge(icon = Icons.Default.RestartAlt, tint = AccentAmber)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = AppStrings.resetSettingsLabel(lang),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = AppStrings.resetSettingsSub(lang),
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                val resetPrefsInteraction = remember { MutableInteractionSource() }
                                Button(
                                    onClick = { showResetConfirm = true },
                                    interactionSource = resetPrefsInteraction,
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .pressBounce(resetPrefsInteraction, pressedScale = 0.94f)
                                        .testTag("reset_all_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = AppStrings.resetSettingsLabel(lang),
                                        tint = AccentAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(AppStrings.resetCrop(lang), color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        SettingsSectionHeader(
                            title = AppStrings.aboutAndPrivacy(lang),
                            accentColor = SuccessGreen
                        )

                        SettingsGroupCard {
                            // Version & Changelog Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClickable(pressedScale = 0.98f) { showWhatsNew = true }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SettingsIconBadge(icon = Icons.Default.AutoAwesome, tint = AccentCyan)
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
                                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
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

                            SettingsHairlineDivider()

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
                                is UpdateStatus.UpToDate -> SuccessGreen
                                is UpdateStatus.Error -> ErrorRed
                                else -> AccentCyan
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClickable(pressedScale = 0.98f) {
                                        viewModel.checkForUpdates(openDialog = true)
                                    }
                                    .padding(14.dp)
                                    .testTag("settings_check_updates_row"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SettingsIconBadge(icon = Icons.Default.SystemUpdate, tint = updateBadgeColor)
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

                            SettingsHairlineDivider()

                            SettingsSwitchRow(
                                icon = Icons.Default.SystemUpdate,
                                iconTint = AccentCyan,
                                title = AppStrings.autoCheckUpdates(lang),
                                subtitle = AppStrings.autoCheckUpdatesSub(lang),
                                checked = settings.autoCheckUpdates,
                                onCheckedChange = { checked ->
                                    viewModel.updateSettings { it.copy(autoCheckUpdates = checked) }
                                }
                            )

                            SettingsHairlineDivider()

                            // Privacy guarantee note
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                SettingsIconBadge(icon = Icons.Default.Security, tint = AccentAmber)
                                Spacer(modifier = Modifier.width(12.dp))
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
                }
            }
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

        // Reset Preferences Confirmation Dialog
        if (showResetConfirm) {
            ResetPreferencesConfirmDialog(
                lang = lang,
                onConfirm = {
                    showResetConfirm = false
                    viewModel.resetAllSettingsToDefaults()
                },
                onDismiss = { showResetConfirm = false }
            )
        }
    }
}
