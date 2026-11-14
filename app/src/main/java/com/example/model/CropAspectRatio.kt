package com.example.model

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    ORIGINAL("Original", null),
    SQUARE("1:1", 1.0f),
    PORTRAIT_4_5("4:5", 4f / 5f),
    STORY_9_16("9:16", 9f / 16f),
    LANDSCAPE_16_9("16:9", 16f / 9f),
    STANDARD_3_4("3:4", 3f / 4f),
    CLASSIC_2_3("2:3", 2f / 3f)
}

enum class FrameStyle(val label: String) {
    NONE("None"),
    CLASSIC_WHITE("White"),
    POLAROID("Polaroid"),
    CINEMA("Cinema"),
    OBSIDIAN("Obsidian")
}

data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val width: Float get() = (right - left).coerceIn(0f, 1f)
    val height: Float get() = (bottom - top).coerceIn(0f, 1f)
    val isFull: Boolean
        get() = left <= 0.002f && top <= 0.002f && right >= 0.998f && bottom >= 0.998f

    fun clamped(minSpan: Float = 0.12f): CropRect {
        val span = minSpan.coerceIn(0.05f, 0.5f)
        val l = left.coerceIn(0f, 1f - span)
        val t = top.coerceIn(0f, 1f - span)
        val r = right.coerceIn(l + span, 1f)
        val b = bottom.coerceIn(t + span, 1f)
        return CropRect(l, t, r, b)
    }

    companion object {
        fun effectiveRatio(
            bitmapWidth: Int,
            bitmapHeight: Int,
            aspectRatio: CropAspectRatio
        ): Float? {
            return when (aspectRatio) {
                CropAspectRatio.FREE -> null
                CropAspectRatio.ORIGINAL -> if (bitmapWidth > 0 && bitmapHeight > 0) {
                    bitmapWidth.toFloat() / bitmapHeight.toFloat()
                } else null
                else -> aspectRatio.ratio
            }
        }

        fun fittedForAspectRatio(
            bitmapWidth: Int,
            bitmapHeight: Int,
            aspectRatio: CropAspectRatio
        ): CropRect {
            if (bitmapWidth <= 0 || bitmapHeight <= 0) return CropRect()
            val targetRatio = effectiveRatio(bitmapWidth, bitmapHeight, aspectRatio) ?: return CropRect()
            val imgRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()
            if (kotlin.math.abs(imgRatio - targetRatio) <= 0.005f) return CropRect()

            val normW: Float
            val normH: Float
            if (imgRatio > targetRatio) {
                normH = 1f
                normW = (targetRatio / imgRatio).coerceIn(0.12f, 1f)
            } else {
                normW = 1f
                normH = (imgRatio / targetRatio).coerceIn(0.12f, 1f)
            }
            val left = (1f - normW) / 2f
            val top = (1f - normH) / 2f
            return CropRect(
                left = left,
                top = top,
                right = left + normW,
                bottom = top + normH
            )
        }
    }
}

data class TransformState(
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRatio: CropAspectRatio = CropAspectRatio.ORIGINAL
) {
    val isDefault: Boolean
        get() = rotationDegrees == 0 && !flipHorizontal && !flipVertical && cropRatio == CropAspectRatio.ORIGINAL
}
