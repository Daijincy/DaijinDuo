package com.daijinduo.github

import android.content.Context
import android.content.SharedPreferences

/**
 * 效果设置（SharedPreferences 持久化）。
 *
 * 三个可调项，均注入 WebView 渲染内核（window.__setConfig）：
 *  - 模糊强度 blurStrength   ：折叠处最大模糊占图像宽度的比例（0..0.08）
 *  - 陀螺仪灵敏度 sensitivity ：折叠对倾斜角的增益（0.5x..2.0x，1.0=原版）
 *  - 模糊触发角度 blurTriggerAngle：达到全模糊的折叠物理角度（30..120°，90=原版）
 */
object SettingsStore {
    private const val PREFS = "daijinduo_settings"
    private const val K_BLUR = "blur_strength"
    private const val K_SENS = "sensitivity"
    private const val K_ANGLE = "blur_trigger_angle"

    const val MIN_BLUR = 0f
    const val MAX_BLUR = 0.08f
    const val DEFAULT_BLUR = 0.0315f

    const val MIN_SENS = 0.5f
    const val MAX_SENS = 2.0f
    const val DEFAULT_SENS = 1.0f

    const val MIN_ANGLE = 30f
    const val MAX_ANGLE = 120f
    const val DEFAULT_ANGLE = 90f

    fun blurStrength(c: Context): Float = prefs(c).getFloat(K_BLUR, DEFAULT_BLUR)
    fun sensitivity(c: Context): Float = prefs(c).getFloat(K_SENS, DEFAULT_SENS)
    fun blurTriggerAngle(c: Context): Float = prefs(c).getFloat(K_ANGLE, DEFAULT_ANGLE)

    fun setBlurStrength(c: Context, v: Float) =
        prefs(c).edit().putFloat(K_BLUR, v.coerceIn(MIN_BLUR, MAX_BLUR)).apply()

    fun setSensitivity(c: Context, v: Float) =
        prefs(c).edit().putFloat(K_SENS, v.coerceIn(MIN_SENS, MAX_SENS)).apply()

    fun setBlurTriggerAngle(c: Context, v: Float) =
        prefs(c).edit().putFloat(K_ANGLE, v.coerceIn(MIN_ANGLE, MAX_ANGLE)).apply()

    /** 一次性取回全部设置，供注入使用 */
    fun snapshot(c: Context): Triple<Float, Float, Float> =
        Triple(blurStrength(c), sensitivity(c), blurTriggerAngle(c))

    private fun prefs(c: Context): SharedPreferences =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
