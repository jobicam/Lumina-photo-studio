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
    fun testApplyRetouchStroke_singleTapBlemishHeal() {
        val bmp = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        for (y in 0 until 40) {
            for (x in 0 until 40) {
                bmp.setPixel(x, y, Color.rgb(210, 165, 140))
            }
        }
        // Dark blemish spot right at center (20, 20)
        bmp.setPixel(20, 20, Color.rgb(60, 40, 35))

        val stroke = RetouchStroke(
            type = RetouchType.BLEMISH,
            points = listOf(StrokePoint(normX = 0.5f, normY = 0.5f)),
            radius = 24f,
            intensity = 1.0f
        )

        BitmapProcessor.applyRetouchStroke(bmp, stroke)
        val healedColor = bmp.getPixel(20, 20)
        assertNotEquals("Single-tap blemish heal should modify the spot pixel", Color.rgb(60, 40, 35), healedColor)
        assertTrue("Healed center pixel should brighten toward surrounding skin", Color.red(healedColor) > 120)
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
        assertTrue("Placeholder URL should return actionable Error", placeholderUrl is com.example.model.UpdateStatus.Error)
        assertTrue(
            (placeholderUrl as com.example.model.UpdateStatus.Error).message.contains("<owner>")
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
    }

    @Test
    fun testBlemishDetector_ignoresCleanSkinAndNonSkinRegions() {
        val cleanSkin = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        for (y in 0 until 160) {
            for (x in 0 until 160) {
                if (x < 120) {
                    cleanSkin.setPixel(x, y, Color.rgb(215, 165, 140))
                } else {
                    // Blue non-skin background with a dark spot that must be ignored
                    val dx = x - 140
                    val dy = y - 80
                    if (dx * dx + dy * dy <= 25) {
                        cleanSkin.setPixel(x, y, Color.rgb(10, 30, 100))
                    } else {
                        cleanSkin.setPixel(x, y, Color.rgb(25, 95, 225))
                    }
                }
            }
        }

        val suggestions = com.example.processing.BlemishDetector.detectBlemishes(cleanSkin, sensitivity = 0.7f)
        assertTrue("Clean skin and non-skin spots should yield 0 suggestions, got ${suggestions.size}", suggestions.isEmpty())
    }

    @Test
    fun testBlemishDetector_detectsCompactSkinBlobsAndSupportsHealing() {
        val portrait = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        for (y in 0 until 160) {
            for (x in 0 until 160) {
                if (x in 16..143 && y in 16..143) {
                    portrait.setPixel(x, y, Color.rgb(215, 165, 140))
                } else {
                    // Non-skin border
                    portrait.setPixel(x, y, Color.rgb(30, 90, 210))
                }
            }
        }

        // Paint two distinct blemish blobs inside the skin region
        val spot1X = 48 // normX = 0.30
        val spot1Y = 56 // normY = 0.35
        val spot2X = 112 // normX = 0.70
        val spot2Y = 96 // normY = 0.60
        for (y in 0 until 160) {
            for (x in 0 until 160) {
                val d1 = (x - spot1X) * (x - spot1X) + (y - spot1Y) * (y - spot1Y)
                val d2 = (x - spot2X) * (x - spot2X) + (y - spot2Y) * (y - spot2Y)
                if (d1 <= 20) {
                    // Reddish inflamed blemish
                    portrait.setPixel(x, y, Color.rgb(170, 92, 82))
                } else if (d2 <= 20) {
                    // Darker brownish spot
                    portrait.setPixel(x, y, Color.rgb(125, 84, 70))
                }
            }
        }

        val detected = com.example.processing.BlemishDetector.detectBlemishes(portrait, sensitivity = 0.65f)
        assertEquals("Should detect exactly the 2 skin blemishes", 2, detected.size)

        val nearSpot1 = detected.any { kotlin.math.hypot((it.normX - 0.30f).toDouble(), (it.normY - 0.35f).toDouble()) < 0.05 }
        val nearSpot2 = detected.any { kotlin.math.hypot((it.normX - 0.70f).toDouble(), (it.normY - 0.60f).toDouble()) < 0.05 }
        assertTrue("Expected suggestion near (0.30, 0.35), got $detected", nearSpot1)
        assertTrue("Expected suggestion near (0.70, 0.60), got $detected", nearSpot2)

        // Heal all detected spots using the same normalized 400px reference radius as PhotoEditorViewModel
        val healed = portrait.copy(Bitmap.Config.ARGB_8888, true)
        for (s in detected) {
            BitmapProcessor.applyRetouchStroke(
                healed,
                RetouchStroke(
                    type = RetouchType.BLEMISH,
                    points = listOf(StrokePoint(s.normX, s.normY)),
                    radius = (s.normRadius * 400f * 1.35f).coerceIn(16f, 60f),
                    intensity = 1.0f
                )
            )
        }

        val afterHeal = com.example.processing.BlemishDetector.detectBlemishes(healed, sensitivity = 0.65f)
        assertTrue("Re-scanning after healing all suggested spots should find fewer/no spots (got ${afterHeal.size})", afterHeal.isEmpty())
    }
}
