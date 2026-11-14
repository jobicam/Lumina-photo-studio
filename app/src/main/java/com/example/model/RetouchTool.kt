package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

enum class RetouchType(
    val title: String,
    val subtitle: String,
    val defaultRadius: Float,
    val defaultIntensity: Float
) {
    BLEMISH(
        title = "Blemish Heal",
        subtitle = "Tap or brush spots to vanish them",
        defaultRadius = 24f,
        defaultIntensity = 1.0f
    ),
    SKIN_SMOOTH(
        title = "Skin Smooth",
        subtitle = "Softens skin texture & evens pores",
        defaultRadius = 40f,
        defaultIntensity = 0.65f
    ),
    TEETH_WHITEN(
        title = "Teeth Whiten",
        subtitle = "Removes yellow tint & brightens teeth",
        defaultRadius = 28f,
        defaultIntensity = 0.70f
    ),
    EYE_POP(
        title = "Eye Pop",
        subtitle = "Enhances iris sparkle & eye clarity",
        defaultRadius = 30f,
        defaultIntensity = 0.60f
    ),
    SCULPT(
        title = "Sculpt & Warp",
        subtitle = "Nudge & refine contours smoothly",
        defaultRadius = 45f,
        defaultIntensity = 0.50f
    ),
    DETAIL_SHARPEN(
        title = "Detail Brush",
        subtitle = "Sharpen eyes, brows, lips & hair",
        defaultRadius = 32f,
        defaultIntensity = 0.75f
    ),
    GLOW(
        title = "Highlighter",
        subtitle = "Adds a subtle radiant beauty glow",
        defaultRadius = 48f,
        defaultIntensity = 0.45f
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
