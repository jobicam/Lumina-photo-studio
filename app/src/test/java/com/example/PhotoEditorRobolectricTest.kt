package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.AdjustmentState
import com.example.model.RetouchStroke
import com.example.model.RetouchType
import com.example.model.StrokePoint
import com.example.processing.BitmapProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhotoEditorRobolectricTest {

    @Test
    fun testCalculateHistogram_tracksRgbAndLuminance() {
        val bmp = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        for (y in 0 until 20) {
            for (x in 0 until 20) {
                val color = if (x < 10) Color.rgb(220, 20, 20) else Color.rgb(20, 40, 210)
                bmp.setPixel(x, y, color)
            }
        }

        val hist = BitmapProcessor.calculateHistogram(bmp)
        assertEquals(256, hist.red.size)
        assertEquals(256, hist.green.size)
        assertEquals(256, hist.blue.size)
        assertEquals(256, hist.luminance.size)
        assertTrue(hist.maxCount >= 200)
        assertTrue("Red bin 220 should contain left half pixels", hist.red[220] == 200)
        assertTrue("Blue bin 210 should contain right half pixels", hist.blue[210] == 200)
    }

    @Test
    fun testRenderAdjustments_v2TonalControlsAndSharpness() {
        val bmp = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        for (y in 0 until 32) {
            for (x in 0 until 32) {
                val v = ((x + y) * 4).coerceIn(20, 235)
                bmp.setPixel(x, y, Color.rgb(v, (v * 0.8f).toInt(), (v * 0.6f).toInt()))
            }
        }

        val adjusted = BitmapProcessor.renderAdjustments(
            source = bmp,
            adjustments = AdjustmentState(
                highlights = -40f,
                shadows = 45f,
                whites = 20f,
                blacks = -15f,
                vibrance = 35f,
                clarity = 30f,
                sharpness = 50f
            )
        )

        assertEquals(32, adjusted.width)
        assertEquals(32, adjusted.height)
        // Shadow pixel near (2,2) should be lifted noticeably vs source
        val origShadow = Color.red(bmp.getPixel(2, 2))
        val newShadow = Color.red(adjusted.getPixel(2, 2))
        assertTrue("Shadows slider should lift dark tones ($origShadow -> $newShadow)", newShadow > origShadow)
    }

    @Test
    fun testApplyPortraitSkinSmoothing_smoothsSkinAndPreservesNonSkin() {
        val bmp = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        for (y in 0 until 24) {
            for (x in 0 until 24) {
                if (x < 12) {
                    // Alternating textured skin pixels
                    val delta = if ((x + y) % 2 == 0) 10 else -10
                    bmp.setPixel(x, y, Color.rgb(215 + delta, 165 + delta, 140 + delta))
                } else {
                    // Pure blue background
                    bmp.setPixel(x, y, Color.rgb(20, 80, 220))
                }
            }
        }

        val smoothed = BitmapProcessor.applyPortraitSkinSmoothing(bmp, intensity = 0.8f)

        // Blue non-skin pixel should remain unchanged
        assertEquals(Color.rgb(20, 80, 220), smoothed.getPixel(18, 12))

        // Skin pixel texture difference between adjacent pixels should reduce after smoothing
        val p1 = Color.red(smoothed.getPixel(5, 10))
        val p2 = Color.red(smoothed.getPixel(6, 10))
        assertTrue("Skin contrast should soften from 20 to smaller delta (was ${kotlin.math.abs(p1 - p2)})", kotlin.math.abs(p1 - p2) < 20)
    }

    @Test
    fun testApplyRetouchStroke_eyePopBrightensTargetRegion() {
        val bmp = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.rgb(70, 110, 150))

        val stroke = RetouchStroke(
            type = RetouchType.EYE_POP,
            points = listOf(StrokePoint(normX = 0.5f, normY = 0.5f)),
            radius = 30f,
            intensity = 1.0f
        )

        BitmapProcessor.applyRetouchStroke(bmp, stroke)
        val brightened = bmp.getPixel(20, 20)
        assertNotEquals("Eye Pop should enhance target pixel", Color.rgb(70, 110, 150), brightened)
        assertTrue("Eye Pop should lift target brightness", Color.blue(brightened) > 150)
    }

    @Test
    fun testComputeAutoEnhance_brightensUnderexposedPhoto() {
        val darkBmp = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        darkBmp.eraseColor(Color.rgb(35, 30, 28))

        val enhanced = BitmapProcessor.computeAutoEnhance(darkBmp)
        assertTrue("Dark image should get positive exposure boost, was ${enhanced.exposure}", enhanced.exposure > 15f)
        assertTrue("Should boost vibrance", enhanced.vibrance > 0f)
    }

    @Test
    fun testStudioFramesAndSoftGlow_renderCorrectly() {
        val bmp = Bitmap.createBitmap(100, 80, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.rgb(90, 120, 150))

        val polaroid = BitmapProcessor.renderAdjustments(
            source = bmp,
            adjustments = AdjustmentState(softGlow = 60f),
            frameStyle = com.example.model.FrameStyle.POLAROID
        )
        assertTrue("Polaroid frame should add width padding", polaroid.width > bmp.width)
        assertTrue("Polaroid frame should add taller bottom margin", polaroid.height > bmp.height + (polaroid.width - bmp.width))

        val cinema = BitmapProcessor.applyStudioFrame(bmp, com.example.model.FrameStyle.CINEMA)
        assertEquals(bmp.width, cinema.width)
        assertEquals(bmp.height, cinema.height)
        assertEquals(Color.rgb(10, 10, 13), cinema.getPixel(50, 2))
    }

    @Test
    fun testBuildPresetColorMatrix_producesDistinctMatricesForLiveCameraPreview() {
        val identity = BitmapProcessor.buildPresetColorMatrix(com.example.model.FilterPresets.NONE)
        val grayscale = BitmapProcessor.buildPresetColorMatrix(com.example.model.FilterPresets.GRAYSCALE)
        val sepia = BitmapProcessor.buildPresetColorMatrix(com.example.model.FilterPresets.SEPIA)

        assertEquals(20, identity.array.size)
        assertEquals(1.0f, identity.array[0], 0.001f)
        assertTrue("Grayscale matrix should differ from identity", !identity.array.contentEquals(grayscale.array))
        assertTrue("Sepia matrix should differ from identity", !identity.array.contentEquals(sepia.array))
    }

    @Test
    fun testCropBitmap_aspectRatioPresetsAndCustomRegion() {
        val bmp = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        for (y in 0 until 100) {
            for (x in 0 until 200) {
                val color = if (x < 100) Color.rgb(200, 40, 40) else Color.rgb(40, 80, 220)
                bmp.setPixel(x, y, color)
            }
        }

        // 1:1 centered on 200x100 -> 100x100
        val squareRect = com.example.model.CropRect.fittedForAspectRatio(
            bmp.width, bmp.height, com.example.model.CropAspectRatio.SQUARE
        )
        val croppedSquare = BitmapProcessor.cropBitmap(bmp, squareRect)
        assertEquals(100, croppedSquare.width)
        assertEquals(100, croppedSquare.height)

        // 4:5 centered on 200x100 -> 80x100
        val rect45 = com.example.model.CropRect.fittedForAspectRatio(
            bmp.width, bmp.height, com.example.model.CropAspectRatio.PORTRAIT_4_5
        )
        val cropped45 = BitmapProcessor.cropBitmap(bmp, rect45)
        assertEquals(80, cropped45.width)
        assertEquals(100, cropped45.height)

        // 16:9 centered on 100x100 -> 100x56
        val rect169 = com.example.model.CropRect.fittedForAspectRatio(
            100, 100, com.example.model.CropAspectRatio.LANDSCAPE_16_9
        )
        val cropped169 = BitmapProcessor.cropBitmap(croppedSquare, rect169)
        assertEquals(100, cropped169.width)
        assertEquals(56, cropped169.height)

        // Custom right-half crop should preserve blue region pixels
        val rightHalf = BitmapProcessor.cropBitmap(
            bmp,
            com.example.model.CropRect(left = 0.55f, top = 0.1f, right = 0.95f, bottom = 0.9f)
        )
        assertEquals(80, rightHalf.width)
        assertEquals(80, rightHalf.height)
        assertEquals(Color.rgb(40, 80, 220), rightHalf.getPixel(10, 10))
    }

    @Test
    fun testAppUpdateChecker_verifiesInstalledPackageAndRejectsInvalidUrls() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val result = com.example.processing.AppUpdateChecker.checkForUpdate(context, "")
        assertTrue("Blank feed URL should verify installed build as UpToDate", result is com.example.model.UpdateStatus.UpToDate)
        val upToDate = result as com.example.model.UpdateStatus.UpToDate
        assertEquals(BuildConfig.VERSION_NAME, upToDate.currentVersionName)
        assertEquals(BuildConfig.VERSION_CODE.toLong(), upToDate.currentVersionCode)

        val invalidScheme = com.example.processing.AppUpdateChecker.checkForUpdate(context, "ftp://example.com/version.json")
        assertTrue("Non-HTTP(S) feed URL should return Error", invalidScheme is com.example.model.UpdateStatus.Error)

        val placeholderUrl = com.example.processing.AppUpdateChecker.checkForUpdate(
            context,
            "https://api.github.com/repos/<owner>/<repo>/releases/latest"
        )
        assertTrue("Placeholder URL should return Error", placeholderUrl is com.example.model.UpdateStatus.Error)
        assertEquals(
            "https://api.github.com/repos/jobicam/Lumina-photo-studio/releases/latest",
            com.example.processing.AppUpdateChecker.normalizeFeedUrl(
                com.example.processing.AppUpdateChecker.DEFAULT_UPDATE_FEED_URL
            )
        )
    }

    @Test
    fun testLocalizedGaussianSkinSmoothBrush_preservesTextureAndIsolation() {
        val base = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 0 until 64) {
            for (x in 0 until 64) {
                if (x < 50) {
                    // Alternating high-frequency skin texture (delta = 24)
                    val delta = if ((x + y) % 2 == 0) 12 else -12
                    base.setPixel(x, y, Color.rgb(212 + delta, 162 + delta, 138 + delta))
                } else {
                    // Non-skin bright blue strip on right edge
                    base.setPixel(x, y, Color.rgb(20, 90, 230))
                }
            }
        }

        val noDetailBmp = base.copy(Bitmap.Config.ARGB_8888, true)
        val highDetailBmp = base.copy(Bitmap.Config.ARGB_8888, true)

        BitmapProcessor.applyRetouchStroke(
            noDetailBmp,
            RetouchStroke(
                type = RetouchType.SKIN_SMOOTH,
                points = listOf(StrokePoint(normX = 0.4f, normY = 0.5f)),
                radius = 50f,
                intensity = 0.9f,
                texturePreserve = 0.0f
            )
        )

        BitmapProcessor.applyRetouchStroke(
            highDetailBmp,
            RetouchStroke(
                type = RetouchType.SKIN_SMOOTH,
                points = listOf(StrokePoint(normX = 0.4f, normY = 0.5f)),
                radius = 50f,
                intensity = 0.9f,
                texturePreserve = 0.65f
            )
        )

        val cx = (0.4f * 64).toInt()
        val cy = (0.5f * 64).toInt()
        val noDetailDelta = kotlin.math.abs(
            Color.red(noDetailBmp.getPixel(cx, cy)) - Color.red(noDetailBmp.getPixel(cx + 1, cy))
        )
        val highDetailDelta = kotlin.math.abs(
            Color.red(highDetailBmp.getPixel(cx, cy)) - Color.red(highDetailBmp.getPixel(cx + 1, cy))
        )

        assertTrue("Skin smooth brush should soften checkerboard contrast below 24 (was $noDetailDelta)", noDetailDelta < 24)
        assertTrue(
            "Higher texturePreserve ($highDetailDelta) should keep more micro-texture than 0.0 ($noDetailDelta)",
            highDetailDelta > noDetailDelta
        )

        // Far corner skin pixel outside localized Gaussian brush radius should stay unchanged
        assertEquals(base.getPixel(2, 2), noDetailBmp.getPixel(2, 2))
        // Non-skin blue pixel inside/near right region should stay completely untouched
        assertEquals(Color.rgb(20, 90, 230), noDetailBmp.getPixel(55, 32))
    }

    @Test
    fun testUpgradeMigrationFromOldVersion_preservesSettingsAndRecordsPreviousVersion() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val settingsPrefs = app.getSharedPreferences("lumina_settings_prefs", android.content.Context.MODE_PRIVATE)
        val versionPrefs = app.getSharedPreferences("lumina_version_prefs", android.content.Context.MODE_PRIVATE)

        // Simulate a device upgrading from v2.4.0 (versionCode 10) with custom settings
        settingsPrefs.edit()
            .putString("app_language", "km")
            .putInt("jpeg_quality", 88)
            .putString("update_feed_url", "https://example.com/version.json")
            .putInt("last_migrated_version_code", 10)
            .commit()
        versionPrefs.edit()
            .putString("last_seen_version", "2.4.0+10")
            .remove("upgraded_from_version")
            .commit()

        val vm = com.example.viewmodel.PhotoEditorViewModel(app)
        val state = vm.uiState.value

        assertEquals(com.example.model.AppLanguage.KHMER, state.settings.appLanguage)
        assertEquals(88, state.settings.jpegQuality)
        assertEquals("https://example.com/version.json", state.settings.updateFeedUrl)
        assertEquals("2.4.0", state.upgradedFromVersion)
        assertEquals(BuildConfig.VERSION_CODE, settingsPrefs.getInt("last_migrated_version_code", 0))
        assertEquals("2.8.0", BuildConfig.VERSION_NAME)
        assertEquals(14, BuildConfig.VERSION_CODE)
    }

    @Test
    fun testBackgroundBlur_blursOuterBackgroundWhileKeepingFocusSharp() {
        val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        for (y in 0 until 120) {
            for (x in 0 until 120) {
                // High-contrast 4px checkerboard across entire frame (red/blue & bright/dark)
                val bright = (((x / 2) + (y / 2)) % 2) == 0
                val c = if (bright) Color.rgb(220, 225, 235) else Color.rgb(30, 50, 180)
                bmp.setPixel(x, y, c)
            }
        }

        val blurred = BitmapProcessor.renderAdjustments(
            source = bmp,
            adjustments = AdjustmentState(
                backgroundBlur = 90f,
                blurFocusX = 0.5f,
                blurFocusY = 0.5f,
                blurFocusRadius = 0.24f,
                blurProtectSubject = false
            )
        )

        fun patchContrast(b: Bitmap, startX: Int, startY: Int): Int {
            var maxL = 0
            var minL = 255
            for (dy in 0 until 8) {
                for (dx in 0 until 8) {
                    val p = b.getPixel(startX + dx, startY + dy)
                    val lum = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3
                    if (lum > maxL) maxL = lum
                    if (lum < minL) minL = lum
                }
            }
            return maxL - minL
        }

        val centerContrast = patchContrast(blurred, 56, 56)
        val cornerContrast = patchContrast(blurred, 6, 6)

        assertTrue("Focus center should stay crisp (contrast=$centerContrast)", centerContrast > 120)
        assertTrue(
            "Background corner should be heavily blurred compared to focus center (corner=$cornerContrast, center=$centerContrast)",
            cornerContrast < centerContrast / 2
        )
    }

    @Test
    fun testBackgroundBlur_protectSubjectKeepsOffCenterSkinSharp() {
        val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        for (y in 0 until 120) {
            for (x in 0 until 120) {
                if (x in 12..36 && y in 72..96) {
                    // Textured skin patch far from focal center (0.8, 0.2)
                    val delta = if (((x / 2) + (y / 2)) % 2 == 0) 24 else -24
                    bmp.setPixel(x, y, Color.rgb(212 + delta, 162 + delta, 138 + delta))
                } else {
                    val bright = (((x / 2) + (y / 2)) % 2) == 0
                    bmp.setPixel(x, y, if (bright) Color.rgb(210, 220, 235) else Color.rgb(25, 70, 195))
                }
            }
        }

        val unprotected = BitmapProcessor.applyBackgroundBlur(
            source = bmp,
            blurStrength = 90f,
            focusX = 0.80f,
            focusY = 0.20f,
            focusRadius = 0.20f,
            protectSubject = false
        )
        val protectedBmp = BitmapProcessor.applyBackgroundBlur(
            source = bmp,
            blurStrength = 90f,
            focusX = 0.80f,
            focusY = 0.20f,
            focusRadius = 0.20f,
            protectSubject = true
        )

        fun regionRange(b: Bitmap): Int {
            var maxR = 0
            var minR = 255
            for (y in 78..90) {
                for (x in 18..30) {
                    val r = Color.red(b.getPixel(x, y))
                    if (r > maxR) maxR = r
                    if (r < minR) minR = r
                }
            }
            return maxR - minR
        }

        val protectedRange = regionRange(protectedBmp)
        val unprotectedRange = regionRange(unprotected)
        assertTrue(
            "Protect Subject should preserve skin detail ($protectedRange) vs unprotected ($unprotectedRange)",
            protectedRange > unprotectedRange + 15
        )
    }

    @Test
    fun testViewModel_tabSelectionAndBackgroundBlurResetSplitViewAndPreserveInitialTab() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.PhotoEditorViewModel(app)

        // Wait briefly for init background sample load to settle cleanly
        val deadline = System.currentTimeMillis() + 5000L
        while (vm.uiState.value.workingBitmap == null && System.currentTimeMillis() < deadline) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // Split view should auto-disable when switching to interactive Retouch or Crop tools
        vm.toggleSplitView()
        assertTrue(vm.uiState.value.isSplitView)
        vm.selectTab(com.example.viewmodel.EditorTab.RETOUCH)
        assertEquals(com.example.viewmodel.EditorTab.RETOUCH, vm.uiState.value.activeTab)
        assertTrue("Split view should turn off when entering Retouch", !vm.uiState.value.isSplitView)

        vm.toggleSplitView()
        assertTrue(vm.uiState.value.isSplitView)
        vm.selectTab(com.example.viewmodel.EditorTab.CROP)
        assertEquals(com.example.viewmodel.EditorTab.CROP, vm.uiState.value.activeTab)
        assertTrue("Split view should turn off when entering Crop", !vm.uiState.value.isSplitView)

        // Quick Background Blur shortcut should enable blur, switch to Effects, and disable split view
        vm.toggleSplitView()
        assertTrue(vm.uiState.value.isSplitView)
        vm.openBackgroundBlurTool()
        assertEquals(com.example.viewmodel.EditorTab.ADJUST, vm.uiState.value.activeTab)
        assertEquals(com.example.model.AdjustCategory.EFFECTS, vm.uiState.value.activeAdjustCategory)
        assertEquals(50f, vm.uiState.value.adjustments.backgroundBlur, 0.01f)
        assertTrue("Split view should turn off for tap-to-focus Background Blur", !vm.uiState.value.isSplitView)

        // Loading a sample photo with an explicit initialTab should keep that tab instead of reverting to FILTERS
        vm.loadSamplePhoto(
            isPortrait = false,
            autoNavigate = true,
            initialTab = com.example.viewmodel.EditorTab.CURVES
        )
        val navDeadline = System.currentTimeMillis() + 5000L
        while ((vm.uiState.value.currentScreen != com.example.viewmodel.AppScreen.EDITOR || vm.uiState.value.isProcessing) &&
            System.currentTimeMillis() < navDeadline
        ) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertEquals(com.example.viewmodel.AppScreen.EDITOR, vm.uiState.value.currentScreen)
        assertEquals(com.example.viewmodel.EditorTab.CURVES, vm.uiState.value.activeTab)
    }

    @Test
    fun testImageExporter_shareBitmapUsesContentUriAndPrunesStaleCacheFiles() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val cacheImages = java.io.File(app.cacheDir, "images").apply { mkdirs() }
        val staleFile = java.io.File(cacheImages, "lumina_share_stale.jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3))
            setLastModified(System.currentTimeMillis() - 100_000_000L)
        }

        val bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.CYAN)

        val result = kotlinx.coroutines.runBlocking {
            com.example.processing.ImageExporter.shareBitmap(app, bmp)
        }
        assertTrue("shareBitmap should succeed with FileProvider content URI", result.isSuccess)
        assertTrue("Expired share cache files (>24h) should be pruned", !staleFile.exists())

        val chooserIntent = result.getOrThrow()
        @Suppress("DEPRECATION")
        val targetIntent = chooserIntent.getParcelableExtra<android.content.Intent>(android.content.Intent.EXTRA_INTENT)
        @Suppress("DEPRECATION")
        val streamUri = targetIntent?.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
        assertEquals("content", streamUri?.scheme)
    }

    @Test
    fun testRetouchRestoreBrush_restoresOriginalPixelsAndSculptWarpsSmoothly() {
        val original = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 0 until 64) {
            for (x in 0 until 64) {
                // Warm skin base with a vertical feature stripe at x == 32 for warp check
                val c = if (x == 32) Color.rgb(120, 70, 55) else Color.rgb(215, 165, 140)
                original.setPixel(x, y, c)
            }
        }

        // Paint glow over center, then restore center back toward original
        val working = original.copy(Bitmap.Config.ARGB_8888, true)
        BitmapProcessor.applyRetouchStroke(
            working,
            RetouchStroke(
                type = RetouchType.GLOW,
                points = listOf(StrokePoint(0.5f, 0.5f)),
                radius = 60f,
                intensity = 1.0f
            ),
            original
        )
        val glowCenter = working.getPixel(32, 32)
        assertNotEquals("Glow stroke should brighten center pixel", original.getPixel(32, 32), glowCenter)

        BitmapProcessor.applyRetouchStroke(
            working,
            RetouchStroke(
                type = RetouchType.GLOW,
                points = listOf(StrokePoint(0.5f, 0.5f)),
                radius = 60f,
                intensity = 1.0f,
                isEraser = true
            ),
            original
        )
        val restoredCenter = working.getPixel(32, 32)
        assertEquals("Restore brush at 100% center should match original pixel", original.getPixel(32, 32), restoredCenter)
        val adjacentDelta = kotlin.math.abs(Color.red(working.getPixel(31, 32)) - Color.red(original.getPixel(31, 32)))
        assertTrue("Near-center pixel should restore close to original (delta=$adjacentDelta)", adjacentDelta <= 3)

        // Directional Sculpt drag stroke across vertical stripe at (32, 32) -> rightwards
        val sculptBmp = original.copy(Bitmap.Config.ARGB_8888, true)
        BitmapProcessor.applyRetouchStroke(
            sculptBmp,
            RetouchStroke(
                type = RetouchType.SCULPT,
                points = listOf(
                    StrokePoint(0.45f, 0.50f),
                    StrokePoint(0.58f, 0.50f),
                    StrokePoint(0.70f, 0.50f)
                ),
                radius = 55f,
                intensity = 0.85f
            ),
            original
        )
        // Pixel slightly right of the original stripe at (34, 32) should shift darker toward the stripe
        assertTrue(
            "Directional sculpt drag should displace dark contour pixel rightward",
            Color.red(sculptBmp.getPixel(34, 32)) < Color.red(original.getPixel(34, 32))
        )
    }

    @Test
    fun testViewModel_retouchEraserToggleAndToolSelection() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.viewmodel.PhotoEditorViewModel(app)

        assertTrue(!vm.uiState.value.isEraser)
        vm.toggleEraser()
        assertTrue(vm.uiState.value.isEraser)
        vm.selectRetouchTool(RetouchType.EYE_POP)
        assertEquals(RetouchType.EYE_POP, vm.uiState.value.activeRetouchTool)
        assertTrue("Selecting a retouch brush should exit Restore/Eraser mode", !vm.uiState.value.isEraser)
    }
}
