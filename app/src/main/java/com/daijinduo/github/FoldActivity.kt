package com.daijinduo.github

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.daijinduo.github.fold.FoldRenderer
import com.daijinduo.github.fold.MotionTracker
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 全屏效果页：GLSurfaceView 渲染折叠动画，陀螺仪驱动。
 * 轻点 3 次（800ms 窗口内）退出；无传感器读数时自动演示。
 */
class FoldActivity : Activity() {

    private lateinit var glView: GLSurfaceView
    private val renderer = FoldRenderer()
    private var motion: MotionTracker? = null

    private var demoActive = true
    private var demoStart = 0L
    private val demoHandler = Handler(Looper.getMainLooper())

    private var lastTapTime = 0L
    private var tapCount = 0

    private val demoRunnable = object : Runnable {
        override fun run() {
            if (demoActive) {
                val t = (SystemClock.elapsedRealtime() - demoStart) / 1000f
                val cycle = t % 6f
                val phase = if (cycle < 3f) 0f else 1f
                val tt = cycle % 3f
                renderer.turn = sin(PI * tt / 3f).toFloat().coerceIn(0f, 1f)
                renderer.phase = phase
            }
            demoHandler.postDelayed(this, 16)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterImmersiveFullscreen()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        // 轻点提示（短暂显示后淡出）
        val hint = TextView(this).apply {
            text = getString(R.string.tap_hint)
            setTextColor(0xCCF4F6FB.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
            alpha = 0f
            setPadding(0, 0, 0, 0)
        }

        // 全屏透明点击层：3 次点击退出
        val tapOverlay = View(this).apply {
            isClickable = true
            setOnClickListener {
                val now = SystemClock.elapsedRealtime()
                if (now - lastTapTime > 800L) tapCount = 0
                lastTapTime = now
                tapCount++
                if (tapCount >= 3) finish()
            }
        }

        val root = FrameLayout(this).apply {
            addView(glView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(
                tapOverlay,
                FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            )
            addView(
                hint,
                FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                    gravity = Gravity.CENTER
                    bottomMargin = 96
                }
            )
        }
        setContentView(root)

        hint.animate().alpha(1f).setDuration(500).withEndAction {
            hint.animate().alpha(0f).setStartDelay(2200).setDuration(700)
        }

        // 传感器
        val tracker = MotionTracker(
            context = this,
            onReading = { turn, phase ->
                renderer.turn = turn
                renderer.phase = phase
            },
            onActiveChanged = { active ->
                demoActive = !active
                if (active) {
                    renderer.turn = 0f
                    renderer.phase = 0f
                }
            }
        )
        motion = tracker
        if (!tracker.available()) {
            Toast.makeText(this, R.string.sensor_unavailable, Toast.LENGTH_LONG).show()
        }

        intent.data?.let(::loadImage)
    }

    private fun enterImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun loadImage(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val bitmap = decodeSampled(uri)
            runOnUiThread {
                if (bitmap != null) {
                    renderer.pendingBitmap = bitmap
                } else {
                    Toast.makeText(
                        this@FoldActivity,
                        R.string.image_load_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun decodeSampled(uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            val longSide = max(bounds.outWidth, bounds.outHeight)
            while (longSide / sample > MAX_TEXTURE_DIM) sample *= 2
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val raw = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null
            applyExifRotation(uri, raw)
        } catch (_: Exception) {
            null
        }
    }

    /** BitmapFactory 不解 EXIF 方向，这里按 EXIF 旋转位图（竖拍/横拍照片方向正确） */
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

    override fun onResume() {
        super.onResume()
        glView.onResume()
        demoStart = SystemClock.elapsedRealtime()
        demoHandler.post(demoRunnable)
        motion?.start()
    }

    override fun onPause() {
        super.onPause()
        motion?.stop()
        demoHandler.removeCallbacks(demoRunnable)
        glView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        demoHandler.removeCallbacksAndMessages(null)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    companion object {
        private const val MAX_TEXTURE_DIM = 4096
        private const val MATCH_PARENT = FrameLayout.LayoutParams.MATCH_PARENT
    }
}
