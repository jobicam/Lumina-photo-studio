package com.example.processing

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import com.example.model.UpdateManifest
import com.example.model.UpdateStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object AppUpdateChecker {

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
        releaseNotes: String = "Photo Zoom In/Out (1.0x-6.0x) with two-finger retouch pan, automated skin blemish detection, and upgraded spot healing.",
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
    fun checkForUpdate(context: Context, feedUrl: String): UpdateStatus {
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
                message = "Replace <owner> and <repo> with your actual GitHub username and repository name, or clear the Feed URL to use on-device verification.",
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
                null -> "No published GitHub release found for $owner/$repo (HTTP 404). Make sure the repository is Public and has a published Release, or clear the URL to verify your installed build."
            }
        } else {
            "Update manifest not found at that URL (HTTP 404). Verify the URL or clear it to use on-device verification."
        }
    }
}
