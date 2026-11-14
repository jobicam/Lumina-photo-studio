package com.example.model

import android.net.Uri

data class SavedImageInfo(
    val uri: Uri,
    val filename: String,
    val albumPath: String = "Pictures/Lumina",
    val mimeType: String,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long = 0L,
    val savedTimestamp: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() = formatFileSize(fileSizeBytes)

    val dimensionsText: String
        get() = "$width × $height px"

    companion object {
        fun formatFileSize(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.2f MB", bytes / (1024f * 1024f))
                bytes >= 1024 -> "${bytes / 1024} KB"
                bytes > 0 -> "$bytes B"
                else -> "Saved"
            }
        }
    }
}
