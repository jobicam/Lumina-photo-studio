package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.RetouchCategory
import com.example.model.RetouchType
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings

fun retouchToolAccent(tool: RetouchType): Color = when (tool.category) {
    RetouchCategory.SKIN -> AccentPink
    RetouchCategory.FEATURES -> AccentCyan
    RetouchCategory.CONTOUR -> if (tool == RetouchType.SCULPT) AccentPurple else AccentAmber
}

fun retouchToolIcon(tool: RetouchType): ImageVector = when (tool) {
    RetouchType.SKIN_SMOOTH -> Icons.Default.Face
    RetouchType.TEETH_WHITEN -> Icons.Default.SentimentSatisfiedAlt
    RetouchType.EYE_POP -> Icons.Default.RemoveRedEye
    RetouchType.SCULPT -> Icons.Default.Gesture
    RetouchType.DETAIL_SHARPEN -> Icons.Default.Tune
    RetouchType.GLOW -> Icons.Default.AutoAwesome
}

@Composable
fun RetouchPanel(
    activeTool: RetouchType,
    brushRadius: Float,
    brushIntensity: Float,
    onSelectTool: (RetouchType) -> Unit,
    onRadiusChanged: (Float) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    isEraser: Boolean = false,
    onToggleEraser: () -> Unit = {},
    texturePreserve: Float = 0.30f,
    onTexturePreserveChanged: (Float) -> Unit = {},
    onAutoSkinSmooth: () -> Unit = {},
    onOpenBackgroundBlur: () -> Unit = {},
    modifier: Modifier = Modifier,
    lang: AppLanguage = AppLanguage.SYSTEM,
    hapticFeedback: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    var lastRadiusInt by remember { mutableIntStateOf(brushRadius.toInt()) }
    var lastIntensityInt by remember { mutableIntStateOf((brushIntensity * 100).toInt()) }
    var lastDetailInt by remember { mutableIntStateOf((texturePreserve * 100).toInt()) }

    val activeAccent by animateColorAsState(
        targetValue = if (isEraser) AccentAmber else retouchToolAccent(activeTool),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "retouch_active_accent"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 5.dp)
            .testTag("retouch_panel")
    ) {
        // 1. Studio Header Bar: Spacious Active Tool / Restore Info + Restore Brush Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                activeAccent.copy(alpha = 0.16f),
                                StudioSurfaceVariant.copy(alpha = 0.70f)
                            )
                        )
                    )
                    .border(1.dp, activeAccent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isEraser) Icons.Default.History else retouchToolIcon(activeTool),
                    contentDescription = null,
                    tint = activeAccent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEraser) AppStrings.restoreBrushSubtitle(lang) else AppStrings.retouchSubtitle(activeTool, lang),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Restore Original (Eraser) Brush Toggle
            val eraserBg by animateColorAsState(
                targetValue = if (isEraser) AccentAmber.copy(alpha = 0.25f) else StudioCard,
                animationSpec = tween(180),
                label = "eraser_bg"
            )
            val eraserBorder by animateColorAsState(
                targetValue = if (isEraser) AccentAmber else StudioBorder,
                animationSpec = tween(180),
                label = "eraser_border"
            )
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(eraserBg)
                    .border(1.dp, eraserBorder, RoundedCornerShape(10.dp))
                    .bounceClickable(pressedScale = 0.93f) {
                        if (hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleEraser()
                    }
                    .padding(horizontal = 10.dp)
                    .testTag("retouch_eraser_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = AppStrings.restoreBrushLabel(lang),
                        tint = if (isEraser) AccentAmber else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = AppStrings.restoreBrushLabel(lang),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEraser) AccentAmber else TextPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // 2. Redesigned Studio Tool Selector Dock
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RetouchType.values()) { tool ->
                val isSelected = !isEraser && tool == activeTool
                val toolAccent = retouchToolAccent(tool)
                val title = AppStrings.retouchTitle(tool, lang)
                val cardInteraction = remember { MutableInteractionSource() }
                val pressScale by rememberPressScale(cardInteraction, pressedScale = 0.92f)
                val selectScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.0f else 0.97f,
                    animationSpec = StudioMotion.PressSpring,
                    label = "tool_card_scale"
                )
                val indicatorWidth by animateDpAsState(
                    targetValue = if (isSelected) 18.dp else 8.dp,
                    animationSpec = StudioMotion.PanelHeightSpring,
                    label = "tool_indicator_width"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) toolAccent else StudioBorder,
                    animationSpec = tween(180),
                    label = "tool_card_border"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val combined = selectScale * pressScale
                            scaleX = combined
                            scaleY = combined
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(
                                    listOf(
                                        toolAccent.copy(alpha = 0.24f),
                                        StudioSurfaceVariant
                                    )
                                )
                            } else {
                                Brush.verticalGradient(listOf(StudioCard, StudioCard))
                            }
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = LocalIndication.current
                        ) {
                            if (hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTool(tool)
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("retouch_tool_${tool.name.lowercase()}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (isSelected) toolAccent.copy(alpha = 0.25f)
                                    else StudioBackground.copy(alpha = 0.7f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = retouchToolIcon(tool),
                                contentDescription = title,
                                tint = if (isSelected) toolAccent else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = title,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .width(indicatorWidth)
                                    .height(2.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) toolAccent else toolAccent.copy(alpha = 0.35f))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // 3. Unified Live Brush Deck (Nib Preview + Quick Size Presets + Sliders)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(StudioSurfaceVariant.copy(alpha = 0.72f), StudioCard)
                    )
                )
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveBrushNibPreview(
                brushRadius = brushRadius,
                brushIntensity = brushIntensity,
                accentColor = activeAccent,
                isEraser = isEraser
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.sizeLabel(lang),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.width(48.dp),
                        maxLines = 1
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
                            thumbColor = activeAccent,
                            activeTrackColor = activeAccent,
                            inactiveTrackColor = StudioBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .testTag("brush_size_slider")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        BrushSizePresetChip("S", 20f, brushRadius, activeAccent, "retouch_size_preset_s") {
                            if (hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRadiusChanged(20f)
                        }
                        BrushSizePresetChip("M", 42f, brushRadius, activeAccent, "retouch_size_preset_m") {
                            if (hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRadiusChanged(42f)
                        }
                        BrushSizePresetChip("L", 66f, brushRadius, activeAccent, "retouch_size_preset_l") {
                            if (hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRadiusChanged(66f)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.intensityLabel(lang),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.width(48.dp),
                        maxLines = 1
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
                            .height(26.dp)
                            .testTag("brush_intensity_slider")
                    )
                    Text(
                        text = "${(brushIntensity * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = AccentAmber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(42.dp)
                    )
                }
            }
        }

        // 4. Context-Aware Tool Controls Strip
        val hasContextControls = isEraser ||
            activeTool == RetouchType.SKIN_SMOOTH ||
            activeTool == RetouchType.SCULPT

        AnimatedVisibility(
            visible = hasContextControls,
            enter = fadeIn(tween(180)) + expandVertically(tween(200)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 5.dp)
            ) {
                when {
                    isEraser -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentAmber.copy(alpha = 0.14f))
                                .border(1.dp, AccentAmber.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.restoreBrushSubtitle(lang),
                                color = TextPrimary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(AccentAmber)
                                    .bounceClickable(pressedScale = 0.92f, onClick = onToggleEraser)
                                    .padding(horizontal = 9.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = AppStrings.done(lang),
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    activeTool == RetouchType.SKIN_SMOOTH -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(StudioCard)
                                .border(1.dp, AccentPink.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppStrings.texturePreserveLabel(lang),
                                fontSize = 11.sp,
                                color = AccentPink,
                                fontWeight = FontWeight.SemiBold,
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
                                    .height(28.dp)
                                    .testTag("texture_preserve_slider")
                            )
                            Text(
                                text = "${(texturePreserve * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = AccentPink,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(38.dp)
                            )
                        }
                    }

                    activeTool == RetouchType.SCULPT -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentPurple.copy(alpha = 0.12f))
                                .border(1.dp, AccentPurple.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gesture,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.sculptDragHint(lang),
                                color = TextPrimary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // 5. Spacious Studio One-Tap Actions Strip (Auto Smooth + Blur Background)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                AccentPink.copy(alpha = 0.22f),
                                AccentPurple.copy(alpha = 0.18f)
                            )
                        )
                    )
                    .border(1.dp, AccentPink.copy(alpha = 0.50f), RoundedCornerShape(10.dp))
                    .bounceClickable(pressedScale = 0.95f, onClick = onAutoSkinSmooth)
                    .padding(horizontal = 10.dp)
                    .testTag("auto_skin_smooth_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = AppStrings.autoSmoothSkin(lang),
                        tint = AccentPink,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = AppStrings.autoSmoothSkin(lang),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentCyan.copy(alpha = 0.14f))
                    .border(1.dp, AccentCyan.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                    .bounceClickable(pressedScale = 0.95f, onClick = onOpenBackgroundBlur)
                    .padding(horizontal = 10.dp)
                    .testTag("quick_blur_bg_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = AppStrings.quickBlurBg(lang),
                        tint = AccentCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = AppStrings.quickBlurBg(lang),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveBrushNibPreview(
    brushRadius: Float,
    brushIntensity: Float,
    accentColor: Color,
    isEraser: Boolean
) {
    val animatedNibSize by animateDpAsState(
        targetValue = (brushRadius * 0.42f).dp.coerceIn(8.dp, 34.dp),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "brush_nib_size"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(StudioBackground)
                .border(1.dp, StudioBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = (animatedNibSize.toPx() / 2f).coerceAtMost(size.minDimension / 2f - 2.dp.toPx())
                drawCircle(
                    color = accentColor.copy(alpha = (0.18f + brushIntensity * 0.42f).coerceIn(0.18f, 0.60f)),
                    radius = r
                )
                drawCircle(
                    color = accentColor,
                    radius = r,
                    style = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = if (isEraser) PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f) else null
                    )
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.8.dp.toPx()
                )
            }
        }
        Text(
            text = "${brushRadius.toInt()}px",
            fontSize = 9.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BrushSizePresetChip(
    label: String,
    targetRadius: Float,
    currentRadius: Float,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    val isActive = kotlin.math.abs(currentRadius - targetRadius) <= 6f
    val chipBg by animateColorAsState(
        targetValue = if (isActive) accentColor.copy(alpha = 0.24f) else StudioBackground.copy(alpha = 0.7f),
        animationSpec = tween(160),
        label = "chip_bg_$label"
    )
    val chipBorder by animateColorAsState(
        targetValue = if (isActive) accentColor else StudioBorder,
        animationSpec = tween(160),
        label = "chip_border_$label"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(chipBg)
            .border(
                width = 1.dp,
                color = chipBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .bounceClickable(pressedScale = 0.88f, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) accentColor else TextSecondary
        )
    }
}
