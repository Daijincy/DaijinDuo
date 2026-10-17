package com.daijinduo.github

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.daijinduo.github.ui.BgDark
import com.daijinduo.github.ui.GalleryScreen
import com.daijinduo.github.ui.HomeScreen
import com.daijinduo.github.ui.LiquidGlassBar
import com.daijinduo.github.ui.ShowPage
import com.daijinduo.github.ui.SoloTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoloTheme {
                DuoApp()
            }
        }
    }
}

private enum class Screen { HOME, GALLERY }

/**
 * 应用骨架：玻璃悬浮底栏（Duo / Show 双页）。
 * Duo = 主要功能（选图 → 折叠效果）；Show = 敬请期待。
 */
@Composable
private fun DuoApp() {
    var tab by remember { mutableIntStateOf(0) } // 0=Duo 1=Show
    val hazeState = remember { HazeState() }

    Box(Modifier.fillMaxSize().background(BgDark)) {
        // 页面内容：作为玻璃模糊的底层来源
        Box(
            Modifier
                .hazeSource(hazeState)
                .fillMaxSize()
                .padding(bottom = 92.dp)
        ) {
            when (tab) {
                0 -> DuoPage()
                else -> ShowPage()
            }
        }
        // 玻璃悬浮底栏
        LiquidGlassBar(
            selected = tab,
            onSelect = { tab = it },
            hazeState = hazeState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** Duo 页：主页（选图入口）→ 相册 → 全屏折叠 */
@Composable
private fun DuoPage() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current

    val systemPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { startFoldActivity(context, it) }
    }

    when (screen) {
        Screen.HOME -> HomeScreen(
            onPickFromGallery = { screen = Screen.GALLERY },
            onSystemPicker = {
                systemPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )

        Screen.GALLERY -> GalleryScreen(
            onBack = { screen = Screen.HOME },
            onPick = { startFoldActivity(context, it) },
            onSystemPicker = {
                systemPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }
}

private fun startFoldActivity(context: Context, uri: Uri) {
    context.startActivity(
        Intent(context, WebViewActivity::class.java).setData(uri)
    )
}
