package com.daijinduo.github.fold

import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 折叠效果渲染器：上传截图 → 构建 Gaussian 金字塔 → 按 turn/phase 绘制折叠。
 * turn/phase 由 [MotionTracker]（陀螺仪）或演示动画驱动。
 */
class FoldRenderer : GLSurfaceView.Renderer {

    /** UI 线程设置，GL 线程消费 */
    @Volatile
    var pendingBitmap: Bitmap? = null

    @Volatile
    var turn: Float = 0f

    @Volatile
    var phase: Float = 0f

    var config: FoldShaders.FoldConfig = FoldShaders.FoldConfig()

    private var foldProgram = 0
    private var pyramid: GaussianPyramid? = null
    private var quadVao = 0
    private var imageTexture = 0
    private var textureReady = false
    private var viewW = 1
    private var viewH = 1

    // uniform 位置缓存
    private var uImage = -1
    private var uImageSize = -1
    private var uCoverScale = -1
    private var uAspect = -1
    private var uTurn = -1
    private var uPhase = -1
    private var uBlurAngle = -1
    private var uBlurSpread = -1
    private var uBlackGradient = -1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        foldProgram = GlUtil.linkProgram(FoldShaders.VERT, FoldShaders.FOLD_FRAG)
        quadVao = GlUtil.createQuadVao()
        pyramid = GaussianPyramid().also { it.init() }
        uImage = GLES30.glGetUniformLocation(foldProgram, "u_image")
        uImageSize = GLES30.glGetUniformLocation(foldProgram, "u_imageSize")
        uCoverScale = GLES30.glGetUniformLocation(foldProgram, "u_coverScale")
        uAspect = GLES30.glGetUniformLocation(foldProgram, "u_aspect")
        uTurn = GLES30.glGetUniformLocation(foldProgram, "u_turn")
        uPhase = GLES30.glGetUniformLocation(foldProgram, "u_phase")
        uBlurAngle = GLES30.glGetUniformLocation(foldProgram, "u_blurAngle")
        uBlurSpread = GLES30.glGetUniformLocation(foldProgram, "u_blurSpread")
        uBlackGradient = GLES30.glGetUniformLocation(foldProgram, "u_blackGradient")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        viewW = width
        viewH = height
    }

    override fun onDrawFrame(gl: GL10?) {
        pendingBitmap?.let {
            pendingBitmap = null
            upload(it)
        }
        if (textureReady) {
            draw()
        } else {
            GLES30.glClearColor(0.02f, 0.02f, 0.03f, 1f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        }
    }

    private fun upload(src: Bitmap) {
        val target = centerCropToView(src)
        // GL 纹理原点在左下，位图原点在左上：垂直翻转
        val flipped = Bitmap.createBitmap(target.width, target.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(flipped)
        canvas.translate(0f, target.height.toFloat())
        canvas.scale(1f, -1f)
        canvas.drawBitmap(target, 0f, 0f, null)

        val tex = pyramid?.build(flipped) ?: 0
        if (tex != 0) {
            if (imageTexture != 0) {
                val old = intArrayOf(imageTexture)
                GLES30.glDeleteTextures(1, old, 0)
            }
            imageTexture = tex
            textureReady = true
        }
        target.recycle()
        flipped.recycle()
    }

    /** 居中裁切到屏幕宽高比，长边不超过 4096（纹理上限内） */
    private fun centerCropToView(src: Bitmap): Bitmap {
        val longSide = maxOf(viewW, viewH)
        val maxDim = 4096
        val viewScale = if (longSide > maxDim) maxDim.toFloat() / longSide else 1f
        val tw = (viewW * viewScale).toInt().coerceAtLeast(1)
        val th = (viewH * viewScale).toInt().coerceAtLeast(1)
        val scale = maxOf(tw.toFloat() / src.width, th.toFloat() / src.height)
        val sw = (tw / scale).toInt().coerceAtMost(src.width)
        val sh = (th / scale).toInt().coerceAtMost(src.height)
        val sx = (src.width - sw) / 2
        val sy = (src.height - sh) / 2
        val cropped = Bitmap.createBitmap(src, sx, sy, sw, sh, null, false)
        if (cropped.width != tw || cropped.height != th) {
            val scaled = Bitmap.createScaledBitmap(cropped, tw, th, true)
            if (scaled != cropped) cropped.recycle()
            return scaled
        }
        return cropped
    }

    private fun draw() {
        GLES30.glViewport(0, 0, viewW, viewH)
        GLES30.glUseProgram(foldProgram)
        GLES30.glBindVertexArray(quadVao)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, imageTexture)

        GLES30.glUniform1i(uImage, 0)
        GLES30.glUniform2f(uImageSize, viewW.toFloat(), viewH.toFloat())
        GLES30.glUniform2f(uCoverScale, 1f, 1f)
        GLES30.glUniform1f(uAspect, viewW.toFloat() / viewH.toFloat())
        GLES30.glUniform1f(uTurn, turn.coerceIn(0f, 1f))
        GLES30.glUniform1f(uPhase, if (phase >= 0.5f) 1f else 0f)
        GLES30.glUniform3f(
            uBlurAngle,
            config.strength, config.fullAngle, config.anglePower
        )
        GLES30.glUniform3f(
            uBlurSpread,
            config.fullDistance, config.distancePower, config.edgeBlur
        )
        GLES30.glUniform3f(
            uBlackGradient,
            config.gradientOpacity, config.gradientStart, config.gradientEnd
        )

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)
        GLES30.glBindVertexArray(0)
    }
}
