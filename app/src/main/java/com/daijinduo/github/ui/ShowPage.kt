package com.daijinduo.github.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Show 页：敬请期待 */
@Composable
fun ShowPage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "☆",
                color = AccentB.copy(alpha = 0.9f),
                fontSize = 34.sp,
                letterSpacing = 0.sp
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "S H O W",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "敬 请 期 待",
                color = TextSecondary,
                fontSize = 14.sp,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "—— 更多折叠玩法即将到来 ——",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
