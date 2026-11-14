package com.example.model

import androidx.compose.ui.graphics.Color

enum class HslColorChannel(
    val displayName: String,
    val centerHue: Float, // 0 to 360
    val previewColor: Color
) {
    RED("Red", 0f, Color(0xFFEF5350)),
    ORANGE("Orange", 30f, Color(0xFFFF9800)),
    YELLOW("Yellow", 60f, Color(0xFFFFEE58)),
    GREEN("Green", 120f, Color(0xFF66BB6A)),
    CYAN("Cyan", 180f, Color(0xFF26C6DA)),
    BLUE("Blue", 240f, Color(0xFF42A5F5)),
    PURPLE("Purple", 280f, Color(0xFFAB47BC)),
    MAGENTA("Magenta", 320f, Color(0xFFEC407A))
}

data class HslChannelAdjustment(
    val hue: Float = 0f,         // -100f to 100f (corresponds to -30° to +30° shift)
    val saturation: Float = 0f,  // -100f to 100f
    val luminance: Float = 0f    // -100f to 100f
) {
    val isDefault: Boolean
        get() = hue == 0f && saturation == 0f && luminance == 0f
}

data class HslState(
    val adjustments: Map<HslColorChannel, HslChannelAdjustment> = HslColorChannel.values().associateWith { HslChannelAdjustment() },
    val activeChannel: HslColorChannel = HslColorChannel.ORANGE
) {
    val isDefault: Boolean
        get() = adjustments.values.all { it.isDefault }

    fun getAdjustment(channel: HslColorChannel): HslChannelAdjustment =
        adjustments[channel] ?: HslChannelAdjustment()

    fun updateActiveAdjustment(adjustment: HslChannelAdjustment): HslState {
        val updated = adjustments.toMutableMap()
        updated[activeChannel] = adjustment
        return copy(adjustments = updated)
    }

    fun resetActiveChannel(): HslState {
        return updateActiveAdjustment(HslChannelAdjustment())
    }

    fun resetAll(): HslState {
        return HslState()
    }
}
