package com.daijinduo.github.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daijinduo.github.R

/**
 * Duo 主页：复古简约 · 思源宋体
 * 白描边 Logo · 字标 · 两个入口按钮一左一右（相册=黑底白框 / 文件=白底黑字）。
 */
@Composable
fun HomeScreen(
    onPickFromGallery: () -> Unit,
    onSystemPicker: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .padding(horizontal = 28.dp)
    ) {
        Spacer(Modifier.weight(0.5f))

        // Logo（白色描边双窗）
        Image(
            painter = painterResource(R.drawable.logo_daijin),
            contentDescription = "DaijinDuo Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(104.dp)
                .clip(RoundedCornerShape(28.dp))
        )

        Spacer(Modifier.height(26.dp))

        // 字标
        Text(
            text = "DAIJINDUO",
            color = TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "利用 Android 陀螺仪 · 复刻 iPhone DUO 偏转折叠",
            color = TextSecondary,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(0.6f))

        // 动态折叠预览（复古线稿风）
        FoldPreviewCard(Modifier.align(Alignment.CenterHorizontally))

        Spacer(Modifier.weight(0.6f))

        // 两个入口：一左一右，尺寸克制
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            EntryButton(
                title = "相 册",
                subtitle = "本机图片",
                dark = true,
                modifier = Modifier.weight(1f),
                onClick = onPickFromGallery
            )
            EntryButton(
                title = "文 件",
                subtitle = "系统选择",
                dark = false,
                modifier = Modifier.weight(1f),
                onClick = onSystemPicker
            )
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = "轻点三次退出全屏 · 屏幕朝上平放 · 倾斜折叠",
            color = TextSecondary.copy(alpha = 0.55f),
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(22.dp))
    }
}

/** 复古入口按钮：dark=黑底白框（相册）/ 亮=白底黑字（文件） */
@Composable
private fun EntryButton(
    title: String,
    subtitle: String,
    dark: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val bg = if (dark) Color(0xFF0A0A0A) else PaperWhite
    val fg = if (dark) TextPrimary else Color(0xFF111111)
    val sub = if (dark) TextSecondary else Color(0xFF4A463C)
    val borderColor = if (dark) Color.White.copy(alpha = 0.75f) else Color.Transparent

    Column(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(width = 1.2.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = fg,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = sub,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
    }
}

/** 动态折叠预览卡片：两半折纸，随动画缓慢开合（复古线稿） */
@Composable
private fun FoldPreviewCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fold")
    val angle by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "foldAngle"
    )

    Box(
        modifier = modifier
            .size(width = 232.dp, height = 132.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceDark.copy(alpha = 0.6f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(12.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val hingeX = w * 0.58f
            val top = 0f
            val corner = 8.dp.toPx()

            // 右半（平展）—— 桌面模拟：暗蓝灰渐变
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF232B3A),
                        Color(0xFF181F2E),
                        Color(0xFF121722)
                    )
                ),
                topLeft = Offset(hingeX, top),
                size = Size(w - hingeX, h)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                topLeft = Offset(hingeX, top),
                size = Size(w - hingeX, h)
            )

            // 左半：透视压缩 + 暗面
            val fold = angle.coerceIn(0.05f, 1.35f)
            val leftW = hingeX
            val cosAngle = kotlin.math.cos(fold.coerceAtMost(1.2f)).coerceAtLeast(0.18f)
            val path = Path()
            path.moveTo(hingeX, top + corner)
            path.quadraticBezierTo(hingeX, top, hingeX - corner, top)
            path.lineTo(hingeX - leftW * cosAngle, top)
            path.quadraticBezierTo(hingeX - leftW * cosAngle - corner, top, hingeX - leftW * cosAngle, top + corner)
            path.lineTo(hingeX - leftW * cosAngle, h - corner)
            path.quadraticBezierTo(hingeX - leftW * cosAngle, h, hingeX - leftW * cosAngle - corner, h)
            path.lineTo(hingeX - corner, h)
            path.quadraticBezierTo(hingeX, h, hingeX, h - corner)
            path.close()
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF0B0E16),
                        Color(0xFF141D30),
                        AccentB.copy(alpha = 0.28f)
                    )
                )
            )

            // 折叠边缘发丝光
            val edgeX = hingeX - leftW * cosAngle
            drawLine(
                color = AccentB.copy(alpha = 0.8f),
                start = Offset(edgeX, top + 6.dp.toPx()),
                end = Offset(edgeX, h - 6.dp.toPx()),
                strokeWidth = 1.4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 铰链高光
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(hingeX, top + 3.dp.toPx()),
                end = Offset(hingeX, h - 3.dp.toPx()),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 桌面图标点（装饰）
            val iconColors = listOf(AccentB, AccentC, AccentB, AccentC)
            iconColors.forEachIndexed { idx, c ->
                val cx = hingeX + (w - hingeX) * (0.24f + 0.26f * (idx % 2)) + (idx / 2) * 5.dp.toPx()
                val cy = top + h * (0.28f + 0.28f * (idx / 2))
                drawCircle(
                    color = c.copy(alpha = 0.8f),
                    radius = 5.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }

            // 圆角遮罩线
            drawRoundRect(
                color = Color.White.copy(alpha = 0.05f),
                topLeft = Offset(0f, 0f),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
