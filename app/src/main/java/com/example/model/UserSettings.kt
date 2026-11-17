package com.example.model

import com.example.processing.AppUpdateChecker

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
    val updateFeedUrl: String = AppUpdateChecker.DEFAULT_UPDATE_FEED_URL
)
