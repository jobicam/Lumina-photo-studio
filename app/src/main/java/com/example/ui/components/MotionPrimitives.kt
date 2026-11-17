package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * Shared motion tokens and tactile press modifiers for Lumina Photo Studio.
 */
object StudioMotion {
    val PressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val PanelHeightSpring = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val SlideSpring = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val ColorSpring = spring<Color>(
        stiffness = Spring.StiffnessMediumLow
    )

    const val FADE_IN_MS = 200
    const val FADE_OUT_MS = 150
}

@Composable
fun rememberPressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.95f,
    enabled: Boolean = true
): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = StudioMotion.PressSpring,
        label = "press_scale"
    )
}

/**
 * Applies a responsive spring scale-down on press without intercepting pointer events,
 * keeping Material ripples, accessibility semantics, and testTags intact.
 */
fun Modifier.pressBounce(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.95f,
    enabled: Boolean = true
): Modifier = composed {
    val scale by rememberPressScale(
        interactionSource = interactionSource,
        pressedScale = pressedScale,
        enabled = enabled
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Combines a tactile spring press-bounce with standard Material ripple clickable behavior.
 */
fun Modifier.bounceClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by rememberPressScale(
        interactionSource = interactionSource,
        pressedScale = pressedScale,
        enabled = enabled
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun rememberAnimatedAccentColor(
    targetColor: Color,
    label: String = "animated_color"
): State<Color> = animateColorAsState(
    targetValue = targetColor,
    animationSpec = tween(durationMillis = 180),
    label = label
)
