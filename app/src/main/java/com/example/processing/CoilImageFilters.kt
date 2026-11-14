package com.example.processing

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation

/**
 * Coil Transformation that converts an image to Grayscale (monochrome).
 */
class CoilGrayscaleTransformation(
    val intensity: Float = 1.0f
) : Transformation {

    override val cacheKey: String = "coil_filter_grayscale_${intensity}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return BitmapProcessor.applyGrayscale(input, intensity)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is CoilGrayscaleTransformation && other.intensity == intensity
    }

    override fun hashCode(): Int {
        return intensity.hashCode()
    }
}

/**
 * Coil Transformation that applies vintage Sepia toning.
 */
class CoilSepiaTransformation(
    val intensity: Float = 1.0f
) : Transformation {

    override val cacheKey: String = "coil_filter_sepia_${intensity}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return BitmapProcessor.applySepia(input, intensity)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is CoilSepiaTransformation && other.intensity == intensity
    }

    override fun hashCode(): Int {
        return intensity.hashCode()
    }
}

/**
 * Coil Transformation that applies a High-Contrast photographic look.
 */
class CoilHighContrastTransformation(
    val contrastLevel: Float = 1.6f
) : Transformation {

    override val cacheKey: String = "coil_filter_high_contrast_${contrastLevel}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        return BitmapProcessor.applyHighContrast(input, contrastLevel)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is CoilHighContrastTransformation && other.contrastLevel == contrastLevel
    }

    override fun hashCode(): Int {
        return contrastLevel.hashCode()
    }
}
