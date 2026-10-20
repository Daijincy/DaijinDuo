package com.daijinduo.github

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.daijinduo.github.ui.DoubleTapHintBanner
import com.daijinduo.github.ui.FoldAdjustOverlay
import com.daijinduo.github.ui.SoloTheme
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * WebView 全屏效果页：内嵌折叠渲染内核（WebGL2 单页，复刻自 solotilt.com），
 * 不依赖原生 GLES 3.0 管线，兼容性更好。轻点 3 次退出。
 */
class WebViewActivity : Activity(), SensorEventListener {

    private lateinit var webView: WebView

    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null

    // 加速度计最新值（含重力，单位 m/s²，与网页 devicemotion 一致）
    @Volatile private var sx = 0f
    @Volatile private var sy = 0f
    @Volatile private var sz = 0f

    // v0.3：双击呼出效果调节面板 / 首次进入提示
    private var showPanel by mutableStateOf(false)
    private var showHint by mutableStateOf(false)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 沉浸式全屏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        webView = WebView(this)
        webView.setBackgroundColor(0xFF000000.toInt())
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
        }
        // v0.3：双击检测（在 WebView 触摸分发前观察，不消费，不干扰拖拽/点击）
        val doubleTapDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    showPanel = true
                    showHint = false
                    return true
                }
            }
        )
        webView.setOnTouchListener { _, event ->
            doubleTapDetector.onTouchEvent(event)
            false // 不消费，让 WebView 正常处理滚动/点击/3 次退出
        }
        // 页面就绪（JS 初始化完成）后回调；同时 onPageFinished 兜底
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pushSettings()
                pushImageIfReady()
                maybeShowHint()
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                runOnUiThread {
                    Toast.makeText(
                        this@WebViewActivity,
                        "页面加载失败: ${error?.description ?: "unknown"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        webView.webChromeClient = WebChromeClient()
        // JS 桥：页面就绪通知 + 错误上报 + 传感器拉取 + 轻点 3 次退出
        webView.addJavascriptInterface(
            object {
                @android.webkit.JavascriptInterface
                fun onReady() {
                    runOnUiThread {
                        pushSettings()
                        pushImageIfReady()
                        maybeShowHint()
                    }
                }

                @android.webkit.JavascriptInterface
                fun onError(msg: String) {
                    runOnUiThread {
                        Toast.makeText(this@WebViewActivity, "内核错误: $msg", Toast.LENGTH_LONG).show()
                    }
                }

                @android.webkit.JavascriptInterface
                fun sensor(): String {
                    return "$sx,$sy,$sz"
                }

                @android.webkit.JavascriptInterface
                fun finish() {
                    runOnUiThread { this@WebViewActivity.finish() }
                }
            },
            "AndroidBridge"
        )

        // v0.3：效果调节面板 + 首次提示（Compose 覆盖层，全屏、平时不拦截触摸）
        val overlay = ComposeView(this)
        overlay.setContent {
            SoloTheme {
                Box(Modifier.fillMaxSize()) {
                    if (showPanel) {
                        FoldAdjustOverlay(
                            initialBlur = SettingsStore.blurStrength(this@WebViewActivity),
                            initialSens = SettingsStore.sensitivity(this@WebViewActivity),
                            initialAngle = SettingsStore.blurTriggerAngle(this@WebViewActivity),
                            applySettings = { b, s, a -> applySettingsToWeb(b, s, a) },
                            onClose = { showPanel = false }
                        )
                    } else if (showHint) {
                        DoubleTapHintBanner(
                            onDismiss = { dontShowAgain ->
                                if (dontShowAgain) {
                                    SettingsStore.setHintDismissed(this@WebViewActivity, true)
                                }
                                showHint = false
                            },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }

        setContentView(
            FrameLayout(this).apply {
                addView(
                    webView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                addView(
                    overlay,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
        )

        // 直接用 HTML 字符串注入，绕开部分 ROM 对 file:///android_asset 加载的限制
        val html = assets.open("daijinduo/index.html").bufferedReader().use { it.readText() }
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    private var imagePushed = false

    /** 注入用户设置到渲染内核（模糊强度 / 灵敏度 / 触发角度） */
    private fun pushSettings() {
        val (blur, sens, angle) = SettingsStore.snapshot(this)
        webView.evaluateJavascript(
            "window.__setConfig && window.__setConfig({strength:$blur, sensitivity:$sens, fullAngle:$angle})",
            null
        )
    }

    /** v0.3：面板滑块实时应用——写持久化 + 注入内核 */
    private fun applySettingsToWeb(blur: Float, sens: Float, angle: Float) {
        SettingsStore.setBlurStrength(this, blur)
        SettingsStore.setSensitivity(this, sens)
        SettingsStore.setBlurTriggerAngle(this, angle)
        webView.evaluateJavascript(
            "window.__setConfig && window.__setConfig({strength:$blur, sensitivity:$sens, fullAngle:$angle})",
            null
        )
    }

    /** v0.3：首次进入提示（未勾选「不再提示」时显示） */
    private fun maybeShowHint() {
        if (!SettingsStore.hintDismissed(this)) {
            showHint = true
        }
    }

    /** 页面就绪后传图（幂等，避免重复注入） */
    private fun pushImageIfReady() {
        if (imagePushed) return
        val uri = intent.data ?: return
        imagePushed = true
        loadImage(uri)
    }

    /** 解码图片 → JPEG base64 → 注入页面（最长边 ≤2048，控制体积） */
    private fun loadImage(uri: Uri) {
        Thread {
            try {
                val bitmap = decodeSampled(uri)
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, baos)
                val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                runOnUiThread {
                    webView.evaluateJavascript(
                        "window.__setImage && window.__setImage('data:image/jpeg;base64,$b64')",
                        null
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "图片加载失败", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun decodeSampled(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        val longSide = max(bounds.outWidth, bounds.outHeight)
        while (longSide / sample > 2048) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IllegalStateException("decode failed")
        return applyExifRotation(uri, raw)
    }

    /** BitmapFactory 不解 EXIF 方向，按 EXIF 旋转 */
    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val orientation = contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }
            val out = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (out != bitmap) bitmap.recycle()
            out
        } catch (_: Exception) {
            bitmap
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onResume() {
        super.onResume()
        accelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    // ---- SensorEventListener：更新加速度计共享值（JS 桥拉取）----
    override fun onSensorChanged(event: SensorEvent) {
        sx = event.values[0]
        sy = event.values[1]
        sz = event.values[2]
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }
}
