package com.example.processing

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import com.example.model.UpdateManifest
import com.example.model.UpdateStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

object AppUpdateChecker {

    const val DEFAULT_UPDATE_FEED_URL = "https://github.com/jobicam/Lumina-photo-studio"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val mapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        Any::class.java
    )

    private val listType = Types.newParameterizedType(
        List::class.java,
        mapType
    )

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Normalizes common GitHub web repo/releases URLs to the GitHub Releases API endpoint
     * while preserving raw JSON/version.json URLs as-is.
     */
    fun normalizeFeedUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""

        // Convert GitHub file viewer URL (.../blob/branch/path) to raw.githubusercontent.com
        val githubBlobRegex = Regex(
            "^https?://(?:www\\.)?github\\.com/([^/]+)/([^/]+)/blob/([^/]+)/(.+)$",
            RegexOption.IGNORE_CASE
        )
        githubBlobRegex.matchEntire(trimmed)?.let { blobMatch ->
            val owner = blobMatch.groupValues[1]
            val repo = blobMatch.groupValues[2]
            val branch = blobMatch.groupValues[3]
            val path = blobMatch.groupValues[4]
            return "https://raw.githubusercontent.com/$owner/$repo/$branch/$path"
        }

        val githubRepoRegex = Regex(
            "^https?://(?:www\\.)?github\\.com/([^/]+)/([^/]+?)(?:\\.git)?(?:/(?:releases(?:/latest|/tag/[^/]+)?|tags))?/?$",
            RegexOption.IGNORE_CASE
        )
        val match = githubRepoRegex.matchEntire(trimmed)
        if (match != null) {
            val owner = match.groupValues[1]
            val repo = match.groupValues[2]
            return "https://api.github.com/repos/$owner/$repo/releases/latest"
        }
        return trimmed
    }

    fun containsTemplatePlaceholder(url: String): Boolean {
        val lower = url.lowercase()
        return url.contains('<') ||
            url.contains('>') ||
            lower.contains("{owner}") ||
            lower.contains("{repo}") ||
            lower.contains("your_username") ||
            lower.contains("your_repo")
    }

    /**
     * Parses either a custom update manifest JSON:
     * { "versionCode": 11, "versionName": "2.5.0", "releaseNotes": "...", "downloadUrl": "...", "mandatory": false }
     * or a GitHub Releases / Tags API JSON object or array payload.
     */
    fun parseManifestJson(json: String, fallbackReleaseUrl: String = ""): UpdateManifest? {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val map: Map<String, Any?> = if (trimmed.startsWith("[")) {
                val listAdapter = moshi.adapter<List<Map<String, Any?>>>(listType)
                listAdapter.fromJson(trimmed)?.firstOrNull() ?: return null
            } else {
                val adapter = moshi.adapter<Map<String, Any?>>(mapType)
                adapter.fromJson(trimmed) ?: return null
            }

            val rawVersionCode = (map["versionCode"] as? Number)?.toLong()
                ?: (map["latestVersionCode"] as? Number)?.toLong()
                ?: (map["version_code"] as? Number)?.toLong()
                ?: 0L

            val rawVersionName = (map["versionName"] as? String)
                ?: (map["latestVersionName"] as? String)
                ?: (map["version_name"] as? String)
                ?: (map["tag_name"] as? String)
                ?: (map["name"] as? String)
                ?: (map["version"] as? String)
                ?: ""

            val normalizedName = rawVersionName.trim().removePrefix("v").removePrefix("V")
            if (rawVersionCode <= 0L && normalizedName.isBlank()) return null

            val notes = (map["releaseNotes"] as? String)
                ?: (map["release_notes"] as? String)
                ?: (map["body"] as? String)
                ?: (map["notes"] as? String)
                ?: ""

            val rawAssets = map["assets"] as? List<*>
            val apkAssetUrl = rawAssets?.asSequence()
                ?.mapNotNull { it as? Map<*, *> }
                ?.mapNotNull { asset ->
                    val assetUrl = (asset["browser_download_url"] as? String)?.trim().orEmpty()
                    val cleanAssetUrl = assetUrl.substringBefore('?').substringBefore('#')
                    val assetName = (asset["name"] as? String).orEmpty()
                    if (assetUrl.isNotBlank() &&
                        (assetName.endsWith(".apk", ignoreCase = true) ||
                            cleanAssetUrl.endsWith(".apk", ignoreCase = true))
                    ) {
                        assetUrl
                    } else {
                        null
                    }
                }
                ?.firstOrNull()

            val downloadUrl = apkAssetUrl
                ?: (map["downloadUrl"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: (map["download_url"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: (map["releaseUrl"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: (map["html_url"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: fallbackReleaseUrl.trim().takeIf { it.isNotEmpty() }
                ?: (map["url"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: ""

            val mandatory = (map["mandatory"] as? Boolean)
                ?: (map["isMandatory"] as? Boolean)
                ?: (map["forceUpdate"] as? Boolean)
                ?: false

            UpdateManifest(
                versionCode = rawVersionCode,
                versionName = normalizedName,
                releaseNotes = notes.trim(),
                downloadUrl = downloadUrl.trim(),
                isMandatory = mandatory
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Serializes a valid version.json manifest for hosting on GitHub/CDN so older installs
     * (e.g. v2.4.0) can detect and update to the latest build.
     */
    fun buildVersionManifestJson(
        versionCode: Long = BuildConfig.VERSION_CODE.toLong(),
        versionName: String = BuildConfig.VERSION_NAME,
        releaseNotes: String = "Added Motion Blur (Scene/Background/Subject), optical Depth Lens Bokeh Blur, 3-way Colour Grade tonal wheels, and custom Presets & 3D .cube LUT import/sharing.",
        downloadUrl: String = ""
    ): String {
        val adapter = moshi.adapter<Map<String, Any>>(mapType).indent("  ")
        val payload = linkedMapOf<String, Any>(
            "versionCode" to versionCode,
            "versionName" to versionName,
            "releaseNotes" to releaseNotes,
            "downloadUrl" to downloadUrl,
            "mandatory" to false
        )
        return adapter.toJson(payload)
    }

    /**
     * Compares two semantic version strings (e.g. "2.4.0" vs "2.3.1", "v2.10.0" vs "2.4.0").
     * Returns > 0 if candidate > current, < 0 if candidate < current, 0 if equal.
     */
    fun compareSemanticVersions(candidate: String, current: String): Int {
        val a = parseVersionParts(candidate)
        val b = parseVersionParts(current)
        val maxLen = maxOf(a.size, b.size)
        for (i in 0 until maxLen) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av != bv) return av.compareTo(bv)
        }
        return 0
    }

    private fun parseVersionParts(raw: String): List<Int> {
        val numericSegment = Regex("""\d+(?:\.\d+)*""").find(raw.trim())?.value
            ?: return listOf(0)
        return numericSegment.split('.').map { part ->
            part.toIntOrNull() ?: 0
        }
    }

    fun isUpdateAvailable(
        currentVersionCode: Long,
        currentVersionName: String,
        manifest: UpdateManifest
    ): Boolean {
        if (manifest.versionCode > 0L && manifest.versionCode > currentVersionCode) {
            return true
        }
        if (manifest.versionName.isNotBlank() &&
            compareSemanticVersions(manifest.versionName, currentVersionName) > 0
        ) {
            return true
        }
        return false
    }

    fun getInstalledVersion(context: Context): Pair<String, Long> {
        return try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = PackageInfoCompat.getLongVersionCode(pkgInfo)
            val name = pkgInfo.versionName ?: BuildConfig.VERSION_NAME
            name to code
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME to BuildConfig.VERSION_CODE.toLong()
        }
    }

    @Suppress("DEPRECATION")
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks for an app update.
     * - When [feedUrl] is configured (https/http JSON endpoint or GitHub Releases API), fetches the
     *   remote manifest via OkHttp, parses it with Moshi, and compares versions against the installed APK.
     * - When [feedUrl] is empty, verifies the installed APK version via PackageManager and reports
     *   up-to-date status along with local build verification details.
     */
    fun checkForUpdate(context: Context, feedUrl: String = DEFAULT_UPDATE_FEED_URL): UpdateStatus {
        val now = System.currentTimeMillis()
        val (currentName, currentCode) = getInstalledVersion(context)
        val trimmedUrl = normalizeFeedUrl(feedUrl)

        if (trimmedUrl.isEmpty()) {
            return UpdateStatus.UpToDate(
                currentVersionName = currentName,
                currentVersionCode = currentCode,
                checkedAtMillis = now,
                sourceLabel = "On-Device Build Verification"
            )
        }

        if (containsTemplatePlaceholder(trimmedUrl)) {
            return UpdateStatus.Error(
                message = "Invalid update feed configuration.",
                checkedAtMillis = now
            )
        }

        if (!trimmedUrl.startsWith("https://", ignoreCase = true) &&
            !trimmedUrl.startsWith("http://", ignoreCase = true)
        ) {
            return UpdateStatus.Error(
                message = "Update feed URL must start with https:// or http://",
                checkedAtMillis = now
            )
        }

        if (!isNetworkAvailable(context)) {
            return UpdateStatus.Error(
                message = "No internet connection detected",
                checkedAtMillis = now
            )
        }

        return try {
            val primaryResult = fetchManifest(trimmedUrl, currentName)
            val resolvedManifest = if (primaryResult.httpCode == 404) {
                val fallback = tryGitHub404Fallback(trimmedUrl, currentName)
                if (fallback != null) {
                    fallback
                } else {
                    val repoAccessible = checkGitHubRepoAccessible(trimmedUrl, currentName)
                    return UpdateStatus.Error(
                        message = buildFriendly404Message(trimmedUrl, repoAccessible),
                        checkedAtMillis = now
                    )
                }
            } else if (!primaryResult.isSuccessful) {
                return UpdateStatus.Error(
                    message = "Update server returned HTTP ${primaryResult.httpCode}",
                    checkedAtMillis = now
                )
            } else {
                parseManifestJson(primaryResult.body)
                    ?: return UpdateStatus.Error(
                        message = "Invalid update manifest format",
                        checkedAtMillis = now
                    )
            }

            if (isUpdateAvailable(currentCode, currentName, resolvedManifest)) {
                UpdateStatus.UpdateAvailable(
                    currentVersionName = currentName,
                    currentVersionCode = currentCode,
                    latestVersionName = resolvedManifest.versionName.ifBlank { currentName },
                    latestVersionCode = resolvedManifest.versionCode,
                    releaseNotes = resolvedManifest.releaseNotes,
                    downloadUrl = resolvedManifest.downloadUrl,
                    isMandatory = resolvedManifest.isMandatory,
                    checkedAtMillis = now
                )
            } else {
                UpdateStatus.UpToDate(
                    currentVersionName = currentName,
                    currentVersionCode = currentCode,
                    checkedAtMillis = now,
                    sourceLabel = "Remote Manifest Verified"
                )
            }
        } catch (e: Exception) {
            UpdateStatus.Error(
                message = e.localizedMessage ?: "Unable to reach update server",
                checkedAtMillis = now
            )
        }
    }

    private data class HttpFetchResult(
        val httpCode: Int,
        val isSuccessful: Boolean,
        val body: String
    )

    private fun fetchManifest(url: String, currentName: String): HttpFetchResult {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "LuminaPhoto/$currentName")
            .get()
            .build()

        return client.newCall(request).execute().use { response ->
            HttpFetchResult(
                httpCode = response.code,
                isSuccessful = response.isSuccessful,
                body = response.body?.string().orEmpty()
            )
        }
    }

    private fun tryGitHub404Fallback(url: String, currentName: String): UpdateManifest? {
        val ghLatestRegex = Regex(
            "^https?://api\\.github\\.com/repos/([^/]+)/([^/]+)/releases/latest/?$",
            RegexOption.IGNORE_CASE
        )
        val match = ghLatestRegex.matchEntire(url) ?: return null
        val owner = match.groupValues[1]
        val repo = match.groupValues[2]
        val releasesPageUrl = "https://github.com/$owner/$repo/releases"

        // 1. Check recent releases (includes pre-releases)
        val releasesRes = fetchManifest("https://api.github.com/repos/$owner/$repo/releases?per_page=1", currentName)
        if (releasesRes.isSuccessful) {
            parseManifestJson(releasesRes.body, fallbackReleaseUrl = releasesPageUrl)?.let { return it }
        }

        // 2. Check repository tags (if tags exist without a formal Release)
        val tagsRes = fetchManifest("https://api.github.com/repos/$owner/$repo/tags?per_page=1", currentName)
        if (tagsRes.isSuccessful) {
            parseManifestJson(tagsRes.body, fallbackReleaseUrl = releasesPageUrl)?.let { return it }
        }

        // 3. Check direct release download asset (works even if GitHub API rate-limits or differs)
        val directAssetRes = fetchManifest("https://github.com/$owner/$repo/releases/latest/download/version.json", currentName)
        if (directAssetRes.isSuccessful) {
            parseManifestJson(directAssetRes.body, fallbackReleaseUrl = releasesPageUrl)?.let { return it }
        }

        // 4. Check raw version.json across HEAD / main / master branches
        val rawCandidates = listOf(
            "https://raw.githubusercontent.com/$owner/$repo/HEAD/version.json",
            "https://raw.githubusercontent.com/$owner/$repo/main/version.json",
            "https://raw.githubusercontent.com/$owner/$repo/master/version.json"
        )
        for (rawUrl in rawCandidates) {
            val rawRes = fetchManifest(rawUrl, currentName)
            if (rawRes.isSuccessful) {
                parseManifestJson(rawRes.body, fallbackReleaseUrl = releasesPageUrl)?.let { return it }
            }
        }

        return null
    }

    private fun checkGitHubRepoAccessible(url: String, currentName: String): Boolean? {
        val ghRepoRegex = Regex(
            "^https?://api\\.github\\.com/repos/([^/]+)/([^/]+)/.*$",
            RegexOption.IGNORE_CASE
        )
        val match = ghRepoRegex.matchEntire(url) ?: return null
        val owner = match.groupValues[1]
        val repo = match.groupValues[2]
        return runCatching {
            fetchManifest("https://api.github.com/repos/$owner/$repo", currentName).isSuccessful
        }.getOrNull()
    }

    internal fun buildFriendly404Message(url: String, repoAccessible: Boolean? = null): String {
        val ghRepoRegex = Regex(
            "^https?://(?:api\\.github\\.com/repos|raw\\.githubusercontent\\.com)/([^/]+)/([^/]+)/.*$",
            RegexOption.IGNORE_CASE
        )
        val match = ghRepoRegex.matchEntire(url)
        return if (match != null) {
            val owner = match.groupValues[1]
            val repo = match.groupValues[2]
            when (repoAccessible) {
                true -> "Repository $owner/$repo is public, but no GitHub Release, tag, or version.json is published yet (HTTP 404). Run the release workflow or publish a Release on GitHub."
                false -> "Repository $owner/$repo is Private or not found (HTTP 404). Make the GitHub repository Public so the app can check releases without login."
                null -> "No published GitHub release found for $owner/$repo (HTTP 404). Make sure the repository is Public and has a published Release."
            }
        } else {
            "Update manifest not found at the configured release feed (HTTP 404)."
        }
    }

    fun isDirectApkUrl(url: String): Boolean {
        val cleanUrl = url.trim().substringBefore('?').substringBefore('#')
        return cleanUrl.endsWith(".apk", ignoreCase = true)
    }

    fun isValidApkFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4L) return false
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(4)
                val read = input.read(header)
                read == 4 &&
                    header[0] == 0x50.toByte() && // 'P'
                    header[1] == 0x4B.toByte() && // 'K'
                    header[2] == 0x03.toByte() &&
                    header[3] == 0x04.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        } else {
            true
        }
    }

    fun createManageUnknownSourcesIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun createInstallApkIntent(context: Context, apkFile: File): Intent {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun cleanupPartialApkDownloads(context: Context) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates")
            if (updatesDir.exists()) {
                updatesDir.listFiles()?.forEach { f ->
                    if (f.name.endsWith(".part")) f.delete()
                }
            }
        }
    }

    fun downloadApkUpdate(
        context: Context,
        downloadUrl: String,
        versionName: String = "",
        isCancelled: () -> Boolean = { false },
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<File> {
        val trimmedUrl = downloadUrl.trim()
        if (!trimmedUrl.startsWith("https://", ignoreCase = true) &&
            !trimmedUrl.startsWith("http://", ignoreCase = true)
        ) {
            return Result.failure(IllegalArgumentException("Invalid APK download URL"))
        }

        if (!isNetworkAvailable(context)) {
            return Result.failure(IllegalStateException("No internet connection detected"))
        }

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        // Remove previous update files to prevent cache bloat
        updatesDir.listFiles()?.forEach { it.delete() }

        val safeVer = versionName.trim().replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "latest" }
        val partFile = File(updatesDir, "lumina-update-$safeVer.apk.part")
        val targetFile = File(updatesDir, "lumina-update-$safeVer.apk")

        return try {
            val request = Request.Builder()
                .url(trimmedUrl)
                .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                .header("User-Agent", "LuminaPhoto/${BuildConfig.VERSION_NAME}")
                .get()
                .build()

            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IllegalStateException("Download failed (HTTP ${response.code})"))
                }
                val contentType = response.header("Content-Type").orEmpty().lowercase()
                if (contentType.contains("text/html")) {
                    return Result.failure(
                        IllegalStateException("Release link returned a web page instead of an APK file.")
                    )
                }

                val body = response.body
                    ?: return Result.failure(IllegalStateException("Empty download response"))

                val totalBytes = body.contentLength()
                var downloaded = 0L
                var lastReportedBytes = 0L
                val buffer = ByteArray(16 * 1024)

                onProgress(0L, totalBytes)

                body.byteStream().use { input ->
                    FileOutputStream(partFile).use { output ->
                        while (true) {
                            if (isCancelled()) {
                                throw CancellationException("Download cancelled")
                            }
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (downloaded - lastReportedBytes >= 32 * 1024L ||
                                (totalBytes > 0L && downloaded >= totalBytes)
                            ) {
                                lastReportedBytes = downloaded
                                onProgress(downloaded, totalBytes)
                            }
                        }
                        output.flush()
                    }
                }
                onProgress(downloaded, totalBytes)
            }

            if (!isValidApkFile(partFile)) {
                partFile.delete()
                return Result.failure(
                    IllegalStateException("Downloaded file is not a valid Android APK package.")
                )
            }

            if (targetFile.exists()) targetFile.delete()
            if (!partFile.renameTo(targetFile)) {
                partFile.copyTo(targetFile, overwrite = true)
                partFile.delete()
            }

            Result.success(targetFile)
        } catch (ce: CancellationException) {
            partFile.delete()
            Result.failure(ce)
        } catch (e: Exception) {
            partFile.delete()
            Result.failure(e)
        }
    }
}
