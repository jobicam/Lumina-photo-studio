package com.example.processing

import android.graphics.Bitmap
import com.example.model.BlemishSuggestion
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Automated on-device blemish detector using skin segmentation and multi-feature
 * Difference-of-Means blob detection with connected-component filtering.
 */
object BlemishDetector {

    /**
     * Scans [source] for compact dark/inflamed blemish blobs inside detected skin regions
     * and returns sorted [BlemishSuggestion] removal targets in normalized [0f..1f] coordinates.
     */
    fun detectBlemishes(
        source: Bitmap,
        sensitivity: Float = 0.65f,
        maxSuggestions: Int = 20
    ): List<BlemishSuggestion> {
        val origW = source.width
        val origH = source.height
        if (origW < 16 || origH < 16) return emptyList()

        // 1. Analyze at a consistent ~360px max dimension for fast, scale-invariant blob detection
        val maxAnalysisDim = 360
        val analysisBmp = if (origW > maxAnalysisDim || origH > maxAnalysisDim) {
            val scale = min(maxAnalysisDim.toFloat() / origW, maxAnalysisDim.toFloat() / origH)
            val tw = (origW * scale).toInt().coerceAtLeast(16)
            val th = (origH * scale).toInt().coerceAtLeast(16)
            Bitmap.createScaledBitmap(source, tw, th, true)
        } else {
            source
        }

        try {
            val w = analysisBmp.width
            val h = analysisBmp.height
            val total = w * h
            val pixels = IntArray(total)
            analysisBmp.getPixels(pixels, 0, w, 0, 0, w, h)

            // 2. Extract luminance, redness index, and YCbCr/HSV skin probability
            val lum = FloatArray(total)
            val redness = FloatArray(total)
            val isSkin = BooleanArray(total)
            var skinPixelCount = 0

            for (i in 0 until total) {
                val c = pixels[i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF

                lum[i] = 0.299f * r + 0.587f * g + 0.114f * b
                redness[i] = r - 0.5f * (g + b)

                if (BitmapProcessor.isSkinTone(r, g, b)) {
                    isSkin[i] = true
                    skinPixelCount++
                }
            }

            // If photo has almost no skin tones, bail out early
            if (skinPixelCount < (total * 0.015f).toInt().coerceAtLeast(24)) {
                return emptyList()
            }

            // 3. Compute local skin density via integral image (box window) to restrict
            // blob detection strictly to interior skin regions (ignoring hair/eye/lip borders)
            val skinIntegral = computeIntIntegral(isSkin, w, h)
            val regionRadius = (min(w, h) / 28).coerceIn(3, 10)
            val validSkinInterior = BooleanArray(total)

            for (y in 0 until h) {
                val y0 = (y - regionRadius).coerceAtLeast(0)
                val y1 = (y + regionRadius).coerceAtMost(h - 1)
                val row = y * w
                for (x in 0 until w) {
                    val idx = row + x
                    val c = pixels[idx]
                    val r = (c shr 16) and 0xFF
                    val g = (c shr 8) and 0xFF
                    val b = c and 0xFF

                    // Skip pitch-black features (pupils, lashes) & strongly non-skin colors
                    val pixelLum = lum[idx]
                    if (pixelLum < 22f) continue
                    if (b > r + 10 || g > r + 8) continue

                    val x0 = (x - regionRadius).coerceAtLeast(0)
                    val x1 = (x + regionRadius).coerceAtMost(w - 1)
                    val boxArea = (x1 - x0 + 1) * (y1 - y0 + 1)
                    val skinHits = queryIntIntegral(skinIntegral, w, x0, y0, x1, y1)
                    val localSkinRatio = skinHits.toFloat() / boxArea

                    // Interior skin OR blemish core surrounded >= 62% by skin
                    if ((isSkin[idx] && localSkinRatio >= 0.55f) || localSkinRatio >= 0.68f) {
                        validSkinInterior[idx] = true
                    }
                }
            }

            // 4. Local background reference (skin-weighted box blur) vs fine center response (DoM blob filter)
            val bgRadius = (min(w, h) / 36).coerceIn(4, 11)
            val fineLum = boxBlurFloat(lum, w, h, radius = 1)
            val fineRed = boxBlurFloat(redness, w, h, radius = 1)
            val bgLum = skinMaskedBoxBlur(lum, isSkin, w, h, radius = bgRadius)
            val bgRed = skinMaskedBoxBlur(redness, isSkin, w, h, radius = bgRadius)

            val sens = sensitivity.coerceIn(0f, 1f)
            val scoreThreshold = 17.5f - sens * 8.5f // 9.0 .. 17.5 (default ~12.0)

            val blobScore = FloatArray(total)
            val candidateMask = BooleanArray(total)

            for (i in 0 until total) {
                if (!validSkinInterior[i]) continue
                val darkContrast = (bgLum[i] - fineLum[i]).coerceAtLeast(0f)
                val redContrast = (fineRed[i] - bgRed[i]).coerceAtLeast(0f)
                val score = darkContrast * 0.90f + redContrast * 1.15f
                blobScore[i] = score
                if (score >= scoreThreshold) {
                    candidateMask[i] = true
                }
            }

            // 5. Connected-component blob extraction (BFS)
            val visited = BooleanArray(total)
            val queue = IntArray(total)
            val minDim = min(w, h).toFloat()
            val minBlobArea = max(3, ((minDim * 0.006f) * (minDim * 0.006f)).toInt())
            val maxBlobArea = (total * 0.012f).toInt().coerceAtLeast(36)

            data class RawBlob(
                val cx: Float,
                val cy: Float,
                val radiusPx: Float,
                val area: Int,
                val confidence: Float,
                val rankWeight: Float
            )

            val rawBlobs = mutableListOf<RawBlob>()
            val dx8 = intArrayOf(-1, 0, 1, -1, 1, -1, 0, 1)
            val dy8 = intArrayOf(-1, -1, -1, 0, 0, 1, 1, 1)

            for (startIdx in 0 until total) {
                if (!candidateMask[startIdx] || visited[startIdx]) continue

                var head = 0
                var tail = 0
                queue[tail++] = startIdx
                visited[startIdx] = true

                var area = 0
                var weightSum = 0f
                var weightedX = 0f
                var weightedY = 0f
                var maxScore = 0f
                var minX = w
                var maxX = 0
                var minY = h
                var maxY = 0

                while (head < tail) {
                    val curr = queue[head++]
                    val cx = curr % w
                    val cy = curr / w
                    val sc = blobScore[curr]

                    area++
                    weightSum += sc
                    weightedX += cx * sc
                    weightedY += cy * sc
                    if (sc > maxScore) maxScore = sc
                    if (cx < minX) minX = cx
                    if (cx > maxX) maxX = cx
                    if (cy < minY) minY = cy
                    if (cy > maxY) maxY = cy

                    for (dir in 0 until 8) {
                        val nx = cx + dx8[dir]
                        val ny = cy + dy8[dir]
                        if (nx in 0 until w && ny in 0 until h) {
                            val nIdx = ny * w + nx
                            if (candidateMask[nIdx] && !visited[nIdx]) {
                                visited[nIdx] = true
                                queue[tail++] = nIdx
                            }
                        }
                    }
                }

                if (area < minBlobArea || area > maxBlobArea) continue

                val bw = (maxX - minX + 1).toFloat()
                val bh = (maxY - minY + 1).toFloat()
                val aspect = min(bw, bh) / max(bw, bh).coerceAtLeast(1f)
                val fillRatio = area / (bw * bh).coerceAtLeast(1f)

                // Blobs must be reasonably compact/circular rather than thin wrinkles or edge streaks
                if (aspect < 0.35f || fillRatio < 0.28f) continue

                val centerX = if (weightSum > 0f) weightedX / weightSum else (minX + maxX) / 2f
                val centerY = if (weightSum > 0f) weightedY / weightSum else (minY + maxY) / 2f

                // Skip spots right at the outer 2% image border
                if (centerX < w * 0.02f || centerX > w * 0.98f ||
                    centerY < h * 0.02f || centerY > h * 0.98f
                ) {
                    continue
                }

                val eqRadius = sqrt(area.toFloat() / PI.toFloat())
                val healRadiusPx = (eqRadius * 1.75f).coerceIn(minDim * 0.018f, minDim * 0.075f)
                val meanScore = weightSum / area
                val confidence = ((meanScore - scoreThreshold * 0.65f) / 28f).coerceIn(0.35f, 1.0f)

                rawBlobs.add(
                    RawBlob(
                        cx = centerX,
                        cy = centerY,
                        radiusPx = healRadiusPx,
                        area = area,
                        confidence = confidence,
                        rankWeight = confidence * sqrt(area.toFloat())
                    )
                )
            }

            // 6. Non-maximum suppression to avoid overlapping target rings on the same blemish
            val sorted = rawBlobs.sortedByDescending { it.rankWeight }
            val accepted = mutableListOf<BlemishSuggestion>()
            var nextId = 1

            for (blob in sorted) {
                val overlaps = accepted.any { existing ->
                    val exPx = existing.normX * w
                    val eyPx = existing.normY * h
                    val erPx = existing.normRadius * minDim
                    val minDist = max(blob.radiusPx, erPx) * 1.65f
                    val dx = blob.cx - exPx
                    val dy = blob.cy - eyPx
                    (dx * dx + dy * dy) < (minDist * minDist)
                }
                if (overlaps) continue

                accepted.add(
                    BlemishSuggestion(
                        id = nextId++,
                        normX = (blob.cx / w).coerceIn(0f, 1f),
                        normY = (blob.cy / h).coerceIn(0f, 1f),
                        normRadius = (blob.radiusPx / minDim).coerceIn(0.018f, 0.075f),
                        confidence = blob.confidence,
                        areaPixels = blob.area
                    )
                )
                if (accepted.size >= maxSuggestions) break
            }

            return accepted
        } finally {
            if (analysisBmp !== source && !analysisBmp.isRecycled) {
                analysisBmp.recycle()
            }
        }
    }

    private fun computeIntIntegral(mask: BooleanArray, w: Int, h: Int): IntArray {
        val integral = IntArray((w + 1) * (h + 1))
        val iw = w + 1
        for (y in 1..h) {
            var rowSum = 0
            val srcRow = (y - 1) * w
            val dstRow = y * iw
            val prevRow = (y - 1) * iw
            for (x in 1..w) {
                if (mask[srcRow + x - 1]) rowSum++
                integral[dstRow + x] = integral[prevRow + x] + rowSum
            }
        }
        return integral
    }

    private fun queryIntIntegral(integral: IntArray, w: Int, x0: Int, y0: Int, x1: Int, y1: Int): Int {
        val iw = w + 1
        val ax = x0
        val ay = y0
        val bx = x1 + 1
        val by = y1 + 1
        return integral[by * iw + bx] - integral[ay * iw + bx] - integral[by * iw + ax] + integral[ay * iw + ax]
    }

    private fun boxBlurFloat(values: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val count = w * h
        val horiz = FloatArray(count)
        val out = FloatArray(count)

        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                var sum = 0f
                var n = 0
                val x0 = (x - radius).coerceAtLeast(0)
                val x1 = (x + radius).coerceAtMost(w - 1)
                for (nx in x0..x1) {
                    sum += values[row + nx]
                    n++
                }
                horiz[row + x] = if (n > 0) sum / n else values[row + x]
            }
        }

        for (y in 0 until h) {
            val row = y * w
            val y0 = (y - radius).coerceAtLeast(0)
            val y1 = (y + radius).coerceAtMost(h - 1)
            for (x in 0 until w) {
                var sum = 0f
                var n = 0
                for (ny in y0..y1) {
                    sum += horiz[ny * w + x]
                    n++
                }
                out[row + x] = if (n > 0) sum / n else horiz[row + x]
            }
        }
        return out
    }

    /**
     * Separable skin-only box blur so nearby hair/background pixels do not skew the local skin average.
     */
    private fun skinMaskedBoxBlur(
        values: FloatArray,
        isSkin: BooleanArray,
        w: Int,
        h: Int,
        radius: Int
    ): FloatArray {
        val count = w * h
        val horizSum = FloatArray(count)
        val horizCnt = IntArray(count)
        val out = FloatArray(count)

        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                var sum = 0f
                var n = 0
                val x0 = (x - radius).coerceAtLeast(0)
                val x1 = (x + radius).coerceAtMost(w - 1)
                for (nx in x0..x1) {
                    val nIdx = row + nx
                    if (isSkin[nIdx]) {
                        sum += values[nIdx]
                        n++
                    }
                }
                horizSum[row + x] = sum
                horizCnt[row + x] = n
            }
        }

        for (y in 0 until h) {
            val row = y * w
            val y0 = (y - radius).coerceAtLeast(0)
            val y1 = (y + radius).coerceAtMost(h - 1)
            for (x in 0 until w) {
                var sum = 0f
                var n = 0
                for (ny in y0..y1) {
                    val nIdx = ny * w + x
                    sum += horizSum[nIdx]
                    n += horizCnt[nIdx]
                }
                out[row + x] = if (n > 0) sum / n else values[row + x]
            }
        }
        return out
    }
}
