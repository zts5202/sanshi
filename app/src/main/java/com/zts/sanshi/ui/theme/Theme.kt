package com.zts.sanshi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Bg = Color(0xFFFFFFFF)
val Surface = Color(0xFFF8FAFC)
val Ink = Color(0xFF0A0A0A)
val InkMuted = Color(0xFF6B7280)
val InkDisabled = Color(0xFF9CA3AF)
val Blue = Color(0xFF2563EB)
val BluePressed = Color(0xFF1D4ED8)
val Border = Color(0xFFE5E7EB)
val Danger = Color(0xFFDC2626)
val SuccessSoft = Color(0xFFEFF6FF)
val DisabledFill = Color(0xFFF3F4F6)
val EmptyIcon = Color(0xFFCBD5E1)
val Shadow = Color(0x0F0A0A0A)

private val SanshiColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = SuccessSoft,
    onPrimaryContainer = Ink,
    secondary = Blue,
    onSecondary = Color.White,
    background = Bg,
    onBackground = Ink,
    surface = Bg,
    onSurface = Ink,
    surfaceVariant = Surface,
    onSurfaceVariant = InkMuted,
    outline = Border,
    error = Danger,
    onError = Color.White,
)

private val SanshiTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        color = Ink,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = Ink,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = InkMuted,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = InkMuted,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
)

@Composable
fun SanshiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SanshiColors,
        typography = SanshiTypography,
        content = content,
    )
}
