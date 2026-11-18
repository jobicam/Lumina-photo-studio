package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.CustomPreset
import com.example.model.FilterBrowserMode
import com.example.model.Lut3D
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings

@Composable
fun FilterBrowserModeHeader(
    mode: FilterBrowserMode,
    onSelectMode: (FilterBrowserMode) -> Unit,
    onSavePresetClick: () -> Unit,
    onImportClick: () -> Unit,
    lang: AppLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterBrowserMode.values().forEach { tab ->
            val isSelected = mode == tab
            val bg by animateColorAsState(
                targetValue = if (isSelected) AccentCyan else StudioSurfaceVariant,
                animationSpec = tween(160),
                label = "browser_mode_bg_${tab.name}"
            )
            val txt by animateColorAsState(
                targetValue = if (isSelected) Color.Black else TextPrimary,
                animationSpec = tween(160),
                label = "browser_mode_txt_${tab.name}"
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .border(
                        1.dp,
                        if (isSelected) AccentCyan else StudioBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .bounceClickable(pressedScale = 0.94f) { onSelectMode(tab) }
                    .padding(horizontal = 11.dp, vertical = 6.dp)
                    .testTag("filter_browser_${tab.name.lowercase()}")
            ) {
                Text(
                    text = AppStrings.filterBrowserLabel(tab, lang),
                    color = txt,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(AccentAmber.copy(alpha = 0.16f))
                .border(1.dp, AccentAmber.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                .bounceClickable(pressedScale = 0.94f, onClick = onSavePresetClick)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("save_custom_preset_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = AppStrings.savePresetBtn(lang),
                    tint = AccentAmber,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = AppStrings.savePresetBtn(lang).removePrefix("+ "),
                    color = AccentAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(StudioCard)
                .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                .bounceClickable(pressedScale = 0.94f, onClick = onImportClick)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("import_preset_lut_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FileOpen,
                    contentDescription = AppStrings.importPresetLutBtn(lang),
                    tint = AccentCyan,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = AppStrings.importPresetLutBtn(lang),
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun CustomPresetsBrowser(
    presets: List<CustomPreset>,
    onApplyPreset: (CustomPreset) -> Unit,
    onSharePreset: (CustomPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onSaveNewClick: () -> Unit,
    lang: AppLanguage
) {
    if (presets.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(StudioCard)
                .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                .bounceClickable(pressedScale = 0.98f, onClick = onSaveNewClick)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("empty_custom_presets_card")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.savePresetDialogTitle(lang),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = AppStrings.emptyPresetsHint(lang),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        return
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(presets, key = { it.id }) { preset ->
            Column(
                modifier = Modifier
                    .width(142.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(StudioCard)
                    .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                    .bounceClickable(pressedScale = 0.95f) { onApplyPreset(preset) }
                    .padding(10.dp)
                    .testTag("custom_preset_card_${preset.id}")
            ) {
                Text(
                    text = preset.name,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val badges = buildList {
                    if (preset.lutId != null) add("3D LUT")
                    if (preset.colorGrade.hasVisibleEffect) add("Grade")
                    if (!preset.curves.isDefault) add("Curves")
                    if (!preset.hsl.isDefault) add("HSL")
                    if (isEmpty()) add("Look")
                }.joinToString(" • ")
                Text(
                    text = badges,
                    color = AccentCyan,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSharePreset(preset) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .testTag("share_preset_${preset.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = AppStrings.shareItemLabel(lang),
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDeletePreset(preset.id) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .testTag("delete_preset_${preset.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = AppStrings.deleteItemLabel(lang),
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Lut3DBrowser(
    luts: List<Lut3D>,
    activeLut: Lut3D?,
    lutIntensity: Float,
    onSelectLut: (Lut3D?) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    onShareLut: (Lut3D) -> Unit,
    onDeleteLut: (String) -> Unit,
    lang: AppLanguage,
    hapticFeedback: Boolean,
    onEditFinished: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var lastStep by remember { mutableIntStateOf((lutIntensity * 100).toInt()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (activeLut != null) {
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
                    value = lutIntensity,
                    onValueChange = { v ->
                        val step = (v * 100).toInt()
                        if (hapticFeedback && step != lastStep) {
                            lastStep = step
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
                        .testTag("lut_intensity_slider")
                )
                Text(
                    text = "${(lutIntensity * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(44.dp)
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "lut_none") {
                val isNone = activeLut == null
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(74.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioCard)
                        .border(
                            1.5.dp,
                            if (isNone) AccentCyan else StudioBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .bounceClickable(pressedScale = 0.94f) { onSelectLut(null) }
                        .padding(10.dp)
                        .testTag("lut_card_none")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = AppStrings.noneLutLabel(lang),
                        tint = if (isNone) AccentCyan else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = AppStrings.noneLutLabel(lang),
                        color = if (isNone) AccentCyan else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (isNone) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            items(luts, key = { it.id }) { lut ->
                val isSelected = activeLut?.id == lut.id
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    StudioCard,
                                    if (lut.isBuiltIn) Color(0xFF1C2733) else Color(0xFF28221A)
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) AccentCyan else StudioBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .bounceClickable(pressedScale = 0.95f) { onSelectLut(lut) }
                        .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                        .testTag("lut_card_${lut.id}")
                ) {
                    Column(modifier = Modifier.width(88.dp)) {
                        Text(
                            text = lut.name,
                            color = if (isSelected) AccentCyan else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${lut.size}³ .cube",
                            color = if (lut.isBuiltIn) TextSecondary else AccentAmber,
                            fontSize = 9.sp
                        )
                    }
                    IconButton(
                        onClick = { onShareLut(lut) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(34.dp)
                            .testTag("share_lut_${lut.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = AppStrings.shareItemLabel(lang),
                            tint = AccentCyan,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    if (!lut.isBuiltIn) {
                        IconButton(
                            onClick = { onDeleteLut(lut.id) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(34.dp)
                                .testTag("delete_lut_${lut.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = AppStrings.deleteItemLabel(lang),
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavePresetDialog(
    lang: AppLanguage,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var presetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface,
        title = {
            Text(
                text = AppStrings.savePresetDialogTitle(lang),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                singleLine = true,
                placeholder = {
                    Text(
                        text = AppStrings.presetNamePlaceholder(lang),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = StudioBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_preset_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(presetName.ifBlank { "My Studio Look" })
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("confirm_save_preset_button")
            ) {
                Text(text = AppStrings.save(lang), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = AppStrings.cancel(lang), color = TextSecondary)
            }
        }
    )
}
