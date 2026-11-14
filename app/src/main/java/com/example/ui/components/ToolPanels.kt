package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdjustmentState
import com.example.model.AppLanguage
import com.example.model.CropAspectRatio
import com.example.model.CropRect
import com.example.model.CurveChannel
import com.example.model.CurvesState
import com.example.model.FilterCategory
import com.example.model.FilterPreset
import com.example.model.FilterPresets
import com.example.model.FrameStyle
import com.example.model.HistogramData
import com.example.model.HslChannelAdjustment
import com.example.model.HslColorChannel
import com.example.model.HslState
import com.example.model.RetouchType
import com.example.model.SingleCurve
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPink
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings

// -------------------------------------------------------------
// 1. FILTERS PANEL
// -------------------------------------------------------------
@Composable
fun FiltersPanel(
    activePreset: FilterPreset,
    filterIntensity: Float,
    onSelectPreset: (FilterPreset) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    lang: AppLanguage = AppLanguage.SYSTEM,
    hapticFeedback: Boolean = true,
    onEditFinished: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf(FilterCategory.ALL) }
    val haptics = LocalHapticFeedback.current
    var lastIntPercent by remember { mutableIntStateOf((filterIntensity * 100).toInt()) }

    val filteredList = remember(selectedCategory) {
        if (selectedCategory == FilterCategory.ALL) {
            FilterPresets.ALL_PRESETS
        } else {
            listOf(FilterPresets.NONE) + FilterPresets.ALL_PRESETS.filter { it.category == selectedCategory }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("filters_panel")
    ) {
        // Intensity Slider (when a non-original filter is selected)
        if (activePreset != FilterPresets.NONE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.strength(lang),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.width(64.dp)
                )
                Slider(
                    value = filterIntensity,
                    onValueChange = { v ->
                        val step = (v * 100).toInt()
                        if (hapticFeedback && step != lastIntPercent) {
                            lastIntPercent = step
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onIntensityChanged(v)
                    },
                    onValueChangeFinished = onEditFinished,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = StudioBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_intensity_slider")
                )
                Text(
                    text = "${(filterIntensity * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(44.dp)
                )
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterCategory.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) AccentCyan else StudioCard)
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("filter_cat_${cat.name.lowercase()}")
                ) {
                    Text(
                        text = AppStrings.filterCategory(cat, lang),
                        color = if (isSelected) Color.Black else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Preset Thumbnails Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredList) { preset ->
                val isSelected = activePreset.id == preset.id
                FilterItemCard(
                    preset = preset,
                    isSelected = isSelected,
                    onClick = { onSelectPreset(preset) }
                )
            }
        }
    }
}

@Composable
private fun FilterItemCard(
    preset: FilterPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
            .testTag("filter_preset_${preset.id}")
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
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
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) AccentCyan else StudioBorder,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = preset.name.take(3).uppercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
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
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = preset.name,
            color = if (isSelected) AccentCyan else TextPrimary,
            fontSize = 11.sp,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// -------------------------------------------------------------
// 2. RETOUCH PANEL
// -------------------------------------------------------------
@Composable
fun RetouchPanel(
    activeTool: RetouchType,
    brushRadius: Float,
    brushIntensity: Float,
    onSelectTool: (RetouchType) -> Unit,
    onRadiusChanged: (Float) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    texturePreserve: Float = 0.30f,
    onTexturePreserveChanged: (Float) -> Unit = {},
    onAutoSkinSmooth: () -> Unit = {},
    modifier: Modifier = Modifier,
    lang: AppLanguage = AppLanguage.SYSTEM,
    hapticFeedback: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    var lastRadiusInt by remember { mutableIntStateOf(brushRadius.toInt()) }
    var lastIntensityInt by remember { mutableIntStateOf((brushIntensity * 100).toInt()) }
    var lastDetailInt by remember { mutableIntStateOf((texturePreserve * 100).toInt()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("retouch_panel")
    ) {
        // Helpful tip banner with Auto Skin Smooth action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = AppStrings.retouchSubtitle(activeTool, lang),
                        fontSize = 11.sp,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }
            }

            // Quick Auto Skin Smoothing trigger
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentPink.copy(alpha = 0.2f))
                    .border(1.dp, AccentPink.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onAutoSkinSmooth)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("auto_skin_smooth_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = AppStrings.autoSmoothSkin(lang),
                        tint = AccentPink,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = AppStrings.autoSmoothSkin(lang),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPink,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tool Selector Carousel
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RetouchType.values()) { tool ->
                val isSelected = tool == activeTool
                val icon = when (tool) {
                    RetouchType.BLEMISH -> Icons.Default.AutoFixHigh
                    RetouchType.SKIN_SMOOTH -> Icons.Default.Face
                    RetouchType.TEETH_WHITEN -> Icons.Default.SentimentSatisfiedAlt
                    RetouchType.EYE_POP -> Icons.Default.RemoveRedEye
                    RetouchType.SCULPT -> Icons.Default.Crop
                    RetouchType.DETAIL_SHARPEN -> Icons.Default.Tune
                    RetouchType.GLOW -> Icons.Default.AutoAwesome
                }
                val title = AppStrings.retouchTitle(tool, lang)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) StudioSurfaceVariant else StudioCard)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) AccentCyan else StudioBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectTool(tool) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("retouch_tool_${tool.name.lowercase()}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) AccentCyan else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            color = if (isSelected) AccentCyan else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Brush Radius Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppStrings.sizeLabel(lang),
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.width(64.dp)
            )
            Slider(
                value = brushRadius,
                onValueChange = { v ->
                    val step = v.toInt()
                    if (hapticFeedback && step != lastRadiusInt) {
                        lastRadiusInt = step
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onRadiusChanged(v)
                },
                valueRange = 10f..80f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentCyan,
                    activeTrackColor = AccentCyan,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("brush_size_slider")
            )
            // Visual circle preview of brush size
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((brushRadius * 0.35f).dp.coerceIn(6.dp, 24.dp))
                        .background(AccentCyan, CircleShape)
                )
            }
        }

        // Brush Intensity Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppStrings.intensityLabel(lang),
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.width(64.dp)
            )
            Slider(
                value = brushIntensity,
                onValueChange = { v ->
                    val step = (v * 100).toInt()
                    if (hapticFeedback && step != lastIntensityInt) {
                        lastIntensityInt = step
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onIntensityChanged(v)
                },
                valueRange = 0.1f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentAmber,
                    activeTrackColor = AccentAmber,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("brush_intensity_slider")
            )
            Text(
                text = "${(brushIntensity * 100).toInt()}%",
                fontSize = 12.sp,
                color = AccentAmber,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(44.dp)
            )
        }

        // Detail Preservation Slider (when Skin Smooth tool is active)
        if (activeTool == RetouchType.SKIN_SMOOTH) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.texturePreserveLabel(lang),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.width(64.dp)
                )
                Slider(
                    value = texturePreserve,
                    onValueChange = { v ->
                        val step = (v * 100).toInt()
                        if (hapticFeedback && step != lastDetailInt) {
                            lastDetailInt = step
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onTexturePreserveChanged(v)
                    },
                    valueRange = 0f..0.75f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPink,
                        activeTrackColor = AccentPink,
                        inactiveTrackColor = StudioBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("texture_preserve_slider")
                )
                Text(
                    text = "${(texturePreserve * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = AccentPink,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(44.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 3. ADJUST PANEL
// -------------------------------------------------------------
enum class AdjustCategory(val label: String) {
    LIGHT("Light"),
    COLOR("Color"),
    DETAIL("Detail"),
    EFFECTS("Effects")
}

@Composable
fun AdjustPanel(
    adjustments: AdjustmentState,
    onAdjustmentsChanged: ((AdjustmentState) -> AdjustmentState) -> Unit,
    onReset: () -> Unit,
    onAutoEnhance: () -> Unit = {},
    modifier: Modifier = Modifier,
    lang: AppLanguage = AppLanguage.SYSTEM,
    hapticFeedback: Boolean = true,
    onEditFinished: () -> Unit = {}
) {
    var activeCategory by remember { mutableStateOf(AdjustCategory.LIGHT) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("adjust_panel")
    ) {
        // Category Switcher + Auto Enhance + Reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AdjustCategory.values().forEach { cat ->
                    val isSelected = activeCategory == cat
                    val catLabel = when (cat) {
                        AdjustCategory.LIGHT -> AppStrings.catLight(lang)
                        AdjustCategory.COLOR -> AppStrings.catColor(lang)
                        AdjustCategory.DETAIL -> AppStrings.catDetail(lang)
                        AdjustCategory.EFFECTS -> AppStrings.catEffects(lang)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) AccentCyan else StudioCard)
                            .clickable { activeCategory = cat }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("adjust_cat_${cat.name.lowercase()}")
                    ) {
                        Text(
                            text = catLabel,
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(AccentCyan.copy(alpha = 0.16f))
                        .border(1.dp, AccentCyan.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onAutoEnhance)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("auto_enhance_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = AppStrings.autoEnhance(lang),
                            tint = AccentCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.autoEnhance(lang),
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("adjust_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset adjustments",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Sliders based on category with vertical scrolling
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            when (activeCategory) {
                AdjustCategory.LIGHT -> {
                    AdjustSliderItem(
                        label = AppStrings.sliderExposure(lang),
                        testTagKey = "exposure",
                        value = adjustments.exposure,
                        range = -100f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(exposure = v) } }
                    )
                    AdjustSliderItem(
                        label = AppStrings.sliderContrast(lang),
                        testTagKey = "contrast",
                        value = adjustments.contrast,
                        range = -100f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(contrast = v) } }
                    )
                    AdjustSliderItem(
                        label = AppStrings.sliderHighlights(lang),
                        testTagKey = "highlights",
                        value = adjustments.highlights,
                        range = -100f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(highlights = v) } }
                    )
                    AdjustSliderItem(
                        label = AppStrings.sliderShadows(lang),
                        testTagKey = "shadows",
                        value = adjustments.shadows,
                        range = -100f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(shadows = v) } }
                    )
                    AdjustSliderItem(
                        label = AppStrings.sliderWhites(lang),
                        testTagKey = "whites",
                        value = adjustments.whites,
                        range = -100f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(whites = v) } }
                    )
                    AdjustSliderItem(
                        label = AppStrings.sliderBlacks(lang),
                        testTagKey = "blacks",
                        value = adjustments.blacks,
                        range = -100f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(blacks = v) } }
                    )
                }
                AdjustCategory.COLOR -> {
                    AdjustSliderItem(
                        label = AppStrings.sliderWarmth(lang),
                        testTagKey = "warmth",
                        value = adjustments.temperature,
                        range = -100f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(temperature = v) } }
                    )
                    AdjustSliderItem(
                        label = AppStrings.sliderTint(lang),
                        testTagKey = "tint",
                        value = adjustments.tint,
                        range = -100f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(tint = v) } }
                    )
                    AdjustSliderItem(
                        label = AppStrings.sliderSaturation(lang),
                        testTagKey = "saturation",
                        value = adjustments.saturation,
                        range = -100f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(saturation = v) } }
                    )
                    AdjustSliderItem(
                        label = AppStrings.sliderVibrance(lang),
                        testTagKey = "vibrance",
                        value = adjustments.vibrance,
                        range = -100f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(vibrance = v) } }
                    )
                }
                AdjustCategory.DETAIL -> {
                    AdjustSliderItem(
                        label = AppStrings.sliderClarity(lang),
                        testTagKey = "clarity",
                        value = adjustments.clarity,
                        range = -100f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(clarity = v) } }
                    )
                    AdjustSliderItem(
                        label = AppStrings.sliderSharpness(lang),
                        testTagKey = "sharpness",
                        value = adjustments.sharpness,
                        range = 0f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(sharpness = v) } }
                    )
                }
                AdjustCategory.EFFECTS -> {
                    AdjustSliderItem(
                        label = AppStrings.sliderSoftGlow(lang),
                        testTagKey = "soft_glow",
                        value = adjustments.softGlow,
                        range = 0f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(softGlow = v) } }
                    )
                    AdjustSliderItem(
                        label = AppStrings.sliderVignette(lang),
                        testTagKey = "vignette",
                        value = adjustments.vignette,
                        range = -100f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(vignette = v) } }
                    )
                    AdjustSliderItem(
                        label = AppStrings.sliderGrain(lang),
                        testTagKey = "film_grain",
                        value = adjustments.grain,
                        range = 0f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(grain = v) } }
                    )
                    AdjustSliderItem(
                        label = AppStrings.sliderFade(lang),
                        testTagKey = "film_fade",
                        value = adjustments.fade,
                        range = 0f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(fade = v) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustSliderItem(
    label: String,
    testTagKey: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    hapticFeedback: Boolean = true,
    onEditFinished: () -> Unit = {},
    onValueChange: (Float) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var lastStep by remember(testTagKey) { mutableIntStateOf(value.toInt()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            maxLines = 1,
            modifier = Modifier.width(86.dp)
        )
        Slider(
            value = value,
            onValueChange = { v ->
                val step = v.toInt()
                if (hapticFeedback && step != lastStep) {
                    lastStep = step
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onValueChange(v)
            },
            onValueChangeFinished = onEditFinished,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = AccentCyan,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("slider_$testTagKey")
        )
        Text(
            text = "${if (value > 0) "+" else ""}${value.toInt()}",
            fontSize = 12.sp,
            color = if (value != 0f) AccentCyan else TextSecondary,
            fontWeight = if (value != 0f) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .width(40.dp)
                .clickable {
                    onValueChange(0f)
                    onEditFinished()
                }
        )
    }
}

// -------------------------------------------------------------
// 4. CURVES PANEL
// -------------------------------------------------------------
@Composable
fun CurvesPanel(
    curves: CurvesState,
    onSelectChannel: (CurveChannel) -> Unit,
    onCurveChanged: (SingleCurve) -> Unit,
    onResetChannel: () -> Unit,
    modifier: Modifier = Modifier,
    histogram: HistogramData? = null,
    lang: AppLanguage = AppLanguage.SYSTEM,
    onEditFinished: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("curves_panel"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Channel Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CurveChannel.values().forEach { ch ->
                    val isSelected = curves.activeChannel == ch
                    val dotColor = when (ch) {
                        CurveChannel.RGB -> Color.White
                        CurveChannel.RED -> Color(0xFFFF5252)
                        CurveChannel.GREEN -> Color(0xFF69F0AE)
                        CurveChannel.BLUE -> Color(0xFF448AFF)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) StudioSurfaceVariant else StudioCard)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) dotColor else StudioBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectChannel(ch) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("curve_channel_${ch.name.lowercase()}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(dotColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ch.label,
                                color = if (isSelected) dotColor else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onResetChannel,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("curves_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset curve channel",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Curves interactive canvas with live histogram underlay
        CurvesCanvas(
            curve = curves.getActiveCurve(),
            channel = curves.activeChannel,
            onCurveChanged = onCurveChanged,
            histogram = histogram,
            onEditFinished = onEditFinished
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = AppStrings.curvesHint(lang),
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

// -------------------------------------------------------------
// 5. HSL PANEL
// -------------------------------------------------------------
@Composable
fun HslPanel(
    hsl: HslState,
    onSelectChannel: (HslColorChannel) -> Unit,
    onAdjustmentChanged: (HslChannelAdjustment) -> Unit,
    onResetChannel: () -> Unit,
    modifier: Modifier = Modifier,
    lang: AppLanguage = AppLanguage.SYSTEM,
    hapticFeedback: Boolean = true,
    onEditFinished: () -> Unit = {}
) {
    val activeAdj = hsl.getAdjustment(hsl.activeChannel)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("hsl_panel")
    ) {
        // Color Swatches Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HslColorChannel.values().forEach { ch ->
                val isSelected = hsl.activeChannel == ch
                val hasAdjustment = !hsl.getAdjustment(ch).isDefault

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectChannel(ch) }
                        .padding(vertical = 4.dp)
                        .testTag("hsl_color_${ch.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(ch.previewColor, CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else StudioBorder,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasAdjustment) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AppStrings.hslChannel(ch, lang),
                        fontSize = 10.sp,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            IconButton(
                onClick = onResetChannel,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("hsl_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset HSL channel",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Sliders for active channel
        AdjustSliderItem(
            label = AppStrings.sliderHue(lang),
            testTagKey = "hsl_hue",
            value = activeAdj.hue,
            range = -100f..100f,
            hapticFeedback = hapticFeedback,
            onEditFinished = onEditFinished,
            onValueChange = { v -> onAdjustmentChanged(activeAdj.copy(hue = v)) }
        )
        AdjustSliderItem(
            label = AppStrings.sliderSaturation(lang),
            testTagKey = "hsl_saturation",
            value = activeAdj.saturation,
            range = -100f..100f,
            hapticFeedback = hapticFeedback,
            onEditFinished = onEditFinished,
            onValueChange = { v -> onAdjustmentChanged(activeAdj.copy(saturation = v)) }
        )
        AdjustSliderItem(
            label = AppStrings.sliderLuminance(lang),
            testTagKey = "hsl_luminance",
            value = activeAdj.luminance,
            range = -100f..100f,
            hapticFeedback = hapticFeedback,
            onEditFinished = onEditFinished,
            onValueChange = { v -> onAdjustmentChanged(activeAdj.copy(luminance = v)) }
        )
    }
}

// -------------------------------------------------------------
// 6. CROP & TRANSFORM PANEL
// -------------------------------------------------------------
@Composable
fun CropPanel(
    onRotate: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    selectedRatio: CropAspectRatio = CropAspectRatio.FREE,
    cropRect: CropRect = CropRect(),
    onSelectRatio: (CropAspectRatio) -> Unit,
    onApplyCrop: () -> Unit = {},
    onResetCrop: () -> Unit = {},
    activeFrame: FrameStyle = FrameStyle.NONE,
    onSelectFrame: (FrameStyle) -> Unit = {},
    modifier: Modifier = Modifier,
    lang: AppLanguage = AppLanguage.SYSTEM
) {
    val canApplyCrop = !cropRect.isFull
    val canResetCrop = !cropRect.isFull || selectedRatio != CropAspectRatio.FREE

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
            .testTag("crop_panel")
    ) {
        // Transform buttons: Rotate, Flip H, Flip V
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onRotate,
                colors = ButtonDefaults.buttonColors(containerColor = StudioCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("rotate_button")
            ) {
                Icon(imageVector = Icons.Default.RotateRight, contentDescription = AppStrings.rotate90(lang), tint = AccentCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.rotate90(lang), color = TextPrimary, fontSize = 12.sp, maxLines = 1, softWrap = false)
            }

            Button(
                onClick = onFlipH,
                colors = ButtonDefaults.buttonColors(containerColor = StudioCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("flip_h_button")
            ) {
                Icon(imageVector = Icons.Default.Flip, contentDescription = AppStrings.flipH(lang), tint = AccentCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.flipH(lang), color = TextPrimary, fontSize = 12.sp, maxLines = 1, softWrap = false)
            }

            Button(
                onClick = onFlipV,
                colors = ButtonDefaults.buttonColors(containerColor = StudioCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("flip_v_button")
            ) {
                Icon(imageVector = Icons.Default.Flip, contentDescription = AppStrings.flipV(lang), tint = AccentCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.flipV(lang), color = TextPrimary, fontSize = 12.sp, maxLines = 1, softWrap = false)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Aspect Ratio Header + Reset / Apply Crop Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppStrings.aspectRatio(lang),
                fontSize = 12.sp,
                color = TextSecondary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canResetCrop) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioCard)
                            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                            .clickable(onClick = onResetCrop)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("reset_crop_button")
                    ) {
                        Text(
                            text = AppStrings.resetCrop(lang),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canApplyCrop) AccentCyan else StudioCard)
                        .border(
                            width = 1.dp,
                            color = if (canApplyCrop) AccentCyan else StudioBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = canApplyCrop, onClick = onApplyCrop)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("apply_crop_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = AppStrings.applyCrop(lang),
                            tint = if (canApplyCrop) Color.Black else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.applyCrop(lang),
                            color = if (canApplyCrop) Color.Black else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Ratio Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CropAspectRatio.values().forEach { r ->
                val isSelected = selectedRatio == r
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentCyan else StudioCard)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AccentCyan else StudioBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectRatio(r) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("crop_ratio_${r.name.lowercase()}")
                ) {
                    Text(
                        text = AppStrings.cropRatioLabel(r, lang),
                        color = if (isSelected) Color.Black else AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = AppStrings.studioFrames(lang),
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Studio Frames Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FrameStyle.values().forEach { frame ->
                val isSelected = activeFrame == frame
                val frameBg = when (frame) {
                    FrameStyle.NONE -> StudioSurfaceVariant
                    FrameStyle.CLASSIC_WHITE -> Color(0xFFF6F6F4)
                    FrameStyle.POLAROID -> Color(0xFFF7F4EC)
                    FrameStyle.CINEMA -> Color(0xFF0E0E12)
                    FrameStyle.OBSIDIAN -> Color(0xFF14151B)
                }
                val frameAccent = when (frame) {
                    FrameStyle.NONE -> StudioBorder
                    FrameStyle.CLASSIC_WHITE -> Color(0xFFCCCCCC)
                    FrameStyle.POLAROID -> Color(0xFFD0C9BA)
                    FrameStyle.CINEMA -> AccentCyan.copy(alpha = 0.7f)
                    FrameStyle.OBSIDIAN -> Color(0xFFD4AF37)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AccentCyan.copy(alpha = 0.16f) else StudioCard)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AccentCyan else StudioBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectFrame(frame) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("frame_style_${frame.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(frameBg)
                            .border(1.5.dp, frameAccent, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = AppStrings.frameLabel(frame, lang),
                        color = if (isSelected) AccentCyan else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

