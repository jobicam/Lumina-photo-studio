package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.ColorGradeState
import com.example.model.ColorGradeTone
import com.example.model.ColorWheelValue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ColorGradePanel(
    colorGrade: ColorGradeState,
    onSelectTone: (ColorGradeTone) -> Unit,
    onWheelChanged: (ColorWheelValue) -> Unit,
    onBalanceChanged: (Float) -> Unit,
    onResetWheel: () -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier,
    lang: AppLanguage = AppLanguage.SYSTEM,
    hapticFeedback: Boolean = true,
    onEditFinished: () -> Unit = {}
) {
    val activeWheel = colorGrade.getActiveWheel()
    val haptics = LocalHapticFeedback.current
    var lastLumStep by remember(colorGrade.activeTone) { mutableIntStateOf(activeWheel.luminance.toInt()) }
    var lastBalStep by remember { mutableIntStateOf(colorGrade.balance.toInt()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("color_grade_panel")
    ) {
        // Tone selector chips + Reset controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorGradeTone.values().forEach { tone ->
                    val isSelected = colorGrade.activeTone == tone
                    val wheel = colorGrade.getWheel(tone)
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) AccentCyan else StudioCard,
                        animationSpec = tween(160),
                        label = "grade_tone_bg_${tone.name}"
                    )
                    val chipText by animateColorAsState(
                        targetValue = if (isSelected) Color.Black else TextPrimary,
                        animationSpec = tween(160),
                        label = "grade_tone_text_${tone.name}"
                    )
                    val swatchColor = if (wheel.saturation > 0.02f) {
                        Color(
                            AndroidColor.HSVToColor(
                                floatArrayOf(wheel.hueDegrees, wheel.saturation.coerceIn(0.25f, 1f), 1f)
                            )
                        )
                    } else {
                        null
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(chipBg)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) AccentCyan else StudioBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .bounceClickable(pressedScale = 0.94f) { onSelectTone(tone) }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                            .testTag("grade_tone_${tone.name.lowercase()}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (swatchColor != null) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = AppStrings.colorGradeToneLabel(tone, lang),
                                color = chipText,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioSurfaceVariant)
                    .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                    .bounceClickable(pressedScale = 0.92f, onClick = onResetWheel)
                    .padding(horizontal = 9.dp, vertical = 6.dp)
                    .testTag("reset_color_wheel_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = AppStrings.resetWheelLabel(lang),
                        tint = AccentCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = AppStrings.resetWheelLabel(lang),
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Color Wheel + Tonal Sliders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TonalColorWheel(
                wheelValue = activeWheel,
                onWheelChanged = onWheelChanged,
                onEditFinished = onEditFinished,
                modifier = Modifier
                    .size(156.dp)
                    .testTag("color_grade_wheel_canvas")
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = AppStrings.sliderLuminance(lang),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${if (activeWheel.luminance > 0) "+" else ""}${activeWheel.luminance.toInt()}",
                        fontSize = 11.sp,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = activeWheel.luminance,
                    onValueChange = { v ->
                        val step = v.toInt()
                        if (hapticFeedback && step != lastLumStep) {
                            lastLumStep = step
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onWheelChanged(activeWheel.copy(luminance = v))
                    },
                    onValueChangeFinished = onEditFinished,
                    valueRange = -100f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = StudioBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("slider_grade_luminance")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = AppStrings.colorGradeBalance(lang),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${if (colorGrade.balance > 0) "+" else ""}${colorGrade.balance.toInt()}",
                        fontSize = 11.sp,
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = colorGrade.balance,
                    onValueChange = { v ->
                        val step = v.toInt()
                        if (hapticFeedback && step != lastBalStep) {
                            lastBalStep = step
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onBalanceChanged(v)
                    },
                    onValueChangeFinished = onEditFinished,
                    valueRange = -100f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentCyan,
                        activeTrackColor = AccentCyan,
                        inactiveTrackColor = StudioBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("slider_grade_balance")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.colorGradeToneLabel(colorGrade.activeTone, lang),
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    if (!colorGrade.isDefault) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioCard)
                                .bounceClickable(pressedScale = 0.94f, onClick = onResetAll)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("reset_all_grade_button")
                        ) {
                            Text(
                                text = AppStrings.resetAllGradeLabel(lang),
                                color = AccentCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = AppStrings.colorGradeWheelHint(lang),
            fontSize = 10.sp,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun TonalColorWheel(
    wheelValue: ColorWheelValue,
    onWheelChanged: (ColorWheelValue) -> Unit,
    onEditFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColors = remember {
        listOf(
            Color(0xFFFF3B30), // 0° Red
            Color(0xFFFFCC00), // 60° Yellow
            Color(0xFF34C759), // 120° Green
            Color(0xFF00C7BE), // 180° Cyan
            Color(0xFF007AFF), // 240° Blue
            Color(0xFFFF2D55), // 300° Magenta
            Color(0xFFFF3B30)  // 360° Red
        )
    }

    fun updateFromTouch(pos: Offset, width: Float, height: Float) {
        val cx = width / 2f
        val cy = height / 2f
        val maxR = (min(width, height) / 2f) - 10f
        if (maxR <= 1f) return
        val dx = pos.x - cx
        val dy = pos.y - cy
        val dist = hypot(dx, dy)
        val sat = (dist / maxR).coerceIn(0f, 1f)
        var angleDeg = (atan2(dy, dx) * (180f / PI.toFloat()))
        if (angleDeg < 0f) angleDeg += 360f
        onWheelChanged(
            wheelValue.copy(
                hueDegrees = angleDeg,
                saturation = if (sat < 0.04f) 0f else sat
            )
        )
    }

    Box(
        modifier = modifier
            .pointerInput(wheelValue) {
                detectTapGestures(
                    onDoubleTap = {
                        onWheelChanged(wheelValue.copy(hueDegrees = 0f, saturation = 0f))
                        onEditFinished()
                    },
                    onTap = { pos ->
                        updateFromTouch(pos, size.width.toFloat(), size.height.toFloat())
                        onEditFinished()
                    }
                )
            }
            .pointerInput(wheelValue) {
                detectDragGestures(
                    onDragStart = { start ->
                        updateFromTouch(start, size.width.toFloat(), size.height.toFloat())
                    },
                    onDragEnd = { onEditFinished() },
                    onDragCancel = { onEditFinished() }
                ) { change, _ ->
                    change.consume()
                    updateFromTouch(change.position, size.width.toFloat(), size.height.toFloat())
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (min(size.width, size.height) / 2f) - 8.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)

            // Sweep rainbow hue wheel
            drawCircle(
                brush = Brush.sweepGradient(colors = hueColors, center = center),
                radius = radius,
                center = center
            )
            // White center saturation fade
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1F222A), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            // Subtle outer rim & center crosshair
            drawCircle(
                color = Color.White.copy(alpha = 0.28f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = 3.dp.toPx(),
                center = center
            )

            // Active handle puck
            val rad = wheelValue.hueDegrees * (PI.toFloat() / 180f)
            val puckR = wheelValue.saturation.coerceIn(0f, 1f) * radius
            val puckCenter = Offset(
                x = center.x + cos(rad) * puckR,
                y = center.y + sin(rad) * puckR
            )
            val puckColor = if (wheelValue.saturation > 0.02f) {
                Color(AndroidColor.HSVToColor(floatArrayOf(wheelValue.hueDegrees, wheelValue.saturation, 1f)))
            } else {
                Color.White
            }

            drawLine(
                color = Color.White.copy(alpha = 0.55f),
                start = center,
                end = puckCenter,
                strokeWidth = 1.5.dp.toPx()
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = 10.dp.toPx(),
                center = puckCenter
            )
            drawCircle(
                color = puckColor,
                radius = 8.dp.toPx(),
                center = puckCenter
            )
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = puckCenter,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
