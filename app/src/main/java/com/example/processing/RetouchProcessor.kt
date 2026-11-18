package com.example.processing

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.RetouchStroke
import com.example.model.RetouchType
import com.example.model.SculptMode
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * High-precision portrait & local retouching engine:
 * - Continuous sub-segment interpolation & single-pass stroke coverage envelopes
 * - Hardness-controlled radial brush falloff
 * - YCbCr + HSV skin-masked frequency separation for natural Skin Smoothing
 * - Lip/gum-safe optical enamel whitening for Teeth Whiten
 * - Pupil/lash-protected iris clarity + catchlight sparkle for Eye Pop
 * - Luminance-only noise-cored unsharp mask for Detail Brush
 * - Smooth directional Push, inward Slim pinch, and outward Plump expand for Sculpt & Warp
 */
object RetouchProcessor {

    private data class PixelPoint(val x: Float, val y: Float, val pressure: Float)

    fun applyRetouchStroke(
        targetBitmap: Bitmap,
        stroke: RetouchStroke,
        originalBitmap: Bitmap? = null
    ) {
        if (stroke.points.isEmpty()) return
        val width = targetBitmap.width
        val height = targetBitmap.height
        if (width <= 1 || height <= 1) return

        val rad = (stroke.radius * (min(width, height) / 400f))
            .toInt()
            .coerceIn(6, (min(width, height) / 3).coerceAtLeast(6))
        val hardness = stroke.hardness.coerceIn(0.10f, 1.0f)

        if (!stroke.isEraser && stroke.type == RetouchType.SCULPT) {
            applySculptStroke(targetBitmap, stroke, rad, hardness)
            return
        }

        val interpolated = interpolateStrokePoints(stroke, width, height, rad)
        if (interpolated.isEmpty()) return

        var minPxX = width - 1
        var maxPxX = 0
        var minPxY = height - 1
        var maxPxY = 0
        for (pt in interpolated) {
            val ix = pt.x.roundToInt().coerceIn(0, width - 1)
            val iy = pt.y.roundToInt().coerceIn(0, height - 1)
            if (ix < minPxX) minPxX = ix
            if (ix > maxPxX) maxPxX = ix
            if (iy < minPxY) minPxY = iy
            if (iy > maxPxY) maxPxY = iy
        }

        // Extra 2px margin for neighborhood filters (Gaussian / sharpen)
        val margin = rad + 2
        val x0 = (minPxX - margin).coerceAtLeast(0)
        val x1 = (maxPxX + margin).coerceAtMost(width - 1)
        val y0 = (minPxY - margin).coerceAtLeast(0)
        val y1 = (maxPxY + margin).coerceAtMost(height - 1)
        if (x1 < x0 || y1 < y0) return

        val patchW = x1 - x0 + 1
        val patchH = y1 - y0 + 1
        val coverage = buildStrokeCoverageEnvelope(interpolated, x0, y0, patchW, patchH, rad, hardness)

        if (stroke.isEraser) {
            if (originalBitmap != null && originalBitmap.width == width && originalBitmap.height == height) {
                restoreOriginalPatch(targetBitmap, originalBitmap, x0, y0, patchW, patchH, coverage, stroke.intensity)
            }
            return
        }

        when (stroke.type) {
            RetouchType.SKIN_SMOOTH -> smoothSkinPatch(
                targetBitmap, x0, y0, patchW, patchH, coverage, stroke.intensity, stroke.texturePreserve, rad
            )
            RetouchType.TEETH_WHITEN -> whitenTeethPatch(
                targetBitmap, x0, y0, patchW, patchH, coverage, stroke.intensity
            )
            RetouchType.EYE_POP -> brightenEyesPatch(
                targetBitmap, x0, y0, patchW, patchH, coverage, stroke.intensity
            )
            RetouchType.DETAIL_SHARPEN -> sharpenDetailPatch(
                targetBitmap, x0, y0, patchW, patchH, coverage, stroke.intensity
            )
            RetouchType.GLOW -> applyGlowPatch(
                targetBitmap, x0, y0, patchW, patchH, coverage, stroke.intensity
            )
            RetouchType.SCULPT -> Unit
        }
    }

    private fun interpolateStrokePoints(
        stroke: RetouchStroke,
        width: Int,
        height: Int,
        radiusPx: Int
    ): List<PixelPoint> {
        val raw = stroke.points.map { pt ->
            PixelPoint(
                x = (pt.normX * width).coerceIn(0f, (width - 1).toFloat()),
                y = (pt.normY * height).coerceIn(0f, (height - 1).toFloat()),
                pressure = pt.pressure.coerceIn(0.3f, 1.0f)
            )
        }
        if (raw.size == 1) return raw

        val spacing = max(2.0f, radiusPx * 0.25f)
        val out = ArrayList<PixelPoint>(raw.size * 2)
        out.add(raw.first())

        var prev = raw.first()
        for (i in 1 until raw.size) {
            val curr = raw[i]
            val dx = curr.x - prev.x
            val dy = curr.y - prev.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist >= spacing) {
                val steps = ceil(dist / spacing).toInt().coerceAtLeast(1)
                for (s in 1..steps) {
                    val f = s.toFloat() / steps
                    out.add(
                        PixelPoint(
                            x = prev.x + dx * f,
                            y = prev.y + dy * f,
                            pressure = prev.pressure + (curr.pressure - prev.pressure) * f
                        )
                    )
                }
                prev = curr
            } else if (i == raw.lastIndex) {
                out.add(curr)
            }
        }
        return out
    }

    private fun computeBrushFalloff(dist: Float, radius: Float, hardness: Float): Float {
        if (dist >= radius) return 0f
        if (dist <= 0.001f) return 1f
        val norm = dist / radius
        val plateau = (hardness * 0.72f).coerceIn(0.02f, 0.72f)
        if (norm <= plateau) {
            // Very gentle roll inside core so center reads cleanly at 1.0
            val innerT = norm / plateau
            return 1f - innerT * innerT * 0.06f
        }
        val edgeT = (1f - (norm - plateau) / (1f - plateau)).coerceIn(0f, 1f)
        return (edgeT * edgeT * (3f - 2f * edgeT)) * 0.94f
    }

    private fun buildStrokeCoverageEnvelope(
        points: List<PixelPoint>,
        x0: Int,
        y0: Int,
        patchW: Int,
        patchH: Int,
        radius: Int,
        hardness: Float
    ): FloatArray {
        val env = FloatArray(patchW * patchH)
        val radF = radius.toFloat().coerceAtLeast(1f)
        val radSq = radF * radF

        for (pt in points) {
            val px0 = max(x0, (pt.x - radF).toInt())
            val px1 = min(x0 + patchW - 1, ceil(pt.x + radF).toInt())
            val py0 = max(y0, (pt.y - radF).toInt())
            val py1 = min(y0 + patchH - 1, ceil(pt.y + radF).toInt())

            for (y in py0..py1) {
                val dy = y - pt.y
                val row = (y - y0) * patchW
                for (x in px0..px1) {
                    val dx = x - pt.x
                    val dSq = dx * dx + dy * dy
                    if (dSq <= radSq) {
                        val w = computeBrushFalloff(sqrt(dSq), radF, hardness) * pt.pressure
                        val idx = row + (x - x0)
                        if (w > env[idx]) {
                            env[idx] = w
                        }
                    }
                }
            }
        }
        return env
    }

    private fun restoreOriginalPatch(
        targetBitmap: Bitmap,
        originalBitmap: Bitmap,
        x0: Int,
        y0: Int,
        patchW: Int,
        patchH: Int,
        coverage: FloatArray,
        intensity: Float
    ) {
        val dstPatch = IntArray(patchW * patchH)
        val origPatch = IntArray(patchW * patchH)
        targetBitmap.getPixels(dstPatch, 0, patchW, x0, y0, patchW, patchH)
        originalBitmap.getPixels(origPatch, 0, patchW, x0, y0, patchW, patchH)

        val strength = intensity.coerceIn(0.05f, 1f)
        for (i in dstPatch.indices) {
            val w = coverage[i]
            if (w <= 0.002f) continue
            val alpha = (w * strength).coerceIn(0f, 1f)
            val cur = dstPatch[i]
            val orig = origPatch[i]

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

            dstPatch[i] = (outA shl 24) or (outR shl 16) or (outG shl 8) or outB
        }
        targetBitmap.setPixels(dstPatch, 0, patchW, x0, y0, patchW, patchH)
    }

    fun isSkinTone(r: Int, g: Int, b: Int): Boolean {
        if (r <= g || g <= b || r - g < 8 || r < 45) return false
        val cb = (-0.168736f * r - 0.331264f * g + 0.500000f * b + 128f)
        val cr = (0.500000f * r - 0.418688f * g - 0.081312f * b + 128f)
        val inSkinYCbCr = cb in 77f..135f && cr in 130f..178f

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

    fun computeSkinProbability(r: Int, g: Int, b: Int): Float {
        if (!isSkinTone(r, g, b)) return 0f
        val cb = (-0.168736f * r - 0.331264f * g + 0.500000f * b + 128f)
        val cr = (0.500000f * r - 0.418688f * g - 0.081312f * b + 128f)
        val dCb = (cb - 106f) / 30f
        val dCr = (cr - 154f) / 25f
        val distSq = dCb * dCb + dCr * dCr
        return exp(-distSq * 0.45f).coerceIn(0.45f, 1.0f)
    }

    private fun buildGaussianKernel1D(radius: Int, sigma: Float): FloatArray {
        val r = radius.coerceAtLeast(1)
        val s = sigma.coerceAtLeast(0.45f)
        val kernel = FloatArray(r * 2 + 1)
        val denom = 2f * s * s
        var sum = 0f
        for (k in -r..r) {
            val w = exp(-(k * k).toFloat() / denom)
            kernel[k + r] = w
            sum += w
        }
        if (sum > 0f) {
            for (i in kernel.indices) kernel[i] /= sum
        }
        return kernel
    }

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

    private fun smoothSkinPatch(
        bitmap: Bitmap,
        x0: Int,
        y0: Int,
        patchW: Int,
        patchH: Int,
        coverage: FloatArray,
        intensity: Float,
        texturePreserve: Float,
        radius: Int
    ) {
        val srcPatch = IntArray(patchW * patchH)
        val dstPatch = IntArray(patchW * patchH)
        bitmap.getPixels(srcPatch, 0, patchW, x0, y0, patchW, patchH)
        System.arraycopy(srcPatch, 0, dstPatch, 0, srcPatch.size)

        val rawSkinMask = FloatArray(patchW * patchH)
        for (i in srcPatch.indices) {
            if (coverage[i] <= 0.002f) continue
            val c = srcPatch[i]
            val cR = (c shr 16) and 0xFF
            val cG = (c shr 8) and 0xFF
            val cB = c and 0xFF
            rawSkinMask[i] = computeSkinProbability(cR, cG, cB)
        }

        val featheredMask = featherSkinMask(rawSkinMask, patchW, patchH, radius = 2, sigma = 1.0f)
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
            val maskAlpha = featheredMask[i] * coverage[i]
            if (maskAlpha <= 0.004f) continue

            val c = srcPatch[i]
            val cA = (c shr 24) and 0xFF
            val cR = (c shr 16) and 0xFF
            val cG = (c shr 8) and 0xFF
            val cB = c and 0xFF

            val diffR = cR - wideR[i]
            val diffG = cG - wideG[i]
            val diffB = cB - wideB[i]
            val edgeEnergySq = diffR * diffR + diffG * diffG + diffB * diffB
            val edgePreservation = exp(-edgeEnergySq / edgeThresholdSq)

            val alpha = (maskAlpha * edgePreservation * blendBase).coerceIn(0f, 1f)

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

        val rawSkinMask = FloatArray(width * height)
        for (i in srcPixels.indices) {
            val c = srcPixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            rawSkinMask[i] = computeSkinProbability(r, g, b)
        }

        val maskFeatherRadius = (min(width, height) / 300).coerceIn(1, 3)
        val featheredMask = featherSkinMask(rawSkinMask, width, height, radius = maskFeatherRadius, sigma = 1.15f)

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
            val edgePreservation = exp(-edgeEnergySq / edgeThresholdSq)

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
     * Lip-Safe Optical Teeth Whitening:
     * Protects pink/red lip and gum tones via hue shield, removes warm-yellow chroma,
     * and applies clean pearlescent enamel lift.
     */
    private fun whitenTeethPatch(
        bitmap: Bitmap,
        x0: Int,
        y0: Int,
        patchW: Int,
        patchH: Int,
        coverage: FloatArray,
        intensity: Float
    ) {
        val patch = IntArray(patchW * patchH)
        bitmap.getPixels(patch, 0, patchW, x0, y0, patchW, patchH)
        val hsv = FloatArray(3)
        val strength = intensity.coerceIn(0.05f, 1f)

        for (i in patch.indices) {
            val w = coverage[i]
            if (w <= 0.002f) continue

            val curr = patch[i]
            val cA = (curr ushr 24) and 0xFF
            val cR = (curr shr 16) and 0xFF
            val cG = (curr shr 8) and 0xFF
            val cB = curr and 0xFF
            Color.RGBToHSV(cR, cG, cB, hsv)

            val hue = hsv[0]
            val sat = hsv[1]
            val valLum = hsv[2]

            // Lip & gum red/pink protection shield
            val isLipOrGum = (hue < 16f || hue > 322f) && sat > 0.18f
            if (isLipOrGum || valLum < 0.20f) continue

            val falloff = w * strength
            when {
                hue in 18f..74f && sat > 0.08f -> {
                    // Neutralize warm yellow/ivory tones and lift brightness cleanly
                    hsv[1] = (sat * (1.0f - falloff * 0.78f)).coerceIn(0f, 1f)
                    hsv[2] = (valLum + (1f - valLum) * falloff * 0.34f).coerceIn(0f, 1f)
                    patch[i] = Color.HSVToColor(cA, hsv)
                }
                sat <= 0.22f && valLum >= 0.38f -> {
                    // Clean neutral/cool white enamel polish
                    hsv[1] = (sat * (1.0f - falloff * 0.35f)).coerceIn(0f, 1f)
                    hsv[2] = (valLum + (1f - valLum) * falloff * 0.22f).coerceIn(0f, 1f)
                    patch[i] = Color.HSVToColor(cA, hsv)
                }
            }
        }
        bitmap.setPixels(patch, 0, patchW, x0, y0, patchW, patchH)
    }

    /**
     * Studio Eye Pop:
     * Protects deep pupil & eyelash blacks, amplifies mid-tone iris vibrancy and radial micro-clarity,
     * cleans sclera whites, and pops catchlight highlights.
     */
    private fun brightenEyesPatch(
        bitmap: Bitmap,
        x0: Int,
        y0: Int,
        patchW: Int,
        patchH: Int,
        coverage: FloatArray,
        intensity: Float
    ) {
        val src = IntArray(patchW * patchH)
        val dst = IntArray(patchW * patchH)
        bitmap.getPixels(src, 0, patchW, x0, y0, patchW, patchH)
        System.arraycopy(src, 0, dst, 0, src.size)

        val hsv = FloatArray(3)
        val strength = intensity.coerceIn(0.05f, 1f)

        for (y in 0 until patchH) {
            val row = y * patchW
            val yUp = (y - 1).coerceAtLeast(0) * patchW
            val yDn = (y + 1).coerceAtMost(patchH - 1) * patchW
            for (x in 0 until patchW) {
                val idx = row + x
                val w = coverage[idx]
                if (w <= 0.002f) continue

                val curr = src[idx]
                val cA = (curr ushr 24) and 0xFF
                val cR = (curr shr 16) and 0xFF
                val cG = (curr shr 8) and 0xFF
                val cB = curr and 0xFF

                Color.RGBToHSV(cR, cG, cB, hsv)
                val v = hsv[2]
                val s = hsv[1]

                // Protect deep pupils & dark lashes from washing out to grey
                val pupilGuard = ((v - 0.08f) / 0.14f).coerceIn(0f, 1f)
                if (pupilGuard <= 0.01f) continue

                val falloff = w * strength * pupilGuard

                // Subtle local micro-contrast lift around center pixel
                val xL = (x - 1).coerceAtLeast(0)
                val xR = (x + 1).coerceAtMost(patchW - 1)
                val pUp = src[yUp + x]
                val pDn = src[yDn + x]
                val pLt = src[row + xL]
                val pRt = src[row + xR]
                val avgLum = (
                    lum709(pUp) + lum709(pDn) + lum709(pLt) + lum709(pRt)
                    ) * 0.25f
                val curLum = 0.2126f * cR + 0.7152f * cG + 0.0722f * cB
                val microBoost = ((curLum - avgLum) * 0.35f * falloff) / 255f

                if (s < 0.18f && v > 0.52f) {
                    // Sclera / specular highlight clean lift
                    hsv[1] = (s * (1f - falloff * 0.22f)).coerceIn(0f, 1f)
                    hsv[2] = (v + (1f - v) * falloff * 0.24f + microBoost).coerceIn(0f, 1f)
                } else {
                    // Iris clarity & rich color vibrancy + brightness boost
                    hsv[1] = (s * (1.0f + falloff * 0.38f)).coerceIn(0f, 1f)
                    hsv[2] = (v * (1.0f + falloff * 0.30f) + microBoost).coerceIn(0f, 1f)
                }
                dst[idx] = Color.HSVToColor(cA, hsv)
            }
        }
        bitmap.setPixels(dst, 0, patchW, x0, y0, patchW, patchH)
    }

    private fun lum709(argb: Int): Float {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    /**
     * Luminance-Only Edge-Safe Unsharp Mask with Noise Coring:
     * Sharpens brows, eyelashes, hair & lip lines crisply without chromatic color noise.
     */
    private fun sharpenDetailPatch(
        bitmap: Bitmap,
        x0: Int,
        y0: Int,
        patchW: Int,
        patchH: Int,
        coverage: FloatArray,
        intensity: Float
    ) {
        val src = IntArray(patchW * patchH)
        val dst = IntArray(patchW * patchH)
        bitmap.getPixels(src, 0, patchW, x0, y0, patchW, patchH)
        System.arraycopy(src, 0, dst, 0, src.size)

        val lums = FloatArray(patchW * patchH)
        for (i in src.indices) {
            lums[i] = lum709(src[i])
        }

        val strength = intensity.coerceIn(0.05f, 1f) * 1.15f
        val coringThreshold = 3.0f

        for (y in 1 until patchH - 1) {
            val row = y * patchW
            for (x in 1 until patchW - 1) {
                val idx = row + x
                val w = coverage[idx]
                if (w <= 0.002f) continue

                val c = src[idx]
                val cA = (c ushr 24) and 0xFF
                val cR = (c shr 16) and 0xFF
                val cG = (c shr 8) and 0xFF
                val cB = c and 0xFF

                val cLum = lums[idx]
                val blurLum = (
                    lums[idx - patchW - 1] + lums[idx - patchW] * 2f + lums[idx - patchW + 1] +
                        lums[idx - 1] * 2f + cLum * 4f + lums[idx + 1] * 2f +
                        lums[idx + patchW - 1] + lums[idx + patchW] * 2f + lums[idx + patchW + 1]
                    ) / 16f

                val rawDiff = cLum - blurLum
                val absDiff = abs(rawDiff)
                if (absDiff <= coringThreshold) continue

                val coredDiff = if (rawDiff > 0f) rawDiff - coringThreshold else rawDiff + coringThreshold
                val delta = (coredDiff * w * strength).coerceIn(-48f, 48f)

                val outR = (cR + delta).roundToInt().coerceIn(0, 255)
                val outG = (cG + delta).roundToInt().coerceIn(0, 255)
                val outB = (cB + delta).roundToInt().coerceIn(0, 255)
                dst[idx] = (cA shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }
        bitmap.setPixels(dst, 0, patchW, x0, y0, patchW, patchH)
    }

    /**
     * Studio Beauty Highlighter / Pearlescent Glow:
     * Soft warm highlight lift with natural highlight compression to avoid flat white clipping.
     */
    private fun applyGlowPatch(
        bitmap: Bitmap,
        x0: Int,
        y0: Int,
        patchW: Int,
        patchH: Int,
        coverage: FloatArray,
        intensity: Float
    ) {
        val patch = IntArray(patchW * patchH)
        bitmap.getPixels(patch, 0, patchW, x0, y0, patchW, patchH)

        val glowR = 255
        val glowG = 244
        val glowB = 222
        val strength = intensity.coerceIn(0.05f, 1f) * 0.44f

        for (i in patch.indices) {
            val w = coverage[i]
            if (w <= 0.002f) continue

            val curr = patch[i]
            val cA = (curr ushr 24) and 0xFF
            val cR = (curr shr 16) and 0xFF
            val cG = (curr shr 8) and 0xFF
            val cB = curr and 0xFF

            val falloff = w * strength
            val screenR = 255 - ((255 - cR) * (255 - (glowR * falloff).roundToInt()) / 255)
            val screenG = 255 - ((255 - cG) * (255 - (glowG * falloff).roundToInt()) / 255)
            val screenB = 255 - ((255 - cB) * (255 - (glowB * falloff).roundToInt()) / 255)

            patch[i] = (cA shl 24) or
                (screenR.coerceIn(0, 255) shl 16) or
                (screenG.coerceIn(0, 255) shl 8) or
                screenB.coerceIn(0, 255)
        }
        bitmap.setPixels(patch, 0, patchW, x0, y0, patchW, patchH)
    }

    /**
     * Sculpt & Warp Engine:
     * Supports Directional Nudge (PUSH), Inward Contour Slimming (SLIM), and Outward Volume Expand (PLUMP)
     * with interpolated continuous sub-steps and full-image bounds-safe bilinear sampling.
     */
    private fun applySculptStroke(
        targetBitmap: Bitmap,
        stroke: RetouchStroke,
        radius: Int,
        hardness: Float
    ) {
        val width = targetBitmap.width
        val height = targetBitmap.height
        val radF = radius.toFloat().coerceAtLeast(1f)
        val raw = stroke.points.map {
            PixelPoint(
                x = (it.normX * width).coerceIn(0f, (width - 1).toFloat()),
                y = (it.normY * height).coerceIn(0f, (height - 1).toFloat()),
                pressure = it.pressure.coerceIn(0.3f, 1.0f)
            )
        }
        if (raw.isEmpty()) return

        when (stroke.sculptMode) {
            SculptMode.PUSH -> {
                if (raw.size == 1) {
                    sculptRadialWarpArea(targetBitmap, raw[0].x, raw[0].y, radF, stroke.intensity, hardness, isSlim = true)
                    return
                }
                val stepMin = max(3f, radF * 0.35f)
                val maxSegStep = max(6f, radF * 1.20f)
                var prev = raw[0]
                for (i in 1 until raw.size) {
                    val curr = raw[i]
                    val dx = curr.x - prev.x
                    val dy = curr.y - prev.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < stepMin && i < raw.lastIndex) continue
                    if (dist > 0.5f) {
                        val subSteps = ceil(dist / maxSegStep).toInt().coerceIn(1, 5)
                        val segDx = dx / subSteps
                        val segDy = dy / subSteps
                        for (s in 1..subSteps) {
                            val cx = prev.x + segDx * s
                            val cy = prev.y + segDy * s
                            sculptDirectionalArea(
                                targetBitmap, cx, cy, radF, stroke.intensity, hardness, segDx, segDy
                            )
                        }
                        prev = curr
                    } else if (i == raw.lastIndex && prev === raw[0]) {
                        sculptRadialWarpArea(targetBitmap, curr.x, curr.y, radF, stroke.intensity, hardness, isSlim = true)
                    }
                }
            }
            SculptMode.SLIM, SculptMode.PLUMP -> {
                val isSlim = stroke.sculptMode == SculptMode.SLIM
                val interp = interpolateStrokePoints(stroke, width, height, radius)
                // Space radial pulses evenly so dragging slim/plump stays silky smooth
                val stepMin = max(4f, radF * 0.45f)
                var prevPt: PixelPoint? = null
                for (idx in interp.indices) {
                    val pt = interp[idx]
                    val prev = prevPt
                    if (prev != null) {
                        val d = sqrt((pt.x - prev.x) * (pt.x - prev.x) + (pt.y - prev.y) * (pt.y - prev.y))
                        if (d < stepMin && idx < interp.lastIndex) continue
                    }
                    sculptRadialWarpArea(
                        targetBitmap, pt.x, pt.y, radF, stroke.intensity * pt.pressure, hardness, isSlim = isSlim
                    )
                    prevPt = pt
                }
            }
        }
    }

    private fun sculptDirectionalArea(
        bitmap: Bitmap,
        cx: Float,
        cy: Float,
        radius: Float,
        intensity: Float,
        hardness: Float,
        pushDx: Float,
        pushDy: Float
    ) {
        val width = bitmap.width
        val height = bitmap.height
        val pushMag = sqrt(pushDx * pushDx + pushDy * pushDy)
        if (pushMag <= 0.25f) return

        val pad = ceil(radius * 0.5f).toInt().coerceAtLeast(2)
        val x0 = (cx - radius).toInt().coerceAtLeast(0)
        val x1 = ceil(cx + radius).toInt().coerceAtMost(width - 1)
        val y0 = (cy - radius).toInt().coerceAtLeast(0)
        val y1 = ceil(cy + radius).toInt().coerceAtMost(height - 1)
        if (x1 < x0 || y1 < y0) return

        val srcX0 = (x0 - pad).coerceAtLeast(0)
        val srcX1 = (x1 + pad).coerceAtMost(width - 1)
        val srcY0 = (y0 - pad).coerceAtLeast(0)
        val srcY1 = (y1 + pad).coerceAtMost(height - 1)
        val srcW = srcX1 - srcX0 + 1
        val srcH = srcY1 - srcY0 + 1
        val srcBuf = IntArray(srcW * srcH)
        bitmap.getPixels(srcBuf, 0, srcW, srcX0, srcY0, srcW, srcH)

        val dstW = x1 - x0 + 1
        val dstH = y1 - y0 + 1
        val dstBuf = IntArray(dstW * dstH)
        bitmap.getPixels(dstBuf, 0, dstW, x0, y0, dstW, dstH)

        val maxShift = radius * 0.45f * intensity.coerceIn(0.1f, 1f)
        val shiftScale = (maxShift / pushMag).coerceAtMost(0.85f * intensity)
        val dirX = pushDx * shiftScale
        val dirY = pushDy * shiftScale
        val radSq = radius * radius
        val edgeSharp = (0.75f + hardness * 0.5f).coerceIn(0.75f, 1.25f)

        for (y in y0..y1) {
            val dy = y - cy
            val dstRow = (y - y0) * dstW
            for (x in x0..x1) {
                val dx = x - cx
                val distSq = dx * dx + dy * dy
                if (distSq <= radSq) {
                    val normDistSq = distSq / radSq
                    val t = 1f - normDistSq
                    val falloff = (t * t * edgeSharp).coerceAtMost(1f)
                    val sampleX = (x - dirX * falloff) - srcX0
                    val sampleY = (y - dirY * falloff) - srcY0
                    dstBuf[dstRow + (x - x0)] = sampleBilinear(srcBuf, srcW, srcH, sampleX, sampleY)
                }
            }
        }
        bitmap.setPixels(dstBuf, 0, dstW, x0, y0, dstW, dstH)
    }

    private fun sculptRadialWarpArea(
        bitmap: Bitmap,
        cx: Float,
        cy: Float,
        radius: Float,
        intensity: Float,
        hardness: Float,
        isSlim: Boolean
    ) {
        val width = bitmap.width
        val height = bitmap.height
        val pad = ceil(radius * 0.35f).toInt().coerceAtLeast(2)
        val x0 = (cx - radius).toInt().coerceAtLeast(0)
        val x1 = ceil(cx + radius).toInt().coerceAtMost(width - 1)
        val y0 = (cy - radius).toInt().coerceAtLeast(0)
        val y1 = ceil(cy + radius).toInt().coerceAtMost(height - 1)
        if (x1 < x0 || y1 < y0) return

        val srcX0 = (x0 - pad).coerceAtLeast(0)
        val srcX1 = (x1 + pad).coerceAtMost(width - 1)
        val srcY0 = (y0 - pad).coerceAtLeast(0)
        val srcY1 = (y1 + pad).coerceAtMost(height - 1)
        val srcW = srcX1 - srcX0 + 1
        val srcH = srcY1 - srcY0 + 1
        val srcBuf = IntArray(srcW * srcH)
        bitmap.getPixels(srcBuf, 0, srcW, srcX0, srcY0, srcW, srcH)

        val dstW = x1 - x0 + 1
        val dstH = y1 - y0 + 1
        val dstBuf = IntArray(dstW * dstH)
        bitmap.getPixels(dstBuf, 0, dstW, x0, y0, dstW, dstH)

        val radSq = radius * radius
        val strength = intensity.coerceIn(0.05f, 1f) * (0.28f + hardness * 0.14f)

        for (y in y0..y1) {
            val dy = y - cy
            val dstRow = (y - y0) * dstW
            for (x in x0..x1) {
                val dx = x - cx
                val distSq = dx * dx + dy * dy
                if (distSq <= radSq) {
                    val normDist = sqrt(distSq) / radius
                    val smooth = (1f - normDist) * (1f - normDist)
                    // Slim (Pinch): inverse samples outward (>1.0); Plump (Expand): inverse samples inward (<1.0)
                    val radialScale = if (isSlim) {
                        1.0f + strength * smooth
                    } else {
                        (1.0f - strength * smooth).coerceAtLeast(0.45f)
                    }
                    val sampleX = (cx + dx * radialScale) - srcX0
                    val sampleY = (cy + dy * radialScale) - srcY0
                    dstBuf[dstRow + (x - x0)] = sampleBilinear(srcBuf, srcW, srcH, sampleX, sampleY)
                }
            }
        }
        bitmap.setPixels(dstBuf, 0, dstW, x0, y0, dstW, dstH)
    }

    private fun sampleBilinear(
        buf: IntArray,
        w: Int,
        h: Int,
        sampleX: Float,
        sampleY: Float
    ): Int {
        val maxX = (w - 1).coerceAtLeast(0)
        val maxY = (h - 1).coerceAtLeast(0)
        val sx = sampleX.coerceIn(0f, maxX.toFloat())
        val sy = sampleY.coerceIn(0f, maxY.toFloat())
        val ix = sx.toInt().coerceAtMost(maxX)
        val iy = sy.toInt().coerceAtMost(maxY)
        val nx = (ix + 1).coerceAtMost(maxX)
        val ny = (iy + 1).coerceAtMost(maxY)
        val fx = sx - ix
        val fy = sy - iy

        val c00 = buf[iy * w + ix]
        val c10 = buf[iy * w + nx]
        val c01 = buf[ny * w + ix]
        val c11 = buf[ny * w + nx]

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

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
