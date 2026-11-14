package com.example.model

enum class FilterCategory(val label: String) {
    ALL("All"),
    PORTRAIT("Portrait"),
    CINEMATIC("Cinematic"),
    VINTAGE("Vintage"),
    BLACK_WHITE("B&W"),
    AESTHETIC("Aesthetic"),
    LANDSCAPE("Nature")
}

data class FilterPreset(
    val id: String,
    val name: String,
    val category: FilterCategory,
    val description: String,
    // Base adjustments applied by this preset
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val saturation: Float = 0f,
    val vibrance: Float = 0f,
    val clarity: Float = 0f,
    val fade: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    // Custom RGB Tint shifts for distinct photographic looks
    val redTint: Float = 1.0f,
    val greenTint: Float = 1.0f,
    val blueTint: Float = 1.0f,
    val isGrayscale: Boolean = false,
    val isSepia: Boolean = false
)

object FilterPresets {
    val NONE = FilterPreset(
        id = "original",
        name = "Original",
        category = FilterCategory.ALL,
        description = "No filter applied"
    )

    val GRAYSCALE = FilterPreset(
        id = "grayscale",
        name = "Grayscale",
        category = FilterCategory.BLACK_WHITE,
        description = "Classic neutral black and white conversion",
        contrast = 15f,
        isGrayscale = true
    )

    val SEPIA = FilterPreset(
        id = "sepia",
        name = "Sepia",
        category = FilterCategory.VINTAGE,
        description = "Warm antique sepia monochrome emulsion",
        contrast = 12f,
        isSepia = true
    )

    val HIGH_CONTRAST = FilterPreset(
        id = "high_contrast",
        name = "High Contrast",
        category = FilterCategory.ALL,
        description = "Bold, punchy highlights and deep rich shadows",
        contrast = 58f,
        highlights = 15f,
        shadows = -20f,
        clarity = 20f
    )

    val ALL_PRESETS = listOf(
        NONE,
        GRAYSCALE,
        SEPIA,
        HIGH_CONTRAST,
        // Portrait
        FilterPreset(
            id = "porcelain",
            name = "Porcelain",
            category = FilterCategory.PORTRAIT,
            description = "Soft, luminous skin with delicate warm highlights",
            exposure = 8f,
            contrast = -5f,
            highlights = 15f,
            shadows = 10f,
            temperature = 5f,
            saturation = -5f,
            vibrance = 12f,
            clarity = -8f,
            fade = 4f,
            redTint = 1.04f,
            greenTint = 1.01f,
            blueTint = 0.98f
        ),
        FilterPreset(
            id = "golden_glow",
            name = "Golden Glow",
            category = FilterCategory.PORTRAIT,
            description = "Rich golden hour sun-kissed warmth",
            exposure = 5f,
            contrast = 12f,
            highlights = -10f,
            shadows = 15f,
            temperature = 35f,
            tint = 8f,
            saturation = 18f,
            vignette = 15f,
            redTint = 1.15f,
            greenTint = 1.05f,
            blueTint = 0.88f
        ),
        FilterPreset(
            id = "velvet_rose",
            name = "Velvet Rose",
            category = FilterCategory.PORTRAIT,
            description = "Dreamy magenta-peach tones and soft contrast",
            contrast = -8f,
            shadows = 18f,
            temperature = 10f,
            tint = 28f,
            saturation = 8f,
            fade = 10f,
            redTint = 1.12f,
            greenTint = 0.96f,
            blueTint = 1.05f
        ),

        // Cinematic
        FilterPreset(
            id = "teal_orange",
            name = "Teal & Orange",
            category = FilterCategory.CINEMATIC,
            description = "Hollywood blockbuster complementary color contrast",
            contrast = 24f,
            highlights = -15f,
            shadows = 20f,
            temperature = 15f,
            saturation = 22f,
            clarity = 15f,
            vignette = 25f,
            redTint = 1.18f,
            greenTint = 0.98f,
            blueTint = 1.25f
        ),
        FilterPreset(
            id = "tokyo_night",
            name = "Cyberpunk",
            category = FilterCategory.CINEMATIC,
            description = "High octane electric neon blues and violets",
            contrast = 35f,
            highlights = 20f,
            shadows = -25f,
            temperature = -25f,
            tint = 30f,
            saturation = 35f,
            clarity = 25f,
            vignette = 30f,
            redTint = 1.10f,
            greenTint = 0.85f,
            blueTint = 1.30f
        ),
        FilterPreset(
            id = "moody_drama",
            name = "Moody Film",
            category = FilterCategory.CINEMATIC,
            description = "Deep crushed blacks and atmospheric muted colors",
            exposure = -8f,
            contrast = 20f,
            highlights = -20f,
            shadows = -15f,
            temperature = -10f,
            saturation = -22f,
            clarity = 18f,
            fade = 12f,
            vignette = 40f,
            redTint = 0.95f,
            greenTint = 1.02f,
            blueTint = 1.08f
        ),

        // Vintage
        FilterPreset(
            id = "kodachrome_64",
            name = "Koda 64",
            category = FilterCategory.VINTAGE,
            description = "Classic 1960s slide film with rich reds and warm greens",
            contrast = 18f,
            highlights = 10f,
            shadows = -12f,
            temperature = 18f,
            tint = -5f,
            saturation = 15f,
            grain = 28f,
            redTint = 1.16f,
            greenTint = 1.08f,
            blueTint = 0.92f
        ),
        FilterPreset(
            id = "retro_1978",
            name = "Retro '78",
            category = FilterCategory.VINTAGE,
            description = "Sun-faded 35mm print with warm amber wash",
            contrast = -12f,
            shadows = 25f,
            temperature = 30f,
            tint = 12f,
            saturation = -10f,
            fade = 22f,
            grain = 35f,
            vignette = 20f,
            redTint = 1.14f,
            greenTint = 1.06f,
            blueTint = 0.82f
        ),
        FilterPreset(
            id = "disposable",
            name = "Disposable Cam",
            category = FilterCategory.VINTAGE,
            description = "Direct flash look with high contrast and film grain",
            exposure = 10f,
            contrast = 30f,
            highlights = 25f,
            shadows = -20f,
            temperature = 12f,
            saturation = 15f,
            grain = 40f,
            redTint = 1.08f,
            greenTint = 1.02f,
            blueTint = 0.94f
        ),

        // B&W
        FilterPreset(
            id = "silver_noir",
            name = "Silver Noir",
            category = FilterCategory.BLACK_WHITE,
            description = "Dramatic high-contrast monochrome with deep blacks",
            contrast = 40f,
            highlights = 20f,
            shadows = -30f,
            clarity = 25f,
            vignette = 35f,
            grain = 15f,
            isGrayscale = true
        ),
        FilterPreset(
            id = "matte_charcoal",
            name = "Matte Charcoal",
            category = FilterCategory.BLACK_WHITE,
            description = "Soft velvety tones with lifted shadows and matte finish",
            contrast = 5f,
            shadows = 25f,
            highlights = -10f,
            clarity = -5f,
            fade = 24f,
            grain = 20f,
            isGrayscale = true
        ),
        FilterPreset(
            id = "street_mono",
            name = "Street 400",
            category = FilterCategory.BLACK_WHITE,
            description = "Punchy documentary street photography look with heavy grain",
            contrast = 25f,
            clarity = 30f,
            grain = 50f,
            vignette = 15f,
            isGrayscale = true
        ),

        // Aesthetic
        FilterPreset(
            id = "pastel_dream",
            name = "Pastel Dream",
            category = FilterCategory.AESTHETIC,
            description = "Ethereal candy colors with high key illumination",
            exposure = 14f,
            contrast = -15f,
            highlights = 20f,
            shadows = 30f,
            temperature = -8f,
            tint = 18f,
            saturation = 12f,
            fade = 15f,
            redTint = 1.05f,
            greenTint = 0.98f,
            blueTint = 1.12f
        ),
        FilterPreset(
            id = "nordic_minimal",
            name = "Nordic",
            category = FilterCategory.AESTHETIC,
            description = "Clean cool desaturated scandi minimalism",
            exposure = 6f,
            contrast = 10f,
            highlights = 15f,
            temperature = -20f,
            tint = -5f,
            saturation = -35f,
            clarity = 10f,
            fade = 8f,
            redTint = 0.92f,
            greenTint = 1.02f,
            blueTint = 1.15f
        ),

        // Landscape
        FilterPreset(
            id = "emerald_forest",
            name = "Emerald Lush",
            category = FilterCategory.LANDSCAPE,
            description = "Vivid natural greens and rich earthy depth",
            contrast = 15f,
            highlights = -12f,
            shadows = 18f,
            temperature = -5f,
            tint = -15f,
            saturation = 28f,
            vibrance = 30f,
            clarity = 15f,
            redTint = 0.94f,
            greenTint = 1.20f,
            blueTint = 0.98f
        ),
        FilterPreset(
            id = "sunset_pop",
            name = "Sunset Pop",
            category = FilterCategory.LANDSCAPE,
            description = "Dramatic sky enhancement and golden landscape glow",
            contrast = 22f,
            highlights = -25f,
            shadows = 20f,
            temperature = 28f,
            tint = 15f,
            saturation = 35f,
            vibrance = 25f,
            vignette = 20f,
            redTint = 1.22f,
            greenTint = 1.04f,
            blueTint = 0.92f
        ),
        FilterPreset(
            id = "portra_400",
            name = "Portra 400",
            category = FilterCategory.PORTRAIT,
            description = "Balanced natural skin tones with creamy highlights and fine grain",
            exposure = 6f,
            contrast = 6f,
            highlights = -12f,
            shadows = 16f,
            temperature = 10f,
            tint = 4f,
            vibrance = 16f,
            fade = 6f,
            grain = 18f,
            redTint = 1.06f,
            greenTint = 1.02f,
            blueTint = 0.96f
        ),
        FilterPreset(
            id = "obsidian_gold",
            name = "Obsidian",
            category = FilterCategory.CINEMATIC,
            description = "Rich crushed shadows with luxurious warm gold highlights",
            contrast = 28f,
            highlights = 12f,
            shadows = -22f,
            temperature = 22f,
            saturation = -12f,
            clarity = 22f,
            vignette = 32f,
            redTint = 1.14f,
            greenTint = 1.04f,
            blueTint = 0.86f
        ),
        FilterPreset(
            id = "dream_halation",
            name = "Halation",
            category = FilterCategory.AESTHETIC,
            description = "Warm sunlit bloom and pastel film halation mood",
            exposure = 10f,
            contrast = -6f,
            highlights = 18f,
            shadows = 18f,
            temperature = 14f,
            tint = 12f,
            vibrance = 14f,
            fade = 14f,
            redTint = 1.10f,
            greenTint = 0.99f,
            blueTint = 1.02f
        ),
        FilterPreset(
            id = "alpine_crisp",
            name = "Alpine",
            category = FilterCategory.LANDSCAPE,
            description = "Cool crisp mountain light with razor-sharp clarity",
            contrast = 18f,
            highlights = -15f,
            shadows = 14f,
            temperature = -14f,
            vibrance = 22f,
            clarity = 28f,
            redTint = 0.95f,
            greenTint = 1.03f,
            blueTint = 1.14f
        )
    )
}
