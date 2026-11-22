package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.UserSettings
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings
import com.example.viewmodel.EditorTab

fun editorTabIcon(tab: EditorTab): ImageVector = when (tab) {
    EditorTab.FILTERS -> Icons.Default.Filter
    EditorTab.RETOUCH -> Icons.Default.AutoFixHigh
    EditorTab.ADJUST -> Icons.Default.Tune
    EditorTab.COLOR_GRADE -> Icons.Default.Palette
    EditorTab.CURVES -> Icons.AutoMirrored.Filled.ShowChart
    EditorTab.HSL -> Icons.Default.ColorLens
    EditorTab.CROP -> Icons.Default.Crop
}

fun editorTabLabel(tab: EditorTab, lang: AppLanguage): String = when (tab) {
    EditorTab.FILTERS -> AppStrings.tabFiltersLabel(lang)
    EditorTab.RETOUCH -> AppStrings.tabRetouchLabel(lang)
    EditorTab.ADJUST -> AppStrings.tabAdjustLabel(lang)
    EditorTab.COLOR_GRADE -> AppStrings.tabColorGradeLabel(lang)
    EditorTab.CURVES -> AppStrings.tabCurvesLabel(lang)
    EditorTab.HSL -> AppStrings.tabHslLabel(lang)
    EditorTab.CROP -> AppStrings.tabCropLabel(lang)
}

@Composable
fun EditorTabOrderList(
    orderedTabs: List<EditorTab>,
    lang: AppLanguage,
    onMoveTab: (EditorTab, Int) -> Unit,
    onResetOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDefaultOrder = orderedTabs == UserSettings.DEFAULT_EDITOR_TAB_ORDER

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = AppStrings.reorderTabsTitle(lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = AppStrings.reorderTabsSub(lang),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            if (!isDefaultOrder) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioSurfaceVariant)
                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        .bounceClickable(pressedScale = 0.92f, onClick = onResetOrder)
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                        .testTag("reset_tab_order_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = AppStrings.resetOrderLabel(lang),
                        tint = AccentCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = AppStrings.resetOrderLabel(lang),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            orderedTabs.forEachIndexed { index, tab ->
                val canMoveUp = index > 0
                val canMoveDown = index < orderedTabs.lastIndex
                val rankColor by animateColorAsState(
                    targetValue = if (index < 4) AccentCyan else TextSecondary,
                    animationSpec = tween(180),
                    label = "tab_rank_color_${tab.name}"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioBackground.copy(alpha = 0.72f))
                        .border(1.dp, StudioBorder.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                        .testTag("reorder_tab_row_${tab.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index < 4) AccentCyan.copy(alpha = 0.14f) else StudioSurfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = rankColor
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Icon(
                            imageVector = editorTabIcon(tab),
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = editorTabLabel(tab, lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val upInteraction = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = { onMoveTab(tab, -1) },
                            enabled = canMoveUp,
                            interactionSource = upInteraction,
                            modifier = Modifier
                                .pressBounce(upInteraction, pressedScale = 0.86f, enabled = canMoveUp)
                                .testTag("move_tab_up_${tab.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move ${editorTabLabel(tab, lang)} earlier",
                                tint = if (canMoveUp) TextPrimary else TextDisabled
                            )
                        }

                        val downInteraction = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = { onMoveTab(tab, 1) },
                            enabled = canMoveDown,
                            interactionSource = downInteraction,
                            modifier = Modifier
                                .pressBounce(downInteraction, pressedScale = 0.86f, enabled = canMoveDown)
                                .testTag("move_tab_down_${tab.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move ${editorTabLabel(tab, lang)} later",
                                tint = if (canMoveDown) TextPrimary else TextDisabled
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderTabsModal(
    orderedTabs: List<EditorTab>,
    lang: AppLanguage,
    onMoveTab: (EditorTab, Int) -> Unit,
    onResetOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                .verticalScroll(rememberScrollState())
                .testTag("reorder_tabs_modal")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(StudioSurfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = AppStrings.reorderTabsTitle(lang),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                val closeInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = onDismiss,
                    interactionSource = closeInteraction,
                    modifier = Modifier.pressBounce(closeInteraction, pressedScale = 0.90f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StudioCard)
                    .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
            ) {
                EditorTabOrderList(
                    orderedTabs = orderedTabs,
                    lang = lang,
                    onMoveTab = onMoveTab,
                    onResetOrder = onResetOrder
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val doneInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onDismiss,
                interactionSource = doneInteraction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pressBounce(doneInteraction, pressedScale = 0.96f)
                    .testTag("reorder_tabs_done_button")
            ) {
                Text(
                    text = AppStrings.done(lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
