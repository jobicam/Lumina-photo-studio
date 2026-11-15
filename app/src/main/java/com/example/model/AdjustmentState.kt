package com.example.model

enum class AdjustCategory(val label: String) {
    LIGHT("Light"),
    COLOR("Color"),
    DETAIL("Detail"),
    EFFECTS("Effects")
}

data class AdjustmentState(
    // Light
    val exposure: Float = 0f,       // -100f to 100f
    val contrast: Float = 0f,       // -100f to 100f
    val highlights: Float = 0f,     // -100f to 100f
    val shadows: Float = 0f,        // -100f to 100f
    val whites: Float = 0f,         // -100f to 100f
    val blacks: Float = 0f,         // -100f to 100f

    // Color
    val temperature: Float = 0f,    // -100f (Cool) to 100f (Warm)
    val tint: Float = 0f,           // -100f (Green) to 100f (Magenta)
    val saturation: Float = 0f,     // -100f to 100f
    val vibrance: Float = 0f,       // -100f to 100f

    // Effects & Detail
    val clarity: Float = 0f,        // -100f to 100f
    val sharpness: Float = 0f,      // 0f to 100f
    val backgroundBlur: Float = 0f, // 0f to 100f
    val blurFocusRadius: Float = 0.32f, // 0.15f to 0.70f
    val blurFocusX: Float = 0.50f,      // 0f to 1f
    val blurFocusY: Float = 0.42f,      // 0f to 1f
    val blurProtectSubject: Boolean = true,
    val softGlow: Float = 0f,       // 0f to 100f
    val vignette: Float = 0f,       // -100f (White) to 100f (Dark)
    val grain: Float = 0f,          // 0f to 100f
    val fade: Float = 0f            // 0f to 100f
) {
    val isDefault: Boolean
        get() = exposure == 0f && contrast == 0f && highlights == 0f && shadows == 0f &&
                whites == 0f && blacks == 0f && temperature == 0f && tint == 0f &&
                saturation == 0f && vibrance == 0f && clarity == 0f && sharpness == 0f &&
                backgroundBlur == 0f && softGlow == 0f && vignette == 0f && grain == 0f && fade == 0f
}
