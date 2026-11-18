package com.example.processing

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.model.AdjustmentState
import com.example.model.ColorGradeState
import com.example.model.CurvesState
import com.example.model.CustomPreset
import com.example.model.HslState
import com.example.model.Lut3D
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.util.UUID

sealed class PresetLutImportResult {
    data class ImportedPreset(val preset: CustomPreset, val embeddedLut: Lut3D? = null) : PresetLutImportResult()
    data class ImportedLut(val lut: Lut3D) : PresetLutImportResult()
    data class Error(val message: String) : PresetLutImportResult()
}

data class PortablePresetBundle(
    val formatVersion: Int = 1,
    val app: String = "LuminaPhotoStudio",
    val preset: CustomPreset,
    val embeddedLutCubeText: String? = null,
    val embeddedLutName: String? = null
)

object PresetLutRepository {

    private const val PRESETS_DIR = "lumina_presets"
    private const val PRESETS_FILE = "custom_presets.json"
    private const val LUTS_DIR = "lumina_luts"
    private const val SHARE_CACHE_DIR = "shared_presets"
    private const val STALE_SHARE_MAX_AGE_MS = 24L * 60L * 60L * 1000L

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val presetListType by lazy {
        Types.newParameterizedType(List::class.java, CustomPreset::class.java)
    }

    private val presetListAdapter by lazy {
        moshi.adapter<List<CustomPreset>>(presetListType)
    }

    private val bundleAdapter by lazy {
        moshi.adapter(PortablePresetBundle::class.java)
    }

    private val singlePresetAdapter by lazy {
        moshi.adapter(CustomPreset::class.java)
    }

    fun loadCustomPresets(context: Context): List<CustomPreset> {
        return try {
            val file = File(File(context.filesDir, PRESETS_DIR), PRESETS_FILE)
            if (!file.exists()) return emptyList()
            val json = file.readText()
            presetListAdapter.fromJson(json).orEmpty().sortedByDescending { it.createdAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustomPreset(context: Context, preset: CustomPreset): List<CustomPreset> {
        val existing = loadCustomPresets(context).filterNot { it.id == preset.id }
        val updated = listOf(preset) + existing
        persistCustomPresets(context, updated)
        return updated
    }

    fun deleteCustomPreset(context: Context, presetId: String): List<CustomPreset> {
        val updated = loadCustomPresets(context).filterNot { it.id == presetId }
        persistCustomPresets(context, updated)
        return updated
    }

    private fun persistCustomPresets(context: Context, presets: List<CustomPreset>) {
        val dir = File(context.filesDir, PRESETS_DIR).apply { mkdirs() }
        val file = File(dir, PRESETS_FILE)
        file.writeText(presetListAdapter.toJson(presets))
    }

    fun loadAllLuts(context: Context): List<Lut3D> {
        val customDir = File(context.filesDir, LUTS_DIR)
        val customLuts = if (customDir.exists()) {
            customDir.listFiles()
                ?.filter { it.isFile && it.extension.equals("cube", ignoreCase = true) }
                ?.sortedByDescending { it.lastModified() }
                ?.mapNotNull { file ->
                    try {
                        val id = file.nameWithoutExtension
                        CubeLutParser.parseCubeLut(
                            id = id,
                            fallbackName = file.nameWithoutExtension.removePrefix("lut_"),
                            content = file.readText(),
                            isBuiltIn = false
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                .orEmpty()
        } else {
            emptyList()
        }
        return customLuts + BuiltInLuts.ALL
    }

    fun saveCustomLut(context: Context, lut: Lut3D): Lut3D {
        val dir = File(context.filesDir, LUTS_DIR).apply { mkdirs() }
        val file = File(dir, "${lut.id}.cube")
        file.writeText(CubeLutParser.serializeCubeLut(lut))
        return lut
    }

    fun deleteCustomLut(context: Context, lutId: String): List<Lut3D> {
        val file = File(File(context.filesDir, LUTS_DIR), "$lutId.cube")
        if (file.exists()) file.delete()
        return loadAllLuts(context)
    }

    fun importFromUri(context: Context, uri: Uri): PresetLutImportResult {
        return try {
            val displayName = resolveDisplayName(context, uri) ?: "Imported"
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return PresetLutImportResult.Error("Unable to read selected file")

            importFromText(context, displayName, content)
        } catch (e: Exception) {
            PresetLutImportResult.Error(e.message ?: "Import failed")
        }
    }

    fun importFromText(context: Context, fileName: String, content: String): PresetLutImportResult {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return PresetLutImportResult.Error("Selected file is empty")
        }

        val lowerName = fileName.lowercase()
        val isJsonFile = lowerName.endsWith(".json") || trimmed.startsWith("{")
        val looksLikeCube = !isJsonFile && (
            lowerName.endsWith(".cube") ||
                trimmed.contains("LUT_3D_SIZE", ignoreCase = true)
            )

        if (looksLikeCube) {
            val cleanName = fileName.substringBeforeLast('.').ifBlank { "Imported 3D LUT" }
            val lutId = "custom_${UUID.randomUUID().toString().take(8)}"
            val parsed = CubeLutParser.parseCubeLut(
                id = lutId,
                fallbackName = cleanName,
                content = trimmed,
                isBuiltIn = false
            )
            saveCustomLut(context, parsed)
            return PresetLutImportResult.ImportedLut(parsed)
        }

        // Otherwise attempt JSON preset bundle or plain CustomPreset JSON
        val bundle = try {
            bundleAdapter.fromJson(trimmed)
        } catch (_: Exception) {
            null
        }
        if (bundle != null) {
            var importedLut: Lut3D? = null
            var targetLutId = bundle.preset.lutId
            if (!bundle.embeddedLutCubeText.isNullOrBlank()) {
                val newLutId = "custom_${UUID.randomUUID().toString().take(8)}"
                val lutName = bundle.embeddedLutName
                    ?: "${bundle.preset.name} LUT"
                val parsedLut = CubeLutParser.parseCubeLut(
                    id = newLutId,
                    fallbackName = lutName,
                    content = bundle.embeddedLutCubeText,
                    isBuiltIn = false
                )
                saveCustomLut(context, parsedLut)
                importedLut = parsedLut
                targetLutId = parsedLut.id
            }

            val importedPreset = bundle.preset.copy(
                id = "preset_${UUID.randomUUID().toString().take(8)}",
                createdAtMillis = System.currentTimeMillis(),
                lutId = targetLutId
            )
            saveCustomPreset(context, importedPreset)
            return PresetLutImportResult.ImportedPreset(importedPreset, importedLut)
        }

        val single = try {
            singlePresetAdapter.fromJson(trimmed)
        } catch (_: Exception) {
            null
        }
        if (single != null) {
            val importedPreset = single.copy(
                id = "preset_${UUID.randomUUID().toString().take(8)}",
                createdAtMillis = System.currentTimeMillis()
            )
            saveCustomPreset(context, importedPreset)
            return PresetLutImportResult.ImportedPreset(importedPreset)
        }

        return PresetLutImportResult.Error("Unsupported file format. Choose a .cube 3D LUT or .json preset.")
    }

    fun exportPresetToJson(preset: CustomPreset, activeLut: Lut3D?): String {
        val cubeText = if (activeLut != null && !activeLut.isBuiltIn) {
            CubeLutParser.serializeCubeLut(activeLut)
        } else {
            null
        }
        val bundle = PortablePresetBundle(
            preset = preset,
            embeddedLutCubeText = cubeText,
            embeddedLutName = activeLut?.name
        )
        return bundleAdapter.toJson(bundle)
    }

    fun createSharePresetIntent(
        context: Context,
        preset: CustomPreset,
        allLuts: List<Lut3D>
    ): Intent {
        pruneStaleShareFiles(context)
        val shareDir = File(context.cacheDir, SHARE_CACHE_DIR).apply { mkdirs() }
        val safeName = sanitizeFilename(preset.name).ifBlank { "LuminaPreset" }
        val outFile = File(shareDir, "${safeName}.lumina.json")
        val associatedLut = preset.lutId?.let { id -> allLuts.firstOrNull { it.id == id } }
        outFile.writeText(exportPresetToJson(preset, associatedLut))

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Lumina Preset: ${preset.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Share Preset: ${preset.name}")
    }

    fun createShareLutIntent(context: Context, lut: Lut3D): Intent {
        pruneStaleShareFiles(context)
        val shareDir = File(context.cacheDir, SHARE_CACHE_DIR).apply { mkdirs() }
        val safeName = sanitizeFilename(lut.name).ifBlank { "LuminaLUT" }
        val outFile = File(shareDir, "${safeName}.cube")
        outFile.writeText(CubeLutParser.serializeCubeLut(lut))

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Lumina 3D LUT: ${lut.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Share 3D LUT: ${lut.name}")
    }

    private fun pruneStaleShareFiles(context: Context) {
        val shareDir = File(context.cacheDir, SHARE_CACHE_DIR)
        if (!shareDir.exists()) return
        val cutoff = System.currentTimeMillis() - STALE_SHARE_MAX_AGE_MS
        shareDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    private fun sanitizeFilename(raw: String): String =
        raw.trim().replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(40)

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: uri.lastPathSegment
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    fun buildPresetFromState(
        name: String,
        adjustments: AdjustmentState,
        filterId: String,
        filterIntensity: Float,
        colorGrade: ColorGradeState,
        curves: CurvesState,
        hsl: HslState,
        lutId: String?,
        lutIntensity: Float
    ): CustomPreset {
        return CustomPreset(
            id = "preset_${UUID.randomUUID().toString().take(8)}",
            name = name.trim().ifBlank { "Custom Look" },
            createdAtMillis = System.currentTimeMillis(),
            adjustments = adjustments,
            filterId = filterId,
            filterIntensity = filterIntensity,
            colorGrade = colorGrade,
            curves = curves,
            hsl = hsl,
            lutId = lutId,
            lutIntensity = lutIntensity
        )
    }
}
