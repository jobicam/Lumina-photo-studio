package com.example.processing

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.ColorGradeState
import com.example.model.ColorWheelValue
import com.example.model.MotionBlurTarget
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object EffectsProcessor {

    /**
     * Optical Depth Lens Blur:
     * Combines an aperture disc bokeh gather weighted by specular highlight bloom
     * with a natural radial + depth-plane + portrait-subject focus mask.
     */
    fun applyDepthLensBlurInPlace(
        bitmap: Bitmap,
        blurStrength: Float,
        bokehBloom: Float = 25f,
        focusX: Float = 0.50f,
        focusY: Float = 0.42f,
        focusRadius: Float = 0.32f,
        protectSubject: Boolean = true
    ) {
        val strengthNorm = (blurStrength / 100f).coerceIn(0f, 1f)
        if (strengthNorm <= 0.01f) return

        val width = bitmap.width
        val height = bitmap.height
        val maxDim = max(width, height)
        // Downscale blur buffer so optical disc bokeh stays fast (~15-30ms) even on 1800px photos
        val workScale = (360f / maxDim.toFloat()).coerceIn(0.20f, 1.0f)
        val smallW = (width * workScale).roundToInt().coerceAtLeast(16)
        val smallH = (height * workScale).roundToInt().coerceAtLeast(16)

        val smallBmp = if (smallW == width && smallH == height) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
        }

        val smallTotal = smallW * smallH
        val smallPixels = IntArray(smallTotal)
        smallBmp.getPixels(smallPixels, 0, smallW, 0, 0, smallW, smallH)
        if (smallBmp !== bitmap && !smallBmp.isRecycled) smallBmp.recycle()

        // 1. Optional subject protection mask (skin-tone foreground protection + dilation + feather)
        val subjectMask = if (protectSubject) {
            val rawMask = FloatArray(smallTotal)
            for (i in 0 until smallTotal) {
                val c = smallPixels[i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                rawMask[i] = BitmapProcessor.computeSkinProbability(r, g, b)
            }
            val dilateR = (min(smallW, smallH) * 0.045f).roundToInt().coerceIn(2, 14)
            val dilated = dilateMask(rawMask, smallW, smallH, dilateR)
            boxBlurFloatMask(dilated, smallW, smallH, (dilateR / 2).coerceAtLeast(2))
        } else {
            null
        }

        // 2. Optical aperture disc bokeh gather with specular highlight bloom weighting
        val blurRadiusPx = (min(smallW, smallH) * 0.068f * strengthNorm).coerceIn(1.5f, 22f)
        val bloomBoost = (bokehBloom / 100f).coerceIn(0f, 1f) * 3.8f
        val blurredSmall = applyDiscBokeh(smallPixels, smallW, smallH, blurRadiusPx, bloomBoost)

        // 3. Composite blurred pixels onto original full-res buffer via feathered radial + depth mask
        val origPixels = IntArray(width * height)
        bitmap.getPixels(origPixels, 0, width, 0, 0, width, height)

        val innerR = focusRadius.coerceIn(0.10f, 0.72f) * 0.72f
        val outerR = (innerR + 0.30f).coerceAtMost(1.15f)
        val invSpan = 1f / (outerR - innerR).coerceAtLeast(0.05f)
        val aspectRatio = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        val focusCx = focusX.coerceIn(0f, 1f)
        val focusCy = focusY.coerceIn(0f, 1f)
        val scaleXToSmall = (smallW - 1f) / (width - 1).coerceAtLeast(1)
        val scaleYToSmall = (smallH - 1f) / (height - 1).coerceAtLeast(1)

        for (y in 0 until height) {
            val ny = y.toFloat() / height
            val dy = ny - focusCy
            // Subtle foreground/background depth gradient: background planes blur naturally stronger
            val depthBoost = (1.0f + (focusCy - ny) * 0.16f).coerceIn(0.88f, 1.15f)
            val sy = y * scaleYToSmall
            val sy0 = sy.toInt().coerceIn(0, smallH - 1)
            val sy1 = min(sy0 + 1, smallH - 1)
            val fy = sy - sy0
            val rowOffset = y * width
            val sRow0 = sy0 * smallW
            val sRow1 = sy1 * smallW

            for (x in 0 until width) {
                val nx = x.toFloat() / width
                val dx = (nx - focusCx) * aspectRatio
                val dist = sqrt(dx * dx + dy * dy)
                val radialT = ((dist - innerR) * invSpan).coerceIn(0f, 1f)
                var blurAlpha = radialT * radialT * (3f - 2f * radialT)
                blurAlpha = (blurAlpha * depthBoost).coerceIn(0f, 1f)

                if (blurAlpha > 0.004f) {
                    val sx = x * scaleXToSmall
                    val sx0 = sx.toInt().coerceIn(0, smallW - 1)
                    val sx1 = min(sx0 + 1, smallW - 1)
                    val fx = sx - sx0

                    if (subjectMask != null) {
                        val sm = bilinearFloat(subjectMask, sRow0, sRow1, sx0, sx1, fx, fy)
                        blurAlpha *= (1f - (sm * 1.18f).coerceIn(0f, 1f))
                    }

                    if (blurAlpha > 0.004f) {
                        val idx = rowOffset + x
                        val orig = origPixels[idx]
                        val a = (orig ushr 24) and 0xFF
                        val oR = (orig shr 16) and 0xFF
                        val oG = (orig shr 8) and 0xFF
                        val oB = orig and 0xFF

                        val c00 = blurredSmall[sRow0 + sx0]
                        val c10 = blurredSmall[sRow0 + sx1]
                        val c01 = blurredSmall[sRow1 + sx0]
                        val c11 = blurredSmall[sRow1 + sx1]
                        val w00 = (1f - fx) * (1f - fy)
                        val w10 = fx * (1f - fy)
                        val w01 = (1f - fx) * fy
                        val w11 = fx * fy

                        val bR = ((c00 shr 16) and 0xFF) * w00 +
                            ((c10 shr 16) and 0xFF) * w10 +
                            ((c01 shr 16) and 0xFF) * w01 +
                            ((c11 shr 16) and 0xFF) * w11
                        val bG = ((c00 shr 8) and 0xFF) * w00 +
                            ((c10 shr 8) and 0xFF) * w10 +
                            ((c01 shr 8) and 0xFF) * w01 +
                            ((c11 shr 8) and 0xFF) * w11
                        val bB = (c00 and 0xFF) * w00 +
                            (c10 and 0xFF) * w10 +
                            (c01 and 0xFF) * w01 +
                            (c11 and 0xFF) * w11

                        val inv = 1f - blurAlpha
                        val outR = (oR * inv + bR * blurAlpha).roundToInt().coerceIn(0, 255)
                        val outG = (oG * inv + bG * blurAlpha).roundToInt().coerceIn(0, 255)
                        val outB = (oB * inv + bB * blurAlpha).roundToInt().coerceIn(0, 255)
                        origPixels[idx] = (a shl 24) or (outR shl 16) or (outG shl 8) or outB
                    }
                }
            }
        }

        bitmap.setPixels(origPixels, 0, width, 0, 0, width, height)
    }

    /**
     * Directional Motion Blur engine supporting Scene, Background, or Subject isolation.
     */
    fun applyMotionBlurInPlace(
        bitmap: Bitmap,
        intensity: Float,
        angleDegrees: Float,
        target: MotionBlurTarget,
        focusX: Float = 0.50f,
        focusY: Float = 0.42f,
        focusRadius: Float = 0.32f,
        protectSkinSubject: Boolean = true
    ) {
        val strengthNorm = (intensity / 100f).coerceIn(0f, 1f)
        if (strengthNorm <= 0.01f) return

        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val srcPixels = IntArray(total)
        bitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)
        val outPixels = srcPixels.copyOf()

        val rad = angleDegrees * (PI.toFloat() / 180f)
        val dirX = cos(rad)
        val dirY = sin(rad)
        val maxTrailPx = (max(width, height) * 0.085f * strengthNorm).coerceIn(2f, 90f)
        val taps = 17
        val halfTaps = taps / 2

        // Precompute tap offsets and symmetric Gaussian-like directional trail weights
        val tapOffsetsX = FloatArray(taps)
        val tapOffsetsY = FloatArray(taps)
        val tapWeights = FloatArray(taps)
        for (k in 0 until taps) {
            val t = (k - halfTaps).toFloat() / halfTaps.toFloat() // -1..1
            tapOffsetsX[k] = t * maxTrailPx * dirX
            tapOffsetsY[k] = t * maxTrailPx * dirY
            // Center-weighted directional falloff for clean, non-stepped motion trails
            tapWeights[k] = 1.0f - 0.55f * (t * t)
        }

        // Build subject mask when targeting BACKGROUND or SUBJECT
        val skinMask: FloatArray? = if (target != MotionBlurTarget.SCENE && protectSkinSubject) {
            val raw = FloatArray(total)
            for (i in 0 until total) {
                val c = srcPixels[i]
                raw[i] = BitmapProcessor.computeSkinProbability(
                    (c shr 16) and 0xFF,
                    (c shr 8) and 0xFF,
                    c and 0xFF
                )
            }
            boxBlurFloatMask(raw, width, height, (min(width, height) * 0.02f).roundToInt().coerceIn(1, 8))
        } else {
            null
        }

        val innerR = focusRadius.coerceIn(0.10f, 0.72f) * 0.78f
        val outerR = (innerR + 0.24f).coerceAtMost(1.1f)
        val invSpan = 1f / (outerR - innerR).coerceAtLeast(0.05f)
        val aspectRatio = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        val focusCx = focusX.coerceIn(0f, 1f)
        val focusCy = focusY.coerceIn(0f, 1f)

        for (y in 0 until height) {
            val ny = y.toFloat() / height
            val dy = ny - focusCy
            val rowOffset = y * width

            for (x in 0 until width) {
                val idx = rowOffset + x
                val motionAlpha = when (target) {
                    MotionBlurTarget.SCENE -> 1f
                    MotionBlurTarget.BACKGROUND, MotionBlurTarget.SUBJECT -> {
                        val nx = x.toFloat() / width
                        val dx = (nx - focusCx) * aspectRatio
                        val dist = sqrt(dx * dx + dy * dy)
                        val t = ((dist - innerR) * invSpan).coerceIn(0f, 1f)
                        val smoothOuter = t * t * (3f - 2f * t)
                        var subjectPresence = 1f - smoothOuter
                        if (skinMask != null) {
                            subjectPresence = max(subjectPresence, (skinMask[idx] * 1.15f).coerceIn(0f, 1f))
                        }
                        if (target == MotionBlurTarget.BACKGROUND) {
                            1f - subjectPresence
                        } else {
                            subjectPresence
                        }
                    }
                }

                if (motionAlpha <= 0.01f) continue

                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var wSum = 0f

                for (k in 0 until taps) {
                    val sx = (x + tapOffsetsX[k] * motionAlpha).roundToInt().coerceIn(0, width - 1)
                    val sy = (y + tapOffsetsY[k] * motionAlpha).roundToInt().coerceIn(0, height - 1)
                    val sample = srcPixels[sy * width + sx]
                    val sr = (sample shr 16) and 0xFF
                    val sg = (sample shr 8) and 0xFF
                    val sb = sample and 0xFF
                    // Slight bright-highlight streak boost so specular reflections trail realistically
                    val lum = (sr * 0.299f + sg * 0.587f + sb * 0.114f) / 255f
                    val highlightBoost = if (lum > 0.72f) 1f + (lum - 0.72f) * 1.6f else 1f
                    val w = tapWeights[k] * highlightBoost
                    sumR += sr * w
                    sumG += sg * w
                    sumB += sb * w
                    wSum += w
                }

                val orig = srcPixels[idx]
                val a = (orig ushr 24) and 0xFF
                val oR = (orig shr 16) and 0xFF
                val oG = (orig shr 8) and 0xFF
                val oB = orig and 0xFF

                val mR = sumR / wSum
                val mG = sumG / wSum
                val mB = sumB / wSum
                val inv = 1f - motionAlpha

                val outR = (oR * inv + mR * motionAlpha).roundToInt().coerceIn(0, 255)
                val outG = (oG * inv + mG * motionAlpha).roundToInt().coerceIn(0, 255)
                val outB = (oB * inv + mB * motionAlpha).roundToInt().coerceIn(0, 255)
                outPixels[idx] = (a shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        bitmap.setPixels(outPixels, 0, width, 0, 0, width, height)
    }

    /**
     * Applies 3-Way Tonal Color Grading (Shadows, Midtones, Highlights color wheels + Balance + Intensity).
     */
    fun applyColorGradingInPlace(bitmap: Bitmap, colorGrade: ColorGradeState) {
        if (!colorGrade.hasVisibleEffect) return

        val mix = colorGrade.intensity.coerceIn(0f, 1f)
        val (shR, shG, shB, shL) = wheelToRgbOffset(colorGrade.shadows)
        val (midR, midG, midB, midL) = wheelToRgbOffset(colorGrade.midtones)
        val (hiR, hiG, hiB, hiL) = wheelToRgbOffset(colorGrade.highlights)
        val balShift = (colorGrade.balance / 100f) * 0.22f

        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in 0 until total) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            val r = ((c shr 16) and 0xFF) / 255f
            val g = ((c shr 8) and 0xFF) / 255f
            val b = (c and 0xFF) / 255f

            val rawLum = 0.299f * r + 0.587f * g + 0.114f * b
            val lum = (rawLum + balShift * rawLum * (1f - rawLum) * 2.5f).coerceIn(0f, 1f)

            val wShadow = (1f - smoothstep(0.05f, 0.58f, lum))
            val wHighlight = smoothstep(0.42f, 0.95f, lum)
            val wMidtone = sin(lum * PI.toFloat()).let { it * it }

            val deltaR = (wShadow * (shR + shL) + wMidtone * (midR + midL) + wHighlight * (hiR + hiL)) * mix
            val deltaG = (wShadow * (shG + shL) + wMidtone * (midG + midL) + wHighlight * (hiG + hiL)) * mix
            val deltaB = (wShadow * (shB + shL) + wMidtone * (midB + midL) + wHighlight * (hiB + hiL)) * mix

            val outR = ((r + deltaR) * 255f).roundToInt().coerceIn(0, 255)
            val outG = ((g + deltaG) * 255f).roundToInt().coerceIn(0, 255)
            val outB = ((b + deltaB) * 255f).roundToInt().coerceIn(0, 255)
            pixels[i] = (a shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private data class WheelDelta(val r: Float, val g: Float, val b: Float, val lum: Float)

    private fun wheelToRgbOffset(wheel: ColorWheelValue): WheelDelta {
        val sat = wheel.saturation.coerceIn(0f, 1f)
        val lumShift = (wheel.luminance / 100f) * 0.22f
        if (sat <= 0.001f) {
            return WheelDelta(0f, 0f, 0f, lumShift)
        }
        val argb = Color.HSVToColor(floatArrayOf(((wheel.hueDegrees % 360f) + 360f) % 360f, 1f, 1f))
        val hr = Color.red(argb) / 255f
        val hg = Color.green(argb) / 255f
        val hb = Color.blue(argb) / 255f
        // Zero-mean chromatic push so hue wheels shift color cleanly without unwanted brightness clipping
        val mean = (hr + hg + hb) / 3f
        val gain = sat * 0.36f
        return WheelDelta(
            r = (hr - mean) * gain,
            g = (hg - mean) * gain,
            b = (hb - mean) * gain,
            lum = lumShift
        )
    }

    private fun applyDiscBokeh(
        src: IntArray,
        w: Int,
        h: Int,
        radius: Float,
        bloomBoost: Float
    ): IntArray {
        val out = IntArray(w * h)
        // 25-point golden-angle Vogel disc kernel for realistic circular aperture bokeh
        val numTaps = 25
        val tapX = IntArray(numTaps)
        val tapY = IntArray(numTaps)
        val goldenAngle = 2.3999632f
        for (i in 0 until numTaps) {
            val frac = (i + 0.5f) / numTaps
            val r = radius * sqrt(frac)
            val theta = i * goldenAngle
            tapX[i] = (cos(theta) * r).roundToInt()
            tapY[i] = (sin(theta) * r).roundToInt()
        }

        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var wSum = 0f

                for (k in 0 until numTaps) {
                    val sx = (x + tapX[k]).coerceIn(0, w - 1)
                    val sy = (y + tapY[k]).coerceIn(0, h - 1)
                    val sample = src[sy * w + sx]
                    val sr = (sample shr 16) and 0xFF
                    val sg = (sample shr 8) and 0xFF
                    val sb = sample and 0xFF
                    val lum = (sr * 0.299f + sg * 0.587f + sb * 0.114f) / 255f
                    val weight = if (bloomBoost > 0f && lum > 0.68f) {
                        val spec = (lum - 0.68f) / 0.32f
                        1f + bloomBoost * spec * spec
                    } else {
                        1f
                    }
                    sumR += sr * weight
                    sumG += sg * weight
                    sumB += sb * weight
                    wSum += weight
                }

                val centerA = (src[rowOffset + x] ushr 24) and 0xFF
                val outR = (sumR / wSum).roundToInt().coerceIn(0, 255)
                val outG = (sumG / wSum).roundToInt().coerceIn(0, 255)
                val outB = (sumB / wSum).roundToInt().coerceIn(0, 255)
                out[rowOffset + x] = (centerA shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }
        return out
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun bilinearFloat(
        mask: FloatArray,
        row0: Int,
        row1: Int,
        x0: Int,
        x1: Int,
        fx: Float,
        fy: Float
    ): Float {
        val v00 = mask[row0 + x0]
        val v10 = mask[row0 + x1]
        val v01 = mask[row1 + x0]
        val v11 = mask[row1 + x1]
        val top = v00 + (v10 - v00) * fx
        val bottom = v01 + (v11 - v01) * fx
        return top + (bottom - top) * fy
    }

    private fun dilateMask(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val temp = FloatArray(src.size)
        val dst = FloatArray(src.size)
        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                var maxVal = 0f
                val xStart = max(0, x - radius)
                val xEnd = min(w - 1, x + radius)
                for (nx in xStart..xEnd) {
                    val v = src[rowOffset + nx]
                    if (v > maxVal) maxVal = v
                }
                temp[rowOffset + x] = maxVal
            }
        }
        for (x in 0 until w) {
            for (y in 0 until h) {
                var maxVal = 0f
                val yStart = max(0, y - radius)
                val yEnd = min(h - 1, y + radius)
                for (ny in yStart..yEnd) {
                    val v = temp[ny * w + x]
                    if (v > maxVal) maxVal = v
                }
                dst[y * w + x] = maxVal
            }
        }
        return dst
    }

    private fun boxBlurFloatMask(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        if (radius <= 0) return src
        val temp = FloatArray(src.size)
        val dst = FloatArray(src.size)
        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                var sum = 0f
                var count = 0
                val xStart = max(0, x - radius)
                val xEnd = min(w - 1, x + radius)
                for (nx in xStart..xEnd) {
                    sum += src[rowOffset + nx]
                    count++
                }
                temp[rowOffset + x] = sum / count
            }
        }
        for (x in 0 until w) {
            for (y in 0 until h) {
                var sum = 0f
                var count = 0
                val yStart = max(0, y - radius)
                val yEnd = min(h - 1, y + radius)
                for (ny in yStart..yEnd) {
                    sum += temp[ny * w + x]
                    count++
                }
                dst[y * w + x] = sum / count
            }
        }
        return dst
    }
}
