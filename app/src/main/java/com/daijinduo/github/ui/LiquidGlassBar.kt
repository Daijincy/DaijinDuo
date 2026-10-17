package com.daijinduo.github.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.LocalHazeStyle
import dev.chrisbanes.haze.haze
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 玻璃悬浮 Tab 底栏（iOS 26 液态玻璃 Liquid Glass 风格，正宗写法）
 *
 * 构成：悬浮胶囊容器 · Haze 真实背景模糊（毛玻璃透镜）·
 *      顶部渐变高光描边 · 弹性滑动的液态选中 Pill · 双页切换（Duo / Show）。
 */
@Composable
fun LiquidGlassBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
        ) {
            val glassStyle = HazeStyle(
                backgroundColor = Color(0xFF171717),
                tint = HazeTint(Color(0xFF000000).copy(alpha = 0.42f)),
                blurRadius = 24.dp,
                noiseFactor = 0.06f
            )

            // 选中 Pill：弹性滑动（液态吸附）
            val pillWidth = (maxWidth - 12.dp) / 2
            val pillX by animateDpAsState(
                targetValue = if (selected == 0) 6.dp else 6.dp + pillWidth,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "glassPill"
            )

            // 玻璃容器（Haze 1.x：style 通过 LocalHazeStyle CompositionLocal 提供）
            CompositionLocalProvider(LocalHazeStyle provides glassStyle) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(33.dp))
                    .haze(hazeState)
                    // 顶部高光描边（玻璃质感）
                    .drawWithCache {
                        onDrawBehind {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.32f),
                                        Color.White.copy(alpha = 0.05f),
                                        Color.Transparent
                                    )
                                ),
                                size = Size(size.width, 4.dp.toPx())
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                ),
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, 1.dp.toPx())
                            )
                        }
                    }
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        ),
                        shape = RoundedCornerShape(33.dp)
                    )
            ) {
                // 选中 Pill
                Box(
                    Modifier
                        .offset(x = pillX, y = 5.dp)
                        .width(pillWidth)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.085f))
                        .drawWithCache {
                            onDrawBehind {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.14f),
                                            Color.Transparent
                                        )
                                    ),
                                    size = Size(size.width, 2.5.dp.toPx())
                                )
                            }
                        }
                )

                Row(Modifier.fillMaxSize()) {
                    GlassTab(
                        label = "Duo",
                        icon = TabIcon.DUO,
                        selected = selected == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(0) }
                    )
                    GlassTab(
                        label = "Show",
                        icon = TabIcon.SHOW,
                        selected = selected == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(1) }
                    )
                }
            }
            }
        }
    }
}

private enum class TabIcon { DUO, SHOW }

@Composable
private fun GlassTab(
    label: String,
    icon: TabIcon,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val color = if (selected) TextPrimary else TextSecondary.copy(alpha = 0.75f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(33.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TabGlyph(icon = icon, color = color, modifier = Modifier.size(22.dp))
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

/** 复古线框图标：Duo = 双窗（呼应品牌 Logo）；Show = 五角星 */
@Composable
private fun TabGlyph(icon: TabIcon, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (icon) {
            TabIcon.DUO -> {
                // 左窗
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.08f, h * 0.22f),
                    size = Size(w * 0.38f, h * 0.56f),
                    cornerRadius = CornerRadius(w * 0.07f)
                )
                // 右窗
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.54f, h * 0.22f),
                    size = Size(w * 0.38f, h * 0.56f),
                    cornerRadius = CornerRadius(w * 0.07f)
                )
                // 中缝
                drawLine(
                    color = color,
                    start = Offset(w * 0.485f, h * 0.28f),
                    end = Offset(w * 0.485f, h * 0.72f),
                    strokeWidth = w * 0.045f
                )
            }

            TabIcon.SHOW -> {
                val path = Path()
                val cx = w / 2f
                val cy = h / 2f
                val outer = w * 0.42f
                val inner = outer * 0.45f
                for (i in 0 until 10) {
                    val radius = if (i % 2 == 0) outer else inner
                    val angle = -PI / 2 + i * PI / 5
                    val x = cx + (cos(angle) * radius).toFloat()
                    val y = cy + (sin(angle) * radius).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path = path, color = color)
            }
        }
    }
}
