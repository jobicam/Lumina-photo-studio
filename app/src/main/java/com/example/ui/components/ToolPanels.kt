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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
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
                    color = borderColor,
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
                    AdjustSliderItem(
                        label = AppStrings.sliderBackgroundBlur(lang),
                        testTagKey = "background_blur",
                        value = adjustments.backgroundBlur,
                        range = 0f..100f,
                        hapticFeedback = hapticFeedback,
                        onEditFinished = onEditFinished,
                        onValueChange = { v -> onAdjustmentsChanged { it.copy(backgroundBlur = v) } }
                    )
                    AnimatedVisibility(
                        visible = adjustments.backgroundBlur > 0f,
                        enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                        exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AdjustSliderItem(
                                label = AppStrings.sliderFocusSize(lang),
                                testTagKey = "blur_focus_size",
                                value = (adjustments.blurFocusRadius * 100f).coerceIn(15f, 70f),
                                range = 15f..70f,
                                defaultValue = 32f,
                                hapticFeedback = hapticFeedback,
                                onEditFinished = onEditFinished,
                                onValueChange = { v ->
                                    onAdjustmentsChanged {
                                        it.copy(blurFocusRadius = (v / 100f).coerceIn(0.15f, 0.70f))
                                    }
                                }
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val protectActive = adjustments.blurProtectSubject
                                val protectBg by animateColorAsState(
                                    targetValue = if (protectActive) AccentCyan.copy(alpha = 0.2f) else StudioCard,
                                    animationSpec = tween(160),
                                    label = "protect_subject_bg"
                                )
                                val protectBorder by animateColorAsState(
                                    targetValue = if (protectActive) AccentCyan else StudioBorder,
                                    animationSpec = tween(160),
                                    label = "protect_subject_border"
                                )
                                Box(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(protectBg)
                                        .border(
                                            1.dp,
                                            protectBorder,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .bounceClickable(pressedScale = 0.94f) {
                                            onAdjustmentsChanged { it.copy(blurProtectSubject = !it.blurProtectSubject) }
                                            onEditFinished()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                        .testTag("blur_protect_subject_toggle")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = AppStrings.protectSubjectLabel(lang),
                                            tint = if (protectActive) AccentCyan else TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = AppStrings.protectSubjectLabel(lang),
                                            color = if (protectActive) AccentCyan else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (protectActive) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(StudioSurfaceVariant)
                                        .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                                        .bounceClickable(pressedScale = 0.94f) {
                                            onAdjustmentsChanged {
                                                it.copy(
                                                    blurFocusX = 0.50f,
                                                    blurFocusY = 0.42f,
                                                    blurFocusRadius = 0.32f
                                                )
                                            }
                                            onEditFinished()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                        .testTag("blur_reset_focus_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CenterFocusStrong,
                                            contentDescription = AppStrings.blurFocusHint(lang),
                                            tint = TextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = AppStrings.blurFocusHint(lang),
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
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
            text = "${if (value > 0 && range.start < 0f) "+" else ""}${value.toInt()}",
            fontSize = 12.sp,
            color = if (value != defaultValue) AccentCyan else TextSecondary,
            fontWeight = if (value != defaultValue) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .width(40.dp)
                .clickable {
                    onValueChange(defaultValue)
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
            val rotateInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onRotate,
                interactionSource = rotateInteraction,
                colors = ButtonDefaults.buttonColors(containerColor = StudioCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .pressBounce(rotateInteraction, pressedScale = 0.93f)
                    .testTag("rotate_button")
            ) {
                Icon(imageVector = Icons.Default.RotateRight, contentDescription = AppStrings.rotate90(lang), tint = AccentCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.rotate90(lang), color = TextPrimary, fontSize = 12.sp, maxLines = 1, softWrap = false)
            }

            val flipHInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onFlipH,
                interactionSource = flipHInteraction,
                colors = ButtonDefaults.buttonColors(containerColor = StudioCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .pressBounce(flipHInteraction, pressedScale = 0.93f)
                    .testTag("flip_h_button")
            ) {
                Icon(imageVector = Icons.Default.Flip, contentDescription = AppStrings.flipH(lang), tint = AccentCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.flipH(lang), color = TextPrimary, fontSize = 12.sp, maxLines = 1, softWrap = false)
            }

            val flipVInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onFlipV,
                interactionSource = flipVInteraction,
                colors = ButtonDefaults.buttonColors(containerColor = StudioCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .pressBounce(flipVInteraction, pressedScale = 0.93f)
                    .testTag("flip_v_button")
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

