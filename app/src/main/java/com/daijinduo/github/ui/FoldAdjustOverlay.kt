package com.daijinduo.github.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daijinduo.github.SettingsStore

/**
 * 全屏效果页的「效果调节」面板（v0.3）。
 *
 * 双击屏幕呼出：黑色半透明遮罩 + 中央三滑块卡（模糊强度 / 陀螺仪灵敏度 / 模糊触发角度）。
 * 滑块实时应用（写入 SharedPreferences 并注入渲染内核），点击遮罩关闭。
 */
@Composable
fun FoldAdjustOverlay(
    initialBlur: Float,
    initialSens: Float,
    initialAngle: Float,
    applySettings: (blur: Float, sens: Float, angle: Float) -> Unit,
    onClose: () -> Unit
) {
    var blur by remember { mutableFloatStateOf(initialBlur) }
    var sens by remember { mutableFloatStateOf(initialSens) }
    var angle by remember { mutableFloatStateOf(initialAngle) }

    // 全屏黑色遮罩：拦截触摸，点击关闭
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        // 中央面板：复古简约黑底细框
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF111111))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "效果调节",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "✕",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onClose).padding(6.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            OverlayRow(
                label = "模糊强度",
                valueText = "%.3f".format(blur),
                value = blur,
                min = SettingsStore.MIN_BLUR,
                max = SettingsStore.MAX_BLUR,
                onChange = { blur = it; applySettings(blur, sens, angle) }
            )
            OverlayRow(
                label = "陀螺仪灵敏度",
                valueText = "%.1f×".format(sens),
                value = sens,
                min = SettingsStore.MIN_SENS,
                max = SettingsStore.MAX_SENS,
                onChange = { sens = it; applySettings(blur, sens, angle) }
            )
            OverlayRow(
                label = "模糊触发角度",
                valueText = "%.0f°".format(angle),
                value = angle,
                min = SettingsStore.MIN_ANGLE,
                max = SettingsStore.MAX_ANGLE,
                onChange = { angle = it; applySettings(blur, sens, angle) }
            )
        }
    }
}

/** 面板内单条设置：标签 + 滑块 + 当前值（黑白极简） */
@Composable
private fun OverlayRow(
    label: String,
    valueText: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.width(84.dp)
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            modifier = Modifier.weight(1f).height(34.dp),
            colors = SliderDefaults.colors(
                thumbColor = AccentB,
                activeTrackColor = Color.White.copy(alpha = 0.85f),
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
        Text(
            text = valueText,
            color = TextPrimary,
            fontSize = 11.5.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(52.dp)
        )
    }
}

/**
 * 首次进入全屏时的提示横幅（v0.3）：
 * 告知「双击屏幕呼出效果调节」，可勾选「不再提示」后关闭，下次不再显示。
 */
@Composable
fun DoubleTapHintBanner(
    onDismiss: (dontShowAgain: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var dontShow by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF181818).copy(alpha = 0.9f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "双击屏幕呼出效果调节",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "模糊强度 · 陀螺仪灵敏度 · 模糊触发角度",
                color = TextSecondary,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { dontShow = !dontShow }
                ) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, if (dontShow) AccentB else Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .background(if (dontShow) AccentB else Color.Transparent)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(text = "不再提示", color = TextSecondary, fontSize = 12.sp)
                }
                Text(
                    text = "知道了",
                    color = AccentB,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onDismiss(dontShow) }.padding(4.dp)
                )
            }
        }
    }
}
