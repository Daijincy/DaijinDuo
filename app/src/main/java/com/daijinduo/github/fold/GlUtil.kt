package com.daijinduo.github.fold

import android.opengl.GLES30
import android.util.Log

/**
 * GL 小工具：编译 shader、链接 program、创建全屏四边形 VAO/VBO。
 * 编译/链接失败时返回 0 并打日志，绝不抛异常（GL 线程抛异常会导致渲染线程死亡 → 黑屏）。
 */
object GlUtil {
    private const val TAG = "DaijinDuoGL"

    fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        if (shader == 0) return 0
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            Log.e(TAG, "shader compile failed: $log\n$source")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun linkProgram(vertexSource: String, fragmentSource: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        if (vs == 0 || fs == 0) {
            if (vs != 0) GLES30.glDeleteShader(vs)
            if (fs != 0) GLES30.glDeleteShader(fs)
            return 0
        }
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vs)
        GLES30.glAttachShader(program, fs)
        GLES30.glLinkProgram(program)
        val status = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(program)
            Log.e(TAG, "program link failed: $log")
            GLES30.glDeleteProgram(program)
            GLES30.glDeleteShader(vs)
            GLES30.glDeleteShader(fs)
            return 0
        }
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        return program
    }

    /** 全屏四边形（两个三角形，6 顶点，position + uv 交错） */
    val FULLSCREEN_QUAD = floatArrayOf(
        // x, y, u, v
        -1f, -1f, 0f, 0f,
         1f, -1f, 1f, 0f,
        -1f,  1f, 0f, 1f,
        -1f,  1f, 0f, 1f,
         1f, -1f, 1f, 0f,
         1f,  1f, 1f, 1f
    )

    fun createQuadVao(): Int {
        val vao = IntArray(1)
        val vbo = IntArray(1)
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(1, vbo, 0)
        GLES30.glBindVertexArray(vao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, FULLSCREEN_QUAD.size * 4, java.nio.ByteBuffer
            .allocateDirect(FULLSCREEN_QUAD.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(FULLSCREEN_QUAD)
            .apply { position(0) }, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        GLES30.glBindVertexArray(0)
        return vao[0]
    }
}
