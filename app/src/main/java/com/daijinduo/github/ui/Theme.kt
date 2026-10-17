package com.daijinduo.github.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ============ 复古简约 · 色板 ============
// 品牌：黑底 · 白色描边双窗（DaijinDuo Logo）
val AccentA = Color(0xFF000000)        // 纯黑（相册按钮底 · 主按钮）
val AccentB = Color(0xFFC9A227)        // 做旧金（点缀 · 强调）
val AccentC = Color(0xFF8A8577)        // 灰金（次级点缀）
val BgDark = Color(0xFF070707)         // 近黑底
val SurfaceDark = Color(0xFF121212)    // 卡片底
val TextPrimary = Color(0xFFF0EAD9)    // 米白（正文）
val TextSecondary = Color(0xFFA39D8B)  // 灰米（次级）
val PaperWhite = Color(0xFFF5F1E6)     // 纸白（白底按钮文字）

// ============ 思源宋体（Source Han Serif）============
// Android 系统 serif = Noto Serif CJK（即思源宋体），免内置字体资源、零体积开销
private val Serif = FontFamily.Serif

private val SoloColorScheme = darkColorScheme(
    primary = AccentB,
    onPrimary = Color(0xFF111111),
    secondary = AccentC,
    tertiary = AccentB,
    background = BgDark,
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = Color.White.copy(alpha = 0.18f)
)

private val SoloTypography = Typography(
    displayLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp),
    displayMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp),
    displaySmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 16.sp)
)

@Composable
fun SoloTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SoloColorScheme,
        typography = SoloTypography,
        content = content
    )
}
