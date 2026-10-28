package com.zts.sanshi.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.min

val LocalReduceMotion = staticCompositionLocalOf { false }

object Motion {
    val EnterEasing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)
    val ExitEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    fun ms(base: Int, reduceMotion: Boolean): Int = if (reduceMotion) min(base, 80) else base

    fun stagger(index: Int, reduceMotion: Boolean): Int = if (reduceMotion) 0 else index * 40
}
