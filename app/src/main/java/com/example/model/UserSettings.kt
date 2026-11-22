package com.example.model

import com.example.processing.AppUpdateChecker
import com.example.viewmodel.EditorTab

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    SYSTEM("system", "System Default", "តាមប្រព័ន្ធ"),
    ENGLISH("en", "English", "English"),
    KHMER("km", "Khmer", "ភាសាខ្មែរ")
}

enum class ExportFormat(val displayName: String, val extension: String, val mime: String) {
    JPEG("JPEG (Compressed)", "jpg", "image/jpeg"),
    PNG("PNG (Lossless)", "png", "image/png")
}

data class UserSettings(
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val exportFormat: ExportFormat = ExportFormat.JPEG,
    val jpegQuality: Int = 95,
    val maxResolution: Int = 2048,
    val defaultFrontCamera: Boolean = false,
    val showGridOverlay: Boolean = false,
    val hapticFeedback: Boolean = true,
    val autoSaveDrafts: Boolean = true,
    val autoCheckUpdates: Boolean = true,
    val updateFeedUrl: String = AppUpdateChecker.DEFAULT_UPDATE_FEED_URL,
    val editorTabOrder: List<EditorTab> = DEFAULT_EDITOR_TAB_ORDER,
    val defaultCropGridStyle: CropGridStyle = CropGridStyle.RULE_OF_THIRDS
) {
    companion object {
        val DEFAULT_EDITOR_TAB_ORDER: List<EditorTab> = listOf(
            EditorTab.FILTERS,
            EditorTab.CROP,
            EditorTab.ADJUST,
            EditorTab.RETOUCH,
            EditorTab.COLOR_GRADE,
            EditorTab.CURVES,
            EditorTab.HSL
        )

        fun normalizeTabOrder(raw: List<EditorTab>): List<EditorTab> {
            val distinct = raw.distinct().toMutableList()
            DEFAULT_EDITOR_TAB_ORDER.forEach { tab ->
                if (tab !in distinct) distinct.add(tab)
            }
            return distinct
        }

        fun serializeTabOrder(tabs: List<EditorTab>): String =
            normalizeTabOrder(tabs).joinToString(",") { it.name }

        fun parseTabOrder(raw: String?): List<EditorTab> {
            if (raw.isNullOrBlank()) return DEFAULT_EDITOR_TAB_ORDER
            val parsed = raw.split(",")
                .mapNotNull { token ->
                    EditorTab.values().firstOrNull { it.name == token.trim() }
                }
            return normalizeTabOrder(parsed)
        }
    }
}
