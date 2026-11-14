package com.example

import com.example.model.AdjustmentState
import com.example.model.CurvePoint
import com.example.model.CurvesState
import com.example.model.FilterPresets
import com.example.model.HslColorChannel
import com.example.model.HslState
import com.example.model.SingleCurve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoEditorUnitTest {

    @Test
    fun testAdjustmentState_defaults() {
        val state = AdjustmentState()
        assertTrue(state.isDefault)

        val modified = state.copy(exposure = 20f)
        assertFalse(modified.isDefault)
    }

    @Test
    fun testCurves_linearInterpolation() {
        val curve = SingleCurve()
        assertTrue(curve.isLinear())

        // Linear midpoint
        assertEquals(0.5f, curve.evaluate(0.5f), 0.01f)

        // LUT size should be 256
        val lut = curve.buildLut()
        assertEquals(256, lut.size)
        assertEquals(0, lut[0])
        assertEquals(255, lut[255])
    }

    @Test
    fun testCurves_customPoints() {
        // Curve with high midpoint (brightened midtones)
        val curve = SingleCurve(
            listOf(CurvePoint(0f, 0f), CurvePoint(0.5f, 0.8f), CurvePoint(1f, 1f))
        )
        assertFalse(curve.isLinear())
        val midVal = curve.evaluate(0.5f)
        assertTrue("Midpoint value should be greater than 0.7f", midVal >= 0.7f)
    }

    @Test
    fun testHslState_defaults() {
        val hsl = HslState()
        assertTrue(hsl.isDefault)
        assertEquals(HslColorChannel.ORANGE, hsl.activeChannel)
    }

    @Test
    fun testFilterPresets_available() {
        assertTrue(FilterPresets.ALL_PRESETS.isNotEmpty())
        val original = FilterPresets.NONE
        assertEquals("original", original.id)
        val tealOrange = FilterPresets.ALL_PRESETS.firstOrNull { it.id == "teal_orange" }
        assertTrue(tealOrange != null)

        // Verify Grayscale, Sepia, and High Contrast presets
        val grayscale = FilterPresets.GRAYSCALE
        assertEquals("grayscale", grayscale.id)
        assertTrue(grayscale.isGrayscale)

        val sepia = FilterPresets.SEPIA
        assertEquals("sepia", sepia.id)
        assertTrue(sepia.isSepia)

        val highContrast = FilterPresets.HIGH_CONTRAST
        assertEquals("high_contrast", highContrast.id)
        assertTrue(highContrast.contrast > 50f)
    }

    @Test
    fun testExportFormat_and_fileSizeFormatting() {
        assertEquals("jpg", com.example.model.ExportFormat.JPEG.extension)
        assertEquals("png", com.example.model.ExportFormat.PNG.extension)

        val formattedMB = com.example.model.SavedImageInfo.formatFileSize(2097152L)
        assertTrue("Formatted size should contain MB, was: $formattedMB", formattedMB.contains("MB"))

        val formattedKB = com.example.model.SavedImageInfo.formatFileSize(500 * 1024L)
        assertTrue("Formatted size should contain KB, was: $formattedKB", formattedKB.contains("KB"))
    }

    @Test
    fun testSkinToneDetection_portraitTones() {
        // Fair warm skin tone (R=225, G=175, B=150)
        assertTrue(com.example.processing.BitmapProcessor.isSkinTone(225, 175, 150))

        // Olive / Tan skin tone (R=185, G=135, B=100)
        assertTrue(com.example.processing.BitmapProcessor.isSkinTone(185, 135, 100))

        // Non-skin colors (pure green, pure blue, pure gray)
        assertFalse("Pure green should not be detected as skin", com.example.processing.BitmapProcessor.isSkinTone(30, 200, 30))
        assertFalse("Pure blue sky should not be detected as skin", com.example.processing.BitmapProcessor.isSkinTone(40, 100, 220))
        assertFalse("Neutral gray should not be detected as skin", com.example.processing.BitmapProcessor.isSkinTone(128, 128, 128))
    }

    @Test
    fun testAdjustmentState_v2TonalFields_affectIsDefault() {
        val base = AdjustmentState()
        assertFalse(base.copy(highlights = -25f).isDefault)
        assertFalse(base.copy(shadows = 30f).isDefault)
        assertFalse(base.copy(whites = 15f).isDefault)
        assertFalse(base.copy(blacks = -10f).isDefault)
        assertFalse(base.copy(clarity = 20f).isDefault)
        assertFalse(base.copy(sharpness = 40f).isDefault)
        assertFalse(base.copy(vibrance = 15f).isDefault)
        assertFalse(base.copy(tint = -12f).isDefault)
        assertFalse(base.copy(softGlow = 25f).isDefault)
    }

    @Test
    fun testCurvesState_channelUpdateAndReset() {
        val state = CurvesState()
        val redCurve = SingleCurve(listOf(CurvePoint(0f, 0f), CurvePoint(0.5f, 0.7f), CurvePoint(1f, 1f)))
        val updated = state.copy(activeChannel = com.example.model.CurveChannel.RED).updateActiveCurve(redCurve)
        assertFalse(updated.getActiveCurve().isLinear())
        val reset = updated.resetActiveCurve()
        assertTrue(reset.getActiveCurve().isLinear())
    }

    @Test
    fun testAppStrings_englishAndKhmerCoverage() {
        val en = com.example.model.AppLanguage.ENGLISH
        val km = com.example.model.AppLanguage.KHMER

        assertEquals("Save", com.example.util.AppStrings.save(en))
        assertEquals("រក្សាទុក", com.example.util.AppStrings.save(km))
        assertEquals("Histogram", com.example.util.AppStrings.histogramHud(en))
        assertTrue(com.example.util.AppStrings.histogramHud(km).isNotBlank())
        assertTrue(com.example.util.AppStrings.autoEnhance(km).isNotBlank())
        assertTrue(com.example.util.AppStrings.sliderSoftGlow(km).isNotBlank())
        assertEquals("Live Filters", com.example.util.AppStrings.liveFilters(en))
        assertTrue(com.example.util.AppStrings.liveFilters(km).isNotBlank())

        com.example.model.RetouchType.values().forEach { tool ->
            assertTrue(com.example.util.AppStrings.retouchTitle(tool, en).isNotBlank())
            assertTrue(com.example.util.AppStrings.retouchTitle(tool, km).isNotBlank())
            assertTrue(com.example.util.AppStrings.retouchSubtitle(tool, en).isNotBlank())
            assertTrue(com.example.util.AppStrings.retouchSubtitle(tool, km).isNotBlank())
        }
        com.example.model.FilterCategory.values().forEach { cat ->
            assertTrue(com.example.util.AppStrings.filterCategory(cat, en).isNotBlank())
            assertTrue(com.example.util.AppStrings.filterCategory(cat, km).isNotBlank())
        }
        com.example.model.FrameStyle.values().forEach { frame ->
            assertTrue(com.example.util.AppStrings.frameLabel(frame, en).isNotBlank())
            assertTrue(com.example.util.AppStrings.frameLabel(frame, km).isNotBlank())
        }
        assertTrue(FilterPresets.ALL_PRESETS.any { it.id == "portra_400" })
        assertTrue(FilterPresets.ALL_PRESETS.any { it.id == "obsidian_gold" })
        assertTrue(FilterPresets.ALL_PRESETS.any { it.id == "dream_halation" })
        assertTrue(FilterPresets.ALL_PRESETS.any { it.id == "alpine_crisp" })
        assertEquals("Apply Crop", com.example.util.AppStrings.applyCrop(en))
        assertTrue(com.example.util.AppStrings.applyCrop(km).isNotBlank())
        assertTrue(com.example.util.AppStrings.resetCrop(km).isNotBlank())
        assertTrue(com.example.util.AppStrings.statusCropApplied(km).isNotBlank())
    }

    @Test
    fun testCropRect_fittedForAspectRatios() {
        // 1600x800 (2:1) -> 1:1 should center-crop width to 0.5 (left=0.25, right=0.75)
        val sq = com.example.model.CropRect.fittedForAspectRatio(
            1600, 800, com.example.model.CropAspectRatio.SQUARE
        )
        assertEquals(0.25f, sq.left, 0.01f)
        assertEquals(0.75f, sq.right, 0.01f)
        assertEquals(0f, sq.top, 0.01f)
        assertEquals(1f, sq.bottom, 0.01f)
        assertFalse(sq.isFull)

        // 1000x1000 (1:1) -> 4:5 (0.8) should center-crop width to 0.8 (left=0.1, right=0.9)
        val portrait45 = com.example.model.CropRect.fittedForAspectRatio(
            1000, 1000, com.example.model.CropAspectRatio.PORTRAIT_4_5
        )
        assertEquals(0.1f, portrait45.left, 0.01f)
        assertEquals(0.9f, portrait45.right, 0.01f)
        assertEquals(0f, portrait45.top, 0.01f)
        assertEquals(1f, portrait45.bottom, 0.01f)

        // 1000x1000 (1:1) -> 16:9 should center-crop height to 9/16 = 0.5625
        val wide169 = com.example.model.CropRect.fittedForAspectRatio(
            1000, 1000, com.example.model.CropAspectRatio.LANDSCAPE_16_9
        )
        assertEquals(0f, wide169.left, 0.01f)
        assertEquals(1f, wide169.right, 0.01f)
        assertEquals(0.5625f, wide169.height, 0.01f)

        // FREE & ORIGINAL on any photo should start full
        val freeRect = com.example.model.CropRect.fittedForAspectRatio(
            1200, 800, com.example.model.CropAspectRatio.FREE
        )
        assertTrue(freeRect.isFull)
        val origRect = com.example.model.CropRect.fittedForAspectRatio(
            1200, 800, com.example.model.CropAspectRatio.ORIGINAL
        )
        assertTrue(origRect.isFull)
    }

    @Test
    fun testAppUpdateChecker_manifestParsingAndVersionComparison() {
        val en = com.example.model.AppLanguage.ENGLISH
        val km = com.example.model.AppLanguage.KHMER
        assertEquals("Check for Updates", com.example.util.AppStrings.checkForUpdates(en))
        assertTrue(com.example.util.AppStrings.checkForUpdates(km).isNotBlank())
        assertTrue(com.example.util.AppStrings.upToDateTitle(km).isNotBlank())
        assertTrue(com.example.util.AppStrings.updateAvailableTitle(km).isNotBlank())

        // Semantic version comparison
        assertTrue(com.example.processing.AppUpdateChecker.compareSemanticVersions("2.5.0", "2.4.0") > 0)
        assertTrue(com.example.processing.AppUpdateChecker.compareSemanticVersions("2.10.0", "2.4.0") > 0)
        assertEquals(0, com.example.processing.AppUpdateChecker.compareSemanticVersions("v2.4.0", "2.4.0"))
        assertTrue(com.example.processing.AppUpdateChecker.compareSemanticVersions("2.3.0", "2.4.0") < 0)

        // Custom version.json parsing
        val customJson = """
            {
              "versionCode": 12,
              "versionName": "2.6.0",
              "releaseNotes": "Added HDR Tone Mapping",
              "downloadUrl": "https://example.com/lumina-v2.6.0.apk",
              "mandatory": true
            }
        """.trimIndent()
        val parsedCustom = com.example.processing.AppUpdateChecker.parseManifestJson(customJson)
        assertTrue(parsedCustom != null)
        assertEquals(12L, parsedCustom!!.versionCode)
        assertEquals("2.6.0", parsedCustom.versionName)
        assertEquals("Added HDR Tone Mapping", parsedCustom.releaseNotes)
        assertEquals("https://example.com/lumina-v2.6.0.apk", parsedCustom.downloadUrl)
        assertTrue(parsedCustom.isMandatory)
        assertTrue(com.example.processing.AppUpdateChecker.isUpdateAvailable(10L, "2.4.0", parsedCustom))

        // GitHub Releases API JSON parsing
        val githubJson = """
            {
              "tag_name": "v2.4.0",
              "body": "Latest stable release",
              "html_url": "https://github.com/example/lumina/releases/tag/v2.4.0"
            }
        """.trimIndent()
        val parsedGh = com.example.processing.AppUpdateChecker.parseManifestJson(githubJson)
        assertTrue(parsedGh != null)
        assertEquals("2.4.0", parsedGh!!.versionName)
        assertFalse(com.example.processing.AppUpdateChecker.isUpdateAvailable(10L, "2.4.0", parsedGh))

        // Invalid JSON returns null
        assertEquals(null, com.example.processing.AppUpdateChecker.parseManifestJson("{not valid json"))
    }

    @Test
    fun testComputeSkinProbability_and_v25Strings() {
        val warmSkinProb = com.example.processing.BitmapProcessor.computeSkinProbability(225, 175, 150)
        assertTrue("Warm skin tone should have strong probability (> 0.45f), was $warmSkinProb", warmSkinProb >= 0.45f)

        val nonSkinProb = com.example.processing.BitmapProcessor.computeSkinProbability(30, 100, 220)
        assertEquals(0f, nonSkinProb, 0.001f)

        val en = com.example.model.AppLanguage.ENGLISH
        val km = com.example.model.AppLanguage.KHMER
        assertEquals("Detail", com.example.util.AppStrings.texturePreserveLabel(en))
        assertTrue(com.example.util.AppStrings.texturePreserveLabel(km).isNotBlank())
        assertTrue(com.example.util.AppStrings.skinSmoothBannerTitle(en).contains("Gaussian"))
        assertTrue(com.example.util.AppStrings.skinSmoothBannerTitle(km).isNotBlank())
        assertTrue(com.example.util.AppStrings.skinSmoothBannerDesc(en).isNotBlank())
        assertTrue(com.example.util.AppStrings.skinSmoothBannerDesc(km).isNotBlank())
        assertTrue(com.example.util.AppStrings.studioCapabilities(en).contains("2.5"))
    }

    @Test
    fun testUpgradeFromOlderVersion_manifestAndGithubAssets() {
        // GitHub release with .apk asset should extract the direct .apk browser_download_url
        val githubWithAsset = """
            {
              "tag_name": "v2.5.0",
              "body": "Localized Gaussian smoothing & detail slider",
              "html_url": "https://github.com/example/lumina/releases/tag/v2.5.0",
              "assets": [
                {
                  "name": "LuminaPhoto.apk",
                  "browser_download_url": "https://github.com/example/lumina/releases/download/v2.5.0/LuminaPhoto.apk"
                }
              ]
            }
        """.trimIndent()
        val parsed = com.example.processing.AppUpdateChecker.parseManifestJson(githubWithAsset)
        assertTrue(parsed != null)
        assertEquals("2.5.0", parsed!!.versionName)
        assertEquals(
            "https://github.com/example/lumina/releases/download/v2.5.0/LuminaPhoto.apk",
            parsed.downloadUrl
        )
        // Older installs (v2.3.0 Build 9 & v2.4.0 Build 10) must detect v2.5.0 as available
        assertTrue(com.example.processing.AppUpdateChecker.isUpdateAvailable(9L, "2.3.0", parsed))
        assertTrue(com.example.processing.AppUpdateChecker.isUpdateAvailable(10L, "2.4.0", parsed))

        // Manifest builder round-trip
        val json = com.example.processing.AppUpdateChecker.buildVersionManifestJson(
            versionCode = 11L,
            versionName = "2.5.0",
            releaseNotes = "Gaussian skin smooth",
            downloadUrl = "https://example.com/LuminaPhoto.apk"
        )
        val reparsed = com.example.processing.AppUpdateChecker.parseManifestJson(json)
        assertTrue(reparsed != null)
        assertEquals(11L, reparsed!!.versionCode)
        assertEquals("2.5.0", reparsed.versionName)
        assertTrue(com.example.processing.AppUpdateChecker.isUpdateAvailable(10L, "2.4.0", reparsed))

        val en = com.example.model.AppLanguage.ENGLISH
        val km = com.example.model.AppLanguage.KHMER
        assertEquals("Updated from v2.4.0 → v2.5.0", com.example.util.AppStrings.updatedFromLabel("2.4.0", "2.5.0", en))
        assertTrue(com.example.util.AppStrings.updatedFromLabel("2.4.0", "2.5.0", km).isNotBlank())
        assertTrue(com.example.util.AppStrings.copyVersionManifest(km).isNotBlank())
        assertTrue(com.example.util.AppStrings.upgradeFeedHelper(km).isNotBlank())
        assertTrue(com.example.util.AppStrings.clearFeedUrl(km).isNotBlank())

        // GitHub web URL normalization -> API latest endpoint
        assertEquals(
            "https://api.github.com/repos/alice/lumina/releases/latest",
            com.example.processing.AppUpdateChecker.normalizeFeedUrl("https://github.com/alice/lumina")
        )
        assertEquals(
            "https://api.github.com/repos/alice/lumina/releases/latest",
            com.example.processing.AppUpdateChecker.normalizeFeedUrl("https://github.com/alice/lumina/releases")
        )
        assertTrue(
            com.example.processing.AppUpdateChecker.containsTemplatePlaceholder(
                "https://api.github.com/repos/<owner>/<repo>/releases/latest"
            )
        )
        assertFalse(
            com.example.processing.AppUpdateChecker.containsTemplatePlaceholder(
                "https://api.github.com/repos/alice/lumina/releases/latest"
            )
        )

        // JSON array response (e.g. /releases?per_page=1 or /tags?per_page=1)
        val tagsArrayJson = """[ { "name": "v2.5.0" } ]"""
        val parsedTag = com.example.processing.AppUpdateChecker.parseManifestJson(
            tagsArrayJson,
            fallbackReleaseUrl = "https://github.com/alice/lumina/releases"
        )
        assertTrue(parsedTag != null)
        assertEquals("2.5.0", parsedTag!!.versionName)
        assertEquals("https://github.com/alice/lumina/releases", parsedTag.downloadUrl)
    }

    @Test
    fun testAppUpdateChecker_prefixedTagsQueryParamsAndEmptyDownloadFallback() {
        // Prefixed and suffixed tag names should parse numeric semver cleanly
        assertTrue(com.example.processing.AppUpdateChecker.compareSemanticVersions("lumina-v2.6.0-rc1", "2.5.0") > 0)
        assertEquals(0, com.example.processing.AppUpdateChecker.compareSemanticVersions("android-2.5.0+11", "2.5.0"))

        // Asset URL with query parameters should still match as .apk
        val queryAssetJson = """
            {
              "tag_name": "lumina-v2.6.0",
              "html_url": "https://github.com/alice/lumina/releases/tag/v2.6.0",
              "assets": [
                {
                  "name": "LuminaPhoto",
                  "browser_download_url": "https://cdn.example.com/LuminaPhoto.apk?download=1"
                }
              ]
            }
        """.trimIndent()
        val parsedQuery = com.example.processing.AppUpdateChecker.parseManifestJson(queryAssetJson)
        assertTrue(parsedQuery != null)
        assertEquals("https://cdn.example.com/LuminaPhoto.apk?download=1", parsedQuery!!.downloadUrl)
        assertTrue(com.example.processing.AppUpdateChecker.isUpdateAvailable(11L, "2.5.0", parsedQuery))

        // Empty downloadUrl should fall back to fallbackReleaseUrl
        val emptyUrlJson = """
            {
              "versionCode": 12,
              "versionName": "2.6.0",
              "downloadUrl": ""
            }
        """.trimIndent()
        val parsedEmpty = com.example.processing.AppUpdateChecker.parseManifestJson(
            emptyUrlJson,
            fallbackReleaseUrl = "https://github.com/alice/lumina/releases"
        )
        assertTrue(parsedEmpty != null)
        assertEquals("https://github.com/alice/lumina/releases", parsedEmpty!!.downloadUrl)
    }
}
