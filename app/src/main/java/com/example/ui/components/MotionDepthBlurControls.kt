package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdjustmentState
import com.example.model.AppLanguage
import com.example.model.MotionBlurTarget
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings

@Composable
fun DepthLensBlurSection(
    adjustments: AdjustmentState,
    lang: AppLanguage,
    hapticFeedback: Boolean,
    onAdjustmentsChanged: ((AdjustmentState) -> AdjustmentState) -> Unit,
    onEditFinished: () -> Unit
) {
    Text(
        text = AppStrings.depthLensBlurSection(lang).uppercase(),
        color = AccentCyan,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
    )

    EffectSliderRow(
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
            EffectSliderRow(
                label = AppStrings.sliderBokehBloom(lang),
                testTagKey = "lens_bokeh_bloom",
                value = adjustments.lensBokehBloom,
                range = 0f..100f,
                hapticFeedback = hapticFeedback,
                onEditFinished = onEditFinished,
                onValueChange = { v -> onAdjustmentsChanged { it.copy(lensBokehBloom = v) } }
            )
            EffectSliderRow(
                label = AppStrings.sliderFocusSize(lang),
                testTagKey = "blur_focus_size",
                value = (adjustments.blurFocusRadius * 100f).coerceIn(15f, 70f),
                range = 15f..70f,
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
                        .border(1.dp, protectBorder, RoundedCornerShape(14.dp))
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
}

@Composable
fun MotionBlurSection(
    adjustments: AdjustmentState,
    lang: AppLanguage,
    hapticFeedback: Boolean,
    onAdjustmentsChanged: ((AdjustmentState) -> AdjustmentState) -> Unit,
    onEditFinished: () -> Unit
) {
    Text(
        text = AppStrings.motionBlurSection(lang).uppercase(),
        color = AccentAmber,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp)
    )

    // Target selector: Scene / Background / Subject
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MotionBlurTarget.values().forEach { target ->
            val isSelected = adjustments.motionBlurTarget == target
            val bg by animateColorAsState(
                targetValue = if (isSelected) AccentAmber else StudioCard,
                animationSpec = tween(160),
                label = "motion_target_bg_${target.name}"
            )
            val txt by animateColorAsState(
                targetValue = if (isSelected) Color.Black else TextPrimary,
                animationSpec = tween(160),
                label = "motion_target_txt_${target.name}"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) AccentAmber else StudioBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .bounceClickable(pressedScale = 0.94f) {
                        onAdjustmentsChanged {
                            val nextStrength = if (it.motionBlurIntensity <= 0f) 40f else it.motionBlurIntensity
                            it.copy(motionBlurTarget = target, motionBlurIntensity = nextStrength)
                        }
                        onEditFinished()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("motion_target_${target.name.lowercase()}")
            ) {
                Text(
                    text = AppStrings.motionTargetLabel(target, lang),
                    color = txt,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }

    EffectSliderRow(
        label = AppStrings.sliderMotionTrail(lang),
        testTagKey = "motion_blur_intensity",
        value = adjustments.motionBlurIntensity,
        range = 0f..100f,
        hapticFeedback = hapticFeedback,
        onEditFinished = onEditFinished,
        onValueChange = { v -> onAdjustmentsChanged { it.copy(motionBlurIntensity = v) } }
    )

    AnimatedVisibility(
        visible = adjustments.motionBlurIntensity > 0f,
        enter = fadeIn(tween(180)) + expandVertically(tween(200)),
        exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(86.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = AppStrings.sliderMotionAngle(lang),
                    tint = AccentAmber,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer { rotationZ = adjustments.motionBlurAngle }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = AppStrings.sliderMotionAngle(lang),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            val haptics = LocalHapticFeedback.current
            var lastAngleStep by remember { mutableIntStateOf(adjustments.motionBlurAngle.toInt()) }
            Slider(
                value = adjustments.motionBlurAngle,
                onValueChange = { angle ->
                    val step = (angle / 5f).toInt()
                    if (hapticFeedback && step != lastAngleStep) {
                        lastAngleStep = step
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onAdjustmentsChanged { it.copy(motionBlurAngle = angle) }
                },
                onValueChangeFinished = onEditFinished,
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentAmber,
                    activeTrackColor = AccentAmber,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("slider_motion_blur_angle")
            )
            Text(
                text = "${adjustments.motionBlurAngle.toInt()}°",
                fontSize = 12.sp,
                color = AccentAmber,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(44.dp)
            )
        }
    }
}

@Composable
private fun EffectSliderRow(
    label: String,
    testTagKey: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    hapticFeedback: Boolean,
    onEditFinished: () -> Unit,
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
            text = "${value.toInt()}",
            fontSize = 12.sp,
            color = AccentCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(44.dp)
        )
    }
}
