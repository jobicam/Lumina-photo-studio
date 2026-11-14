package com.example.model

data class UpdateManifest(
    val versionCode: Long = 0L,
    val versionName: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val isMandatory: Boolean = false
)

sealed class UpdateStatus {
    data object Idle : UpdateStatus()
    data object Checking : UpdateStatus()

    data class UpToDate(
        val currentVersionName: String,
        val currentVersionCode: Long,
        val checkedAtMillis: Long,
        val sourceLabel: String
    ) : UpdateStatus()

    data class UpdateAvailable(
        val currentVersionName: String,
        val currentVersionCode: Long,
        val latestVersionName: String,
        val latestVersionCode: Long,
        val releaseNotes: String,
        val downloadUrl: String,
        val isMandatory: Boolean,
        val checkedAtMillis: Long
    ) : UpdateStatus()

    data class Error(
        val message: String,
        val checkedAtMillis: Long
    ) : UpdateStatus()
}
