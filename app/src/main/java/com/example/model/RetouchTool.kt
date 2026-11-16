package com.example.model

enum class RetouchCategory {
    SKIN,
    FEATURES,
    CONTOUR
}

enum class RetouchType(
    val title: String,
    val subtitle: String,
    val defaultRadius: Float,
    val defaultIntensity: Float,
    val category: RetouchCategory = RetouchCategory.SKIN
) {
    SKIN_SMOOTH(
        title = "Skin Smooth",
        subtitle = "Softens skin texture & evens pores",
        defaultRadius = 40f,
        defaultIntensity = 0.65f,
        category = RetouchCategory.SKIN
    ),
    TEETH_WHITEN(
        title = "Teeth Whiten",
        subtitle = "Removes yellow tint & brightens teeth",
        defaultRadius = 28f,
        defaultIntensity = 0.70f,
        category = RetouchCategory.FEATURES
    ),
    EYE_POP(
        title = "Eye Pop",
        subtitle = "Enhances iris sparkle & eye clarity",
        defaultRadius = 30f,
        defaultIntensity = 0.60f,
        category = RetouchCategory.FEATURES
    ),
    SCULPT(
        title = "Sculpt & Warp",
        subtitle = "Nudge & refine contours smoothly",
        defaultRadius = 45f,
        defaultIntensity = 0.50f,
        category = RetouchCategory.CONTOUR
    ),
    DETAIL_SHARPEN(
        title = "Detail Brush",
        subtitle = "Sharpen eyes, brows, lips & hair",
        defaultRadius = 32f,
        defaultIntensity = 0.75f,
        category = RetouchCategory.FEATURES
    ),
    GLOW(
        title = "Highlighter",
        subtitle = "Adds a subtle radiant beauty glow",
        defaultRadius = 48f,
        defaultIntensity = 0.45f,
        category = RetouchCategory.CONTOUR
    )
}

data class RetouchStroke(
    val type: RetouchType,
    val points: List<StrokePoint>,
    val radius: Float,
    val intensity: Float,
    val isEraser: Boolean = false,
    val texturePreserve: Float = 0.30f
)

data class StrokePoint(
    val normX: Float, // 0.0f to 1.0f relative to image
    val normY: Float,
    val pressure: Float = 1.0f
)

