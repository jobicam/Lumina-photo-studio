package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdjustCategory
import com.example.model.AdjustmentState
import com.example.model.AppLanguage
import com.example.model.CropAspectRatio
import com.example.model.CropGridStyle
import com.example.model.CropRect
import com.example.model.CurveChannel
import com.example.model.CurvesState
import com.example.model.CustomPreset
import com.example.model.FilterBrowserMode
import com.example.model.FilterCategory
import com.example.model.FilterPreset
import com.example.model.FilterPresets
import com.example.model.FrameStyle
import com.example.model.HistogramData
import com.example.model.HslChannelAdjustment
import com.example.model.HslColorChannel
import com.example.model.HslState
import com.example.model.Lut3D
import com.example.model.SingleCurve
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings

// -------------------------------------------------------------
// 1. FILTERS, PRESETS & 3D LUTS PANEL
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
    onEditFinished: () -> Unit = {},
    browserMode: FilterBrowserMode = FilterBrowserMode.FILTERS,
    onSelectBrowserMode: (FilterBrowserMode) -> Unit = {},
    customPresets: List<CustomPreset> = emptyList(),
    availableLuts: List<Lut3D> = emptyList(),
    activeLut: Lut3D? = null,
    lutIntensity: Float = 1.0f,
    onSelectLut: (Lut3D?) -> Unit = {},
    onLutIntensityChanged: (Float) -> Unit = {},
    onApplyCustomPreset: (CustomPreset) -> Unit = {},
    onShareCustomPreset: (CustomPreset) -> Unit = {},
    onDeleteCustomPreset: (String) -> Unit = {},
    onShareLut: (Lut3D) -> Unit = {},
    onDeleteLut: (String) -> Unit = {},
    onSavePresetClick: () -> Unit = {},
    onImportPresetOrLutClick: () -> Unit = {}
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
            .padding(vertical = 6.dp)
            .testTag("filters_panel")
    ) {
        FilterBrowserModeHeader(
            mode = browserMode,
            onSelectMode = onSelectBrowserMode,
            onSavePresetClick = onSavePresetClick,
            onImportClick = onImportPresetOrLutClick,
            lang = lang
        )

        Spacer(modifier = Modifier.height(4.dp))

        when (browserMode) {
            FilterBrowserMode.PRESETS -> {
                CustomPresetsBrowser(
                    presets = customPresets,
                    onApplyPreset = onApplyCustomPreset,
                    onSharePreset = onShareCustomPreset,
                    onDeletePreset = onDeleteCustomPreset,
                    onSaveNewClick = onSavePresetClick,
                    lang = lang
                )
            }
            FilterBrowserMode.LUTS -> {
                Lut3DBrowser(
                    luts = availableLuts,
                    activeLut = activeLut,
                    lutIntensity = lutIntensity,
                    onSelectLut = onSelectLut,
                    onIntensityChanged = onLutIntensityChanged,
                    onShareLut = onShareLut,
                    onDeleteLut = onDeleteLut,
                    lang = lang,
                    hapticFeedback = hapticFeedback,
                    onEditFinished = onEditFinished
                )
            }
            FilterBrowserMode.FILTERS -> {
        // Intensity Slider (when a non-original filter is selected)
        AnimatedVisibility(
            visible = activePreset != FilterPresets.NONE,
            enter = fadeIn(tween(180)) + expandVertically(tween(200)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
        ) {
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
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) AccentCyan else StudioCard,
                    animationSpec = tween(160),
                    label = "filter_cat_bg_${cat.name}"
                )
                val chipText by animateColorAsState(
                    targetValue = if (isSelected) Color.Black else TextPrimary,
                    animationSpec = tween(160),
                    label = "filter_cat_text_${cat.name}"
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(chipBg)
                        .bounceClickable(pressedScale = 0.93f) { selectedCategory = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("filter_cat_${cat.name.lowercase()}")
                ) {
                    Text(
                        text = AppStrings.filterCategory(cat, lang),
                        color = chipText,
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
    }
}

@Composable
private fun FilterItemCard(
    preset: FilterPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cardInteraction = remember { MutableInteractionSource() }
    val pressScale by rememberPressScale(cardInteraction, pressedScale = 0.92f)
    val selectScale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1.0f,
        animationSpec = StudioMotion.PressSpring,
        label = "filter_card_scale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AccentCyan else StudioBorder,
        animationSpec = tween(180),
        label = "filter_card_border"
    )
    val swatchColors = remember(preset.id, preset.category) {
        when {
            preset.id == "none" -> listOf(Color(0xFF3E4756), Color(0xFF232834))
            preset.isSepia -> listOf(Color(0xFFC28E5C), Color(0xFF5D3410))
            preset.isGrayscale && preset.id == "high_contrast" -> listOf(Color(0xFFE0E0E0), Color(0xFF141414))
            preset.isGrayscale -> listOf(Color(0xFF8E939D), Color(0xFF2B2E35))
            preset.category == FilterCategory.PORTRAIT -> listOf(Color(0xFFF3B59B), Color(0xFF9C5E53))
            preset.category == FilterCategory.CINEMATIC -> listOf(Color(0xFF00B4D8), Color(0xFF14284B))
            preset.category == FilterCategory.VINTAGE -> listOf(Color(0xFFD7A86E), Color(0xFF6B4226))
            preset.category == FilterCategory.BLACK_WHITE -> listOf(Color(0xFFA6A6A6), Color(0xFF1E1E1E))
            preset.category == FilterCategory.AESTHETIC -> listOf(Color(0xFFC77DFF), Color(0xFF4C2A85))
            preset.category == FilterCategory.LANDSCAPE -> listOf(Color(0xFF4CAF50), Color(0xFF14452F))
            else -> listOf(Color(0xFF546E7A), Color(0xFF263238))
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .graphicsLayer {
                val combined = selectScale * pressScale
                scaleX = combined
                scaleY = combined
            }
            .clickable(
                interactionSource = cardInteraction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .testTag("filter_preset_${preset.id}")
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(swatchColors))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = preset.name.take(3).uppercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.6f),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
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

// 2. RETOUCH PANEL moved to RetouchPanel.kt

// -------------------------------------------------------------
// 3. ADJUST PANEL
// -------------------------------------------------------------
@Composable
fun AdjustPanel(
    adjustments: AdjustmentState,
    activeCategory: AdjustCategory = AdjustCategory.LIGHT,
    onSelectCategory: (AdjustCategory) -> Unit = {},
    onAdjustmentsChanged: ((AdjustmentState) -> AdjustmentState) -> Unit,
    onReset: () -> Unit,
    onAutoEnhance: () -> Unit = {},
    modifier: Modifier = Modifier,
    lang: AppLanguage = AppLanguage.SYSTEM,
    hapticFeedback: Boolean = true,
    onEditFinished: () -> Unit = {}
) {
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
                    val catBg by animateColorAsState(
                        targetValue = if (isSelected) AccentCyan else StudioCard,
                        animationSpec = tween(160),
                        label = "adjust_cat_bg_${cat.name}"
                    )
                    val catText by animateColorAsState(
                        targetValue = if (isSelected) Color.Black else TextPrimary,
                        animationSpec = tween(160),
                        label = "adjust_cat_text_${cat.name}"
                    )
                    val catLabel = when (cat) {
                        AdjustCategory.LIGHT -> AppStrings.catLight(lang)
                        AdjustCategory.COLOR -> AppStrings.catColor(lang)
                        AdjustCategory.DETAIL -> AppStrings.catDetail(lang)
                        AdjustCategory.EFFECTS -> AppStrings.catEffects(lang)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(catBg)
                            .bounceClickable(pressedScale = 0.93f) { onSelectCategory(cat) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("adjust_cat_${cat.name.lowercase()}")
                    ) {
                        Text(
                            text = catLabel,
                            color = catText,
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
                        .bounceClickable(pressedScale = 0.93f, onClick = onAutoEnhance)
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

                val resetInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = onReset,
                    interactionSource = resetInteraction,
                    modifier = Modifier
                        .pressBounce(resetInteraction, pressedScale = 0.88f)
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

        // Sliders based on category with smooth category transition & vertical scrolling
        AnimatedContent(
            targetState = activeCategory,
            transitionSpec = {
                val toRight = targetState.ordinal > initialState.ordinal
                val slideIn = slideInHorizontally(
                    animationSpec = StudioMotion.SlideSpring
                ) { w -> if (toRight) w / 10 else -w / 10 }
                val slideOut = slideOutHorizontally(
                    animationSpec = tween(130)
                ) { w -> if (toRight) -w / 12 else w / 12 }
                (fadeIn(tween(180)) + slideIn) togetherWith (fadeOut(tween(130)) + slideOut)
            },
            label = "adjust_category_content"
        ) { category ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                when (category) {
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
                    DepthLensBlurSection(
                        adjustments = adjustments,
                        lang = lang,
                        hapticFeedback = hapticFeedback,
                        onAdjustmentsChanged = onAdjustmentsChanged,
                        onEditFinished = onEditFinished
                    )
                    MotionBlurSection(
                        adjustments = adjustments,
                        lang = lang,
                        hapticFeedback = hapticFeedback,
                        onAdjustmentsChanged = onAdjustmentsChanged,
                        onEditFinished = onEditFinished
                    )
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
}

@Composable
private fun AdjustSliderItem(
    label: String,
    testTagKey: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    defaultValue: Float = 0f,
    hapticFeedback: Boolean = true,
    onEditFinished: () -> Unit = {},
    onValueChange: (Float) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var lastStep by remember(testTagKey) { mutableIntStateOf(value.toInt()) }
    val isModified = kotlin.math.abs(value - defaultValue) >= 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.width(88.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isModified) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(AccentCyan, CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                color = if (isModified) TextPrimary else TextSecondary,
                fontWeight = if (isModified) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1
            )
        }
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
        Box(
            modifier = Modifier
                .width(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isModified) AccentCyan.copy(alpha = 0.14f) else Color.Transparent)
                .clickable(enabled = isModified) {
                    onValueChange(defaultValue)
                    onEditFinished()
                }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "${if (value > 0 && range.start < 0f) "+" else ""}${value.toInt()}",
                fontSize = 12.sp,
                color = if (isModified) AccentCyan else TextSecondary,
                fontWeight = if (isModified) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
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
                    val tabBg by animateColorAsState(
                        targetValue = if (isSelected) StudioSurfaceVariant else StudioCard,
                        animationSpec = tween(160),
                        label = "curve_bg_${ch.name}"
                    )
                    val tabBorder by animateColorAsState(
                        targetValue = if (isSelected) dotColor else StudioBorder,
                        animationSpec = tween(160),
                        label = "curve_border_${ch.name}"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(tabBg)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = tabBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .bounceClickable(pressedScale = 0.93f) { onSelectChannel(ch) }
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

            val curvesResetInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onResetChannel,
                interactionSource = curvesResetInteraction,
                modifier = Modifier
                    .pressBounce(curvesResetInteraction, pressedScale = 0.88f)
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
                val swatchInteraction = remember { MutableInteractionSource() }
                val pressScale by rememberPressScale(swatchInteraction, pressedScale = 0.88f)
                val selectScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.10f else 1.0f,
                    animationSpec = StudioMotion.PressSpring,
                    label = "hsl_swatch_scale_${ch.name}"
                )
                val swatchBorder by animateColorAsState(
                    targetValue = if (isSelected) Color.White else StudioBorder,
                    animationSpec = tween(160),
                    label = "hsl_swatch_border_${ch.name}"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            val s = selectScale * pressScale
                            scaleX = s
                            scaleY = s
                        }
                        .clickable(
                            interactionSource = swatchInteraction,
                            indication = LocalIndication.current
                        ) { onSelectChannel(ch) }
                        .padding(vertical = 4.dp)
                        .testTag("hsl_color_${ch.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(ch.previewColor, CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = swatchBorder,
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

            val hslResetInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onResetChannel,
                interactionSource = hslResetInteraction,
                modifier = Modifier
                    .pressBounce(hslResetInteraction, pressedScale = 0.88f)
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
    onRotateLeft: () -> Unit = {},
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    selectedRatio: CropAspectRatio = CropAspectRatio.FREE,
    isRatioInverted: Boolean = false,
    cropRect: CropRect = CropRect(),
    straightenDegrees: Float = 0f,
    cropGridStyle: CropGridStyle = CropGridStyle.RULE_OF_THIRDS,
    onSelectRatio: (CropAspectRatio) -> Unit,
    onSwapRatioOrientation: () -> Unit = {},
    onStraightenChanged: (Float) -> Unit = {},
    onCycleGridStyle: () -> Unit = {},
    onApplyCrop: () -> Unit = {},
    onResetCrop: () -> Unit = {},
    activeFrame: FrameStyle = FrameStyle.NONE,
    onSelectFrame: (FrameStyle) -> Unit = {},
    modifier: Modifier = Modifier,
    lang: AppLanguage = AppLanguage.SYSTEM,
    hapticFeedback: Boolean = true
) {
    val hasStraighten = kotlin.math.abs(straightenDegrees) >= 0.1f
    val canApplyCrop = !cropRect.isFull || hasStraighten
    val canResetCrop = !cropRect.isFull || hasStraighten || selectedRatio != CropAspectRatio.FREE || isRatioInverted
    val canSwapOrientation = selectedRatio != CropAspectRatio.FREE && selectedRatio != CropAspectRatio.SQUARE
    val haptic = LocalHapticFeedback.current
    var lastHapticHalfDeg by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
            .testTag("crop_panel")
    ) {
        // Quick Geometry Row: -90°, +90°, Flip H, Flip V
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CropGeometryActionChip(
                icon = Icons.AutoMirrored.Filled.RotateLeft,
                label = AppStrings.rotateLeft90(lang),
                onClick = onRotateLeft,
                modifier = Modifier
                    .weight(1f)
                    .testTag("rotate_left_button")
            )
            CropGeometryActionChip(
                icon = Icons.AutoMirrored.Filled.RotateRight,
                label = AppStrings.rotateRight90(lang),
                onClick = onRotate,
                modifier = Modifier
                    .weight(1f)
                    .testTag("rotate_button")
            )
            CropGeometryActionChip(
                icon = Icons.Default.Flip,
                label = AppStrings.flipH(lang),
                onClick = onFlipH,
                modifier = Modifier
                    .weight(1f)
                    .testTag("flip_h_button")
            )
            CropGeometryActionChip(
                icon = Icons.Default.Flip,
                label = AppStrings.flipV(lang),
                onClick = onFlipV,
                modifier = Modifier
                    .weight(1f)
                    .testTag("flip_v_button")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fine Straighten Slider (-45° to +45°) with tap-to-reset readout pill
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = if (hasStraighten) AccentCyan else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = AppStrings.straightenLabel(lang),
                        fontSize = 12.sp,
                        color = if (hasStraighten) TextPrimary else TextSecondary,
                        fontWeight = if (hasStraighten) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                val angleText = if (hasStraighten) {
                    val sign = if (straightenDegrees > 0f) "+" else ""
                    "$sign${String.format(java.util.Locale.US, "%.1f", straightenDegrees)}°"
                } else {
                    "0.0°"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (hasStraighten) AccentCyan.copy(alpha = 0.16f) else StudioCard)
                        .border(
                            1.dp,
                            if (hasStraighten) AccentCyan.copy(alpha = 0.55f) else StudioBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .bounceClickable(enabled = hasStraighten, pressedScale = 0.90f) {
                            onStraightenChanged(0f)
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("straighten_value_chip")
                ) {
                    Text(
                        text = angleText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasStraighten) AccentCyan else TextSecondary
                    )
                }
            }

            Slider(
                value = straightenDegrees,
                onValueChange = { next ->
                    val halfStep = kotlin.math.round(next * 2f).toInt()
                    if (hapticFeedback && halfStep != lastHapticHalfDeg) {
                        lastHapticHalfDeg = halfStep
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onStraightenChanged(next)
                },
                valueRange = -45f..45f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentCyan,
                    activeTrackColor = AccentCyan,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .testTag("straighten_slider")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Aspect Ratio Header + Swap Orientation + Grid Toggle + Reset / Apply Crop Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = AppStrings.aspectRatio(lang),
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // Grid overlay style cycle chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioCard)
                        .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                        .bounceClickable(pressedScale = 0.92f, onClick = onCycleGridStyle)
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("crop_grid_cycle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = AppStrings.cropGridLabel(lang),
                        tint = if (cropGridStyle != CropGridStyle.NONE) AccentCyan else TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = AppStrings.cropGridStyleLabel(cropGridStyle, lang),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (cropGridStyle != CropGridStyle.NONE) TextPrimary else TextSecondary
                    )
                }

                // Portrait / Landscape orientation swap chip
                AnimatedVisibility(
                    visible = canSwapOrientation,
                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.85f),
                    exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.85f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isRatioInverted) AccentCyan.copy(alpha = 0.16f) else StudioCard)
                            .border(
                                1.dp,
                                if (isRatioInverted) AccentCyan else StudioBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .bounceClickable(pressedScale = 0.92f, onClick = onSwapRatioOrientation)
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("swap_crop_orientation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = AppStrings.swapOrientationLabel(lang),
                            tint = AccentCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = AppStrings.swapOrientationLabel(lang),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentCyan
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = canResetCrop,
                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.85f),
                    exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.85f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioCard)
                            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                            .bounceClickable(pressedScale = 0.92f, onClick = onResetCrop)
                            .padding(horizontal = 9.dp, vertical = 5.dp)
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

                val applyBg by animateColorAsState(
                    targetValue = if (canApplyCrop) AccentCyan else StudioCard,
                    animationSpec = tween(180),
                    label = "apply_crop_bg"
                )
                val applyBorder by animateColorAsState(
                    targetValue = if (canApplyCrop) AccentCyan else StudioBorder,
                    animationSpec = tween(180),
                    label = "apply_crop_border"
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(applyBg)
                        .border(
                            width = 1.dp,
                            color = applyBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .bounceClickable(enabled = canApplyCrop, pressedScale = 0.93f, onClick = onApplyCrop)
                        .padding(horizontal = 11.dp, vertical = 5.dp)
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
                val ratioBg by animateColorAsState(
                    targetValue = if (isSelected) AccentCyan else StudioCard,
                    animationSpec = tween(160),
                    label = "ratio_bg_${r.name}"
                )
                val ratioBorder by animateColorAsState(
                    targetValue = if (isSelected) AccentCyan else StudioBorder,
                    animationSpec = tween(160),
                    label = "ratio_border_${r.name}"
                )
                val baseLabel = AppStrings.cropRatioLabel(r, lang)
                val chipLabel = if (isSelected && isRatioInverted && baseLabel.contains(':')) {
                    baseLabel.split(':').reversed().joinToString(":")
                } else {
                    baseLabel
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ratioBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = ratioBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .bounceClickable(pressedScale = 0.93f) { onSelectRatio(r) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("crop_ratio_${r.name.lowercase()}")
                ) {
                    Text(
                        text = chipLabel,
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
                val frameChipBg by animateColorAsState(
                    targetValue = if (isSelected) AccentCyan.copy(alpha = 0.16f) else StudioCard,
                    animationSpec = tween(160),
                    label = "frame_bg_${frame.name}"
                )
                val frameChipBorder by animateColorAsState(
                    targetValue = if (isSelected) AccentCyan else StudioBorder,
                    animationSpec = tween(160),
                    label = "frame_border_${frame.name}"
                )
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
                        .background(frameChipBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = frameChipBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .bounceClickable(pressedScale = 0.94f) { onSelectFrame(frame) }
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

@Composable
private fun CropGeometryActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(StudioCard)
            .border(1.dp, StudioBorder, RoundedCornerShape(11.dp))
            .bounceClickable(pressedScale = 0.93f, onClick = onClick)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AccentCyan,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

