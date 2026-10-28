package com.zts.sanshi.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.zts.sanshi.ui.theme.LocalReduceMotion
import com.zts.sanshi.ui.theme.Motion

@Composable
fun Modifier.pressScale(
    interaction: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val reduceMotion = LocalReduceMotion.current
    val pressed by interaction.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pressed, enabled, reduceMotion) {
        if (!enabled || reduceMotion) {
            scale.snapTo(1f)
        } else if (pressed) {
            scale.animateTo(0.96f, tween(durationMillis = 80, easing = Motion.ExitEasing))
        } else {
            scale.animateTo(
                1f,
                spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
            )
        }
    }
    val value = scale.value
    return this.graphicsLayer {
        scaleX = value
        scaleY = value
    }
}
