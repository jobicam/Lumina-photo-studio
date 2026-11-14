package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.model.AppLanguage
import com.example.model.UpdateStatus
import com.example.processing.AppUpdateChecker
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateCheckerModal(
    updateStatus: UpdateStatus,
    lastCheckedMillis: Long,
    currentFeedUrl: String,
    lang: AppLanguage = AppLanguage.SYSTEM,
    onCheckAgain: () -> Unit,
    onSaveFeedUrl: (String) -> Unit,
    onOpenWhatsNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showFeedConfig by remember { mutableStateOf(currentFeedUrl.isNotBlank()) }
    var feedUrlInput by remember(currentFeedUrl) { mutableStateOf(currentFeedUrl) }

    val formattedTime = remember(lastCheckedMillis) {
        if (lastCheckedMillis <= 0L) {
            AppStrings.neverChecked(lang)
        } else {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(lastCheckedMillis))
        }
    }

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
                .testTag("update_checker_modal")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = when (updateStatus) {
                        is UpdateStatus.UpdateAvailable -> AccentAmber
                        is UpdateStatus.Error -> ErrorRed
                        else -> AccentCyan
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (updateStatus) {
                            is UpdateStatus.Checking -> CircularProgressIndicator(
                                color = AccentCyan,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            is UpdateStatus.UpToDate -> Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = AppStrings.upToDateTitle(lang),
                                tint = AccentCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            is UpdateStatus.UpdateAvailable -> Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = AppStrings.updateAvailableTitle(lang),
                                tint = AccentAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            is UpdateStatus.Error -> Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = AppStrings.updateCheckFailedTitle(lang),
                                tint = ErrorRed,
                                modifier = Modifier.size(22.dp)
                            )
                            is UpdateStatus.Idle -> Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = AppStrings.checkForUpdates(lang),
                                tint = AccentCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = when (updateStatus) {
                                is UpdateStatus.Checking -> AppStrings.checkingForUpdates(lang)
                                is UpdateStatus.UpToDate -> AppStrings.upToDateTitle(lang)
                                is UpdateStatus.UpdateAvailable -> AppStrings.updateAvailableTitle(lang)
                                is UpdateStatus.Error -> AppStrings.updateCheckFailedTitle(lang)
                                is UpdateStatus.Idle -> AppStrings.checkForUpdates(lang)
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${AppStrings.lastCheckedLabel(lang)}: $formattedTime",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_update_modal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Installed Version & Result Details Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(StudioCard)
                    .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.installedVersionLabel(lang),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                }

                when (updateStatus) {
                    is UpdateStatus.UpToDate -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = updateStatus.sourceLabel,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    is UpdateStatus.UpdateAvailable -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppStrings.latestVersionLabel(lang),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "v${updateStatus.latestVersionName} (Build ${updateStatus.latestVersionCode})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber
                            )
                        }
                        if (updateStatus.releaseNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = updateStatus.releaseNotes,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                    is UpdateStatus.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = updateStatus.message,
                            fontSize = 12.sp,
                            color = ErrorRed,
                            lineHeight = 17.sp
                        )
                        if (currentFeedUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    feedUrlInput = ""
                                    onSaveFeedUrl("")
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("clear_update_feed_button")
                            ) {
                                Text(
                                    text = AppStrings.clearFeedUrl(lang),
                                    color = AccentCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }

            // Download / Get Update CTA when available
            if (updateStatus is UpdateStatus.UpdateAvailable && updateStatus.downloadUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateStatus.downloadUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Unable to open update link", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_update_url_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.downloadUpdate(lang),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional Custom Release Feed URL Accordion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioCard)
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    .clickable { showFeedConfig = !showFeedConfig }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("toggle_update_feed_config"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.updateFeedUrlLabel(lang),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Icon(
                    imageVector = if (showFeedConfig) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (showFeedConfig) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = feedUrlInput,
                    onValueChange = { feedUrlInput = it },
                    placeholder = {
                        Text(
                            text = AppStrings.updateFeedUrlHint(lang),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    },
                    trailingIcon = if (feedUrlInput.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { feedUrlInput = "" },
                                modifier = Modifier.testTag("clear_feed_input_icon")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear URL",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("update_feed_url_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSaveFeedUrl(feedUrlInput.trim()) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("save_update_feed_button")
                    ) {
                        Text(
                            text = AppStrings.saveUrl(lang),
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val manifest = AppUpdateChecker.buildVersionManifestJson()
                            clipboardManager.setText(AnnotatedString(manifest))
                            Toast.makeText(context, AppStrings.manifestCopiedToast(lang), Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("copy_version_manifest_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = AppStrings.copyVersionManifest(lang),
                            color = AccentAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AppStrings.upgradeFeedHelper(lang),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenWhatsNew()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("open_whats_new_from_update_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = AppStrings.whatsNew(lang),
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Button(
                    onClick = onCheckAgain,
                    enabled = updateStatus !is UpdateStatus.Checking,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("recheck_update_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = AppStrings.checkAgain(lang),
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = AppStrings.checkAgain(lang),
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
