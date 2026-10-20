package com.daijinduo.github

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * WebView 全屏效果页：内嵌折叠渲染内核（WebGL2 单页，复刻自 solotilt.com）。
 *
 * v0.3 修复：覆盖层改用纯 Android View（原生 SeekBar / TextView），不依赖 Compose
 * 生命周期，回归 v0.1/v0.2 的纯 WebView 管线，彻底消除「选图进入全屏闪退」。
 * 轻点 3 次退出；双击呼出效果调节面板；首次进入提示双击进入方法（可不再提示）。
 */
class WebViewActivity : Activity(), SensorEventListener {

    private companion object {
        const val DOUBLE_TAP_WINDOW = 280L   // 双击判定窗口（毫秒）

        // SeekBar 0..1000 与浮点范围互转
        fun pct(v: Float, min: Float, max: Float): Int =
            ((v - min) / (max - min) * 1000).toInt().coerceIn(0, 1000)
        fun inv(p: Int, min: Float, max: Float): Float =
            min + (p / 1000f) * (max - min)
    }

    private lateinit var webView: WebView
    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null

    // 加速度计最新值（含重力，单位 m/s²，与网页 devicemotion 一致）
    @Volatile private var sx = 0f
    @Volatile private var sy = 0f
    @Volatile private var sz = 0f

    // v0.3：双击呼出调节面板 / 首次进入提示（纯 View 覆盖层）
    private var lastTapTime = 0L
    private var panelVisible = false
    private lateinit var overlayRoot: FrameLayout
    private lateinit var hintBanner: View
    private var imagePushed = false

    private val serifReg: Typeface by lazy {
        ResourcesCompat.getFont(this, R.font.notoserifsc_regular) ?: Typeface.SERIF
    }
    private val serifBold: Typeface by lazy {
        ResourcesCompat.getFont(this, R.font.notoserifsc_bold) ?: Typeface.SERIF
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun text(label: String, sp: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = label
            textSize = sp
            setTextColor(color)
            typeface = if (bold) serifBold else serifReg
        }

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
        // v0.3：双击检测（观察触摸不消费，不干扰 WebView 滚动/点击/3 次退出）
        webView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val now = event.eventTime
                val gap = now - lastTapTime
                lastTapTime = now
                if (gap in 1..DOUBLE_TAP_WINDOW) {
                    lastTapTime = 0L
                    showPanel()
                }
            }
            false
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

        // 覆盖层：调节面板 + 首次提示（纯 View，置于 WebView 之上，默认隐藏不拦截触摸）
        overlayRoot = buildPanel()
        hintBanner = buildHintBanner()

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
                    overlayRoot,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                addView(
                    hintBanner,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP
                    )
                )
            }
        )

        // 直接用 HTML 字符串注入，绕开部分 ROM 对 file:///android_asset 加载的限制
        val html = assets.open("daijinduo/index.html").bufferedReader().use { it.readText() }
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    // ==================== 效果调节面板（纯 View）====================

    private fun buildPanel(): FrameLayout {
        // 全屏遮罩：半透明黑，点击空白关闭
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(158, 0, 0, 0))
            setOnClickListener { hidePanel() }
            visibility = View.GONE
            isClickable = true
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            // 黑色细框圆角卡片
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(0xFF111111.toInt())
                setStroke(dp(1), Color.argb(56, 255, 255, 255)) // 0.22 alpha 白边
            }
        }
        val panelPadding = dp(18)
        panel.setPadding(panelPadding, panelPadding, panelPadding, panelPadding)

        // 标题行：效果调节 + ✕
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = text("效果调节", 13f, Color.WHITE, bold = true).apply {
            setTypeface(typeface, Typeface.BOLD)
            letterSpacing = 0.12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = text("✕", 14f, Color.parseColor("#9E9E9E")).apply {
            setOnClickListener { hidePanel() }
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        titleRow.addView(title)
        titleRow.addView(closeBtn)

        panel.addView(titleRow)
        panel.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(10)) })

        // 三条设置：标签 + SeekBar + 当前值
        addSettingRow(panel,
            label = "模糊强度",
            min = SettingsStore.MIN_BLUR, max = SettingsStore.MAX_BLUR,
            initial = SettingsStore.blurStrength(this),
            fmt = { "%.3f".format(it) },
            apply = { b -> applySettingsToWeb(b, SettingsStore.sensitivity(this), SettingsStore.blurTriggerAngle(this)) })
        addSettingRow(panel,
            label = "陀螺仪灵敏度",
            min = SettingsStore.MIN_SENS, max = SettingsStore.MAX_SENS,
            initial = SettingsStore.sensitivity(this),
            fmt = { "%.1f×".format(it) },
            apply = { s -> applySettingsToWeb(SettingsStore.blurStrength(this), s, SettingsStore.blurTriggerAngle(this)) })
        addSettingRow(panel,
            label = "模糊触发角度",
            min = SettingsStore.MIN_ANGLE, max = SettingsStore.MAX_ANGLE,
            initial = SettingsStore.blurTriggerAngle(this),
            fmt = { "%.0f°".format(it) },
            apply = { a -> applySettingsToWeb(SettingsStore.blurStrength(this), SettingsStore.sensitivity(this), a) })

        val margin = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        margin.leftMargin = dp(26)
        margin.rightMargin = dp(26)
        overlay.addView(panel, margin)
        return overlay
    }

    private fun addSettingRow(
        panel: LinearLayout,
        label: String,
        min: Float,
        max: Float,
        initial: Float,
        fmt: (Float) -> String,
        apply: (Float) -> Unit
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val rowPad = dp(3)
        row.setPadding(0, rowPad, 0, rowPad)

        val lb = text(label, 12f, Color.parseColor("#9E9E9E")).apply {
            layoutParams = LinearLayout.LayoutParams(dp(84), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val value = text(fmt(initial), 11.5f, Color.WHITE).apply {
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(dp(52), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val seek = SeekBar(this).apply {
            this.max = 1000
            progress = pct(initial, min, max)
            progressTintList = ColorStateList.valueOf(Color.WHITE)
            progressBackgroundTintList = ColorStateList.valueOf(Color.argb(51, 255, 255, 255))
            thumbTintList = ColorStateList.valueOf(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                0, dp(34), 1f
            )
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    val v = inv(p, min, max)
                    value.text = fmt(v)
                    if (fromUser) apply(v)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }

        row.addView(lb)
        row.addView(seek)
        row.addView(value)
        panel.addView(row)
    }

    private fun showPanel() {
        panelVisible = true
        overlayRoot.visibility = View.VISIBLE
        hintBanner.visibility = View.GONE
    }

    private fun hidePanel() {
        panelVisible = false
        overlayRoot.visibility = View.GONE
    }

    // ==================== 首次进入提示横幅（纯 View）====================

    private fun buildHintBanner(): View {
        val holder = FrameLayout(this)
        holder.visibility = View.GONE

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(0xE6181818.toInt())
                setStroke(dp(1), Color.argb(46, 255, 255, 255)) // 0.18 alpha 白边
            }
        }
        val pad = dp(14)
        card.setPadding(pad, dp(12), pad, dp(12))

        card.addView(text("双击屏幕呼出效果调节", 14f, Color.WHITE, bold = true))
        card.addView(text("模糊强度 · 陀螺仪灵敏度 · 模糊触发角度", 11f, Color.parseColor("#9E9E9E")).apply {
            letterSpacing = 0.02f
            setPadding(0, dp(4), 0, 0)
        })

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        val cb = CheckBox(this).apply {
            text = "不再提示"
            setTextColor(Color.parseColor("#9E9E9E"))
            textSize = 12f
            typeface = serifReg
            buttonTintList = ColorStateList.valueOf(Color.WHITE)
        }
        val ok = text("知道了", 12.5f, Color.WHITE, bold = true).apply {
            setOnClickListener {
                SettingsStore.setHintDismissed(this@WebViewActivity, cb.isChecked)
                hintBanner.visibility = View.GONE
            }
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        row.addView(cb, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(ok)
        card.addView(row)

        val margin = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        )
        margin.leftMargin = dp(18)
        margin.rightMargin = dp(18)
        margin.topMargin = dp(20)
        holder.addView(card, margin)
        return holder
    }

    private fun maybeShowHint() {
        if (!SettingsStore.hintDismissed(this) && !panelVisible) {
            hintBanner.visibility = View.VISIBLE
        }
    }

    // ==================== 设置注入与图片 ====================

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
        if (panelVisible) {
            hidePanel()
            return
        }
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
