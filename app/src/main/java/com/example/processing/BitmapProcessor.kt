package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import com.example.model.AdjustmentState
import com.example.model.ColorGradeState
import com.example.model.CropAspectRatio
import com.example.model.CurvesState
import com.example.model.FilterPreset
import com.example.model.FilterPresets
import com.example.model.FrameStyle
import com.example.model.HslColorChannel
import com.example.model.HistogramData
import com.example.model.HslState
import com.example.model.Lut3D
import com.example.model.RetouchStroke
import com.example.model.RetouchType
import com.example.model.StrokePoint
import com.example.model.TransformState
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object BitmapProcessor {

    /**
     * Resizes a bitmap so its maximum dimension is at most maxDimension.
     */
    fun downscaleIfNeeded(source: Bitmap, maxDimension: Int = 1800): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) {
            return source
        }
        val ratio = min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    /**
     * Applies full adjustment pipeline:
     * Filter Preset -> Adjustments -> Curves & HSL -> Color Grade -> 3D LUT -> Sharpness ->
     * Depth Lens Bokeh Blur -> Motion Blur -> Soft Glow -> Vignette -> Grain -> Frame.
     */
    fun renderAdjustments(
        source: Bitmap,
        adjustments: AdjustmentState,
        preset: FilterPreset = FilterPresets.NONE,
        filterIntensity: Float = 1.0f,
        curves: CurvesState = CurvesState(),
        hsl: HslState = HslState(),
        frameStyle: FrameStyle = FrameStyle.NONE,
        colorGrade: ColorGradeState = ColorGradeState(),
        lut: Lut3D? = null,
        lutIntensity: Float = 1.0f
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1. Compose Color Matrix combining Preset and Sliders
        val combinedMatrix = buildCombinedColorMatrix(adjustments, preset, filterIntensity)

        paint.colorFilter = ColorMatrixColorFilter(combinedMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)

        // 2. Tonal Adjustments (Highlights, Shadows, Whites, Blacks, Clarity), Curves & HSL
        applyToneCurvesAndHslInPlace(output, adjustments, curves, hsl)

        // 2b. 3-Way Tonal Color Grade (Shadows / Midtones / Highlights)
        if (colorGrade.hasVisibleEffect) {
            EffectsProcessor.applyColorGradingInPlace(output, colorGrade)
        }

        // 2c. 3D LUT (.cube) trilinear transform
        if (lut != null && lutIntensity > 0.005f) {
            CubeLutParser.applyLut3DInPlace(output, lut, lutIntensity)
        }

        // 3. Sharpness
        if (adjustments.sharpness > 0f) {
            applySharpnessInPlace(output, adjustments.sharpness)
        }

        // 3b. Optical Depth Lens Blur (Aperture Disc Bokeh + Specular Bloom)
        if (adjustments.backgroundBlur > 0f) {
            EffectsProcessor.applyDepthLensBlurInPlace(
                bitmap = output,
                blurStrength = adjustments.backgroundBlur,
                bokehBloom = adjustments.lensBokehBloom,
                focusX = adjustments.blurFocusX,
                focusY = adjustments.blurFocusY,
                focusRadius = adjustments.blurFocusRadius,
                protectSubject = adjustments.blurProtectSubject
            )
        }

        // 3c. Dynamic Motion Blur (Scene / Background / Subject)
        if (adjustments.motionBlurIntensity > 0f) {
            EffectsProcessor.applyMotionBlurInPlace(
                bitmap = output,
                intensity = adjustments.motionBlurIntensity,
                angleDegrees = adjustments.motionBlurAngle,
                target = adjustments.motionBlurTarget,
                focusX = adjustments.blurFocusX,
                focusY = adjustments.blurFocusY,
                focusRadius = adjustments.blurFocusRadius,
                protectSkinSubject = adjustments.blurProtectSubject
            )
        }

        // 4. Soft Dream Glow (Halation)
        if (adjustments.softGlow > 0f) {
            applySoftGlowInPlace(output, adjustments.softGlow)
        }

        // 5. Vignette
        val effVignette = adjustments.vignette + (preset.vignette * filterIntensity)
        if (effVignette != 0f) {
            applyVignette(canvas, width, height, effVignette)
        }

        // 6. Film Grain
        val effGrain = adjustments.grain + (preset.grain * filterIntensity)
        if (effGrain > 0f) {
            applyGrain(output, effGrain)
        }

        // 7. Studio Frame Overlay
        if (frameStyle != FrameStyle.NONE) {
            val framed = applyStudioFrame(output, frameStyle)
            if (framed !== output) {
                output.recycle()
            }
            return framed
        }

        return output
    }

    /**
     * Builds a hardware/GPU-friendly ColorMatrix for real-time live camera filter preview.
     */
    fun buildPresetColorMatrix(preset: FilterPreset, intensity: Float = 1.0f): ColorMatrix {
        return buildCombinedColorMatrix(AdjustmentState(), preset, intensity)
    }

    private fun buildCombinedColorMatrix(
        adjustments: AdjustmentState,
        preset: FilterPreset,
        filterIntensity: Float
    ): ColorMatrix {
        val combinedMatrix = ColorMatrix()

        val effExp = adjustments.exposure + (preset.exposure * filterIntensity)
        val effContrast = adjustments.contrast + (preset.contrast * filterIntensity)
        val effTemp = adjustments.temperature + (preset.temperature * filterIntensity)
        val effTint = adjustments.tint + (preset.tint * filterIntensity)
        val effSat = adjustments.saturation + (preset.saturation * filterIntensity)
        val effFade = adjustments.fade + (preset.fade * filterIntensity)

        if (preset.isGrayscale && filterIntensity > 0.05f) {
            val grayMatrix = ColorMatrix()
            grayMatrix.setSaturation(1.0f - filterIntensity)
            combinedMatrix.postConcat(grayMatrix)
        }

        if (preset.isSepia && filterIntensity > 0.05f) {
            combinedMatrix.postConcat(getSepiaColorMatrix(filterIntensity))
        }

        if (effExp != 0f) {
            val expFactor = 1.0f + (effExp / 100f) * 0.75f
            val expMatrix = ColorMatrix(
                floatArrayOf(
                    expFactor, 0f, 0f, 0f, (effExp * 0.6f),
                    0f, expFactor, 0f, 0f, (effExp * 0.6f),
                    0f, 0f, expFactor, 0f, (effExp * 0.6f),
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(expMatrix)
        }

        if (effContrast != 0f) {
            val scale = 1.0f + (effContrast / 100f) * 0.9f
            val translate = (-0.5f * scale + 0.5f) * 255f
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(contrastMatrix)
        }

        if (effTemp != 0f || effTint != 0f || (preset.redTint != 1.0f && filterIntensity > 0f)) {
            val rShift = (effTemp * 0.45f) + (preset.redTint - 1.0f) * filterIntensity * 60f
            val gShift = (-effTint * 0.4f) + (preset.greenTint - 1.0f) * filterIntensity * 60f
            val bShift = (-effTemp * 0.45f) + (effTint * 0.2f) + (preset.blueTint - 1.0f) * filterIntensity * 60f

            val tempMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, rShift,
                    0f, 1f, 0f, 0f, gShift,
                    0f, 0f, 1f, 0f, bShift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(tempMatrix)
        }

        if (effSat != 0f) {
            val satFactor = (1.0f + (effSat / 100f)).coerceAtLeast(0f)
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(satFactor)
            combinedMatrix.postConcat(satMatrix)
        }

        if (adjustments.vibrance != 0f) {
            val vibFactor = (1.0f + (adjustments.vibrance / 100f) * 0.75f).coerceAtLeast(0f)
            val vibMatrix = ColorMatrix()
            vibMatrix.setSaturation(vibFactor)
            combinedMatrix.postConcat(vibMatrix)
        }

        if (effFade > 0f) {
            val lift = (effFade / 100f) * 45f
            val fadeScale = 1.0f - (effFade / 100f) * 0.2f
            val fadeMatrix = ColorMatrix(
                floatArrayOf(
                    fadeScale, 0f, 0f, 0f, lift,
                    0f, fadeScale, 0f, 0f, lift,
                    0f, 0f, fadeScale, 0f, lift,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(fadeMatrix)
        }

        return combinedMatrix
    }

    /**
     * Analyzes the image luminance histogram percentiles (2nd, 50th, 98th) to compute
     * balanced exposure, contrast, highlight/shadow recovery, clarity, and vibrance.
     */
    fun computeAutoEnhance(bitmap: Bitmap): AdjustmentState {
        val hist = calculateHistogram(bitmap)
        val lum = hist.luminance
        val total = lum.sum().coerceAtLeast(1)

        var cumulative = 0
        var p2 = 0
        var median = 128
        var p98 = 255
        var foundP2 = false
        var foundMedian = false

        for (bin in 0..255) {
            cumulative += lum[bin]
            val ratio = cumulative.toFloat() / total
            if (!foundP2 && ratio >= 0.02f) {
                p2 = bin
                foundP2 = true
            }
            if (!foundMedian && ratio >= 0.50f) {
                median = bin
                foundMedian = true
            }
            if (ratio >= 0.98f) {
                p98 = bin
                break
            }
        }

        val exposureDelta = ((122f - median) * 0.45f).coerceIn(-38f, 42f).roundToInt().toFloat()
        val spread = (p98 - p2).coerceAtLeast(1)
        val contrastBoost = if (spread < 210) {
            (((210f - spread) / 210f) * 34f).coerceIn(4f, 32f).roundToInt().toFloat()
        } else {
            6f
        }
        val shadowRecovery = if (p2 < 24) {
            (((24f - p2) / 24f) * 26f + 8f).coerceIn(8f, 34f).roundToInt().toFloat()
        } else {
            8f
        }
        val highlightProtection = if (p98 > 235) {
            -(((p98 - 235f) / 20f) * 22f + 6f).coerceIn(6f, 28f).roundToInt().toFloat()
        } else {
            -6f
        }

        return AdjustmentState(
            exposure = exposureDelta,
            contrast = contrastBoost,
            highlights = highlightProtection,
            shadows = shadowRecovery,
            vibrance = 18f,
            clarity = 12f,
            sharpness = 12f
        )
    }

    /**
     * Fast downsampled bloom/halation pass screen-blended over highlights.
     */
    private fun applySoftGlowInPlace(bitmap: Bitmap, softGlow: Float) {
        val alpha = ((softGlow / 100f) * 140f).roundToInt().coerceIn(0, 150)
        if (alpha <= 0) return

        val smallW = (bitmap.width / 8).coerceAtLeast(16)
        val smallH = (bitmap.height / 8).coerceAtLeast(16)
        val downscaled = Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
        try {
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                this.alpha = alpha
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            }
            canvas.drawBitmap(
                downscaled,
                null,
                Rect(0, 0, bitmap.width, bitmap.height),
                paint
            )
        } finally {
            if (downscaled !== bitmap && !downscaled.isRecycled) {
                downscaled.recycle()
            }
        }
    }

    /**
     * Applies an optical depth-of-field background blur to [source] while keeping the focal
     * region (and optionally detected portrait skin/foreground subjects) crisp and sharp.
     */
    fun applyBackgroundBlur(
        source: Bitmap,
        blurStrength: Float,
        focusX: Float = 0.50f,
        focusY: Float = 0.42f,
        focusRadius: Float = 0.32f,
        protectSubject: Boolean = true,
        bokehBloom: Float = 25f
    ): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        if (blurStrength > 0f) {
            EffectsProcessor.applyDepthLensBlurInPlace(
                bitmap = out,
                blurStrength = blurStrength,
                bokehBloom = bokehBloom,
                focusX = focusX,
                focusY = focusY,
                focusRadius = focusRadius,
                protectSubject = protectSubject
            )
        }
        return out
    }

    /**
     * Fast two-pass separable bokeh blur blended via a feathered radial + optional subject-skin mask.
     */
    private fun applyBackgroundBlurInPlace(
        bitmap: Bitmap,
        blurStrength: Float,
        focusX: Float,
        focusY: Float,
        focusRadius: Float,
        protectSubject: Boolean
    ) {
        val strengthNorm = (blurStrength / 100f).coerceIn(0f, 1f)
        if (strengthNorm <= 0.005f) return

        val w = bitmap.width
        val h = bitmap.height
        if (w < 4 || h < 4) return

        val maxDim = max(w, h)
        val scaleDiv = when {
            maxDim >= 1200 -> 4
            maxDim >= 480 -> 3
            maxDim >= 140 -> 2
            else -> 1
        }
        val smallW = (w / scaleDiv).coerceAtLeast(min(w, 24)).coerceAtMost(360)
        val smallH = (h / scaleDiv).coerceAtLeast(min(h, 24)).coerceAtMost(360)

        val smallBmp = if (smallW == w && smallH == h) {
            bitmap.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
        }
        var upscaledBlur: Bitmap? = null

        try {
            val smallCount = smallW * smallH
            val smallPixels = IntArray(smallCount)
            smallBmp.getPixels(smallPixels, 0, smallW, 0, 0, smallW, smallH)

            // Build feathered skin/subject foreground mask before blurring smallPixels
            val subjectMask: FloatArray? = if (protectSubject) {
                val raw = FloatArray(smallCount)
                for (i in 0 until smallCount) {
                    val c = smallPixels[i]
                    val r = (c shr 16) and 0xFF
                    val g = (c shr 8) and 0xFF
                    val b = c and 0xFF
                    raw[i] = computeSkinProbability(r, g, b)
                }
                val dilated = dilateMask(raw, smallW, smallH, radius = 2)
                boxBlurFloatMask(dilated, smallW, smallH, radius = 2)
            } else {
                null
            }

            // Two separable box blur passes approximate a smooth Gaussian bokeh kernel
            val boxRadius = (1f + strengthNorm * 7f).roundToInt()
                .coerceIn(1, max(1, min(smallW, smallH) / 3))
            separableBoxBlurArgb(smallPixels, smallW, smallH, boxRadius)
            separableBoxBlurArgb(smallPixels, smallW, smallH, boxRadius)

            val mutableSmall = if (smallBmp.isMutable) {
                smallBmp
            } else {
                smallBmp.copy(Bitmap.Config.ARGB_8888, true)
            }
            mutableSmall.setPixels(smallPixels, 0, smallW, 0, 0, smallW, smallH)

            upscaledBlur = if (smallW == w && smallH == h) {
                mutableSmall
            } else {
                Bitmap.createScaledBitmap(mutableSmall, w, h, true).also {
                    if (mutableSmall !== smallBmp && !mutableSmall.isRecycled) {
                        mutableSmall.recycle()
                    }
                }
            }

            val total = w * h
            val origPixels = IntArray(total)
            val blurPixels = IntArray(total)
            bitmap.getPixels(origPixels, 0, w, 0, 0, w, h)
            upscaledBlur.getPixels(blurPixels, 0, w, 0, 0, w, h)

            val cx = focusX.coerceIn(0f, 1f) * (w - 1).coerceAtLeast(1)
            val cy = focusY.coerceIn(0f, 1f) * (h - 1).coerceAtLeast(1)
            val minDim = min(w, h).toFloat().coerceAtLeast(1f)
            val rx = (focusRadius.coerceIn(0.10f, 0.85f) * minDim).coerceAtLeast(2f)
            val ry = rx * 1.15f
            val invRx = 1f / rx
            val invRy = 1f / ry
            val blurBlendScale = (0.30f + 0.70f * strengthNorm).coerceIn(0f, 1f)
            val invW = if (w > 1) 1f / (w - 1) else 0f
            val invH = if (h > 1) 1f / (h - 1) else 0f

            for (y in 0 until h) {
                val dyNorm = (y - cy) * invRy
                val dySq = dyNorm * dyNorm
                val row = y * w
                val v = y * invH
                for (x in 0 until w) {
                    val dxNorm = (x - cx) * invRx
                    val normDist = sqrt(dxNorm * dxNorm + dySq)

                    val radialSharp = when {
                        normDist <= 0.65f -> 1f
                        normDist >= 1.45f -> 0f
                        else -> {
                            val t = (normDist - 0.65f) / 0.80f
                            1f - t * t * (3f - 2f * t)
                        }
                    }

                    val subjectSharp = if (subjectMask != null) {
                        sampleMaskBilinear(subjectMask, smallW, smallH, x * invW, v)
                    } else {
                        0f
                    }

                    val sharpWeight = max(radialSharp, subjectSharp).coerceIn(0f, 1f)
                    val blurWeight = ((1f - sharpWeight) * blurBlendScale).coerceIn(0f, 1f)
                    if (blurWeight <= 0.002f) continue

                    val idx = row + x
                    val orig = origPixels[idx]
                    val blur = blurPixels[idx]
                    val oA = (orig ushr 24) and 0xFF
                    val oR = (orig shr 16) and 0xFF
                    val oG = (orig shr 8) and 0xFF
                    val oB = orig and 0xFF

                    val bR = (blur shr 16) and 0xFF
                    val bG = (blur shr 8) and 0xFF
                    val bB = blur and 0xFF

                    val invWeight = 1f - blurWeight
                    val outR = (oR * invWeight + bR * blurWeight).roundToInt().coerceIn(0, 255)
                    val outG = (oG * invWeight + bG * blurWeight).roundToInt().coerceIn(0, 255)
                    val outB = (oB * invWeight + bB * blurWeight).roundToInt().coerceIn(0, 255)
                    origPixels[idx] = (oA shl 24) or (outR shl 16) or (outG shl 8) or outB
                }
            }

            bitmap.setPixels(origPixels, 0, w, 0, 0, w, h)
        } finally {
            if (upscaledBlur != null && upscaledBlur !== smallBmp && upscaledBlur !== bitmap && !upscaledBlur.isRecycled) {
                upscaledBlur.recycle()
            }
            if (smallBmp !== bitmap && !smallBmp.isRecycled) {
                smallBmp.recycle()
            }
        }
    }

    /**
     * O(W * H) sliding-window separable box blur on ARGB pixel array in-place.
     */
    private fun separableBoxBlurArgb(pixels: IntArray, width: Int, height: Int, radius: Int) {
        if (radius <= 0 || width <= 1 || height <= 1) return
        val temp = IntArray(width * height)
        val windowSize = radius * 2 + 1

        // Horizontal pass -> temp
        for (y in 0 until height) {
            val row = y * width
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (k in -radius..radius) {
                val p = pixels[row + k.coerceIn(0, width - 1)]
                sumR += (p shr 16) and 0xFF
                sumG += (p shr 8) and 0xFF
                sumB += p and 0xFF
            }
            for (x in 0 until width) {
                val a = (pixels[row + x] ushr 24) and 0xFF
                temp[row + x] = (a shl 24) or
                    ((sumR / windowSize) shl 16) or
                    ((sumG / windowSize) shl 8) or
                    (sumB / windowSize)
                val outP = pixels[row + (x - radius).coerceIn(0, width - 1)]
                val inP = pixels[row + (x + radius + 1).coerceIn(0, width - 1)]
                sumR += ((inP shr 16) and 0xFF) - ((outP shr 16) and 0xFF)
                sumG += ((inP shr 8) and 0xFF) - ((outP shr 8) and 0xFF)
                sumB += (inP and 0xFF) - (outP and 0xFF)
            }
        }

        // Vertical pass -> pixels
        for (x in 0 until width) {
            var sumR = 0
            var sumG = 0
            var sumB = 0
            for (k in -radius..radius) {
                val p = temp[k.coerceIn(0, height - 1) * width + x]
                sumR += (p shr 16) and 0xFF
                sumG += (p shr 8) and 0xFF
                sumB += p and 0xFF
            }
            for (y in 0 until height) {
                val idx = y * width + x
                val a = (temp[idx] ushr 24) and 0xFF
                pixels[idx] = (a shl 24) or
                    ((sumR / windowSize) shl 16) or
                    ((sumG / windowSize) shl 8) or
                    (sumB / windowSize)
                val outP = temp[(y - radius).coerceIn(0, height - 1) * width + x]
                val inP = temp[(y + radius + 1).coerceIn(0, height - 1) * width + x]
                sumR += ((inP shr 16) and 0xFF) - ((outP shr 16) and 0xFF)
                sumG += ((inP shr 8) and 0xFF) - ((outP shr 8) and 0xFF)
                sumB += (inP and 0xFF) - (outP and 0xFF)
            }
        }
    }

    private fun dilateMask(mask: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        val out = FloatArray(mask.size)
        for (y in 0 until height) {
            val y0 = (y - radius).coerceAtLeast(0)
            val y1 = (y + radius).coerceAtMost(height - 1)
            for (x in 0 until width) {
                val x0 = (x - radius).coerceAtLeast(0)
                val x1 = (x + radius).coerceAtMost(width - 1)
                var maxVal = 0f
                for (ny in y0..y1) {
                    val nRow = ny * width
                    for (nx in x0..x1) {
                        val v = mask[nRow + nx]
                        if (v > maxVal) maxVal = v
                    }
                }
                out[y * width + x] = maxVal
            }
        }
        return out
    }

    private fun boxBlurFloatMask(mask: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        val temp = FloatArray(mask.size)
        val out = FloatArray(mask.size)
        val win = radius * 2 + 1
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var sum = 0f
                for (dx in -radius..radius) {
                    sum += mask[row + (x + dx).coerceIn(0, width - 1)]
                }
                temp[row + x] = sum / win
            }
        }
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                var sum = 0f
                for (dy in -radius..radius) {
                    sum += temp[(y + dy).coerceIn(0, height - 1) * width + x]
                }
                out[row + x] = (sum / win).coerceIn(0f, 1f)
            }
        }
        return out
    }

    private fun sampleMaskBilinear(mask: FloatArray, width: Int, height: Int, normX: Float, normY: Float): Float {
        val gx = normX.coerceIn(0f, 1f) * (width - 1)
        val gy = normY.coerceIn(0f, 1f) * (height - 1)
        val x0 = gx.toInt().coerceIn(0, width - 1)
        val y0 = gy.toInt().coerceIn(0, height - 1)
        val x1 = (x0 + 1).coerceAtMost(width - 1)
        val y1 = (y0 + 1).coerceAtMost(height - 1)
        val fx = gx - x0
        val fy = gy - y0

        val v00 = mask[y0 * width + x0]
        val v10 = mask[y0 * width + x1]
        val v01 = mask[y1 * width + x0]
        val v11 = mask[y1 * width + x1]
        val top = v00 + (v10 - v00) * fx
        val bottom = v01 + (v11 - v01) * fx
        return (top + (bottom - top) * fy).coerceIn(0f, 1f)
    }

    /**
     * Applies a proportional gallery, Polaroid, cinema letterbox, or obsidian hairline frame.
     */
    fun applyStudioFrame(source: Bitmap, frameStyle: FrameStyle): Bitmap {
        if (frameStyle == FrameStyle.NONE) return source

        val w = source.width
        val h = source.height
        val minDim = min(w, h)

        return when (frameStyle) {
            FrameStyle.NONE -> source
            FrameStyle.CLASSIC_WHITE -> {
                val pad = (minDim * 0.048f).roundToInt().coerceAtLeast(8)
                val out = Bitmap.createBitmap(w + pad * 2, h + pad * 2, Bitmap.Config.ARGB_8888)
                out.eraseColor(Color.rgb(248, 248, 246))
                val srcPixels = IntArray(w * h)
                source.getPixels(srcPixels, 0, w, 0, 0, w, h)
                out.setPixels(srcPixels, 0, w, pad, pad, w, h)
                val canvas = Canvas(out)
                val borderPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    color = Color.argb(45, 0, 0, 0)
                    strokeWidth = (minDim * 0.0025f).coerceAtLeast(1f)
                }
                canvas.drawRect(pad.toFloat(), pad.toFloat(), (pad + w).toFloat(), (pad + h).toFloat(), borderPaint)
                out
            }
            FrameStyle.POLAROID -> {
                val sidePad = (minDim * 0.05f).roundToInt().coerceAtLeast(8)
                val bottomPad = (minDim * 0.19f).roundToInt().coerceAtLeast(28)
                val out = Bitmap.createBitmap(w + sidePad * 2, h + sidePad + bottomPad, Bitmap.Config.ARGB_8888)
                out.eraseColor(Color.rgb(247, 245, 240))
                val srcPixels = IntArray(w * h)
                source.getPixels(srcPixels, 0, w, 0, 0, w, h)
                out.setPixels(srcPixels, 0, w, sidePad, sidePad, w, h)
                val canvas = Canvas(out)
                val innerLine = Paint().apply {
                    style = Paint.Style.STROKE
                    color = Color.argb(38, 0, 0, 0)
                    strokeWidth = (minDim * 0.0025f).coerceAtLeast(1f)
                }
                canvas.drawRect(sidePad.toFloat(), sidePad.toFloat(), (sidePad + w).toFloat(), (sidePad + h).toFloat(), innerLine)
                out
            }
            FrameStyle.CINEMA -> {
                val bar = (h * 0.105f).roundToInt().coerceAtLeast(12).coerceAtMost(h / 3)
                val out = source.copy(Bitmap.Config.ARGB_8888, true)
                val barColor = Color.rgb(10, 10, 13)
                val barPixels = IntArray(w * bar) { barColor }
                out.setPixels(barPixels, 0, w, 0, 0, w, bar)
                out.setPixels(barPixels, 0, w, 0, h - bar, w, bar)
                out
            }
            FrameStyle.OBSIDIAN -> {
                val pad = (minDim * 0.048f).roundToInt().coerceAtLeast(8)
                val out = Bitmap.createBitmap(w + pad * 2, h + pad * 2, Bitmap.Config.ARGB_8888)
                out.eraseColor(Color.rgb(16, 17, 22))
                val srcPixels = IntArray(w * h)
                source.getPixels(srcPixels, 0, w, 0, 0, w, h)
                out.setPixels(srcPixels, 0, w, pad, pad, w, h)
                val canvas = Canvas(out)
                val goldPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    color = Color.rgb(212, 175, 55)
                    strokeWidth = (minDim * 0.0035f).coerceAtLeast(1.5f)
                }
                canvas.drawRect(pad.toFloat(), pad.toFloat(), (pad + w).toFloat(), (pad + h).toFloat(), goldPaint)
                out
            }
        }
    }

    /**
     * Calculates 256-bin histogram frequencies for R, G, B, and Luminance.
     * Strides across pixels to deliver real-time under-2ms performance.
     */
    fun calculateHistogram(bitmap: Bitmap): HistogramData {
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val stride = (total / 60000).coerceAtLeast(1)
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)
        val luminance = IntArray(256)
        var maxCount = 1

        var i = 0
        while (i < total) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val lum = ((r * 299) + (g * 587) + (b * 114)) / 1000

            red[r]++
            green[g]++
            blue[b]++
            luminance[lum]++

            if (red[r] > maxCount) maxCount = red[r]
            if (green[g] > maxCount) maxCount = green[g]
            if (blue[b] > maxCount) maxCount = blue[b]
            if (luminance[lum] > maxCount) maxCount = luminance[lum]

            i += stride
        }

        return HistogramData(
            red = red,
            green = green,
            blue = blue,
            luminance = luminance,
            maxCount = maxCount
        )
    }

    /**
     * Builds a 256-element zero-latency lookup table for Highlights, Shadows, Whites, Blacks, and Clarity.
     */
    private fun buildTonalLut(
        highlights: Float,
        shadows: Float,
        whites: Float,
        blacks: Float,
        clarity: Float
    ): IntArray {
        val lut = IntArray(256)
        val hlFactor = (highlights / 100f) * 45f
        val shFactor = (shadows / 100f) * 45f
        val whFactor = (whites / 100f) * 35f
        val blFactor = (blacks / 100f) * 35f
        val clFactor = (clarity / 100f) * 30f

        for (i in 0..255) {
            val norm = i / 255f
            var shift = 0f

            // Shadows bell curve (peak ~0.25)
            if (norm in 0f..0.65f) {
                val weight = sin(norm / 0.65f * Math.PI.toFloat())
                shift += shFactor * weight
            }

            // Highlights bell curve (peak ~0.75)
            if (norm in 0.35f..1f) {
                val weight = sin((norm - 0.35f) / 0.65f * Math.PI.toFloat())
                shift += hlFactor * weight
            }

            // Whites (specular highlight shoulder 0.65 to 1.0)
            if (norm > 0.65f) {
                val weight = (norm - 0.65f) / 0.35f
                shift += whFactor * (weight * weight)
            }

            // Blacks (deep shadow toe 0.0 to 0.35)
            if (norm < 0.35f) {
                val weight = (0.35f - norm) / 0.35f
                shift += blFactor * (weight * weight)
            }

            // Clarity (boosts midtone local contrast S-curve)
            if (clFactor != 0f) {
                val midtoneWeight = sin(norm * Math.PI.toFloat())
                shift += (norm - 0.5f) * clFactor * midtoneWeight * 1.5f
            }

            lut[i] = (i + shift).roundToInt().coerceIn(0, 255)
        }
        return lut
    }

    /**
     * Applies Tone Curves LUT, Highlights/Shadows/Whites/Blacks/Clarity LUT, and HSL
     * directly on the pixel buffer in a single unified pass.
     */
    private fun applyToneCurvesAndHslInPlace(
        bitmap: Bitmap,
        adjustments: AdjustmentState,
        curves: CurvesState,
        hsl: HslState
    ) {
        val hasTonal = adjustments.highlights != 0f || adjustments.shadows != 0f ||
                adjustments.whites != 0f || adjustments.blacks != 0f || adjustments.clarity != 0f
        val hasCurves = !curves.isDefault
        val hasHsl = !hsl.isDefault

        if (!hasTonal && !hasCurves && !hasHsl) return

        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height
        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val toneLut = if (hasTonal) {
            buildTonalLut(
                adjustments.highlights,
                adjustments.shadows,
                adjustments.whites,
                adjustments.blacks,
                adjustments.clarity
            )
        } else null

        val rgbLut = if (hasCurves) curves.rgb.buildLut() else null
        val rLut = if (hasCurves) curves.red.buildLut() else null
        val gLut = if (hasCurves) curves.green.buildLut() else null
        val bLut = if (hasCurves) curves.blue.buildLut() else null

        // Unified chained LUT combining Tonal adjustments and Curves
        val combinedRLut = IntArray(256) { i ->
            val t = toneLut?.get(i) ?: i
            val v = rgbLut?.get(t) ?: t
            rLut?.get(v) ?: v
        }
        val combinedGLut = IntArray(256) { i ->
            val t = toneLut?.get(i) ?: i
            val v = rgbLut?.get(t) ?: t
            gLut?.get(v) ?: v
        }
        val combinedBLut = IntArray(256) { i ->
            val t = toneLut?.get(i) ?: i
            val v = rgbLut?.get(t) ?: t
            bLut?.get(v) ?: v
        }

        if (!hasHsl) {
            // ULTRA HIGH-PERFORMANCE FAST PATH: Tonal + Curves only (no HSV conversion required)
            for (i in 0 until totalPixels) {
                val color = pixels[i]
                val a = (color shr 24) and 0xFF
                val r = combinedRLut[(color shr 16) and 0xFF]
                val g = combinedGLut[(color shr 8) and 0xFF]
                val b = combinedBLut[color and 0xFF]
                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return
        }

        // Tonal + Curves + Selective HSL path
        val hsv = FloatArray(3)
        for (i in 0 until totalPixels) {
            val color = pixels[i]
            val a = (color shr 24) and 0xFF
            val r = combinedRLut[(color shr 16) and 0xFF]
            val g = combinedGLut[(color shr 8) and 0xFF]
            val b = combinedBLut[color and 0xFF]

            Color.RGBToHSV(r, g, b, hsv)
            val hue = hsv[0]
            val sat = hsv[1]
            val lum = hsv[2]

            val matchedChannel = findClosestHslChannel(hue)
            val adj = hsl.getAdjustment(matchedChannel)
            if (!adj.isDefault) {
                val newHue = (hue + (adj.hue / 100f) * 30f + 360f) % 360f
                val newSat = (sat * (1.0f + adj.saturation / 100f)).coerceIn(0f, 1f)
                val newLum = (lum * (1.0f + adj.luminance / 100f)).coerceIn(0f, 1f)

                hsv[0] = newHue
                hsv[1] = newSat
                hsv[2] = newLum
                pixels[i] = Color.HSVToColor(a, hsv)
            } else {
                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    /**
     * Applies crisp 3x3 unsharp-mask convolution sharpening directly in-place.
     */
    private fun applySharpnessInPlace(bitmap: Bitmap, sharpness: Float) {
        if (sharpness <= 0f) return
        val factor = (sharpness / 100f) * 0.75f
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val src = IntArray(total)
        bitmap.getPixels(src, 0, width, 0, 0, width, height)
        val dst = IntArray(total)

        // Keep image boundary pixels
        for (x in 0 until width) {
            dst[x] = src[x]
            dst[(height - 1) * width + x] = src[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            dst[y * width] = src[y * width]
            dst[y * width + (width - 1)] = src[y * width + (width - 1)]
        }

        for (y in 1 until height - 1) {
            val yOffset = y * width
            for (x in 1 until width - 1) {
                val center = src[yOffset + x]
                val left = src[yOffset + x - 1]
                val right = src[yOffset + x + 1]
                val top = src[yOffset - width + x]
                val bottom = src[yOffset + width + x]

                val a = (center shr 24) and 0xFF
                val cr = (center shr 16) and 0xFF
                val cg = (center shr 8) and 0xFF
                val cb = center and 0xFF

                val lr = ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF) + ((top shr 16) and 0xFF) + ((bottom shr 16) and 0xFF)
                val lg = ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF) + ((top shr 8) and 0xFF) + ((bottom shr 8) and 0xFF)
                val lb = (left and 0xFF) + (right and 0xFF) + (top and 0xFF) + (bottom and 0xFF)

                val avgR = lr shr 2
                val avgG = lg shr 2
                val avgB = lb shr 2

                val nr = (cr + (cr - avgR) * factor).roundToInt().coerceIn(0, 255)
                val ng = (cg + (cg - avgG) * factor).roundToInt().coerceIn(0, 255)
                val nb = (cb + (cb - avgB) * factor).roundToInt().coerceIn(0, 255)

                dst[yOffset + x] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        bitmap.setPixels(dst, 0, width, 0, 0, width, height)
    }

    private fun findClosestHslChannel(hue: Float): HslColorChannel {
        return when (hue) {
            in 345f..360f, in 0f..15f -> HslColorChannel.RED
            in 15f..45f -> HslColorChannel.ORANGE
            in 45f..80f -> HslColorChannel.YELLOW
            in 80f..150f -> HslColorChannel.GREEN
            in 150f..210f -> HslColorChannel.CYAN
            in 210f..260f -> HslColorChannel.BLUE
            in 260f..300f -> HslColorChannel.PURPLE
            else -> HslColorChannel.MAGENTA
        }
    }

    private fun applyVignette(canvas: Canvas, width: Int, height: Int, vignette: Float) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = max(width, height) * 0.72f

        val isDark = vignette > 0
        val intensity = (Math.abs(vignette) / 100f).coerceIn(0f, 1f)
        val outerAlpha = (intensity * 210).toInt()

        val gradient = RadialGradient(
            cx, cy, radius,
            intArrayOf(Color.TRANSPARENT, if (isDark) Color.argb(outerAlpha, 0, 0, 0) else Color.argb(outerAlpha, 255, 255, 255)),
            floatArrayOf(0.4f, 1.0f),
            Shader.TileMode.CLAMP
        )

        val vPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
            xfermode = if (isDark) PorterDuffXfermode(PorterDuff.Mode.DARKEN) else PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vPaint)
    }

    private fun applyGrain(bitmap: Bitmap, grain: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height
        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val intensity = (grain / 100f) * 28f
        // Pre-generate 1024 noise samples for high performance cache cycling
        val rand = Random(42)
        val noiseTable = IntArray(1024) {
            ((rand.nextFloat() - 0.5f) * intensity).toInt()
        }

        for (i in 0 until totalPixels step 2) {
            val noise = noiseTable[i and 1023]
            val color = pixels[i]
            val a = (color shr 24) and 0xFF
            var r = ((color shr 16) and 0xFF) + noise
            var g = ((color shr 8) and 0xFF) + noise
            var b = (color and 0xFF) + noise

            r = r.coerceIn(0, 255)
            g = g.coerceIn(0, 255)
            b = b.coerceIn(0, 255)

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    /**
     * Executes interactive retouching strokes directly onto the target working bitmap.
     * When [stroke.isEraser] is true and a matching-dimension [originalBitmap] is supplied,
     * pixels within the stroke radius are smoothly restored toward the unretouched original.
     */
    fun applyRetouchStroke(
        targetBitmap: Bitmap,
        stroke: RetouchStroke,
        originalBitmap: Bitmap? = null
    ) {
        val width = targetBitmap.width
        val height = targetBitmap.height
        val rad = (stroke.radius * (min(width, height) / 400f)).toInt().coerceIn(6, min(width, height) / 4)
        val minStepDistSq = max(4f, (rad * 0.35f) * (rad * 0.35f))

        var lastPx = Int.MIN_VALUE
        var lastPy = Int.MIN_VALUE

        for (point in stroke.points) {
            val px = (point.normX * width).toInt().coerceIn(0, width - 1)
            val py = (point.normY * height).toInt().coerceIn(0, height - 1)

            var stepDx = 0f
            var stepDy = 0f
            if (lastPx != Int.MIN_VALUE) {
                val dx = (px - lastPx).toFloat()
                val dy = (py - lastPy).toFloat()
                if (dx * dx + dy * dy < minStepDistSq) continue
                stepDx = dx
                stepDy = dy
            }
            lastPx = px
            lastPy = py

            if (stroke.isEraser) {
                if (originalBitmap != null && originalBitmap.width == width && originalBitmap.height == height) {
                    restoreOriginalArea(targetBitmap, originalBitmap, px, py, rad, stroke.intensity)
                }
                continue
            }

            when (stroke.type) {
                RetouchType.SKIN_SMOOTH -> smoothSkinArea(targetBitmap, px, py, rad, stroke.intensity, stroke.texturePreserve)
                RetouchType.TEETH_WHITEN -> whitenTeethArea(targetBitmap, px, py, rad, stroke.intensity)
                RetouchType.EYE_POP -> brightenEyesArea(targetBitmap, px, py, rad, stroke.intensity)
                RetouchType.DETAIL_SHARPEN -> sharpenDetailArea(targetBitmap, px, py, rad, stroke.intensity)
                RetouchType.GLOW -> applyGlowHighlight(targetBitmap, px, py, rad, stroke.intensity)
                RetouchType.SCULPT -> sculptWarpArea(targetBitmap, px, py, rad, stroke.intensity, stepDx, stepDy)
            }
        }
    }

    /**
     * Restore Brush (Eraser):
     * Blends pixels back toward [originalBitmap] inside the feathered brush radius.
     */
    private fun restoreOriginalArea(
        targetBitmap: Bitmap,
        originalBitmap: Bitmap,
        cx: Int,
        cy: Int,
        radius: Int,
        intensity: Float
    ) {
        val width = targetBitmap.width
        val height = targetBitmap.height
        val x0 = (cx - radius).coerceAtLeast(0)
        val x1 = (cx + radius).coerceAtMost(width - 1)
        val y0 = (cy - radius).coerceAtLeast(0)
        val y1 = (cy + radius).coerceAtMost(height - 1)
        if (x1 < x0 || y1 < y0) return

        val patchW = x1 - x0 + 1
        val patchH = y1 - y0 + 1
        val dstPatch = IntArray(patchW * patchH)
        val origPatch = IntArray(patchW * patchH)
        targetBitmap.getPixels(dstPatch, 0, patchW, x0, y0, patchW, patchH)
        originalBitmap.getPixels(origPatch, 0, patchW, x0, y0, patchW, patchH)

        val strength = intensity.coerceIn(0.05f, 1f)
        val radF = radius.toFloat().coerceAtLeast(1f)

        for (y in y0..y1) {
            val dy = y - cy
            val rowOffset = (y - y0) * patchW
            for (x in x0..x1) {
                val dx = x - cx
                val dist = sqrt((dx * dx + dy * dy).toFloat())
                if (dist <= radF) {
                    val t = 1f - dist / radF
                    val alpha = (t * t * (3f - 2f * t) * strength).coerceIn(0f, 1f)
                    val idx = rowOffset + (x - x0)
                    val cur = dstPatch[idx]
                    val orig = origPatch[idx]

                    val cA = (cur ushr 24) and 0xFF
                    val cR = (cur shr 16) and 0xFF
                    val cG = (cur shr 8) and 0xFF
                    val cB = cur and 0xFF

                    val oA = (orig ushr 24) and 0xFF
                    val oR = (orig shr 16) and 0xFF
                    val oG = (orig shr 8) and 0xFF
                    val oB = orig and 0xFF

                    val outA = (cA + (oA - cA) * alpha).roundToInt().coerceIn(0, 255)
                    val outR = (cR + (oR - cR) * alpha).roundToInt().coerceIn(0, 255)
                    val outG = (cG + (oG - cG) * alpha).roundToInt().coerceIn(0, 255)
                    val outB = (cB + (oB - cB) * alpha).roundToInt().coerceIn(0, 255)

                    dstPatch[idx] = (outA shl 24) or (outR shl 16) or (outG shl 8) or outB
                }
            }
        }
        targetBitmap.setPixels(dstPatch, 0, patchW, x0, y0, patchW, patchH)
    }

    /**
     * Determines whether an RGB color belongs to human skin tone ranges
     * using standard YCbCr and normalized RGB thresholding models.
     * Works robustly across diverse skin tones (fair, warm, olive, dark brown).
     */
    fun isSkinTone(r: Int, g: Int, b: Int): Boolean {
        // Condition 1: Basic channel ordering & minimum range
        if (r <= g || g <= b || r - g < 8 || r < 45) return false

        // Condition 2: YCbCr skin cluster conversion
        // Y  =  0.299*R + 0.587*G + 0.114*B
        // Cb = -0.169*R - 0.331*G + 0.500*B + 128
        // Cr =  0.500*R - 0.419*G - 0.081*B + 128
        val cb = (-0.168736f * r - 0.331264f * g + 0.500000f * b + 128f)
        val cr = ( 0.500000f * r - 0.418688f * g - 0.081312f * b + 128f)

        val inSkinYCbCr = cb in 77f..135f && cr in 130f..178f

        // Condition 3: HSV Hue check (warm skin tones lie in 0°..50° or 335°..360°)
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val delta = maxC - minC
        val sat = if (maxC == 0) 0f else delta.toFloat() / maxC
        if (sat < 0.10f || sat > 0.85f) return false

        val hue = when {
            delta == 0 -> 0f
            maxC == r -> 60f * (((g - b).toFloat() / delta) % 6f)
            maxC == g -> 60f * (((b - r).toFloat() / delta) + 2f)
            else -> 60f * (((r - g).toFloat() / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }

        val inSkinHue = (hue in 0f..50f || hue in 335f..360f)

        return inSkinYCbCr && inSkinHue
    }

    /**
     * Computes a normalized 1D Gaussian kernel of length (2 * radius + 1).
     */
    private fun buildGaussianKernel1D(radius: Int, sigma: Float): FloatArray {
        val r = radius.coerceAtLeast(1)
        val s = sigma.coerceAtLeast(0.45f)
        val kernel = FloatArray(r * 2 + 1)
        val denom = 2f * s * s
        var sum = 0f
        for (k in -r..r) {
            val w = kotlin.math.exp(-(k * k).toFloat() / denom)
            kernel[k + r] = w
            sum += w
        }
        if (sum > 0f) {
            for (i in kernel.indices) kernel[i] /= sum
        }
        return kernel
    }

    /**
     * Soft continuous skin-tone probability [0f..1f] based on YCbCr & HSV cluster distance.
     */
    fun computeSkinProbability(r: Int, g: Int, b: Int): Float {
        if (!isSkinTone(r, g, b)) return 0f
        val cb = (-0.168736f * r - 0.331264f * g + 0.500000f * b + 128f)
        val cr = (0.500000f * r - 0.418688f * g - 0.081312f * b + 128f)
        // Core YCbCr skin cluster center ~(106, 154)
        val dCb = (cb - 106f) / 30f
        val dCr = (cr - 154f) / 25f
        val distSq = dCb * dCb + dCr * dCr
        return kotlin.math.exp(-distSq * 0.45f).coerceIn(0.45f, 1.0f)
    }

    /**
     * Feathers a raw skin probability mask using a separable Gaussian pass while keeping
     * strictly non-skin pixels at 0f so eyes, hair, and backgrounds never bleed.
     */
    private fun featherSkinMask(
        rawMask: FloatArray,
        width: Int,
        height: Int,
        radius: Int = 2,
        sigma: Float = 1.1f
    ): FloatArray {
        val kernel = buildGaussianKernel1D(radius, sigma)
        val horiz = FloatArray(width * height)
        val out = FloatArray(width * height)

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if (rawMask[row + x] <= 0f) continue
                var acc = 0f
                var wSum = 0f
                for (dx in -radius..radius) {
                    val nx = (x + dx).coerceIn(0, width - 1)
                    val k = kernel[dx + radius]
                    acc += rawMask[row + nx] * k
                    wSum += k
                }
                horiz[row + x] = if (wSum > 0f) acc / wSum else rawMask[row + x]
            }
        }

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if (rawMask[row + x] <= 0f) continue
                var acc = 0f
                var wSum = 0f
                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, height - 1)
                    val k = kernel[dy + radius]
                    acc += horiz[ny * width + x] * k
                    wSum += k
                }
                out[row + x] = (if (wSum > 0f) acc / wSum else horiz[row + x]).coerceIn(0f, 1f)
            }
        }
        return out
    }

    /**
     * Applies a mask-guided separable Gaussian blur to an ARGB pixel buffer, returning
     * float R, G, B channel arrays. Non-skin pixels (mask == 0f) are excluded from neighbor
     * weights so dark brows, lashes, or backgrounds cannot bleed into skin tones.
     */
    private fun maskGuidedGaussianBlurRgb(
        pixels: IntArray,
        mask: FloatArray,
        width: Int,
        height: Int,
        radius: Int,
        sigma: Float
    ): Triple<FloatArray, FloatArray, FloatArray> {
        val count = width * height
        val kernel = buildGaussianKernel1D(radius, sigma)

        val hR = FloatArray(count)
        val hG = FloatArray(count)
        val hB = FloatArray(count)

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                val idx = row + x
                val c = pixels[idx]
                if (mask[idx] <= 0f) {
                    hR[idx] = ((c shr 16) and 0xFF).toFloat()
                    hG[idx] = ((c shr 8) and 0xFF).toFloat()
                    hB[idx] = (c and 0xFF).toFloat()
                    continue
                }
                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var wSum = 0f
                for (dx in -radius..radius) {
                    val nx = (x + dx).coerceIn(0, width - 1)
                    val nIdx = row + nx
                    val m = mask[nIdx]
                    if (m <= 0f) continue
                    val w = kernel[dx + radius] * m
                    val p = pixels[nIdx]
                    sumR += ((p shr 16) and 0xFF) * w
                    sumG += ((p shr 8) and 0xFF) * w
                    sumB += (p and 0xFF) * w
                    wSum += w
                }
                if (wSum > 0f) {
                    hR[idx] = sumR / wSum
                    hG[idx] = sumG / wSum
                    hB[idx] = sumB / wSum
                } else {
                    hR[idx] = ((c shr 16) and 0xFF).toFloat()
                    hG[idx] = ((c shr 8) and 0xFF).toFloat()
                    hB[idx] = (c and 0xFF).toFloat()
                }
            }
        }

        val outR = FloatArray(count)
        val outG = FloatArray(count)
        val outB = FloatArray(count)

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                val idx = row + x
                if (mask[idx] <= 0f) {
                    outR[idx] = hR[idx]
                    outG[idx] = hG[idx]
                    outB[idx] = hB[idx]
                    continue
                }
                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var wSum = 0f
                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, height - 1)
                    val nIdx = ny * width + x
                    val m = mask[nIdx]
                    if (m <= 0f) continue
                    val w = kernel[dy + radius] * m
                    sumR += hR[nIdx] * w
                    sumG += hG[nIdx] * w
                    sumB += hB[nIdx] * w
                    wSum += w
                }
                if (wSum > 0f) {
                    outR[idx] = sumR / wSum
                    outG[idx] = sumG / wSum
                    outB[idx] = sumB / wSum
                } else {
                    outR[idx] = hR[idx]
                    outG[idx] = hG[idx]
                    outB[idx] = hB[idx]
                }
            }
        }

        return Triple(outR, outG, outB)
    }

    /**
     * Skin Smoothing Brush using Localized Gaussian Blur Masks & Frequency-Separated Detail Preservation:
     * 1. Builds a continuous YCbCr/HSV skin probability mask feathered by a Gaussian pass.
     * 2. Multiplies by a localized radial Gaussian brush mask centered at (cx, cy).
     * 3. Separates low-frequency skin tone (wide Gaussian blur) from micro-texture detail (fine Gaussian residual)
     *    and attenuates across high-contrast structural edges so pores soften naturally while features remain crisp.
     */
    private fun smoothSkinArea(
        bitmap: Bitmap,
        cx: Int,
        cy: Int,
        radius: Int,
        intensity: Float,
        texturePreserve: Float = 0.30f
    ) {
        val width = bitmap.width
        val height = bitmap.height
        val x0 = (cx - radius).coerceAtLeast(0)
        val x1 = (cx + radius).coerceAtMost(width - 1)
        val y0 = (cy - radius).coerceAtLeast(0)
        val y1 = (cy + radius).coerceAtMost(height - 1)
        if (x1 < x0 || y1 < y0) return

        val patchW = x1 - x0 + 1
        val patchH = y1 - y0 + 1
        val srcPatch = IntArray(patchW * patchH)
        val dstPatch = IntArray(patchW * patchH)
        bitmap.getPixels(srcPatch, 0, patchW, x0, y0, patchW, patchH)
        System.arraycopy(srcPatch, 0, dstPatch, 0, srcPatch.size)

        // 1. Build localized skin probability mask combined with Gaussian radial falloff
        val rawSkinMask = FloatArray(patchW * patchH)
        val radialWeights = FloatArray(patchW * patchH)
        val sigmaBrush = (radius * 0.58f).coerceAtLeast(1f)
        val twoSigmaBrushSq = 2f * sigmaBrush * sigmaBrush

        for (y in y0..y1) {
            val dy = (y - cy).toFloat()
            val row = (y - y0) * patchW
            for (x in x0..x1) {
                val dx = (x - cx).toFloat()
                val distSq = dx * dx + dy * dy
                if (distSq <= radius * radius) {
                    val idx = row + (x - x0)
                    val c = srcPatch[idx]
                    val cR = (c shr 16) and 0xFF
                    val cG = (c shr 8) and 0xFF
                    val cB = c and 0xFF
                    val skinProb = computeSkinProbability(cR, cG, cB)
                    if (skinProb > 0f) {
                        rawSkinMask[idx] = skinProb
                        radialWeights[idx] = kotlin.math.exp(-distSq / twoSigmaBrushSq)
                    }
                }
            }
        }

        val featheredMask = featherSkinMask(rawSkinMask, patchW, patchH, radius = 2, sigma = 1.0f)

        // 2. Frequency-separated Gaussian blurs: wide tonal blur + fine micro-texture reference
        val wideRadius = (radius / 5).coerceIn(2, 6)
        val wideSigma = (wideRadius * 0.65f).coerceAtLeast(1.2f)
        val (wideR, wideG, wideB) = maskGuidedGaussianBlurRgb(
            srcPatch, featheredMask, patchW, patchH, radius = wideRadius, sigma = wideSigma
        )
        val (fineR, fineG, fineB) = maskGuidedGaussianBlurRgb(
            srcPatch, featheredMask, patchW, patchH, radius = 1, sigma = 0.85f
        )

        val blendBase = intensity.coerceIn(0f, 1f)
        val detailKeep = texturePreserve.coerceIn(0f, 0.75f)
        val edgeThresholdSq = 2f * 36f * 36f

        for (i in srcPatch.indices) {
            val maskAlpha = featheredMask[i] * radialWeights[i]
            if (maskAlpha <= 0.005f) continue

            val c = srcPatch[i]
            val cA = (c shr 24) and 0xFF
            val cR = (c shr 16) and 0xFF
            val cG = (c shr 8) and 0xFF
            val cB = c and 0xFF

            // Structural edge preservation: attenuate smoothing where local contrast is strong
            val diffR = cR - wideR[i]
            val diffG = cG - wideG[i]
            val diffB = cB - wideB[i]
            val edgeEnergySq = diffR * diffR + diffG * diffG + diffB * diffB
            val edgePreservation = kotlin.math.exp(-edgeEnergySq / edgeThresholdSq)

            val alpha = (maskAlpha * edgePreservation * blendBase).coerceIn(0f, 1f)

            // Re-inject controlled high-frequency micro-texture residual (c - fine)
            val detailR = (cR - fineR[i]) * detailKeep
            val detailG = (cG - fineG[i]) * detailKeep
            val detailB = (cB - fineB[i]) * detailKeep

            val targetR = wideR[i] + detailR
            val targetG = wideG[i] + detailG
            val targetB = wideB[i] + detailB

            val outR = (cR * (1f - alpha) + targetR * alpha).roundToInt().coerceIn(0, 255)
            val outG = (cG * (1f - alpha) + targetG * alpha).roundToInt().coerceIn(0, 255)
            val outB = (cB * (1f - alpha) + targetB * alpha).roundToInt().coerceIn(0, 255)

            dstPatch[i] = (cA shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        bitmap.setPixels(dstPatch, 0, patchW, x0, y0, patchW, patchH)
    }

    /**
     * Automatic Full-Portrait Skin Smoothing using Localized Gaussian Blur Masks:
     * Detects skin regions via YCbCr + HSV segmentation, builds a Gaussian-feathered skin mask,
     * and applies mask-guided frequency-separated Gaussian smoothing with texture detail preservation
     * while keeping eyes, hair, lips, and non-skin backgrounds untouched.
     */
    fun applyPortraitSkinSmoothing(
        source: Bitmap,
        intensity: Float = 0.70f,
        texturePreserve: Float = 0.30f
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = source.copy(Bitmap.Config.ARGB_8888, true)

        val srcPixels = IntArray(width * height)
        val dstPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)
        System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.size)

        // 1. Identify continuous skin probability mask [0f..1f]
        val rawSkinMask = FloatArray(width * height)
        for (i in srcPixels.indices) {
            val c = srcPixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            rawSkinMask[i] = computeSkinProbability(r, g, b)
        }

        // 2. Gaussian-feather the skin mask (keeping strictly non-skin pixels at 0f)
        val maskFeatherRadius = (min(width, height) / 300).coerceIn(1, 3)
        val featheredMask = featherSkinMask(rawSkinMask, width, height, radius = maskFeatherRadius, sigma = 1.15f)

        // 3. Mask-guided separable Gaussian blurs for tonal base + micro-texture residual
        val wideRadius = (min(width, height) / 220).coerceIn(2, 6)
        val wideSigma = (wideRadius * 0.72f).coerceAtLeast(1.3f)
        val (wideR, wideG, wideB) = maskGuidedGaussianBlurRgb(
            srcPixels, featheredMask, width, height, radius = wideRadius, sigma = wideSigma
        )
        val (fineR, fineG, fineB) = maskGuidedGaussianBlurRgb(
            srcPixels, featheredMask, width, height, radius = 1, sigma = 0.85f
        )

        val blend = intensity.coerceIn(0f, 1f)
        val detailKeep = texturePreserve.coerceIn(0f, 0.75f)
        val edgeThresholdSq = 2f * 34f * 34f

        for (i in srcPixels.indices) {
            val maskAlpha = featheredMask[i]
            if (maskAlpha <= 0f) continue

            val c = srcPixels[i]
            val cA = (c shr 24) and 0xFF
            val cR = (c shr 16) and 0xFF
            val cG = (c shr 8) and 0xFF
            val cB = c and 0xFF

            val diffR = cR - wideR[i]
            val diffG = cG - wideG[i]
            val diffB = cB - wideB[i]
            val edgeEnergySq = diffR * diffR + diffG * diffG + diffB * diffB
            val edgePreservation = kotlin.math.exp(-edgeEnergySq / edgeThresholdSq)

            val alpha = (maskAlpha * edgePreservation * blend).coerceIn(0f, 1f)

            val detailR = (cR - fineR[i]) * detailKeep
            val detailG = (cG - fineG[i]) * detailKeep
            val detailB = (cB - fineB[i]) * detailKeep

            val targetR = wideR[i] + detailR
            val targetG = wideG[i] + detailG
            val targetB = wideB[i] + detailB

            val outR = (cR * (1f - alpha) + targetR * alpha).roundToInt().coerceIn(0, 255)
            val outG = (cG * (1f - alpha) + targetG * alpha).roundToInt().coerceIn(0, 255)
            val outB = (cB * (1f - alpha) + targetB * alpha).roundToInt().coerceIn(0, 255)

            dstPixels[i] = (cA shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        output.setPixels(dstPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Teeth Whitening:
     * Targets warm/yellowish pixels, reduces yellow saturation, and boosts luminosity.
     */
    private fun whitenTeethArea(bitmap: Bitmap, cx: Int, cy: Int, radius: Int, intensity: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val x0 = (cx - radius).coerceAtLeast(0)
        val x1 = (cx + radius).coerceAtMost(width - 1)
        val y0 = (cy - radius).coerceAtLeast(0)
        val y1 = (cy + radius).coerceAtMost(height - 1)
        if (x1 < x0 || y1 < y0) return

        val patchW = x1 - x0 + 1
        val patchH = y1 - y0 + 1
        val patch = IntArray(patchW * patchH)
        bitmap.getPixels(patch, 0, patchW, x0, y0, patchW, patchH)
        val hsv = FloatArray(3)

        for (y in y0..y1) {
            val dy = y - cy
            val rowOffset = (y - y0) * patchW
            for (x in x0..x1) {
                val dx = x - cx
                val dist = sqrt((dx * dx + dy * dy).toFloat())
                if (dist <= radius) {
                    val t = 1.0f - dist / radius
                    val falloff = t * t * (3f - 2f * t) * intensity
                    val idx = rowOffset + (x - x0)
                    val curr = patch[idx]
                    val cR = (curr shr 16) and 0xFF
                    val cG = (curr shr 8) and 0xFF
                    val cB = curr and 0xFF
                    val cA = (curr shr 24) and 0xFF

                    Color.RGBToHSV(cR, cG, cB, hsv)
                    val hue = hsv[0]
                    if (hue in 20f..75f && hsv[1] > 0.12f) {
                        hsv[1] = (hsv[1] * (1.0f - falloff * 0.75f)).coerceIn(0f, 1f)
                        hsv[2] = (hsv[2] * (1.0f + falloff * 0.35f)).coerceIn(0f, 1f)
                        patch[idx] = Color.HSVToColor(cA, hsv)
                    } else if (hsv[2] > 0.4f) {
                        hsv[2] = (hsv[2] * (1.0f + falloff * 0.20f)).coerceIn(0f, 1f)
                        patch[idx] = Color.HSVToColor(cA, hsv)
                    }
                }
            }
        }
        bitmap.setPixels(patch, 0, patchW, x0, y0, patchW, patchH)
    }

    /**
     * Eye Pop / Iris Brightener:
     * Enhances iris vibrancy, specular reflections, and clarity.
     */
    private fun brightenEyesArea(bitmap: Bitmap, cx: Int, cy: Int, radius: Int, intensity: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val x0 = (cx - radius).coerceAtLeast(0)
        val x1 = (cx + radius).coerceAtMost(width - 1)
        val y0 = (cy - radius).coerceAtLeast(0)
        val y1 = (cy + radius).coerceAtMost(height - 1)
        if (x1 < x0 || y1 < y0) return

        val patchW = x1 - x0 + 1
        val patchH = y1 - y0 + 1
        val patch = IntArray(patchW * patchH)
        bitmap.getPixels(patch, 0, patchW, x0, y0, patchW, patchH)
        val hsv = FloatArray(3)

        for (y in y0..y1) {
            val dy = y - cy
            val rowOffset = (y - y0) * patchW
            for (x in x0..x1) {
                val dx = x - cx
                val dist = sqrt((dx * dx + dy * dy).toFloat())
                if (dist <= radius) {
                    val t = 1.0f - dist / radius
                    val falloff = t * t * (3f - 2f * t) * intensity
                    val idx = rowOffset + (x - x0)
                    val curr = patch[idx]
                    val cA = (curr shr 24) and 0xFF

                    Color.colorToHSV(curr, hsv)
                    hsv[1] = (hsv[1] * (1.0f + falloff * 0.35f)).coerceIn(0f, 1f)
                    hsv[2] = (hsv[2] * (1.0f + falloff * 0.30f)).coerceIn(0f, 1f)
                    patch[idx] = Color.HSVToColor(cA, hsv)
                }
            }
        }
        bitmap.setPixels(patch, 0, patchW, x0, y0, patchW, patchH)
    }

    /**
     * Detail Sharpen:
     * Unsharp mask applied locally to eyebrows, eyelashes, lips, hair.
     */
    private fun sharpenDetailArea(bitmap: Bitmap, cx: Int, cy: Int, radius: Int, intensity: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val x0 = (cx - radius).coerceAtLeast(1)
        val x1 = (cx + radius).coerceAtMost(width - 2)
        val y0 = (cy - radius).coerceAtLeast(1)
        val y1 = (cy + radius).coerceAtMost(height - 2)
        if (x1 < x0 || y1 < y0) return

        // Fetch a 1px padded region so 4-neighbor lookups stay in srcPatch without bounds checks
        val padX0 = x0 - 1
        val padY0 = y0 - 1
        val padW = (x1 + 1) - padX0 + 1
        val padH = (y1 + 1) - padY0 + 1
        val srcPatch = IntArray(padW * padH)
        bitmap.getPixels(srcPatch, 0, padW, padX0, padY0, padW, padH)

        val dstW = x1 - x0 + 1
        val dstH = y1 - y0 + 1
        val dstPatch = IntArray(dstW * dstH)

        for (y in y0..y1) {
            val dy = y - cy
            val srcY = y - padY0
            val dstRow = (y - y0) * dstW
            for (x in x0..x1) {
                val dx = x - cx
                val srcX = x - padX0
                val srcIdx = srcY * padW + srcX
                val center = srcPatch[srcIdx]
                val dist = sqrt((dx * dx + dy * dy).toFloat())

                if (dist <= radius) {
                    val t = 1.0f - dist / radius
                    val weight = t * t * (3f - 2f * t) * intensity * 0.85f
                    val up = srcPatch[(srcY - 1) * padW + srcX]
                    val down = srcPatch[(srcY + 1) * padW + srcX]
                    val left = srcPatch[srcIdx - 1]
                    val right = srcPatch[srcIdx + 1]

                    val cR = (center shr 16) and 0xFF
                    val cG = (center shr 8) and 0xFF
                    val cB = center and 0xFF
                    val cA = (center shr 24) and 0xFF

                    val avgR = (((up shr 16) and 0xFF) + ((down shr 16) and 0xFF) + ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF)) shr 2
                    val avgG = (((up shr 8) and 0xFF) + ((down shr 8) and 0xFF) + ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF)) shr 2
                    val avgB = ((up and 0xFF) + (down and 0xFF) + (left and 0xFF) + (right and 0xFF)) shr 2

                    val sharpR = (cR + (cR - avgR) * weight).toInt().coerceIn(0, 255)
                    val sharpG = (cG + (cG - avgG) * weight).toInt().coerceIn(0, 255)
                    val sharpB = (cB + (cB - avgB) * weight).toInt().coerceIn(0, 255)

                    dstPatch[dstRow + (x - x0)] = (cA shl 24) or (sharpR shl 16) or (sharpG shl 8) or sharpB
                } else {
                    dstPatch[dstRow + (x - x0)] = center
                }
            }
        }
        bitmap.setPixels(dstPatch, 0, dstW, x0, y0, dstW, dstH)
    }

    /**
     * Soft Beauty Highlighter / Glow Brush:
     */
    private fun applyGlowHighlight(bitmap: Bitmap, cx: Int, cy: Int, radius: Int, intensity: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val x0 = (cx - radius).coerceAtLeast(0)
        val x1 = (cx + radius).coerceAtMost(width - 1)
        val y0 = (cy - radius).coerceAtLeast(0)
        val y1 = (cy + radius).coerceAtMost(height - 1)
        if (x1 < x0 || y1 < y0) return

        val patchW = x1 - x0 + 1
        val patchH = y1 - y0 + 1
        val patch = IntArray(patchW * patchH)
        bitmap.getPixels(patch, 0, patchW, x0, y0, patchW, patchH)

        val glowColorR = 255
        val glowColorG = 245
        val glowColorB = 220

        for (y in y0..y1) {
            val dy = y - cy
            val rowOffset = (y - y0) * patchW
            for (x in x0..x1) {
                val dx = x - cx
                val dist = sqrt((dx * dx + dy * dy).toFloat())
                if (dist <= radius) {
                    val norm = dist / radius
                    val smooth = (1.0f - norm * norm)
                    val falloff = smooth * smooth * intensity * 0.42f
                    val idx = rowOffset + (x - x0)
                    val curr = patch[idx]
                    val cR = (curr shr 16) and 0xFF
                    val cG = (curr shr 8) and 0xFF
                    val cB = curr and 0xFF
                    val cA = (curr shr 24) and 0xFF

                    val newR = 255 - ((255 - cR) * (255 - (glowColorR * falloff).toInt()) / 255)
                    val newG = 255 - ((255 - cG) * (255 - (glowColorG * falloff).toInt()) / 255)
                    val newB = 255 - ((255 - cB) * (255 - (glowColorB * falloff).toInt()) / 255)

                    patch[idx] = (cA shl 24) or (newR.coerceIn(0, 255) shl 16) or (newG.coerceIn(0, 255) shl 8) or newB.coerceIn(0, 255)
                }
            }
        }
        bitmap.setPixels(patch, 0, patchW, x0, y0, patchW, patchH)
    }

    /**
     * Sculpt & Warp:
     * Directional forward-push liquify warp with bilinear sampling along drag strokes,
     * falling back to smooth radial contour firming on a single tap.
     */
    private fun sculptWarpArea(
        bitmap: Bitmap,
        cx: Int,
        cy: Int,
        radius: Int,
        intensity: Float,
        pushDx: Float = 0f,
        pushDy: Float = 0f
    ) {
        val width = bitmap.width
        val height = bitmap.height
        val x0 = (cx - radius).coerceAtLeast(0)
        val x1 = (cx + radius).coerceAtMost(width - 1)
        val y0 = (cy - radius).coerceAtLeast(0)
        val y1 = (cy + radius).coerceAtMost(height - 1)
        if (x1 < x0 || y1 < y0) return

        val bWidth = x1 - x0 + 1
        val bHeight = y1 - y0 + 1
        val srcBuffer = IntArray(bWidth * bHeight)
        val dstBuffer = IntArray(bWidth * bHeight)
        bitmap.getPixels(srcBuffer, 0, bWidth, x0, y0, bWidth, bHeight)
        System.arraycopy(srcBuffer, 0, dstBuffer, 0, srcBuffer.size)

        val pushMag = sqrt(pushDx * pushDx + pushDy * pushDy)
        val isDirectional = pushMag > 0.5f
        val radF = radius.toFloat().coerceAtLeast(1f)
        val maxShift = radF * 0.45f * intensity.coerceIn(0.1f, 1f)
        val shiftScale = if (isDirectional) (maxShift / pushMag).coerceAtMost(0.85f * intensity) else 0f
        val dirX = pushDx * shiftScale
        val dirY = pushDy * shiftScale
        val pinchFactor = intensity * 0.28f
        val maxBx = bWidth - 1
        val maxBy = bHeight - 1

        for (y in y0..y1) {
            val dy = (y - cy).toFloat()
            val rowOffset = (y - y0) * bWidth
            for (x in x0..x1) {
                val dx = (x - cx).toFloat()
                val distSq = dx * dx + dy * dy
                val radSq = radF * radF
                if (distSq <= radSq) {
                    val normDistSq = distSq / radSq
                    val falloff = (1.0f - normDistSq) * (1.0f - normDistSq)

                    val sampleX: Float
                    val sampleY: Float
                    if (isDirectional) {
                        // Inverse mapping: pull source pixels from behind the stroke direction
                        sampleX = (x - dirX * falloff) - x0
                        sampleY = (y - dirY * falloff) - y0
                    } else {
                        val normDist = sqrt(normDistSq)
                        val warp = 1.0f - pinchFactor * (1.0f - normDist)
                        sampleX = (cx + dx * warp) - x0
                        sampleY = (cy + dy * warp) - y0
                    }

                    val sx = sampleX.coerceIn(0f, maxBx.toFloat())
                    val sy = sampleY.coerceIn(0f, maxBy.toFloat())
                    val ix = sx.toInt().coerceAtMost(maxBx)
                    val iy = sy.toInt().coerceAtMost(maxBy)
                    val nx = (ix + 1).coerceAtMost(maxBx)
                    val ny = (iy + 1).coerceAtMost(maxBy)
                    val fx = sx - ix
                    val fy = sy - iy

                    val c00 = srcBuffer[iy * bWidth + ix]
                    val c10 = srcBuffer[iy * bWidth + nx]
                    val c01 = srcBuffer[ny * bWidth + ix]
                    val c11 = srcBuffer[ny * bWidth + nx]

                    val w00 = (1f - fx) * (1f - fy)
                    val w10 = fx * (1f - fy)
                    val w01 = (1f - fx) * fy
                    val w11 = fx * fy

                    val a = (((c00 ushr 24) and 0xFF) * w00 + ((c10 ushr 24) and 0xFF) * w10 +
                        ((c01 ushr 24) and 0xFF) * w01 + ((c11 ushr 24) and 0xFF) * w11).roundToInt().coerceIn(0, 255)
                    val r = (((c00 shr 16) and 0xFF) * w00 + ((c10 shr 16) and 0xFF) * w10 +
                        ((c01 shr 16) and 0xFF) * w01 + ((c11 shr 16) and 0xFF) * w11).roundToInt().coerceIn(0, 255)
                    val g = (((c00 shr 8) and 0xFF) * w00 + ((c10 shr 8) and 0xFF) * w10 +
                        ((c01 shr 8) and 0xFF) * w01 + ((c11 shr 8) and 0xFF) * w11).roundToInt().coerceIn(0, 255)
                    val b = ((c00 and 0xFF) * w00 + (c10 and 0xFF) * w10 +
                        (c01 and 0xFF) * w01 + (c11 and 0xFF) * w11).roundToInt().coerceIn(0, 255)

                    dstBuffer[rowOffset + (x - x0)] = (a shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }
        bitmap.setPixels(dstBuffer, 0, bWidth, x0, y0, bWidth, bHeight)
    }

    /**
     * Transform: Crop, Rotate & Flip
     */
    fun transformBitmap(
        source: Bitmap,
        rotationDegrees: Int,
        flipHorizontal: Boolean,
        flipVertical: Boolean,
        cropRatio: CropAspectRatio
    ): Bitmap {
        var bmp = source

        // Rotate & Flip
        if (rotationDegrees != 0 || flipHorizontal || flipVertical) {
            val matrix = Matrix()
            if (rotationDegrees != 0) matrix.postRotate(rotationDegrees.toFloat())
            val scaleX = if (flipHorizontal) -1f else 1f
            val scaleY = if (flipVertical) -1f else 1f
            matrix.postScale(scaleX, scaleY)
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        }

        // Crop Ratio
        val targetRatio = cropRatio.ratio
        if (targetRatio != null) {
            val currentRatio = bmp.width.toFloat() / bmp.height.toFloat()
            if (Math.abs(currentRatio - targetRatio) > 0.01f) {
                var cropW = bmp.width
                var cropH = bmp.height
                if (currentRatio > targetRatio) {
                    // Too wide, crop width
                    cropW = (bmp.height * targetRatio).toInt().coerceIn(1, bmp.width)
                } else {
                    // Too tall, crop height
                    cropH = (bmp.width / targetRatio).toInt().coerceIn(1, bmp.height)
                }
                val offsetX = (bmp.width - cropW) / 2
                val offsetY = (bmp.height - cropH) / 2
                bmp = Bitmap.createBitmap(bmp, offsetX, offsetY, cropW, cropH)
            }
        }

        return bmp
    }

    /**
     * Crops [source] using normalized [0f..1f] bounds from [cropRect].
     */
    fun cropBitmap(source: Bitmap, cropRect: com.example.model.CropRect): Bitmap {
        if (cropRect.isFull) return source
        val minPx = 16.coerceAtMost(min(source.width, source.height))
        val leftPx = (cropRect.left.coerceIn(0f, 1f) * source.width)
            .roundToInt()
            .coerceIn(0, (source.width - minPx).coerceAtLeast(0))
        val topPx = (cropRect.top.coerceIn(0f, 1f) * source.height)
            .roundToInt()
            .coerceIn(0, (source.height - minPx).coerceAtLeast(0))
        val rightPx = (cropRect.right.coerceIn(0f, 1f) * source.width)
            .roundToInt()
            .coerceIn((leftPx + minPx).coerceAtMost(source.width), source.width)
        val bottomPx = (cropRect.bottom.coerceIn(0f, 1f) * source.height)
            .roundToInt()
            .coerceIn((topPx + minPx).coerceAtMost(source.height), source.height)

        val cropW = (rightPx - leftPx).coerceAtLeast(1)
        val cropH = (bottomPx - topPx).coerceAtLeast(1)
        if (leftPx == 0 && topPx == 0 && cropW == source.width && cropH == source.height) {
            return source
        }
        return Bitmap.createBitmap(source, leftPx, topPx, cropW, cropH)
    }

    /**
     * Builds a ColorMatrix for authentic photographic Sepia toning.
     */
    fun getSepiaColorMatrix(intensity: Float = 1.0f): ColorMatrix {
        val i = intensity.coerceIn(0f, 1f)
        val inv = 1f - i
        return ColorMatrix(
            floatArrayOf(
                inv + 0.393f * i, 0.769f * i, 0.189f * i, 0f, 0f,
                0.349f * i, inv + 0.686f * i, 0.168f * i, 0f, 0f,
                0.272f * i, 0.534f * i, inv + 0.131f * i, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    /**
     * Applies Grayscale (monochrome) filter using native Android Bitmap & ColorMatrix.
     */
    fun applyGrayscale(source: Bitmap, intensity: Float = 1.0f): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val matrix = ColorMatrix()
        matrix.setSaturation(1.0f - intensity.coerceIn(0f, 1f))
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /**
     * Applies Sepia tone filter using native Android Bitmap & ColorMatrix.
     */
    fun applySepia(source: Bitmap, intensity: Float = 1.0f): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(getSepiaColorMatrix(intensity))
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /**
     * Applies High-Contrast filter using native Android Bitmap & ColorMatrix.
     */
    fun applyHighContrast(source: Bitmap, contrastLevel: Float = 1.6f): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val scale = contrastLevel.coerceAtLeast(0f)
        val translate = (-0.5f * scale + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }
}
