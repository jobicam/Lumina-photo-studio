package com.example.model

enum class CurveChannel(val label: String) {
    RGB("RGB"),
    RED("Red"),
    GREEN("Green"),
    BLUE("Blue")
}

data class CurvePoint(
    val x: Float, // 0.0 to 1.0
    val y: Float  // 0.0 to 1.0
)

data class SingleCurve(
    val points: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
) {
    fun isLinear(): Boolean {
        if (points.size != 2) return false
        val p0 = points[0]
        val p1 = points[1]
        return p0.x == 0f && p0.y == 0f && p1.x == 1f && p1.y == 1f
    }

    /**
     * Evaluates y given an input x in [0f, 1f] using monotonic cubic interpolation or piecewise linear.
     */
    fun evaluate(x: Float): Float {
        val clampedX = x.coerceIn(0f, 1f)
        val sorted = points.sortedBy { it.x }
        if (sorted.isEmpty()) return clampedX
        if (clampedX <= sorted.first().x) return sorted.first().y.coerceIn(0f, 1f)
        if (clampedX >= sorted.last().x) return sorted.last().y.coerceIn(0f, 1f)

        // Find segment
        for (i in 0 until sorted.size - 1) {
            val pA = sorted[i]
            val pB = sorted[i + 1]
            if (clampedX in pA.x..pB.x) {
                val t = (clampedX - pA.x) / (pB.x - pA.x).coerceAtLeast(0.0001f)
                // Smooth hermite step
                val smoothT = t * t * (3f - 2f * t)
                return (pA.y + (pB.y - pA.y) * smoothT).coerceIn(0f, 1f)
            }
        }
        return clampedX
    }

    /**
     * Builds a 256-entry lookup table (LUT).
     */
    fun buildLut(): IntArray {
        val lut = IntArray(256)
        for (i in 0..255) {
            val normX = i / 255f
            val normY = evaluate(normX)
            lut[i] = (normY * 255f).toInt().coerceIn(0, 255)
        }
        return lut
    }
}

data class CurvesState(
    val rgb: SingleCurve = SingleCurve(),
    val red: SingleCurve = SingleCurve(),
    val green: SingleCurve = SingleCurve(),
    val blue: SingleCurve = SingleCurve(),
    val activeChannel: CurveChannel = CurveChannel.RGB
) {
    val isDefault: Boolean
        get() = rgb.isLinear() && red.isLinear() && green.isLinear() && blue.isLinear()

    fun getActiveCurve(): SingleCurve = when (activeChannel) {
        CurveChannel.RGB -> rgb
        CurveChannel.RED -> red
        CurveChannel.GREEN -> green
        CurveChannel.BLUE -> blue
    }

    fun updateActiveCurve(newCurve: SingleCurve): CurvesState = when (activeChannel) {
        CurveChannel.RGB -> copy(rgb = newCurve)
        CurveChannel.RED -> copy(red = newCurve)
        CurveChannel.GREEN -> copy(green = newCurve)
        CurveChannel.BLUE -> copy(blue = newCurve)
    }

    fun resetActiveCurve(): CurvesState = updateActiveCurve(SingleCurve())

    fun resetAll(): CurvesState = CurvesState()
}
