package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.model.ApkDownloadState
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
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.AppStrings
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateCheckerModal(
    updateStatus: UpdateStatus,
    lastCheckedMillis: Long,
    lang: AppLanguage = AppLanguage.SYSTEM,
    apkDownloadState: ApkDownloadState = ApkDownloadState.Idle,
    onCheckAgain: () -> Unit,
    onOpenWhatsNew: () -> Unit,
    onDownloadAndInstall: ((File) -> Unit) -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var pendingInstallFile by remember { mutableStateOf<File?>(null) }

    val startPackageInstaller: (File) -> Unit = remember(context) {
        { apkFile ->
            try {
                context.startActivity(AppUpdateChecker.createInstallApkIntent(context, apkFile))
            } catch (_: Exception) {
                Toast.makeText(context, "Unable to launch package installer", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val queuedFile = pendingInstallFile
        pendingInstallFile = null
        if (queuedFile != null && AppUpdateChecker.canInstallPackages(context) && AppUpdateChecker.isValidApkFile(queuedFile)) {
            startPackageInstaller(queuedFile)
        }
    }

    val installOrRequestPermission: (File) -> Unit = { apkFile ->
        if (AppUpdateChecker.canInstallPackages(context)) {
            startPackageInstaller(apkFile)
        } else {
            pendingInstallFile = apkFile
            Toast.makeText(context, AppStrings.allowInstallPermissionHint(lang), Toast.LENGTH_LONG).show()
            try {
                unknownSourcesLauncher.launch(AppUpdateChecker.createManageUnknownSourcesIntent(context))
            } catch (_: Exception) {
                startPackageInstaller(apkFile)
            }
        }
    }

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

                val closeUpdateInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = onDismiss,
                    interactionSource = closeUpdateInteraction,
                    modifier = Modifier
                        .pressBounce(closeUpdateInteraction, pressedScale = 0.90f)
                        .testTag("close_update_modal_button")
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
                            val latestLabel = if (updateStatus.latestVersionCode > 0L) {
                                "v${updateStatus.latestVersionName} (Build ${updateStatus.latestVersionCode})"
                            } else {
                                "v${updateStatus.latestVersionName}"
                            }
                            Text(
                                text = latestLabel,
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
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("built_in_update_source_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = AppStrings.updateSourceLabel(lang),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = AppUpdateChecker.DEFAULT_UPDATE_FEED_URL.removePrefix("https://"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AppStrings.customLinkDisabledNote(lang),
                    fontSize = 10.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }

            // Download / Direct In-App Install CTA when available
            AnimatedVisibility(
                visible = updateStatus is UpdateStatus.UpdateAvailable,
                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
            ) {
                if (updateStatus is UpdateStatus.UpdateAvailable) {
                    if (updateStatus.downloadUrl.isNotBlank()) {
                        val isDirectApk = AppUpdateChecker.isDirectApkUrl(updateStatus.downloadUrl)
                        val openBrowserAction = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateStatus.downloadUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Unable to open update link", Toast.LENGTH_SHORT).show()
                            }
                        }

                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            if (isDirectApk) {
                                when (apkDownloadState) {
                                    is ApkDownloadState.Downloading -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(StudioCard)
                                                .border(1.dp, AccentAmber.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                                .padding(12.dp)
                                                .testTag("apk_download_progress_card")
                                        ) {
                                            val downloadedMb = apkDownloadState.bytesDownloaded / (1024.0 * 1024.0)
                                            val sizeText = if (apkDownloadState.totalBytes > 0L) {
                                                val totalMb = apkDownloadState.totalBytes / (1024.0 * 1024.0)
                                                val pct = ((apkDownloadState.progress ?: 0f) * 100f).toInt()
                                                String.format(Locale.US, "%.1f / %.1f MB (%d%%)", downloadedMb, totalMb, pct)
                                            } else {
                                                String.format(Locale.US, "%.1f MB", downloadedMb)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = AppStrings.downloadingApkLabel(lang),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = sizeText,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentAmber
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val progress = apkDownloadState.progress
                                            if (progress != null) {
                                                LinearProgressIndicator(
                                                    progress = { progress },
                                                    color = AccentAmber,
                                                    trackColor = StudioSurfaceVariant,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                )
                                            } else {
                                                LinearProgressIndicator(
                                                    color = AccentAmber,
                                                    trackColor = StudioSurfaceVariant,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            OutlinedButton(
                                                onClick = onCancelDownload,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(40.dp)
                                                    .testTag("cancel_apk_download_button")
                                            ) {
                                                Text(
                                                    text = AppStrings.cancel(lang),
                                                    color = TextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    is ApkDownloadState.ReadyToInstall -> {
                                        val readyMb = String.format(Locale.US, "%.1f MB", apkDownloadState.fileSizeBytes / (1024.0 * 1024.0))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SuccessGreen.copy(alpha = 0.12f))
                                                .border(1.dp, SuccessGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = AppStrings.apkReadyToInstallLabel(lang),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary
                                                )
                                            }
                                            Text(
                                                text = readyMb,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val installInteraction = remember { MutableInteractionSource() }
                                            Button(
                                                onClick = { installOrRequestPermission(File(apkDownloadState.apkFilePath)) },
                                                interactionSource = installInteraction,
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .pressBounce(installInteraction, pressedScale = 0.96f)
                                                    .testTag("install_apk_now_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.InstallMobile,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = AppStrings.installApkNow(lang),
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }

                                            val browserInteraction = remember { MutableInteractionSource() }
                                            OutlinedButton(
                                                onClick = openBrowserAction,
                                                interactionSource = browserInteraction,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .pressBounce(browserInteraction, pressedScale = 0.92f)
                                                    .testTag("open_browser_fallback_button"),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = AppStrings.openInBrowserLabel(lang),
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    is ApkDownloadState.Idle,
                                    is ApkDownloadState.Failed -> {
                                        if (apkDownloadState is ApkDownloadState.Failed) {
                                            Text(
                                                text = apkDownloadState.message,
                                                fontSize = 11.sp,
                                                color = ErrorRed,
                                                lineHeight = 15.sp,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val downloadInteraction = remember { MutableInteractionSource() }
                                            Button(
                                                onClick = { onDownloadAndInstall(installOrRequestPermission) },
                                                interactionSource = downloadInteraction,
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .pressBounce(downloadInteraction, pressedScale = 0.96f)
                                                    .testTag("open_update_url_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = AppStrings.downloadApkUpdate(lang),
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }

                                            val browserInteraction = remember { MutableInteractionSource() }
                                            OutlinedButton(
                                                onClick = openBrowserAction,
                                                interactionSource = browserInteraction,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .pressBounce(browserInteraction, pressedScale = 0.92f)
                                                    .testTag("open_browser_fallback_button"),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = AppStrings.openInBrowserLabel(lang),
                                                    tint = AccentAmber,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            val copyInteraction = remember { MutableInteractionSource() }
                                            OutlinedButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(updateStatus.downloadUrl))
                                                    Toast.makeText(context, AppStrings.linkCopiedToast(lang), Toast.LENGTH_SHORT).show()
                                                },
                                                interactionSource = copyInteraction,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .pressBounce(copyInteraction, pressedScale = 0.92f)
                                                    .testTag("copy_update_url_button"),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = AppStrings.linkCopiedToast(lang),
                                                    tint = AccentAmber,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val downloadInteraction = remember { MutableInteractionSource() }
                                    Button(
                                        onClick = openBrowserAction,
                                        interactionSource = downloadInteraction,
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .pressBounce(downloadInteraction, pressedScale = 0.96f)
                                            .testTag("open_update_url_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = AppStrings.downloadUpdate(lang),
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    val copyInteraction = remember { MutableInteractionSource() }
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(updateStatus.downloadUrl))
                                            Toast.makeText(context, AppStrings.linkCopiedToast(lang), Toast.LENGTH_SHORT).show()
                                        },
                                        interactionSource = copyInteraction,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .pressBounce(copyInteraction, pressedScale = 0.92f)
                                            .testTag("copy_update_url_button"),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = AppStrings.linkCopiedToast(lang),
                                            tint = AccentAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = AppStrings.directUpdateHint(lang),
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    } else {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = AppStrings.noDownloadLinkHint(lang),
                                fontSize = 11.sp,
                                color = AccentAmber,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val whatsNewInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenWhatsNew()
                    },
                    interactionSource = whatsNewInteraction,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .pressBounce(whatsNewInteraction, pressedScale = 0.96f)
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

                val recheckInteraction = remember { MutableInteractionSource() }
                val canRecheck = updateStatus !is UpdateStatus.Checking
                Button(
                    onClick = onCheckAgain,
                    enabled = canRecheck,
                    interactionSource = recheckInteraction,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .pressBounce(recheckInteraction, pressedScale = 0.96f, enabled = canRecheck)
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
