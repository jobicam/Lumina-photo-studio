package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.model.AdjustCategory
import com.example.model.AdjustmentState
import com.example.model.ApkDownloadState
import com.example.model.AppLanguage
import com.example.model.ColorGradeState
import com.example.model.ColorGradeTone
import com.example.model.ColorWheelValue
import com.example.model.CropAspectRatio
import com.example.model.CropRect
import com.example.model.CurveChannel
import com.example.model.CurvesState
import com.example.model.CustomPreset
import com.example.model.ExportFormat
import com.example.model.FilterBrowserMode
import com.example.model.FilterPreset
import com.example.model.FilterPresets
import com.example.model.FrameStyle
import com.example.model.HistogramData
import com.example.model.HslChannelAdjustment
import com.example.model.HslColorChannel
import com.example.model.HslState
import com.example.model.Lut3D
import com.example.model.MotionBlurTarget
import com.example.model.RetouchStroke
import com.example.model.RetouchToolSettings
import com.example.model.RetouchType
import com.example.model.SavedImageInfo
import com.example.model.SculptMode
import com.example.model.SingleCurve
import com.example.model.StrokePoint
import com.example.model.TransformState
import com.example.model.UpdateStatus
import com.example.model.UserSettings
import com.example.processing.AppUpdateChecker
import com.example.processing.BitmapProcessor
import com.example.processing.BuiltInLuts
import com.example.processing.ImageExporter
import com.example.processing.PresetLutImportResult
import com.example.processing.PresetLutRepository
import com.example.util.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

enum class AppScreen(val label: String) {
    HOME("Home"),
    EDITOR("Editor"),
    SETTINGS("Settings")
}

enum class EditorTab(val label: String) {
    FILTERS("Filters"),
    RETOUCH("Retouch"),
    ADJUST("Adjust"),
    COLOR_GRADE("Grade"),
    CURVES("Curves"),
    HSL("HSL"),
    CROP("Crop")
}

data class EditorHistorySnapshot(
    val originalBitmap: Bitmap,
    val workingBitmap: Bitmap,
    val adjustments: AdjustmentState,
    val activePreset: FilterPreset,
    val filterIntensity: Float,
    val colorGrade: ColorGradeState,
    val activeLut: Lut3D?,
    val lutIntensity: Float,
    val curves: CurvesState,
    val hsl: HslState,
    val transform: TransformState,
    val activeFrame: FrameStyle = FrameStyle.NONE,
    val hasRetouchEdits: Boolean = false
)

data class EditorUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val settings: UserSettings = UserSettings(),
    val cacheSizeDisplay: String = "0 KB",
    val originalBitmap: Bitmap? = null,
    val workingBitmap: Bitmap? = null,
    val displayBitmap: Bitmap? = null,
    val activeTab: EditorTab = EditorTab.FILTERS,
    val activeAdjustCategory: AdjustCategory = AdjustCategory.LIGHT,
    val filterBrowserMode: FilterBrowserMode = FilterBrowserMode.FILTERS,
    val activePreset: FilterPreset = FilterPresets.NONE,
    val filterIntensity: Float = 1.0f,
    val adjustments: AdjustmentState = AdjustmentState(),
    val colorGrade: ColorGradeState = ColorGradeState(),
    val curves: CurvesState = CurvesState(),
    val hsl: HslState = HslState(),
    val customPresets: List<CustomPreset> = emptyList(),
    val availableLuts: List<Lut3D> = emptyList(),
    val activeLut: Lut3D? = null,
    val lutIntensity: Float = 1.0f,
    val transform: TransformState = TransformState(),
    val selectedCropRatio: CropAspectRatio = CropAspectRatio.FREE,
    val pendingCropRect: CropRect = CropRect(),
    val activeFrame: FrameStyle = FrameStyle.NONE,
    // Retouch state
    val activeRetouchTool: RetouchType = RetouchType.SKIN_SMOOTH,
    val brushRadius: Float = RetouchType.SKIN_SMOOTH.defaultRadius,
    val brushIntensity: Float = RetouchType.SKIN_SMOOTH.defaultIntensity,
    val brushHardness: Float = RetouchType.SKIN_SMOOTH.defaultHardness,
    val texturePreserve: Float = 0.30f,
    val sculptMode: SculptMode = SculptMode.PUSH,
    val isEraser: Boolean = false,
    val hasRetouchEdits: Boolean = false,
    val recentRetouchPoint: StrokePoint? = null,
    // Compare state
    val isComparing: Boolean = false,
    val isSplitView: Boolean = false,
    val splitRatio: Float = 0.5f,
    // Save & Gallery state
    val showSaveModal: Boolean = false,
    val savedImageInfo: SavedImageInfo? = null,
    val showSaveSuccessModal: Boolean = false,
    // UI state
    val isProcessing: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val statusMessage: String? = null,
    // Real-time Histogram state (v2.0)
    val histogram: HistogramData? = null,
    val showHistogramOverlay: Boolean = false,
    // App Update Checker & Upgrade Migration state (v2.4 - v2.5)
    val updateStatus: UpdateStatus = UpdateStatus.Idle,
    val apkDownloadState: ApkDownloadState = ApkDownloadState.Idle,
    val lastUpdateCheckMillis: Long = 0L,
    val showUpdateDialog: Boolean = false,
    val upgradedFromVersion: String? = null
)

class PhotoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("lumina_settings_prefs", Context.MODE_PRIVATE)
    private val versionPrefs = application.getSharedPreferences("lumina_version_prefs", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        EditorUiState(
            settings = loadSettings(),
            lastUpdateCheckMillis = prefs.getLong("last_update_check_ms", 0L),
            upgradedFromVersion = migrateFromOlderVersionIfNeeded(application)
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<EditorHistorySnapshot>()
    private val redoStack = mutableListOf<EditorHistorySnapshot>()

    private var renderJob: Job? = null
    private var updateCheckJob: Job? = null
    private var apkDownloadJob: Job? = null
    private val bitmapMutex = Mutex()
    private var continuousEditSnapshotted = false
    private val retouchToolSettings = mutableMapOf<RetouchType, RetouchToolSettings>()

    init {
        // Pre-load default sample photo, saved custom presets & 3D LUTs in background
        loadSamplePhoto(isPortrait = true, autoNavigate = false)
        loadPresetsAndLuts()
        calculateCacheSize()
        val s = _uiState.value.settings
        if (s.autoCheckUpdates) {
            checkForUpdates(openDialog = false)
        }
    }

    private fun loadPresetsAndLuts() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val presets = PresetLutRepository.loadCustomPresets(ctx)
            val luts = PresetLutRepository.loadAllLuts(ctx)
            _uiState.update {
                it.copy(
                    customPresets = presets,
                    availableLuts = luts
                )
            }
        }
    }

    private fun migrateFromOlderVersionIfNeeded(application: Application): String? {
        val currentCode = com.example.BuildConfig.VERSION_CODE
        val currentName = com.example.BuildConfig.VERSION_NAME
        val lastMigratedCode = prefs.getInt("last_migrated_version_code", 0)
        val lastSeenRaw = versionPrefs.getString("last_seen_version", "").orEmpty()
        var recordedFrom = versionPrefs.getString("upgraded_from_version", null)

        val previousVersionFromSeen = lastSeenRaw
            .substringBefore('+')
            .trim()
            .takeIf { it.isNotEmpty() && lastSeenRaw != "$currentName+$currentCode" && it != currentName }

        if (previousVersionFromSeen != null) {
            recordedFrom = previousVersionFromSeen
            versionPrefs.edit().putString("upgraded_from_version", previousVersionFromSeen).apply()
        }

        if (lastMigratedCode in 1 until currentCode) {
            // Preserve user settings while safely sanitizing legacy ranges & stale temp cache
            val clampedQuality = prefs.getInt("jpeg_quality", 95).coerceIn(70, 100)
            val safeMaxRes = prefs.getInt("max_resolution", 2048).coerceIn(1200, 4096)
            prefs.edit()
                .putInt("jpeg_quality", clampedQuality)
                .putInt("max_resolution", safeMaxRes)
                .putInt("last_migrated_version_code", currentCode)
                .apply()

            runCatching {
                val imgCache = File(application.cacheDir, "images")
                if (imgCache.exists()) {
                    imgCache.deleteRecursively()
                }
            }
        } else if (lastMigratedCode == 0) {
            prefs.edit().putInt("last_migrated_version_code", currentCode).apply()
        }

        return recordedFrom?.takeIf { it != currentName }
    }

    private fun loadSettings(): UserSettings {
        val langCode = prefs.getString("app_language", AppLanguage.SYSTEM.code)
        val lang = AppLanguage.values().firstOrNull { it.code == langCode } ?: AppLanguage.SYSTEM
        val fmtName = prefs.getString("export_format", ExportFormat.JPEG.name)
        val fmt = ExportFormat.values().firstOrNull { it.name == fmtName } ?: ExportFormat.JPEG
        return UserSettings(
            appLanguage = lang,
            exportFormat = fmt,
            jpegQuality = prefs.getInt("jpeg_quality", 95).coerceIn(70, 100),
            maxResolution = prefs.getInt("max_resolution", 2048),
            defaultFrontCamera = prefs.getBoolean("default_front_camera", false),
            showGridOverlay = prefs.getBoolean("show_grid_overlay", false),
            hapticFeedback = prefs.getBoolean("haptic_feedback", true),
            autoSaveDrafts = prefs.getBoolean("auto_save_drafts", true),
            autoCheckUpdates = prefs.getBoolean("auto_check_updates", true),
            updateFeedUrl = prefs.getString("update_feed_url", null)
                ?.takeIf { it.isNotBlank() }
                ?: AppUpdateChecker.DEFAULT_UPDATE_FEED_URL
        )
    }

    private fun persistSettings(s: UserSettings) {
        prefs.edit()
            .putString("app_language", s.appLanguage.code)
            .putString("export_format", s.exportFormat.name)
            .putInt("jpeg_quality", s.jpegQuality)
            .putInt("max_resolution", s.maxResolution)
            .putBoolean("default_front_camera", s.defaultFrontCamera)
            .putBoolean("show_grid_overlay", s.showGridOverlay)
            .putBoolean("haptic_feedback", s.hapticFeedback)
            .putBoolean("auto_save_drafts", s.autoSaveDrafts)
            .putBoolean("auto_check_updates", s.autoCheckUpdates)
            .putString("update_feed_url", s.updateFeedUrl)
            .apply()
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
        if (screen == AppScreen.SETTINGS) {
            calculateCacheSize()
        }
    }

    fun updateSettings(update: (UserSettings) -> UserSettings) {
        val updated = update(_uiState.value.settings)
        persistSettings(updated)
        _uiState.update { it.copy(settings = updated) }
    }

    fun checkForUpdates(openDialog: Boolean = true) {
        updateCheckJob?.cancel()
        apkDownloadJob?.cancel()
        _uiState.update {
            it.copy(
                updateStatus = UpdateStatus.Checking,
                apkDownloadState = ApkDownloadState.Idle,
                showUpdateDialog = if (openDialog) true else it.showUpdateDialog
            )
        }
        val feedUrl = AppUpdateChecker.DEFAULT_UPDATE_FEED_URL
        updateCheckJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                AppUpdateChecker.checkForUpdate(getApplication(), feedUrl)
            }
            val checkedAt = System.currentTimeMillis()
            prefs.edit().putLong("last_update_check_ms", checkedAt).apply()
            _uiState.update { state ->
                val shouldSurface = openDialog || result is UpdateStatus.UpdateAvailable
                state.copy(
                    updateStatus = result,
                    lastUpdateCheckMillis = checkedAt,
                    showUpdateDialog = shouldSurface || state.showUpdateDialog
                )
            }
        }
    }

    fun downloadAndInstallUpdate(onReadyToInstall: (File) -> Unit) {
        val currentState = _uiState.value
        val existingReady = currentState.apkDownloadState as? ApkDownloadState.ReadyToInstall
        if (existingReady != null) {
            val cachedFile = File(existingReady.apkFilePath)
            if (AppUpdateChecker.isValidApkFile(cachedFile)) {
                onReadyToInstall(cachedFile)
                return
            }
        }

        val available = currentState.updateStatus as? UpdateStatus.UpdateAvailable ?: return
        if (available.downloadUrl.isBlank()) return
        if (currentState.apkDownloadState is ApkDownloadState.Downloading) return

        apkDownloadJob?.cancel()
        _uiState.update {
            it.copy(
                apkDownloadState = ApkDownloadState.Downloading(
                    bytesDownloaded = 0L,
                    totalBytes = -1L,
                    progress = null
                )
            )
        }

        val appCtx = getApplication<Application>()
        apkDownloadJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                AppUpdateChecker.downloadApkUpdate(
                    context = appCtx,
                    downloadUrl = available.downloadUrl,
                    versionName = available.latestVersionName,
                    isCancelled = { apkDownloadJob?.isActive != true },
                    onProgress = { downloaded, total ->
                        val frac = if (total > 0L) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else null
                        _uiState.update {
                            it.copy(
                                apkDownloadState = ApkDownloadState.Downloading(
                                    bytesDownloaded = downloaded,
                                    totalBytes = total,
                                    progress = frac
                                )
                            )
                        }
                    }
                )
            }

            result.fold(
                onSuccess = { apkFile ->
                    _uiState.update {
                        it.copy(
                            apkDownloadState = ApkDownloadState.ReadyToInstall(
                                apkFilePath = apkFile.absolutePath,
                                fileSizeBytes = apkFile.length()
                            )
                        )
                    }
                    onReadyToInstall(apkFile)
                },
                onFailure = { err ->
                    if (err is java.util.concurrent.CancellationException) {
                        _uiState.update { it.copy(apkDownloadState = ApkDownloadState.Idle) }
                    } else {
                        _uiState.update {
                            it.copy(
                                apkDownloadState = ApkDownloadState.Failed(
                                    err.localizedMessage ?: "Failed to download APK update"
                                )
                            )
                        }
                    }
                }
            )
        }
    }

    fun cancelApkDownload() {
        apkDownloadJob?.cancel()
        apkDownloadJob = null
        viewModelScope.launch(Dispatchers.IO) {
            AppUpdateChecker.cleanupPartialApkDownloads(getApplication())
        }
        _uiState.update { it.copy(apkDownloadState = ApkDownloadState.Idle) }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun clearCache() {
        val lang = _uiState.value.settings.appLanguage
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = getApplication<Application>().cacheDir
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            cacheSizeDisplay = "0 KB",
                            statusMessage = AppStrings.statusCacheCleared(lang)
                        )
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(statusMessage = AppStrings.statusCacheClearFailed(lang)) }
                }
            }
        }
    }

    fun calculateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = getApplication<Application>().cacheDir
            val size = getFolderSize(cacheDir)
            val display = formatFileSize(size)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(cacheSizeDisplay = display) }
            }
        }
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        var length = 0L
        file.listFiles()?.forEach { child ->
            length += getFolderSize(child)
        }
        return length
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size <= 0 -> "0 KB"
            size < 1024 * 1024 -> "${(size / 1024).coerceAtLeast(1)} KB"
            else -> String.format(java.util.Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
        }
    }

    fun loadSamplePhoto(
        isPortrait: Boolean,
        autoNavigate: Boolean = true,
        initialTab: EditorTab = EditorTab.FILTERS
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val result = withContext(Dispatchers.IO) {
                val resId = if (isPortrait) R.drawable.sample_portrait else R.drawable.sample_landscape
                val rawBmp = BitmapFactory.decodeResource(getApplication<Application>().resources, resId)
                if (rawBmp != null) {
                    val maxDim = _uiState.value.settings.maxResolution
                    val scaled = BitmapProcessor.downscaleIfNeeded(rawBmp, maxDim)
                    if (scaled !== rawBmp && !rawBmp.isRecycled) {
                        rawBmp.recycle()
                    }
                    val hist = BitmapProcessor.calculateHistogram(scaled)
                    scaled to hist
                } else {
                    null
                }
            }
            if (result != null) {
                setNewInitialBitmap(
                    bitmap = result.first,
                    initialHistogram = result.second,
                    autoNavigate = autoNavigate,
                    initialTab = initialTab
                )
            }
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    fun loadFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val loaded = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val exifOrientation = runCatching {
                        context.contentResolver.openInputStream(uri)?.use { exifStream ->
                            ExifInterface(exifStream).getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL
                            )
                        }
                    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

                    val rawBmp = context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }

                    if (rawBmp != null) {
                        val orientedBmp = applyExifOrientation(rawBmp, exifOrientation)
                        val maxDim = _uiState.value.settings.maxResolution
                        val scaled = BitmapProcessor.downscaleIfNeeded(orientedBmp, maxDim)
                        if (scaled !== orientedBmp && !orientedBmp.isRecycled) {
                            orientedBmp.recycle()
                        }
                        val hist = BitmapProcessor.calculateHistogram(scaled)
                        scaled to hist
                    } else {
                        null
                    }
                }
                if (loaded != null) {
                    setNewInitialBitmap(loaded.first, loaded.second, autoNavigate = true)
                } else {
                    val lang = _uiState.value.settings.appLanguage
                    _uiState.update { it.copy(statusMessage = AppStrings.statusLoadFailed(lang)) }
                }
            } catch (e: Exception) {
                val lang = _uiState.value.settings.appLanguage
                _uiState.update { it.copy(statusMessage = "${AppStrings.statusLoadFailed(lang)}: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private fun applyExifOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return source
        }
        return try {
            val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            if (rotated !== source && !source.isRecycled) {
                source.recycle()
            }
            rotated
        } catch (_: Exception) {
            source
        }
    }

    fun loadCapturedBitmap(bitmap: Bitmap, initialPreset: FilterPreset = FilterPresets.NONE) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val loaded = withContext(Dispatchers.Default) {
                val maxDim = _uiState.value.settings.maxResolution
                val scaled = BitmapProcessor.downscaleIfNeeded(bitmap, maxDim)
                val hist = BitmapProcessor.calculateHistogram(scaled)
                scaled to hist
            }
            setNewInitialBitmap(loaded.first, loaded.second, autoNavigate = true, initialPreset = initialPreset)
            if (initialPreset != FilterPresets.NONE) {
                triggerRender()
            }
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    private fun setNewInitialBitmap(
        bitmap: Bitmap,
        initialHistogram: HistogramData? = null,
        autoNavigate: Boolean = true,
        initialPreset: FilterPreset = FilterPresets.NONE,
        initialTab: EditorTab = EditorTab.FILTERS
    ) {
        renderJob?.cancel()
        undoStack.clear()
        redoStack.clear()
        continuousEditSnapshotted = false
        val copyBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val lang = _uiState.value.settings.appLanguage
        _uiState.update {
            it.copy(
                originalBitmap = bitmap,
                workingBitmap = copyBmp,
                displayBitmap = copyBmp,
                histogram = initialHistogram,
                activeTab = initialTab,
                activeAdjustCategory = AdjustCategory.LIGHT,
                activePreset = initialPreset,
                filterIntensity = 1.0f,
                adjustments = AdjustmentState(),
                colorGrade = ColorGradeState(),
                activeLut = null,
                lutIntensity = 1.0f,
                curves = CurvesState(),
                hsl = HslState(),
                transform = TransformState(),
                selectedCropRatio = CropAspectRatio.FREE,
                pendingCropRect = CropRect(),
                activeFrame = FrameStyle.NONE,
                hasRetouchEdits = false,
                isEraser = false,
                isSplitView = false,
                isComparing = false,
                canUndo = false,
                canRedo = false,
                currentScreen = if (autoNavigate) AppScreen.EDITOR else it.currentScreen,
                statusMessage = if (autoNavigate) AppStrings.statusPhotoReady(lang) else null
            )
        }
    }

    fun beginContinuousEdit() {
        if (!continuousEditSnapshotted) {
            pushHistory(isBitmapMutated = false)
            continuousEditSnapshotted = true
        }
    }

    fun finishContinuousEdit() {
        continuousEditSnapshotted = false
    }

    private fun pushHistory(isBitmapMutated: Boolean = false) {
        val state = _uiState.value
        val original = state.originalBitmap ?: return
        val working = state.workingBitmap ?: return
        val snapshot = EditorHistorySnapshot(
            originalBitmap = original,
            workingBitmap = if (isBitmapMutated) working.copy(Bitmap.Config.ARGB_8888, true) else working,
            adjustments = state.adjustments,
            activePreset = state.activePreset,
            filterIntensity = state.filterIntensity,
            colorGrade = state.colorGrade,
            activeLut = state.activeLut,
            lutIntensity = state.lutIntensity,
            curves = state.curves,
            hsl = state.hsl,
            transform = state.transform,
            activeFrame = state.activeFrame,
            hasRetouchEdits = state.hasRetouchEdits
        )
        undoStack.add(snapshot)
        if (undoStack.size > 10) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        _uiState.update { it.copy(canUndo = true, canRedo = false) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        continuousEditSnapshotted = false
        val currentState = _uiState.value
        val currentOriginal = currentState.originalBitmap ?: return
        val currentWorking = currentState.workingBitmap ?: return

        val redoSnapshot = EditorHistorySnapshot(
            originalBitmap = currentOriginal,
            workingBitmap = currentWorking,
            adjustments = currentState.adjustments,
            activePreset = currentState.activePreset,
            filterIntensity = currentState.filterIntensity,
            colorGrade = currentState.colorGrade,
            activeLut = currentState.activeLut,
            lutIntensity = currentState.lutIntensity,
            curves = currentState.curves,
            hsl = currentState.hsl,
            transform = currentState.transform,
            activeFrame = currentState.activeFrame,
            hasRetouchEdits = currentState.hasRetouchEdits
        )
        redoStack.add(redoSnapshot)
        if (redoStack.size > 10) {
            redoStack.removeAt(0)
        }

        val previous = undoStack.removeAt(undoStack.lastIndex)
        _uiState.update {
            it.copy(
                originalBitmap = previous.originalBitmap,
                workingBitmap = previous.workingBitmap,
                adjustments = previous.adjustments,
                activePreset = previous.activePreset,
                filterIntensity = previous.filterIntensity,
                colorGrade = previous.colorGrade,
                activeLut = previous.activeLut,
                lutIntensity = previous.lutIntensity,
                curves = previous.curves,
                hsl = previous.hsl,
                transform = previous.transform,
                selectedCropRatio = CropAspectRatio.FREE,
                pendingCropRect = CropRect(),
                activeFrame = previous.activeFrame,
                hasRetouchEdits = previous.hasRetouchEdits,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true
            )
        }
        triggerRender()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        continuousEditSnapshotted = false
        val currentState = _uiState.value
        val currentOriginal = currentState.originalBitmap ?: return
        val currentWorking = currentState.workingBitmap ?: return

        val undoSnapshot = EditorHistorySnapshot(
            originalBitmap = currentOriginal,
            workingBitmap = currentWorking,
            adjustments = currentState.adjustments,
            activePreset = currentState.activePreset,
            filterIntensity = currentState.filterIntensity,
            colorGrade = currentState.colorGrade,
            activeLut = currentState.activeLut,
            lutIntensity = currentState.lutIntensity,
            curves = currentState.curves,
            hsl = currentState.hsl,
            transform = currentState.transform,
            activeFrame = currentState.activeFrame,
            hasRetouchEdits = currentState.hasRetouchEdits
        )
        undoStack.add(undoSnapshot)
        if (undoStack.size > 10) {
            undoStack.removeAt(0)
        }

        val next = redoStack.removeAt(redoStack.lastIndex)
        _uiState.update {
            it.copy(
                originalBitmap = next.originalBitmap,
                workingBitmap = next.workingBitmap,
                adjustments = next.adjustments,
                activePreset = next.activePreset,
                filterIntensity = next.filterIntensity,
                colorGrade = next.colorGrade,
                activeLut = next.activeLut,
                lutIntensity = next.lutIntensity,
                curves = next.curves,
                hsl = next.hsl,
                transform = next.transform,
                selectedCropRatio = CropAspectRatio.FREE,
                pendingCropRect = CropRect(),
                activeFrame = next.activeFrame,
                hasRetouchEdits = next.hasRetouchEdits,
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
        }
        triggerRender()
    }

    fun selectTab(tab: EditorTab) {
        continuousEditSnapshotted = false
        _uiState.update {
            it.copy(
                activeTab = tab,
                isSplitView = if (tab == EditorTab.RETOUCH || tab == EditorTab.CROP) false else it.isSplitView
            )
        }
    }

    // --- FILTERS, CUSTOM PRESETS & 3D LUTS ---
    fun setFilterBrowserMode(mode: FilterBrowserMode) {
        continuousEditSnapshotted = false
        _uiState.update { it.copy(filterBrowserMode = mode) }
    }

    fun selectFilter(preset: FilterPreset) {
        continuousEditSnapshotted = false
        pushHistory()
        _uiState.update { it.copy(activePreset = preset) }
        triggerRender()
    }

    fun setFilterIntensity(intensity: Float) {
        beginContinuousEdit()
        _uiState.update { it.copy(filterIntensity = intensity) }
        triggerRender()
    }

    fun selectLut(lut: Lut3D?) {
        continuousEditSnapshotted = false
        val lang = _uiState.value.settings.appLanguage
        pushHistory()
        _uiState.update {
            it.copy(
                activeLut = lut,
                statusMessage = lut?.let { l -> AppStrings.statusLutApplied(l.name, lang) }
            )
        }
        triggerRender()
    }

    fun setLutIntensity(intensity: Float) {
        beginContinuousEdit()
        _uiState.update { it.copy(lutIntensity = intensity.coerceIn(0f, 1f)) }
        triggerRender()
    }

    fun saveCurrentAsCustomPreset(name: String) {
        val state = _uiState.value
        val lang = state.settings.appLanguage
        val newPreset = PresetLutRepository.buildPresetFromState(
            name = name,
            adjustments = state.adjustments,
            filterId = state.activePreset.id,
            filterIntensity = state.filterIntensity,
            colorGrade = state.colorGrade,
            curves = state.curves,
            hsl = state.hsl,
            lutId = state.activeLut?.id,
            lutIntensity = state.lutIntensity
        )
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) {
                PresetLutRepository.saveCustomPreset(getApplication(), newPreset)
            }
            _uiState.update {
                it.copy(
                    customPresets = updated,
                    filterBrowserMode = FilterBrowserMode.PRESETS,
                    statusMessage = AppStrings.statusPresetSaved(newPreset.name, lang)
                )
            }
        }
    }

    fun applyCustomPreset(preset: CustomPreset) {
        continuousEditSnapshotted = false
        pushHistory()
        val lang = _uiState.value.settings.appLanguage
        val matchedFilter = FilterPresets.ALL_PRESETS.firstOrNull { it.id == preset.filterId } ?: FilterPresets.NONE
        val matchedLut = preset.lutId?.let { id ->
            (_uiState.value.availableLuts + BuiltInLuts.ALL).firstOrNull { it.id == id }
        }
        _uiState.update {
            it.copy(
                adjustments = preset.adjustments,
                activePreset = matchedFilter,
                filterIntensity = preset.filterIntensity,
                colorGrade = preset.colorGrade,
                curves = preset.curves,
                hsl = preset.hsl,
                activeLut = matchedLut,
                lutIntensity = preset.lutIntensity,
                statusMessage = AppStrings.statusPresetApplied(preset.name, lang)
            )
        }
        triggerRender()
    }

    fun deleteCustomPreset(presetId: String) {
        val lang = _uiState.value.settings.appLanguage
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) {
                PresetLutRepository.deleteCustomPreset(getApplication(), presetId)
            }
            _uiState.update {
                it.copy(
                    customPresets = updated,
                    statusMessage = AppStrings.statusPresetDeleted(lang)
                )
            }
        }
    }

    fun deleteCustomLut(lutId: String) {
        val lang = _uiState.value.settings.appLanguage
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) {
                PresetLutRepository.deleteCustomLut(getApplication(), lutId)
            }
            val clearedActive = _uiState.value.activeLut?.id == lutId
            _uiState.update {
                it.copy(
                    availableLuts = updated,
                    activeLut = if (clearedActive) null else it.activeLut,
                    statusMessage = AppStrings.statusLutDeleted(lang)
                )
            }
            if (clearedActive) triggerRender()
        }
    }

    fun importPresetOrLut(uri: Uri) {
        val lang = _uiState.value.settings.appLanguage
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val result = withContext(Dispatchers.IO) {
                PresetLutRepository.importFromUri(getApplication(), uri)
            }
            when (result) {
                is PresetLutImportResult.ImportedLut -> {
                    val luts = withContext(Dispatchers.IO) { PresetLutRepository.loadAllLuts(getApplication()) }
                    pushHistory()
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            availableLuts = luts,
                            activeLut = result.lut,
                            lutIntensity = 1.0f,
                            filterBrowserMode = FilterBrowserMode.LUTS,
                            statusMessage = AppStrings.statusLutImported(result.lut.name, lang)
                        )
                    }
                    triggerRender()
                }
                is PresetLutImportResult.ImportedPreset -> {
                    val ctx = getApplication<Application>()
                    val presets = withContext(Dispatchers.IO) { PresetLutRepository.loadCustomPresets(ctx) }
                    val luts = withContext(Dispatchers.IO) { PresetLutRepository.loadAllLuts(ctx) }
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            customPresets = presets,
                            availableLuts = luts,
                            filterBrowserMode = FilterBrowserMode.PRESETS,
                            statusMessage = AppStrings.statusPresetImported(result.preset.name, lang)
                        )
                    }
                    applyCustomPreset(result.preset)
                }
                is PresetLutImportResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            statusMessage = AppStrings.statusImportFailed(result.message, lang)
                        )
                    }
                }
            }
        }
    }

    fun shareCustomPreset(preset: CustomPreset, onIntentReady: (android.content.Intent) -> Unit) {
        val lang = _uiState.value.settings.appLanguage
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    PresetLutRepository.createSharePresetIntent(
                        context = getApplication(),
                        preset = preset,
                        allLuts = _uiState.value.availableLuts
                    )
                }
            }.onSuccess { intent ->
                onIntentReady(intent)
            }.onFailure { err ->
                _uiState.update { it.copy(statusMessage = AppStrings.statusShareFailed(lang, err.localizedMessage)) }
            }
        }
    }

    fun shareLut(lut: Lut3D, onIntentReady: (android.content.Intent) -> Unit) {
        val lang = _uiState.value.settings.appLanguage
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    PresetLutRepository.createShareLutIntent(getApplication(), lut)
                }
            }.onSuccess { intent ->
                onIntentReady(intent)
            }.onFailure { err ->
                _uiState.update { it.copy(statusMessage = AppStrings.statusShareFailed(lang, err.localizedMessage)) }
            }
        }
    }

    // --- ADJUSTMENTS ---
    fun selectAdjustCategory(category: AdjustCategory) {
        continuousEditSnapshotted = false
        _uiState.update { it.copy(activeAdjustCategory = category) }
    }

    fun updateAdjustments(update: (AdjustmentState) -> AdjustmentState) {
        beginContinuousEdit()
        _uiState.update { it.copy(adjustments = update(it.adjustments)) }
        triggerRender()
    }

    fun setBlurFocusPoint(normX: Float, normY: Float) {
        continuousEditSnapshotted = false
        pushHistory()
        _uiState.update { state ->
            state.copy(
                adjustments = state.adjustments.copy(
                    blurFocusX = normX.coerceIn(0f, 1f),
                    blurFocusY = normY.coerceIn(0f, 1f)
                )
            )
        }
        triggerRender()
    }

    fun openBackgroundBlurTool() {
        continuousEditSnapshotted = false
        val lang = _uiState.value.settings.appLanguage
        val needsInitialBlur = _uiState.value.adjustments.backgroundBlur <= 0f
        if (needsInitialBlur) {
            pushHistory()
        }
        _uiState.update { state ->
            state.copy(
                activeTab = EditorTab.ADJUST,
                activeAdjustCategory = AdjustCategory.EFFECTS,
                isSplitView = false,
                adjustments = if (needsInitialBlur) {
                    state.adjustments.copy(backgroundBlur = 50f)
                } else {
                    state.adjustments
                },
                statusMessage = AppStrings.statusBackgroundBlurEnabled(lang)
            )
        }
        if (needsInitialBlur) {
            triggerRender()
        }
    }

    fun resetAdjustments() {
        continuousEditSnapshotted = false
        if (_uiState.value.adjustments.isDefault) return
        pushHistory()
        _uiState.update { it.copy(adjustments = AdjustmentState()) }
        triggerRender()
    }

    /**
     * Analyzes the working bitmap luminance histogram and applies balanced tone & color adjustments.
     */
    fun autoEnhance() {
        val working = _uiState.value.workingBitmap ?: return
        val lang = _uiState.value.settings.appLanguage
        continuousEditSnapshotted = false
        pushHistory()
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val enhanced = withContext(Dispatchers.Default) {
                BitmapProcessor.computeAutoEnhance(working)
            }
            _uiState.update {
                it.copy(
                    adjustments = enhanced,
                    isProcessing = false,
                    statusMessage = AppStrings.statusAutoEnhanceApplied(lang)
                )
            }
            triggerRender()
        }
    }

    // --- CURVES ---
    fun selectCurveChannel(channel: CurveChannel) {
        continuousEditSnapshotted = false
        _uiState.update { it.copy(curves = it.curves.copy(activeChannel = channel)) }
    }

    fun updateActiveCurve(newCurve: SingleCurve) {
        beginContinuousEdit()
        _uiState.update { it.copy(curves = it.curves.updateActiveCurve(newCurve)) }
        triggerRender()
    }

    fun resetActiveCurve() {
        continuousEditSnapshotted = false
        if (_uiState.value.curves.getActiveCurve().isLinear()) return
        pushHistory()
        _uiState.update { it.copy(curves = it.curves.resetActiveCurve()) }
        triggerRender()
    }

    // --- COLOUR GRADE (3-Way Tonal Wheels) ---
    fun selectColorGradeTone(tone: ColorGradeTone) {
        continuousEditSnapshotted = false
        _uiState.update { it.copy(colorGrade = it.colorGrade.copy(activeTone = tone)) }
    }

    fun updateActiveColorWheel(wheelValue: ColorWheelValue) {
        beginContinuousEdit()
        _uiState.update { it.copy(colorGrade = it.colorGrade.updateActiveWheel(wheelValue)) }
        triggerRender()
    }

    fun updateColorGradeBalance(balance: Float) {
        beginContinuousEdit()
        _uiState.update { it.copy(colorGrade = it.colorGrade.copy(balance = balance.coerceIn(-100f, 100f))) }
        triggerRender()
    }

    fun resetActiveColorWheel() {
        continuousEditSnapshotted = false
        if (_uiState.value.colorGrade.getActiveWheel().isDefault) return
        pushHistory()
        _uiState.update { it.copy(colorGrade = it.colorGrade.resetActiveWheel()) }
        triggerRender()
    }

    fun resetAllColorGrade() {
        continuousEditSnapshotted = false
        if (_uiState.value.colorGrade.isDefault) return
        pushHistory()
        _uiState.update { it.copy(colorGrade = ColorGradeState(activeTone = it.colorGrade.activeTone)) }
        triggerRender()
    }

    // --- HSL ---
    fun selectHslChannel(channel: HslColorChannel) {
        continuousEditSnapshotted = false
        _uiState.update { it.copy(hsl = it.hsl.copy(activeChannel = channel)) }
    }

    fun updateHslAdjustment(adjustment: HslChannelAdjustment) {
        beginContinuousEdit()
        _uiState.update { it.copy(hsl = it.hsl.updateActiveAdjustment(adjustment)) }
        triggerRender()
    }

    fun resetActiveHslChannel() {
        continuousEditSnapshotted = false
        if (_uiState.value.hsl.getAdjustment(_uiState.value.hsl.activeChannel).isDefault) return
        pushHistory()
        _uiState.update { it.copy(hsl = it.hsl.resetActiveChannel()) }
        triggerRender()
    }

    // --- RETOUCH TOOLS ---
    fun selectRetouchTool(tool: RetouchType) {
        val current = _uiState.value
        retouchToolSettings[current.activeRetouchTool] = RetouchToolSettings(
            radius = current.brushRadius,
            intensity = current.brushIntensity,
            hardness = current.brushHardness,
            texturePreserve = current.texturePreserve
        )
        val restored = retouchToolSettings[tool] ?: RetouchToolSettings.defaultFor(tool)
        _uiState.update {
            it.copy(
                activeRetouchTool = tool,
                brushRadius = restored.radius,
                brushIntensity = restored.intensity,
                brushHardness = restored.hardness,
                texturePreserve = restored.texturePreserve,
                isEraser = false
            )
        }
    }

    fun setBrushRadius(radius: Float) {
        val clamped = radius.coerceIn(10f, 80f)
        _uiState.update { state ->
            val updated = state.copy(brushRadius = clamped)
            retouchToolSettings[state.activeRetouchTool] = RetouchToolSettings(
                radius = clamped,
                intensity = state.brushIntensity,
                hardness = state.brushHardness,
                texturePreserve = state.texturePreserve
            )
            updated
        }
    }

    fun setBrushIntensity(intensity: Float) {
        val clamped = intensity.coerceIn(0.1f, 1.0f)
        _uiState.update { state ->
            val updated = state.copy(brushIntensity = clamped)
            retouchToolSettings[state.activeRetouchTool] = RetouchToolSettings(
                radius = state.brushRadius,
                intensity = clamped,
                hardness = state.brushHardness,
                texturePreserve = state.texturePreserve
            )
            updated
        }
    }

    fun setBrushHardness(hardness: Float) {
        val clamped = hardness.coerceIn(0.10f, 1.0f)
        _uiState.update { state ->
            val updated = state.copy(brushHardness = clamped)
            retouchToolSettings[state.activeRetouchTool] = RetouchToolSettings(
                radius = state.brushRadius,
                intensity = state.brushIntensity,
                hardness = clamped,
                texturePreserve = state.texturePreserve
            )
            updated
        }
    }

    fun setTexturePreserve(preserve: Float) {
        val clamped = preserve.coerceIn(0f, 0.75f)
        _uiState.update { state ->
            val updated = state.copy(texturePreserve = clamped)
            retouchToolSettings[state.activeRetouchTool] = RetouchToolSettings(
                radius = state.brushRadius,
                intensity = state.brushIntensity,
                hardness = state.brushHardness,
                texturePreserve = clamped
            )
            updated
        }
    }

    fun selectSculptMode(mode: SculptMode) {
        _uiState.update { it.copy(sculptMode = mode, isEraser = false) }
    }

    fun toggleEraser() {
        _uiState.update { it.copy(isEraser = !it.isEraser) }
    }

    fun resetRetouchLayer() {
        val lang = _uiState.value.settings.appLanguage
        continuousEditSnapshotted = false
        viewModelScope.launch {
            bitmapMutex.withLock {
                val orig = _uiState.value.originalBitmap ?: return@withLock
                pushHistory(isBitmapMutated = true)
                val cleanCopy = withContext(Dispatchers.Default) {
                    orig.copy(Bitmap.Config.ARGB_8888, true)
                }
                _uiState.update {
                    it.copy(
                        workingBitmap = cleanCopy,
                        hasRetouchEdits = false,
                        isEraser = false,
                        statusMessage = AppStrings.statusRetouchReset(lang)
                    )
                }
                triggerRender()
            }
        }
    }

    /**
     * Called when the user touches/drags on the canvas while in Retouch mode.
     */
    fun applyRetouchStroke(points: List<StrokePoint>) {
        if (points.isEmpty()) return
        val lang = _uiState.value.settings.appLanguage
        continuousEditSnapshotted = false

        viewModelScope.launch {
            bitmapMutex.withLock {
                val currentWorking = _uiState.value.workingBitmap ?: return@withLock
                pushHistory(isBitmapMutated = true)

                val currentState = _uiState.value
                val stroke = RetouchStroke(
                    type = currentState.activeRetouchTool,
                    points = points,
                    radius = currentState.brushRadius,
                    intensity = currentState.brushIntensity,
                    texturePreserve = currentState.texturePreserve,
                    hardness = currentState.brushHardness,
                    sculptMode = currentState.sculptMode,
                    isEraser = currentState.isEraser
                )

                val updatedWorking = withContext(Dispatchers.Default) {
                    val copy = currentWorking.copy(Bitmap.Config.ARGB_8888, true)
                    BitmapProcessor.applyRetouchStroke(copy, stroke, currentState.originalBitmap)
                    copy
                }

                _uiState.update {
                    it.copy(
                        workingBitmap = updatedWorking,
                        hasRetouchEdits = if (stroke.isEraser) it.hasRetouchEdits else true,
                        recentRetouchPoint = points.lastOrNull(),
                        statusMessage = if (stroke.isEraser) {
                            AppStrings.statusRetouchRestored(lang)
                        } else {
                            AppStrings.statusRetouchApplied(stroke.type, lang)
                        }
                    )
                }
                triggerRender()
            }
        }
    }

    /**
     * Applies full-portrait skin tone detection & bilateral smoothing across the entire image.
     */
    fun applyAutoSkinSmoothing(intensity: Float = 0.70f) {
        val lang = _uiState.value.settings.appLanguage
        val detailPreserve = _uiState.value.texturePreserve
        continuousEditSnapshotted = false

        viewModelScope.launch {
            bitmapMutex.withLock {
                val currentWorking = _uiState.value.workingBitmap ?: return@withLock
                pushHistory(isBitmapMutated = true)
                _uiState.update { it.copy(isProcessing = true) }

                val smoothed = withContext(Dispatchers.Default) {
                    BitmapProcessor.applyPortraitSkinSmoothing(
                        source = currentWorking,
                        intensity = intensity,
                        texturePreserve = detailPreserve
                    )
                }
                _uiState.update {
                    it.copy(
                        workingBitmap = smoothed,
                        hasRetouchEdits = true,
                        isProcessing = false,
                        statusMessage = AppStrings.statusAutoSmoothApplied(lang)
                    )
                }
                triggerRender()
            }
        }
    }

    // --- TRANSFORM & CROP ---
    fun rotateClockwise() {
        continuousEditSnapshotted = false
        applyTransformInternal { it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) }
    }

    fun flipHorizontal() {
        continuousEditSnapshotted = false
        applyTransformInternal { it.copy(flipHorizontal = !it.flipHorizontal) }
    }

    fun flipVertical() {
        continuousEditSnapshotted = false
        applyTransformInternal { it.copy(flipVertical = !it.flipVertical) }
    }

    fun applyCropRatio(ratio: CropAspectRatio) {
        continuousEditSnapshotted = false
        applyTransformInternal { it.copy(cropRatio = ratio) }
    }

    fun selectCropRatio(ratio: CropAspectRatio) {
        val working = _uiState.value.workingBitmap
        val fitted = if (working != null) {
            CropRect.fittedForAspectRatio(working.width, working.height, ratio)
        } else {
            CropRect()
        }
        _uiState.update {
            it.copy(
                selectedCropRatio = ratio,
                pendingCropRect = fitted
            )
        }
    }

    fun updatePendingCropRect(rect: CropRect) {
        _uiState.update { it.copy(pendingCropRect = rect.clamped()) }
    }

    fun resetPendingCropRect() {
        _uiState.update {
            it.copy(
                selectedCropRatio = CropAspectRatio.FREE,
                pendingCropRect = CropRect()
            )
        }
    }

    fun applyPendingCrop() {
        continuousEditSnapshotted = false

        viewModelScope.launch {
            bitmapMutex.withLock {
                val cropRect = _uiState.value.pendingCropRect
                if (cropRect.isFull) return@withLock
                val original = _uiState.value.originalBitmap ?: return@withLock
                val working = _uiState.value.workingBitmap ?: return@withLock
                val lang = _uiState.value.settings.appLanguage
                pushHistory()
                _uiState.update { it.copy(isProcessing = true) }

                val (croppedOrig, croppedWork) = withContext(Dispatchers.Default) {
                    val o = BitmapProcessor.cropBitmap(original, cropRect)
                    val w = BitmapProcessor.cropBitmap(working, cropRect)
                    o to w
                }

                _uiState.update {
                    it.copy(
                        originalBitmap = croppedOrig,
                        workingBitmap = croppedWork,
                        transform = TransformState(),
                        selectedCropRatio = CropAspectRatio.FREE,
                        pendingCropRect = CropRect(),
                        isProcessing = false,
                        statusMessage = AppStrings.statusCropApplied(lang)
                    )
                }
                triggerRender()
            }
        }
    }

    fun selectFrame(frame: FrameStyle) {
        if (_uiState.value.activeFrame == frame) return
        val lang = _uiState.value.settings.appLanguage
        continuousEditSnapshotted = false
        pushHistory()
        _uiState.update {
            it.copy(
                activeFrame = frame,
                statusMessage = AppStrings.statusFrameApplied(frame, lang)
            )
        }
        triggerRender()
    }

    private fun applyTransformInternal(transformUpdate: (TransformState) -> TransformState) {
        val lang = _uiState.value.settings.appLanguage

        viewModelScope.launch {
            bitmapMutex.withLock {
                val original = _uiState.value.originalBitmap ?: return@withLock
                val working = _uiState.value.workingBitmap ?: return@withLock
                val newTransform = transformUpdate(_uiState.value.transform)
                pushHistory()
                _uiState.update { it.copy(isProcessing = true) }

                val (transformedOriginal, transformedWorking) = withContext(Dispatchers.Default) {
                    val origT = BitmapProcessor.transformBitmap(
                        original,
                        rotationDegrees = newTransform.rotationDegrees,
                        flipHorizontal = newTransform.flipHorizontal,
                        flipVertical = newTransform.flipVertical,
                        cropRatio = newTransform.cropRatio
                    )
                    val workT = BitmapProcessor.transformBitmap(
                        working,
                        rotationDegrees = newTransform.rotationDegrees,
                        flipHorizontal = newTransform.flipHorizontal,
                        flipVertical = newTransform.flipVertical,
                        cropRatio = newTransform.cropRatio
                    )
                    origT to workT
                }

                _uiState.update {
                    it.copy(
                        originalBitmap = transformedOriginal,
                        workingBitmap = transformedWorking,
                        transform = TransformState(),
                        selectedCropRatio = CropAspectRatio.FREE,
                        pendingCropRect = CropRect(),
                        isProcessing = false,
                        statusMessage = AppStrings.statusTransformApplied(lang)
                    )
                }
                triggerRender()
            }
        }
    }

    // --- BEFORE / AFTER COMPARE ---
    fun setComparing(isComparing: Boolean) {
        _uiState.update { it.copy(isComparing = isComparing) }
    }

    fun toggleSplitView() {
        _uiState.update { it.copy(isSplitView = !it.isSplitView) }
    }

    fun setSplitRatio(ratio: Float) {
        _uiState.update { it.copy(splitRatio = ratio.coerceIn(0f, 1f)) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun toggleHistogramOverlay() {
        _uiState.update { it.copy(showHistogramOverlay = !it.showHistogramOverlay) }
    }

    // --- RENDER PIPELINE ---
    private fun triggerRender() {
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            val state = _uiState.value
            val working = state.workingBitmap ?: return@launch

            val (rendered, histogram) = withContext(Dispatchers.Default) {
                val r = BitmapProcessor.renderAdjustments(
                    source = working,
                    adjustments = state.adjustments,
                    preset = state.activePreset,
                    filterIntensity = state.filterIntensity,
                    curves = state.curves,
                    hsl = state.hsl,
                    frameStyle = state.activeFrame,
                    colorGrade = state.colorGrade,
                    lut = state.activeLut,
                    lutIntensity = state.lutIntensity
                )
                ensureActive()
                val h = BitmapProcessor.calculateHistogram(r)
                r to h
            }

            _uiState.update {
                it.copy(
                    displayBitmap = rendered,
                    histogram = histogram
                )
            }
        }
    }

    // --- EXPORT & SHARE ---
    fun openSaveModal() {
        val lang = _uiState.value.settings.appLanguage
        if (_uiState.value.displayBitmap != null) {
            _uiState.update { it.copy(showSaveModal = true) }
        } else {
            _uiState.update { it.copy(statusMessage = AppStrings.statusNoPhotoToSave(lang)) }
        }
    }

    fun closeSaveModal() {
        _uiState.update { it.copy(showSaveModal = false) }
    }

    fun closeSaveSuccessModal() {
        _uiState.update { it.copy(showSaveSuccessModal = false) }
    }

    fun saveToGallery(
        customFilename: String? = null,
        format: ExportFormat? = null,
        quality: Int? = null,
        targetMaxDimension: Int? = null
    ) {
        val display = _uiState.value.displayBitmap ?: return
        val currentSettings = _uiState.value.settings
        val lang = currentSettings.appLanguage
        val targetFormat = format ?: currentSettings.exportFormat
        val targetQuality = quality ?: currentSettings.jpegQuality
        val compressFormat = if (targetFormat == ExportFormat.PNG) {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    showSaveModal = false,
                    statusMessage = AppStrings.statusSaving(lang)
                )
            }
            val result = ImageExporter.saveToGallery(
                context = getApplication(),
                bitmap = display,
                customFilename = customFilename,
                format = compressFormat,
                quality = targetQuality,
                targetMaxDimension = targetMaxDimension
            )
            _uiState.update { state ->
                if (result.isSuccess) {
                    val savedInfo = result.getOrNull()
                    state.copy(
                        isProcessing = false,
                        savedImageInfo = savedInfo,
                        showSaveSuccessModal = true,
                        statusMessage = AppStrings.statusSaved(lang)
                    )
                } else {
                    state.copy(
                        isProcessing = false,
                        statusMessage = AppStrings.statusSaveFailed(lang, result.exceptionOrNull()?.message)
                    )
                }
            }
        }
    }

    fun quickSaveToGallery() {
        saveToGallery(customFilename = null, format = null, quality = null, targetMaxDimension = null)
    }

    fun shareImage(onIntentReady: (android.content.Intent) -> Unit) {
        val display = _uiState.value.displayBitmap ?: return
        val lang = _uiState.value.settings.appLanguage
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = AppStrings.statusPreparingShare(lang)) }
            val result = ImageExporter.shareBitmap(getApplication(), display)
            _uiState.update { it.copy(isProcessing = false) }
            result.onSuccess { intent ->
                onIntentReady(intent)
            }.onFailure { err ->
                _uiState.update { it.copy(statusMessage = AppStrings.statusShareFailed(lang, err.localizedMessage)) }
            }
        }
    }
}
