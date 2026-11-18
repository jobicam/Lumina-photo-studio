package com.example.processing

import android.graphics.Bitmap
import com.example.model.Lut3D
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

object CubeLutParser {

    fun parseCubeLut(
        id: String,
        fallbackName: String,
        content: String,
        isBuiltIn: Boolean = false
    ): Lut3D {
        var title = fallbackName.ifBlank { "Custom LUT" }
        var size = 0
        val domainMin = floatArrayOf(0f, 0f, 0f)
        val domainMax = floatArrayOf(1f, 1f, 1f)
        val values = ArrayList<Float>()

        val lines = content.lineSequence()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val upper = line.uppercase(Locale.US)
            when {
                upper.startsWith("TITLE") -> {
                    val rawTitle = line.substringAfter("TITLE", "").trim().trim('"')
                    if (rawTitle.isNotEmpty()) title = rawTitle
                }
                upper.startsWith("LUT_3D_SIZE") -> {
                    val token = line.split(Regex("\\s+")).getOrNull(1)
                    size = token?.toIntOrNull()
                        ?: throw IllegalArgumentException("Invalid LUT_3D_SIZE in .cube file")
                    if (size !in 2..65) {
                        throw IllegalArgumentException("Unsupported LUT_3D_SIZE: $size (expected 2..65)")
                    }
                }
                upper.startsWith("LUT_1D_SIZE") -> {
                    throw IllegalArgumentException("1D .cube LUTs are not supported; please import a 3D .cube LUT")
                }
                upper.startsWith("DOMAIN_MIN") -> {
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size >= 4) {
                        domainMin[0] = parts[1].toFloatOrNull() ?: 0f
                        domainMin[1] = parts[2].toFloatOrNull() ?: 0f
                        domainMin[2] = parts[3].toFloatOrNull() ?: 0f
                    }
                }
                upper.startsWith("DOMAIN_MAX") -> {
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size >= 4) {
                        domainMax[0] = parts[1].toFloatOrNull() ?: 1f
                        domainMax[1] = parts[2].toFloatOrNull() ?: 1f
                        domainMax[2] = parts[3].toFloatOrNull() ?: 1f
                    }
                }
                else -> {
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size >= 3) {
                        val r = parts[0].toFloatOrNull()
                        val g = parts[1].toFloatOrNull()
                        val b = parts[2].toFloatOrNull()
                        if (r != null && g != null && b != null) {
                            values.add(r.coerceIn(0f, 1.5f))
                            values.add(g.coerceIn(0f, 1.5f))
                            values.add(b.coerceIn(0f, 1.5f))
                        }
                    }
                }
            }
        }

        if (size <= 0) {
            throw IllegalArgumentException("Missing LUT_3D_SIZE header in .cube file")
        }
        val expectedFloats = size * size * size * 3
        if (values.size < expectedFloats) {
            throw IllegalArgumentException(
                "Incomplete .cube LUT data: expected ${size * size * size} RGB entries, found ${values.size / 3}"
            )
        }

        val table = FloatArray(expectedFloats) { idx -> values[idx] }
        return Lut3D(
            id = id,
            name = title,
            size = size,
            domainMin = domainMin,
            domainMax = domainMax,
            table = table,
            isBuiltIn = isBuiltIn
        )
    }

    fun serializeCubeLut(lut: Lut3D): String {
        val sb = StringBuilder(lut.size * lut.size * lut.size * 28)
        val safeTitle = lut.name.replace("\"", "'").ifBlank { "Lumina 3D LUT" }
        sb.append("# Exported by Lumina Photo Studio\n")
        sb.append("TITLE \"").append(safeTitle).append("\"\n")
        sb.append("LUT_3D_SIZE ").append(lut.size).append('\n')
        sb.append(
            String.format(
                Locale.US,
                "DOMAIN_MIN %.6f %.6f %.6f\n",
                lut.domainMin[0],
                lut.domainMin[1],
                lut.domainMin[2]
            )
        )
        sb.append(
            String.format(
                Locale.US,
                "DOMAIN_MAX %.6f %.6f %.6f\n",
                lut.domainMax[0],
                lut.domainMax[1],
                lut.domainMax[2]
            )
        )
        val totalEntries = lut.size * lut.size * lut.size
        for (i in 0 until totalEntries) {
            val base = i * 3
            sb.append(
                String.format(
                    Locale.US,
                    "%.6f %.6f %.6f\n",
                    lut.table[base],
                    lut.table[base + 1],
                    lut.table[base + 2]
                )
            )
        }
        return sb.toString()
    }

    /**
     * Applies trilinear 3D LUT color interpolation in-place on [bitmap].
     * Follows Adobe .cube order: Red varies fastest, then Green, then Blue.
     */
    fun applyLut3DInPlace(bitmap: Bitmap, lut: Lut3D, intensity: Float = 1.0f) {
        val mix = intensity.coerceIn(0f, 1f)
        if (mix <= 0.001f) return

        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val size = lut.size
        val maxIdx = (size - 1).toFloat()
        val table = lut.table
        val dMinR = lut.domainMin[0]
        val dMinG = lut.domainMin[1]
        val dMinB = lut.domainMin[2]
        val dSpanR = (lut.domainMax[0] - dMinR).let { if (it <= 0.0001f) 1f else it }
        val dSpanG = (lut.domainMax[1] - dMinG).let { if (it <= 0.0001f) 1f else it }
        val dSpanB = (lut.domainMax[2] - dMinB).let { if (it <= 0.0001f) 1f else it }
        val strideG = size
        val strideB = size * size
        val invMix = 1f - mix

        for (i in 0 until total) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            val rOrig = ((c shr 16) and 0xFF) / 255f
            val gOrig = ((c shr 8) and 0xFF) / 255f
            val bOrig = (c and 0xFF) / 255f

            val nr = ((rOrig - dMinR) / dSpanR).coerceIn(0f, 1f) * maxIdx
            val ng = ((gOrig - dMinG) / dSpanG).coerceIn(0f, 1f) * maxIdx
            val nb = ((bOrig - dMinB) / dSpanB).coerceIn(0f, 1f) * maxIdx

            val r0 = floor(nr).toInt().coerceIn(0, size - 1)
            val g0 = floor(ng).toInt().coerceIn(0, size - 1)
            val b0 = floor(nb).toInt().coerceIn(0, size - 1)
            val r1 = min(r0 + 1, size - 1)
            val g1 = min(g0 + 1, size - 1)
            val b1 = min(b0 + 1, size - 1)

            val fr = nr - r0
            val fg = ng - g0
            val fb = nb - b0

            val idx000 = (b0 * strideB + g0 * strideG + r0) * 3
            val idx100 = (b0 * strideB + g0 * strideG + r1) * 3
            val idx010 = (b0 * strideB + g1 * strideG + r0) * 3
            val idx110 = (b0 * strideB + g1 * strideG + r1) * 3
            val idx001 = (b1 * strideB + g0 * strideG + r0) * 3
            val idx101 = (b1 * strideB + g0 * strideG + r1) * 3
            val idx011 = (b1 * strideB + g1 * strideG + r0) * 3
            val idx111 = (b1 * strideB + g1 * strideG + r1) * 3

            val w000 = (1f - fr) * (1f - fg) * (1f - fb)
            val w100 = fr * (1f - fg) * (1f - fb)
            val w010 = (1f - fr) * fg * (1f - fb)
            val w110 = fr * fg * (1f - fb)
            val w001 = (1f - fr) * (1f - fg) * fb
            val w101 = fr * (1f - fg) * fb
            val w011 = (1f - fr) * fg * fb
            val w111 = fr * fg * fb

            val lutR = table[idx000] * w000 + table[idx100] * w100 +
                table[idx010] * w010 + table[idx110] * w110 +
                table[idx001] * w001 + table[idx101] * w101 +
                table[idx011] * w011 + table[idx111] * w111

            val lutG = table[idx000 + 1] * w000 + table[idx100 + 1] * w100 +
                table[idx010 + 1] * w010 + table[idx110 + 1] * w110 +
                table[idx001 + 1] * w001 + table[idx101 + 1] * w101 +
                table[idx011 + 1] * w011 + table[idx111 + 1] * w111

            val lutB = table[idx000 + 2] * w000 + table[idx100 + 2] * w100 +
                table[idx010 + 2] * w010 + table[idx110 + 2] * w110 +
                table[idx001 + 2] * w001 + table[idx101 + 2] * w101 +
                table[idx011 + 2] * w011 + table[idx111 + 2] * w111

            val outR = ((rOrig * invMix + lutR * mix) * 255f).roundToInt().coerceIn(0, 255)
            val outG = ((gOrig * invMix + lutG * mix) * 255f).roundToInt().coerceIn(0, 255)
            val outB = ((bOrig * invMix + lutB * mix) * 255f).roundToInt().coerceIn(0, 255)
            pixels[i] = (a shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}

object BuiltInLuts {

    private fun buildCube(
        id: String,
        name: String,
        size: Int = 17,
        transform: (r: Float, g: Float, b: Float) -> Triple<Float, Float, Float>
    ): Lut3D {
        val table = FloatArray(size * size * size * 3)
        val denom = (size - 1).toFloat()
        var ptr = 0
        for (bi in 0 until size) {
            val b = bi / denom
            for (gi in 0 until size) {
                val g = gi / denom
                for (ri in 0 until size) {
                    val r = ri / denom
                    val (nr, ng, nb) = transform(r, g, b)
                    table[ptr++] = nr.coerceIn(0f, 1f)
                    table[ptr++] = ng.coerceIn(0f, 1f)
                    table[ptr++] = nb.coerceIn(0f, 1f)
                }
            }
        }
        return Lut3D(
            id = id,
            name = name,
            size = size,
            table = table,
            isBuiltIn = true
        )
    }

    val TEAL_AMBER: Lut3D by lazy {
        buildCube("lut_teal_amber", "Teal & Amber") { r, g, b ->
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            val shWeight = (1f - (lum / 0.55f)).coerceIn(0f, 1f)
            val hiWeight = ((lum - 0.35f) / 0.65f).coerceIn(0f, 1f)
            // Filmic S-curve contrast
            val scR = (r * r * (3f - 2f * r)) * 0.35f + r * 0.65f
            val scG = (g * g * (3f - 2f * g)) * 0.35f + g * 0.65f
            val scB = (b * b * (3f - 2f * b)) * 0.35f + b * 0.65f
            Triple(
                scR - shWeight * 0.08f + hiWeight * 0.10f,
                scG + shWeight * 0.03f + hiWeight * 0.02f,
                scB + shWeight * 0.11f - hiWeight * 0.10f
            )
        }
    }

    val KODACHROME_64: Lut3D by lazy {
        buildCube("lut_kodachrome_64", "Kodachrome 64") { r, g, b ->
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            val rBoost = lum + (r - lum) * 1.24f
            val gBoost = lum + (g - lum) * 1.12f
            val bBoost = lum + (b - lum) * 1.06f
            val cr = rBoost.coerceIn(0f, 1f).pow(1.06f) * 1.04f + 0.015f
            val cg = gBoost.coerceIn(0f, 1f).pow(1.04f) * 0.99f + 0.012f
            val cb = bBoost.coerceIn(0f, 1f).pow(1.10f) * 0.94f + 0.020f
            Triple(cr, cg, cb)
        }
    }

    val TOKYO_NEON: Lut3D by lazy {
        buildCube("lut_tokyo_neon", "Tokyo Neon") { r, g, b ->
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            val shWeight = (1f - (lum / 0.50f)).coerceIn(0f, 1f)
            val hiWeight = ((lum - 0.50f) / 0.50f).coerceIn(0f, 1f)
            val satR = lum + (r - lum) * 1.28f
            val satG = lum + (g - lum) * 1.18f
            val satB = lum + (b - lum) * 1.32f
            Triple(
                satR + shWeight * 0.04f + hiWeight * 0.12f,
                satG + shWeight * 0.05f - hiWeight * 0.04f,
                satB + shWeight * 0.16f + hiWeight * 0.08f
            )
        }
    }

    val BLEACH_BYPASS: Lut3D by lazy {
        buildCube("lut_bleach_bypass", "Bleach Bypass") { r, g, b ->
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            val contrastLum = (lum * lum * (3f - 2f * lum)).coerceIn(0f, 1f)
            val desatR = r * 0.42f + contrastLum * 0.58f
            val desatG = g * 0.42f + contrastLum * 0.58f
            val desatB = b * 0.40f + contrastLum * 0.60f
            val angleBoost = 0.03f * sin(lum * Math.PI.toFloat())
            Triple(desatR - angleBoost * 0.5f, desatG, desatB + angleBoost)
        }
    }

    val ALL: List<Lut3D> by lazy {
        listOf(TEAL_AMBER, KODACHROME_64, TOKYO_NEON, BLEACH_BYPASS)
    }
}
