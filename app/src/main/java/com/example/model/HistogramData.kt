package com.example.model

/**
 * Represents the 256-bucket RGB and Luminance frequency distribution
 * computed from a processed bitmap.
 */
data class HistogramData(
    val red: IntArray = IntArray(256),
    val green: IntArray = IntArray(256),
    val blue: IntArray = IntArray(256),
    val luminance: IntArray = IntArray(256),
    val maxCount: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistogramData

        if (!red.contentEquals(other.red)) return false
        if (!green.contentEquals(other.green)) return false
        if (!blue.contentEquals(other.blue)) return false
        if (!luminance.contentEquals(other.luminance)) return false
        if (maxCount != other.maxCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = red.contentHashCode()
        result = 31 * result + green.contentHashCode()
        result = 31 * result + blue.contentHashCode()
        result = 31 * result + luminance.contentHashCode()
        result = 31 * result + maxCount
        return result
    }
}
