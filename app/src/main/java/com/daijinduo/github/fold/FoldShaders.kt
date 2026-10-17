package com.daijinduo.github.fold

/**
 * 从 solotilt.com 逆向移植的 GLSL shader（GLES 3.0）。
 * 参考：ref/fold.frag.glsl、ref/fold.vert.glsl、ref/gauss5.frag.glsl、ref/NOTES.md
 */
object FoldShaders {

    /** 全屏四边形顶点着色器（折叠渲染与高斯模糊共用） */
    const val VERT = """#version 300 es
precision highp float;
layout(location = 0) in vec2 a_position;
layout(location = 1) in vec2 a_uv;
out vec2 v_uv;
void main() {
  gl_Position = vec4(a_position, 0.0, 1.0);
  v_uv = a_uv;
}
"""

    /**
     * 折叠片段着色器：把每个屏幕像素反投影到"静止图像平面"，
     * 模拟 iPhone Duo 沿垂直铰链的 90° 折叠 + 景深模糊 + 黑边渐变 + 玻璃反光。
     */
    const val FOLD_FRAG = """#version 300 es
precision highp float;
uniform sampler2D u_image;
uniform vec2 u_imageSize;
uniform vec2 u_coverScale;
uniform float u_aspect;
uniform float u_turn;
uniform float u_phase;
uniform vec3 u_blurAngle;
uniform vec3 u_blurSpread;
uniform vec3 u_blackGradient;
in vec2 v_uv;
out vec4 outColor;

vec2 imageCoordinates(vec2 uv) {
  return vec2(0.5) + (uv - vec2(0.5)) * u_coverScale;
}

vec3 sceneSample(vec2 uv, float lod) {
  return textureLod(u_image, uv, lod).rgb;
}

vec3 sampleScreen(vec2 uv, float sigma) {
  uv = imageCoordinates(uv);
  // 金字塔每一级是真实的高斯模糊；sigma 单位为图像像素，用 log2 选级
  float lod = max(0.0, log2(max(sigma, 1.0)));
  vec3 blurred = sceneSample(uv, max(1.0, lod));
  if (sigma >= 2.0) return blurred;
  return mix(sceneSample(uv, 0.0), blurred, smoothstep(0.0, 2.0, sigma));
}

void main() {
  float pixelY = fwidth(v_uv.y);
  vec3 dark = vec3(0.003, 0.004, 0.005);
  float turn = clamp(u_turn, 0.0, 1.0);
  if (turn <= 0.00001) {
    outColor = vec4(sceneSample(imageCoordinates(v_uv), 0.0), 1.0);
    return;
  }

  float outer = step(0.5, u_phase);
  float hinge = mix(1.0, 0.0, outer);
  float fromHinge = abs(v_uv.x - hinge);
  float tilt = turn * 1.570796327;
  float cosine = max(0.0, cos(tilt));
  float sine = sin(tilt);

  float eyeDistance = 2.4 * max(u_aspect, 1.0);
  float depth = fromHinge * u_aspect * sine;
  float perspective = eyeDistance / (eyeDistance - depth);
  vec2 imageUv;
  imageUv.x = hinge + (v_uv.x - hinge) * cosine * perspective;
  imageUv.y = 0.5 + (v_uv.y - 0.5) * perspective;

  // 景深模糊：角度响应 × 距铰链距离响应
  float blurAngle = pow(smoothstep(0.0, radians(u_blurAngle.y), tilt), u_blurAngle.z);
  float blurSpread = pow(smoothstep(0.0, u_blurSpread.x, fromHinge), u_blurSpread.y);
  float defocus = blurAngle * mix(u_blurSpread.z, 1.0, blurSpread);
  float sigma = u_imageSize.x * u_blurAngle.x * defocus;

  // 上下透视黑边的柔和过渡
  float marginSoftness = pixelY + 2.0 * sigma / u_imageSize.y;
  float verticalMask = 1.0 - smoothstep(0.5 - marginSoftness, 0.5 + marginSoftness, abs(imageUv.y - 0.5));

  // 折叠玻璃的暗化
  float glass = sine * pow(fromHinge, 1.6);
  vec3 content = sampleScreen(imageUv, sigma);
  content *= 1.0 - mix(0.28, 0.06, outer) * glass;

  // 铰链附近的镜面反光
  float reflection = exp(-pow((fromHinge - 0.70) / 0.30, 2.0)) * sine;
  content += vec3(0.82, 0.85, 0.86) * reflection * 0.025;

  // 铰链黑边渐变
  float blackFade = clamp((fromHinge - u_blackGradient.y) / max(u_blackGradient.z - u_blackGradient.y, 0.001), 0.0, 1.0);
  content *= 1.0 - u_blackGradient.x * blurAngle * blackFade;

  outColor = vec4(mix(dark, content, verticalMask), 1.0);
}
"""

    /**
     * 可分离 5-tap 高斯模糊（u_step 控制方向），用于构建模糊金字塔。
     */
    const val GAUSS5_FRAG = """#version 300 es
precision highp float;
uniform sampler2D u_source;
uniform vec2 u_step;
uniform float u_level;
in vec2 v_uv;
out vec4 outColor;
void main() {
  vec4 color = textureLod(u_source, v_uv, u_level) * 0.2270270270;
  color += textureLod(u_source, v_uv + u_step * 1.3846153846, u_level) * 0.3162162162;
  color += textureLod(u_source, v_uv - u_step * 1.3846153846, u_level) * 0.3162162162;
  color += textureLod(u_source, v_uv + u_step * 3.2307692308, u_level) * 0.0702702703;
  color += textureLod(u_source, v_uv - u_step * 3.2307692308, u_level) * 0.0702702703;
  outColor = color;
}
"""

    /** 默认模糊/渐变参数（网页端设置面板初始值，见 NOTES.md §5） */
    data class FoldConfig(
        val strength: Float = 0.0315f,      // 最大模糊（占图像宽度比例）
        val fullAngle: Float = 90f,         // 达到最大模糊的物理角度（度）
        val anglePower: Float = 0.5f,       // 角度响应指数
        val fullDistance: Float = 0.7f,     // 距铰链多远处达到全模糊
        val distancePower: Float = 1.45f,   // 距离响应指数
        val edgeBlur: Float = 0.18f,        // 铰链处自身模糊
        val gradientOpacity: Float = 0.7f,  // 黑边不透明度
        val gradientStart: Float = 0.26f,   // 黑边起始
        val gradientEnd: Float = 1.0f       // 黑边结束
    )
}
