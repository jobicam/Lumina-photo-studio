package com.example.model

data class Lut3D(
    val id: String,
    val name: String,
    val size: Int,
    val domainMin: FloatArray = floatArrayOf(0f, 0f, 0f),
    val domainMax: FloatArray = floatArrayOf(1f, 1f, 1f),
    val table: FloatArray,
    val isBuiltIn: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Lut3D) return false
        return id == other.id &&
                name == other.name &&
                size == other.size &&
                isBuiltIn == other.isBuiltIn &&
                domainMin.contentEquals(other.domainMin) &&
                domainMax.contentEquals(other.domainMax) &&
                table.contentEquals(other.table)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + size
        result = 31 * result + isBuiltIn.hashCode()
        result = 31 * result + domainMin.contentHashCode()
        result = 31 * result + domainMax.contentHashCode()
        result = 31 * result + table.contentHashCode()
        return result
    }
}

data class CustomPreset(
    val id: String,
    val name: String,
    val createdAtMillis: Long,
    val adjustments: AdjustmentState = AdjustmentState(),
    val filterId: String = "original",
    val filterIntensity: Float = 1.0f,
    val colorGrade: ColorGradeState = ColorGradeState(),
    val curves: CurvesState = CurvesState(),
    val hsl: HslState = HslState(),
    val lutId: String? = null,
    val lutIntensity: Float = 1.0f
)

enum class FilterBrowserMode {
    FILTERS,
    PRESETS,
    LUTS
}
