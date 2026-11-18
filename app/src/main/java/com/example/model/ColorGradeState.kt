package com.example.model

enum class ColorGradeTone {
    SHADOWS,
    MIDTONES,
    HIGHLIGHTS
}

data class ColorWheelValue(
    val hueDegrees: Float = 0f,   // 0f..360f
    val saturation: Float = 0f,   // 0f..1f
    val luminance: Float = 0f     // -100f..100f
) {
    val isDefault: Boolean
        get() = saturation <= 0.005f && luminance == 0f
}

data class ColorGradeState(
    val activeTone: ColorGradeTone = ColorGradeTone.SHADOWS,
    val shadows: ColorWheelValue = ColorWheelValue(),
    val midtones: ColorWheelValue = ColorWheelValue(),
    val highlights: ColorWheelValue = ColorWheelValue(),
    val balance: Float = 0f,      // -100f..100f shifts shadow/highlight crossover
    val intensity: Float = 1.0f   // 0f..1f global grade mix
) {
    val hasVisibleEffect: Boolean
        get() = intensity > 0.005f && (!shadows.isDefault || !midtones.isDefault || !highlights.isDefault)

    val isDefault: Boolean
        get() = shadows.isDefault && midtones.isDefault && highlights.isDefault &&
                balance == 0f && intensity == 1.0f

    fun getWheel(tone: ColorGradeTone): ColorWheelValue = when (tone) {
        ColorGradeTone.SHADOWS -> shadows
        ColorGradeTone.MIDTONES -> midtones
        ColorGradeTone.HIGHLIGHTS -> highlights
    }

    fun getActiveWheel(): ColorWheelValue = getWheel(activeTone)

    fun updateActiveWheel(value: ColorWheelValue): ColorGradeState = when (activeTone) {
        ColorGradeTone.SHADOWS -> copy(shadows = value)
        ColorGradeTone.MIDTONES -> copy(midtones = value)
        ColorGradeTone.HIGHLIGHTS -> copy(highlights = value)
    }

    fun resetActiveWheel(): ColorGradeState = updateActiveWheel(ColorWheelValue())
}
