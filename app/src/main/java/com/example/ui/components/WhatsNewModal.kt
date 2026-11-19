package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LensBlur
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.model.AppLanguage
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewModal(
    lang: AppLanguage = AppLanguage.SYSTEM,
    upgradedFromVersion: String? = null,
    onCheckUpdates: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE
    val isKhmer = AppStrings.isKhmer(lang)
    var showPreviousVersions by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .testTag("whats_new_modal")
        ) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(StudioSurfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = AppStrings.whatsNew(lang),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "v$versionName (Build $versionCode)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.latestRelease(lang),
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                val closeWhatsNewInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = onDismiss,
                    interactionSource = closeWhatsNewInteraction,
                    modifier = Modifier
                        .pressBounce(closeWhatsNewInteraction, pressedScale = 0.90f)
                        .testTag("close_whats_new_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(
                visible = !upgradedFromVersion.isNullOrBlank(),
                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
            ) {
                upgradedFromVersion?.takeIf { it.isNotBlank() }?.let { prevVer ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1B4332).copy(alpha = 0.65f))
                                .border(1.dp, Color(0xFF52B788).copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("upgraded_from_banner"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color(0xFF74C69D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = AppStrings.updatedFromLabel(prevVer, versionName, lang),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD8F3DC)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // Section 1: CURRENT VERSION HIGHLIGHT (v2.10.1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentCyan.copy(alpha = 0.10f))
                    .border(1.dp, AccentCyan.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isKhmer) "កំណែថ្មី v$versionName (Build $versionCode) — Retouch Studio & UI/UX ប្រណីត" else "What's New in v$versionName — Retouch Upgrade & Studio UI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                WhatsNewItem(
                    icon = Icons.Default.AutoFixHigh,
                    iconTint = AccentPink,
                    title = if (isKhmer) "ស្ទូឌីយោ Retouch កម្រិតខ្ពស់ & គែមជក់ទន់" else "Upgraded Retouch Engine, Sculpt & Live Brush Trail",
                    description = if (isKhmer)
                        "បន្ថែមមុខងារគែមជក់ (Edge) ម៉ូដ Nudge/Slim/Plump ការចងចាំការកំណត់តាមឧបករណ៍ និងបន្ទាត់គូសផ្ទាល់។"
                    else
                        "New brush edge hardness control, Nudge/Slim/Plump sculpt modes, per-tool remembered settings, dual-ring nib cursor & live stroke trail."
                )

                WhatsNewItem(
                    icon = Icons.Default.TouchApp,
                    iconTint = AccentCyan,
                    title = if (isKhmer) "ការកែលម្អ UI/UX ស្ទូឌីយោ & សញ្ញាបង្ហាញការកែ" else "Refined Studio UI/UX & Active Edit Indicators",
                    description = if (isKhmer)
                        "ផ្ទាំងជូនដំណឹង Frosted កាតពណ៌តម្រង Gradient សញ្ញាផ្ទាំងកែសកម្ម និងចុចលើតម្លៃដើម្បីកំណត់ឡើងវិញ។"
                    else
                        "Frosted in-app status banner, tonal gradient filter swatches, active-edit tab dots, canvas info pill, and tap-to-reset slider values."
                )

                WhatsNewItem(
                    icon = Icons.Default.MotionPhotosOn,
                    iconTint = AccentPurple,
                    title = if (isKhmer) "Motion Blur, Depth Bokeh, Colour Grade & LUTs" else "Pro Optics, Colour Grade Wheels & 3D LUTs",
                    description = if (isKhmer)
                        "Motion Blur តាមទិសដៅ Depth Lens Bokeh កង់ពណ៌ភាពយន្ត ៣ កម្រិត និងការនាំចូល 3D LUT (.cube)។"
                    else
                        "Includes v2.10 directional Motion Blur, circular Depth Lens Bokeh, 3-way Colour Grade wheels, and custom Presets & .cube LUTs."
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable: PREVIOUS UPDATES
            val chevronRotation by animateFloatAsState(
                targetValue = if (showPreviousVersions) 180f else 0f,
                animationSpec = tween(220),
                label = "previous_updates_chevron"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCard)
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    .bounceClickable(pressedScale = 0.98f) { showPreviousVersions = !showPreviousVersions }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.previousUpdates(lang),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }

            AnimatedVisibility(
                visible = showPreviousVersions,
                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WhatsNewItem(
                            icon = Icons.Default.Palette,
                            iconTint = AccentCyan,
                            title = "v2.10.0: Motion Blur, Depth Lens Bokeh, Colour Grade & LUTs",
                            description = "Directional Motion Blur (Scene/Background/Subject), optical circular bokeh, 3-way tonal wheels, and custom Presets & 3D .cube LUTs."
                        )

                        WhatsNewItem(
                            icon = Icons.Default.TouchApp,
                            iconTint = AccentCyan,
                            title = "v2.9.0: Smooth Layout Transitions & Press Physics",
                            description = "Directional screen slides, animated editor drawers, spring button feedback, and built-in GitHub release checks."
                        )

                        WhatsNewItem(
                            icon = Icons.Default.AutoFixHigh,
                            iconTint = AccentPink,
                            title = "v2.8.0: Redesigned Retouch Studio & Restore Brush",
                            description = "Accent-coded brush dock, live nib preview with S/M/L presets, directional Sculpt & Warp, and Restore Brush."
                        )

                        WhatsNewItem(
                            icon = Icons.Default.ZoomIn,
                            iconTint = AccentCyan,
                            title = "v2.7.0: Background Blur (Bokeh) & Photo Zoom (1.0x–6.0x)",
                            description = "Optical portrait background blur with tap-to-focus, Subject Protection, and pinch/zoom controls."
                        )

                    WhatsNewItem(
                        icon = Icons.Default.Face,
                        iconTint = AccentPink,
                        title = "v2.5.0: Gaussian Mask Skin Smoothing & Detail Slider",
                        description = "Feathered YCbCr/HSV Gaussian skin smoothing with 0%-75% micro-texture preservation and streamlined update checks."
                    )

                    WhatsNewItem(
                        icon = Icons.Default.SystemUpdate,
                        iconTint = AccentCyan,
                        title = "v2.4.0: Built-In Update Checker & Release Feed Support",
                        description = "Version & build verifier, custom JSON / GitHub Releases API checker, auto-check toggle, and last-checked timestamps."
                    )

                    WhatsNewItem(
                        icon = Icons.Default.Crop,
                        iconTint = AccentCyan,
                        title = "v2.3.0: Interactive Canvas Cropping & Aspect Ratios",
                        description = "Draggable crop handles, Rule-of-Thirds grid, 1:1 / 4:5 / 16:9 presets, live pixel badge, and Undo/Redo support."
                    )

                    WhatsNewItem(
                        icon = Icons.Default.PhotoCamera,
                        iconTint = AccentCyan,
                        title = "v2.2.0: Live Camera Filter Preview",
                        description = "Real-time viewfinder filter preview across 20+ presets with live thumbnail strip and automatic editor handoff."
                    )

                    WhatsNewItem(
                        icon = Icons.Default.AutoAwesome,
                        iconTint = AccentCyan,
                        title = "v2.1.0: 1-Tap Auto Enhance, Studio Frames & Soft Glow",
                        description = "Histogram-based Smart Auto Enhance, White/Polaroid/Cinema/Obsidian frames, Soft Dream Glow, and 4 new film presets."
                    )

                    WhatsNewItem(
                        icon = Icons.Default.TouchApp,
                        iconTint = AccentCyan,
                        title = "v2.0.1: Full-App Bug Fix & Stability Polish",
                        description = "Fixed CameraX lifecycle rebinding, enlarged Curves hit targets, array-buffered retouch speedups, and complete Khmer localization."
                    )

                    WhatsNewItem(
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        iconTint = AccentCyan,
                        title = "v2.0.0: Pro Tonal Engine & Live RGB Histogram HUD",
                        description = "Live RGB + Luminance histogram HUD, Curves histogram underlay, Highlights/Shadows/Whites/Blacks/Clarity/Sharpness, and Rule-of-Thirds grid."
                    )

                    WhatsNewItem(
                        icon = Icons.Default.Face,
                        iconTint = AccentPink,
                        title = "v1.3.0: Portrait Skin Smoothing & Tone Detection",
                        description = "YCbCr + HSV skin segmentation, edge-preserving bilateral blur, and 1-tap Auto Smooth."
                    )

                    WhatsNewItem(
                        icon = Icons.Default.Language,
                        iconTint = AccentCyan,
                        title = "v1.2.0: Khmer Language & Performance Boost",
                        description = "Bundled Kantumruy Pro font, full Khmer UI translation, 30x faster curve processing LUT, and canvas gesture fix."
                    )

                    WhatsNewItem(
                        icon = Icons.Default.SaveAlt,
                        iconTint = Color(0xFF40C4FF),
                        title = "v1.1.0: Gallery Album Export & Save Options",
                        description = "Direct export to Pictures/Lumina with custom filename, JPEG quality, resolution presets, and direct Gallery opening."
                    )

                    WhatsNewItem(
                        icon = Icons.Default.PhotoCamera,
                        iconTint = AccentCyan,
                        title = "v1.0.5: CameraX In-App Viewfinder Capture",
                        description = "Integrated full-screen CameraX live preview with flash toggles and front/back lens switching."
                    )

                    WhatsNewItem(
                        icon = Icons.Default.Filter,
                        iconTint = AccentAmber,
                        title = "v1.0.0: Pro Retouching & 15+ Cinematic Filters",
                        description = "Bilateral skin smoothing, teeth whitening, eye pop, RGB tone curves, 8-channel HSL, and split-screen comparison."
                    )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (onCheckUpdates != null) {
                val whatsNewUpdateInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = onCheckUpdates,
                    interactionSource = whatsNewUpdateInteraction,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .pressBounce(whatsNewUpdateInteraction, pressedScale = 0.96f)
                        .testTag("whats_new_check_updates_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = AppStrings.checkForUpdates(lang),
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.checkForUpdates(lang),
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Got It Button
            val dismissInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onDismiss,
                interactionSource = dismissInteraction,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pressBounce(dismissInteraction, pressedScale = 0.96f)
                    .testTag("dismiss_whats_new_button")
            ) {
                Text(
                    text = AppStrings.gotItLetsEdit(lang),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WhatsNewItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCard)
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
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
