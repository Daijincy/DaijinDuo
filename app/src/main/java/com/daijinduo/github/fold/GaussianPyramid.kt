package com.daijinduo.github.fold

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils

/**
 * Gaussian 模糊金字塔：
 * level 0 = 原图；level k+1 = 上一级经可分离 5-tap 高斯模糊 + 半分辨率降采样。
 * 渲染时用 textureLod(uv, log2(sigma)) 实现与折叠深度成正比的景深模糊。
 */
class GaussianPyramid {
    private var gaussProgram = 0
    private var quadVao = 0
    private var fboH = 0
    private var fboV = 0
    private var texH = 0
    private var maxLevels = 0

    fun init() {
        gaussProgram = GlUtil.linkProgram(FoldShaders.VERT, FoldShaders.GAUSS5_FRAG)
        quadVao = GlUtil.createQuadVao()
        val fbos = IntArray(2)
        GLES30.glGenFramebuffers(2, fbos, 0)
        fboH = fbos[0]
        fboV = fbos[1]
        val texs = IntArray(1)
        GLES30.glGenTextures(1, texs, 0)
        texH = texs[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texH)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }

    fun dispose() {
        GLES30.glDeleteProgram(gaussProgram)
        val f = intArrayOf(fboH, fboV)
        GLES30.glDeleteFramebuffers(2, f, 0)
        val t = intArrayOf(texH)
        GLES30.glDeleteTextures(1, t, 0)
    }

    /**
     * 构建纹理金字塔并返回纹理 id。
     * @param bitmap 已居中裁切到目标宽高比的 RGBA 位图（调用方负责翻转）
     */
    fun build(bitmap: Bitmap): Int {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return 0

        val tex = IntArray(1)
        GLES30.glGenTextures(1, tex, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        // GLUtils 内部正确处理 ARGB_8888 的字节序（勿用手动 ByteBuffer，否则 R/B 通道颠倒）
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)

        // 逐级模糊 + 降采样（水平 → 垂直），与网页端 ce() 结构一致：
        // 每级采样上一级 level(r-1) 的结果；mip 链必须延伸到 1×1，
        // 否则 GLES 认为纹理不完整，MIN_FILTER=LINEAR_MIPMAP_LINEAR 时采样直接返回黑色（黑屏根因）。
        // 网页端：n = floor(log2(max(e,t)))，r = 1..n。
        var lw = w
        var lh = h
        var level = 1
        val maxLvl = 31 - Integer.numberOfLeadingZeros(maxOf(w, h))
        GLES30.glUseProgram(gaussProgram)
        GLES30.glBindVertexArray(quadVao)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        while (level <= maxLvl && level < MAX_LEVELS) {
            // 与网页端 ce() 一致：每级尺寸 = 上一级右移 1 位（e>>r）
            val nw = maxOf(1, lw shr 1)
            val nh = maxOf(1, lh shr 1)

            // 分配目标 mip level 存储
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, level, GLES30.GL_RGBA8, nw, nh, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)

            // 分配中间纹理 texH（半尺寸 nw×nh，网页端 k）。必须分配，否则 FBO 不完整
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texH)
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, nw, nh, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

            // pass 1：水平模糊 + 半采样，采样 tex[0] 的上一级 level(level-1) → texH
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0])
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboH)
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, texH, 0)
            GLES30.glViewport(0, 0, nw, nh)
            GLES30.glUniform1i(GLES30.glGetUniformLocation(gaussProgram, "u_source"), 0)
            GLES30.glUniform2f(GLES30.glGetUniformLocation(gaussProgram, "u_step"), 1.3846153846f / lw, 0f)
            GLES30.glUniform1f(GLES30.glGetUniformLocation(gaussProgram, "u_level"), (level - 1).toFloat())
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)

            // pass 2：垂直模糊 + 半采样，采样 texH（单级纹理，显式 lod=0）→ tex level k
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texH)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboV)
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, tex[0], level)
            GLES30.glViewport(0, 0, nw, nh)
            GLES30.glUniform2f(GLES30.glGetUniformLocation(gaussProgram, "u_step"), 0f, 1.3846153846f / lh)
            GLES30.glUniform1f(GLES30.glGetUniformLocation(gaussProgram, "u_level"), 0f)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)

            lw = nw
            lh = nh
            level++
        }
        maxLevels = level

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glBindVertexArray(0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        GLES30.glUseProgram(0)
        return tex[0]
    }

    companion object {
        const val MAX_LEVELS = 16
    }
}
