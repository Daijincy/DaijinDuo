package com.daijinduo.github.fold

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.min

/**
 * 陀螺仪/重力运动追踪。
 * 严格复刻 solotilt.com 的映射（ref/motion_sampler.js）：
 *   重力矢量 → 与"屏幕朝上"的夹角 s（0..180）→ 水平投影 u → angle ∈ [-180,180]
 *   → 指数平滑 → turn = |angle|/180，phase = angle >= 0
 *
 * 回调在调用线程（建议 GLSurfaceView 渲染线程或主线程）触发。
 */
class MotionTracker(
    context: Context,
    private val onReading: (turn: Float, phase: Float) -> Unit,
    private val onActiveChanged: (active: Boolean) -> Unit
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val emaAlpha = 0.12f
    private var emaX = 0f
    private var emaY = 0f
    private var emaZ = 0f

    private var smoothedAngle = 0f
    private var lastTimestamp = 0L
    private var active = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val gx: Float
            val gy: Float
            val gz: Float
            if (event.sensor.type == Sensor.TYPE_GRAVITY) {
                gx = event.values[0]
                gy = event.values[1]
                gz = event.values[2]
            } else {
                // 无 TYPE_GRAVITY 时用加速度计 EMA 低通近似重力
                emaX = emaAlpha * event.values[0] + (1f - emaAlpha) * emaX
                emaY = emaAlpha * event.values[1] + (1f - emaAlpha) * emaY
                emaZ = emaAlpha * event.values[2] + (1f - emaAlpha) * emaZ
                gx = emaX
                gy = emaY
                gz = emaZ
            }
            compute(event.timestamp, gx, gy, gz)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun compute(timestamp: Long, gx: Float, gy: Float, gz: Float) {
        // 参考实现 Android 端符号翻转 sign = -1
        val r = -gx
        val i = -gy
        val a = -gz
        val mag = hypot(hypot(r, i), a)
        if (mag < 0.5f) return // 读数不稳定，忽略

        val o = hypot(r, i)
        // 与"屏幕朝上"的夹角：0 = 平放朝上，90 = 竖直，180 = 屏幕朝下
        val s = Math.toDegrees(atan2(o.toDouble(), -a.toDouble())).toFloat()

        val angle: Float = when {
            s > 120.00001f -> 0f          // 朝向地面 → 折叠归零
            o < 1e-5f -> 0f               // 完全水平 → 不折叠
            else -> {
                val u = min(120f, s) * r / o
                (-2f * u).coerceIn(-180f, 180f)
            }
        }

        // 指数平滑：h += (y - h) * (1 - exp(-dt * 16))
        if (lastTimestamp != 0L) {
            val dt = ((timestamp - lastTimestamp) / 1_000_000_000.0f).coerceIn(0f, 0.1f)
            smoothedAngle += (angle - smoothedAngle) * (1f - exp(-dt * 16f))
        } else {
            smoothedAngle = angle
        }
        lastTimestamp = timestamp

        val turn = min(1f, abs(smoothedAngle) / 180f)
        val phase = if (smoothedAngle >= 0f) 1f else 0f

        if (!active) {
            active = true
            onActiveChanged(true)
        }
        onReading(turn, phase)
    }

    fun start() {
        if (!active) {
            // 先挂 accel 低通兜底，有 GRAVITY 时优先
            if (gravitySensor != null) {
                sensorManager.registerListener(listener, gravitySensor, SensorManager.SENSOR_DELAY_GAME)
            } else if (accelSensor != null) {
                sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
        active = false
        lastTimestamp = 0L
        smoothedAngle = 0f
        onActiveChanged(false)
    }

    fun available(): Boolean = gravitySensor != null || accelSensor != null
}
