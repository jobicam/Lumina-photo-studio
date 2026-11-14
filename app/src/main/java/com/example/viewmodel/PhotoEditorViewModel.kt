package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.model.AdjustmentState
import com.example.model.AppLanguage
import com.example.model.CropAspectRatio
import com.example.model.CropRect
import com.example.model.CurveChannel
import com.example.model.CurvesState
import com.example.model.ExportFormat
import com.example.model.FilterPreset
import com.example.model.FilterPresets
import com.example.model.FrameStyle
import com.example.model.HistogramData
import com.example.model.HslChannelAdjustment
import com.example.model.HslColorChannel
import com.example.model.HslState
import com.example.model.RetouchStroke
import com.example.model.RetouchType
import com.example.model.SavedImageInfo
import com.example.model.SingleCurve
import com.example.model.StrokePoint
import com.example.model.TransformState
import com.example.model.UpdateStatus
import com.example.model.UserSettings
import com.example.processing.AppUpdateChecker
import com.example.processing.BitmapProcessor
import com.example.processing.ImageExporter
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
    val curves: CurvesState,
    val hsl: HslState,
    val transform: TransformState,
    val activeFrame: FrameStyle = FrameStyle.NONE
)

data class EditorUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val settings: UserSettings = UserSettings(),
    val cacheSizeDisplay: String = "0 KB",
    val originalBitmap: Bitmap? = null,
    val workingBitmap: Bitmap? = null,
    val displayBitmap: Bitmap? = null,
    val activeTab: EditorTab = EditorTab.FILTERS,
    val activePreset: FilterPreset = FilterPresets.NONE,
    val filterIntensity: Float = 1.0f,
    val adjustments: AdjustmentState = AdjustmentState(),
    val curves: CurvesState = CurvesState(),
    val hsl: HslState = HslState(),
    val transform: TransformState = TransformState(),
    val selectedCropRatio: CropAspectRatio = CropAspectRatio.FREE,
    val pendingCropRect: CropRect = CropRect(),
    val activeFrame: FrameStyle = FrameStyle.NONE,
    // Retouch state
    val activeRetouchTool: RetouchType = RetouchType.BLEMISH,
    val brushRadius: Float = 30f,
    val brushIntensity: Float = 0.75f,
    val texturePreserve: Float = 0.30f,
    val isEraser: Boolean = false,
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
    private val bitmapMutex = Mutex()
    private var continuousEditSnapshotted = false

    init {
        // Pre-load default sample photo in the background, stay on Home screen
        loadSamplePhoto(isPortrait = true, autoNavigate = false)
        calculateCacheSize()
        val s = _uiState.value.settings
        if (s.autoCheckUpdates && s.updateFeedUrl.isNotBlank()) {
            checkForUpdates(openDialog = false)
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
            updateFeedUrl = prefs.getString("update_feed_url", "") ?: ""
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
        _uiState.update {
            it.copy(
                updateStatus = UpdateStatus.Checking,
                showUpdateDialog = if (openDialog) true else it.showUpdateDialog
            )
        }
        val feedUrl = _uiState.value.settings.updateFeedUrl
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

    fun saveUpdateFeedUrlAndCheck(url: String) {
        val normalized = AppUpdateChecker.normalizeFeedUrl(url)
        updateSettings { it.copy(updateFeedUrl = normalized) }
        checkForUpdates(openDialog = true)
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
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(statusMessage = "Failed to clear cache") }
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

    fun loadSamplePhoto(isPortrait: Boolean, autoNavigate: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val result = withContext(Dispatchers.IO) {
                val resId = if (isPortrait) R.drawable.sample_portrait else R.drawable.sample_landscape
                val rawBmp = BitmapFactory.decodeResource(getApplication<Application>().resources, resId)
                if (rawBmp != null) {
                    val maxDim = _uiState.value.settings.maxResolution
                    val scaled = BitmapProcessor.downscaleIfNeeded(rawBmp, maxDim)
                    val hist = BitmapProcessor.calculateHistogram(scaled)
                    scaled to hist
                } else {
                    null
                }
            }
            if (result != null) {
                setNewInitialBitmap(result.first, result.second, autoNavigate)
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
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val rawBmp = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (rawBmp != null) {
                        val maxDim = _uiState.value.settings.maxResolution
                        val scaled = BitmapProcessor.downscaleIfNeeded(rawBmp, maxDim)
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
        initialPreset: FilterPreset = FilterPresets.NONE
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
                activeTab = EditorTab.FILTERS,
                activePreset = initialPreset,
                filterIntensity = 1.0f,
                adjustments = AdjustmentState(),
                curves = CurvesState(),
                hsl = HslState(),
                transform = TransformState(),
                selectedCropRatio = CropAspectRatio.FREE,
                pendingCropRect = CropRect(),
                activeFrame = FrameStyle.NONE,
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
            curves = state.curves,
            hsl = state.hsl,
            transform = state.transform,
            activeFrame = state.activeFrame
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
            curves = currentState.curves,
            hsl = currentState.hsl,
            transform = currentState.transform,
            activeFrame = currentState.activeFrame
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
                curves = previous.curves,
                hsl = previous.hsl,
                transform = previous.transform,
                selectedCropRatio = CropAspectRatio.FREE,
                pendingCropRect = CropRect(),
                activeFrame = previous.activeFrame,
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
            curves = currentState.curves,
            hsl = currentState.hsl,
            transform = currentState.transform,
            activeFrame = currentState.activeFrame
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
                curves = next.curves,
                hsl = next.hsl,
                transform = next.transform,
                selectedCropRatio = CropAspectRatio.FREE,
                pendingCropRect = CropRect(),
                activeFrame = next.activeFrame,
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
        }
        triggerRender()
    }

    fun selectTab(tab: EditorTab) {
        continuousEditSnapshotted = false
        _uiState.update { it.copy(activeTab = tab) }
    }

    // --- FILTERS ---
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

    // --- ADJUSTMENTS ---
    fun updateAdjustments(update: (AdjustmentState) -> AdjustmentState) {
        beginContinuousEdit()
        _uiState.update { it.copy(adjustments = update(it.adjustments)) }
        triggerRender()
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
        _uiState.update {
            it.copy(
                activeRetouchTool = tool,
                brushRadius = tool.defaultRadius,
                brushIntensity = tool.defaultIntensity
            )
        }
    }

    fun setBrushRadius(radius: Float) {
        _uiState.update { it.copy(brushRadius = radius) }
    }

    fun setBrushIntensity(intensity: Float) {
        _uiState.update { it.copy(brushIntensity = intensity) }
    }

    fun setTexturePreserve(preserve: Float) {
        _uiState.update { it.copy(texturePreserve = preserve.coerceIn(0f, 0.75f)) }
    }

    fun toggleEraser() {
        _uiState.update { it.copy(isEraser = !it.isEraser) }
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

                val stroke = RetouchStroke(
                    type = _uiState.value.activeRetouchTool,
                    points = points,
                    radius = _uiState.value.brushRadius,
                    intensity = _uiState.value.brushIntensity,
                    texturePreserve = _uiState.value.texturePreserve,
                    isEraser = _uiState.value.isEraser
                )

                val updatedWorking = withContext(Dispatchers.Default) {
                    val copy = currentWorking.copy(Bitmap.Config.ARGB_8888, true)
                    BitmapProcessor.applyRetouchStroke(copy, stroke)
                    copy
                }

                _uiState.update {
                    it.copy(
                        workingBitmap = updatedWorking,
                        recentRetouchPoint = points.lastOrNull(),
                        statusMessage = AppStrings.statusRetouchApplied(stroke.type, lang)
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
        val cropRect = _uiState.value.pendingCropRect
        if (cropRect.isFull) return
        val lang = _uiState.value.settings.appLanguage
        continuousEditSnapshotted = false

        viewModelScope.launch {
            bitmapMutex.withLock {
                val original = _uiState.value.originalBitmap ?: return@withLock
                val working = _uiState.value.workingBitmap ?: return@withLock
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
                    frameStyle = state.activeFrame
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
