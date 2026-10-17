package com.daijinduo.github.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.daijinduo.github.R

// ============ 黑白 · 色板 ============
// 品牌：纯黑白 —— 黑底白字 · 白色描边双窗（DaijinDuo Logo）
val AccentA = Color(0xFF000000)        // 纯黑（相册按钮底 · 主按钮）
val AccentB = Color(0xFFFFFFFF)        // 纯白（强调 · 选中态）
val AccentC = Color(0xFFBDBDBD)        // 浅灰（次级点缀）
val BgDark = Color(0xFF000000)         // 纯黑底
val SurfaceDark = Color(0xFF141414)    // 卡片底
val TextPrimary = Color(0xFFFFFFFF)    // 纯白（正文）
val TextSecondary = Color(0xFF9E9E9E)  // 灰（次级）
val PaperWhite = Color(0xFFFFFFFF)     // 白（白底按钮文字）

// ============ 思源宋体（强制内置，非系统兜底）============
// 子集化打包 Noto Serif SC（思源宋体）Regular/Bold 至 res/font，
// 全局所有文本强制使用，任何设备渲染结果一致。
private val Serif = FontFamily(
    Font(R.font.notoserifsc_regular, FontWeight.Normal),
    Font(R.font.notoserifsc_bold, FontWeight.Bold)
)

private val SoloColorScheme = darkColorScheme(
    primary = AccentB,
    onPrimary = AccentA,
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
