# Solo Tilt (solotilt.com) — 前端实现逆向分析笔记

> 来源：https://solotilt.com/ 静态资源逆向（SvelteKit + WebGL2）。
> 本文用于指导 Android 端（本仓库）对 iPhone Duo 折叠效果的"完美复刻"。

## 1. 技术栈（网页端）

- SvelteKit（SSR + `prerender:false`），Vite 打包，资源位于 `/_app/immutable/`。
- 渲染：单个 `<canvas id="gl">`，**WebGL2**（`antialias:false, alpha:false, powerPreference` 按需），`pixelRatio` 上限 3。
- 传感器：`devicemotion`（`accelerationIncludingGravity - acceleration` 得重力矢量），iOS 需 `DeviceMotionEvent.requestPermission()`。
- 图标/二维码/像素画布等为附属功能，与本效果无关。

## 2. 效果原理（一句话）

**把手机屏幕当作"物理显示面"：手机从"屏幕朝上平放"向任意方向倾斜时，截屏图像围绕一条垂直铰链（hinge）做 90° 折叠透视 + 景深模糊 + 铰链黑边渐变。**

- `u_turn` ∈ [0,1]：折叠量（倾斜越大折叠越深）。
- `u_phase` ∈ {0,1}：铰链在右缘 / 左缘（决定往哪个方向折）。
- 折叠用**单面 + 透视**模拟（非真 3D 网格）：每像素把屏幕坐标反投影回"静止图像平面"，边缘自然透视缩短。
- 模糊 = 上传时预计算的 **Gaussian 金字塔 mip**（每一级是上一级的可分离 5-tap 高斯模糊 + 半分辨率降采样），shader 里按 `lod = log2(sigma)` 采样，实现与折叠深度成正比的景深模糊。
- 铰链处叠加"黑色渐变"（黑边）与轻微镜面反光（`reflection`），模拟玻璃折叠。

## 3. 折叠 Fragment Shader（核心，见 fold.frag.glsl）

关键数学（逐像素，canvas 即物理显示面）：

```
turn = clamp(u_turn, 0, 1)
if turn ≈ 0 → 直接显示原图（采样 level 0）

outer   = step(0.5, u_phase)          // 当前是哪一面
hinge   = mix(1, 0, outer)            // 铰链 x（0=左缘，1=右缘）
fromHinge = abs(uv.x - hinge)

tilt  = turn * π/2                     // 0 → 90°
cosine = max(0, cos(tilt)); sine = sin(tilt)

eyeDistance = 2.4 * max(aspect, 1)     // 观察者到铰链垂直中线的距离（单位：屏高）
depth      = fromHinge * aspect * sine // 沿视线方向的深度（透视缩短量）
perspective = eyeDistance / (eyeDistance - depth)

imageUv.x = hinge + (uv.x - hinge) * cosine * perspective
imageUv.y = 0.5 + (uv.y - 0.5) * perspective

// 模糊：与折叠角度、距铰链距离相关
blurAngle  = pow(smoothstep(0, radians(blurAngleY), tilt), blurAngleZ)
blurSpread = pow(smoothstep(0, blurSpreadX, fromHinge), blurSpreadY)
defocus    = blurAngle * mix(blurSpreadZ, 1, blurSpread)
sigma      = imageSize.x * blurAngleX * defocus   // 像素单位
lod        = max(0, log2(max(sigma,1)))

// 采样：sigma<2 时在 level0 与 level1 间混合，否则直接 textureLod
// 垂直边缘软遮罩（透视导致上下出现黑边时柔和过渡到深色）
// 铰链暗化 glass = sine*pow(fromHinge,1.6)，content *= 1 - mix(0.28,0.06,outer)*glass
// 反光 reflection = exp(-((fromHinge-0.70)/0.30)^2) * sine，加少量亮色
// 黑色渐变 blackFade = clamp((fromHinge-gradientStart)/(gradientEnd-gradientStart),0,1)
//   content *= 1 - gradientOpacity * blurAngle * blackFade
// 最终与深色 (0.003,0.004,0.005) 按垂直遮罩混合
```

## 4. 运动 → 折叠量映射（见 motion_sampler.js）

每帧 `devicemotion` 事件采样（Android 端符号翻转 `sign=-1`）：

```
gravity = accelerationIncludingGravity - acceleration   // 重力矢量
r = sign * gx ; i = sign * gy ; a = sign * gz
if hypot(r,i,a) < 0.5 → 忽略（读数不稳定）

s = atan2(hypot(r,i), -a)  // 与"屏幕朝上"的夹角：0=平放朝上，90=竖直，180=朝下
if s > 120.00001 → facingDown → angle = 0   // 超过 120° 折叠归零
o = hypot(r,i)
if o < 1e-5 → angle = 0                      // 完全水平 → 不折
u = min(120, s) * r / o                      // 投影到水平方向
angle = clamp(-2 * u, -180, 180)
```

渲染循环里的映射（指数平滑，时间常数 ~16/s）：

```
y = liveMotionAngle                // 无拖拽预览时
h += (y - h) * (1 - exp(-dt * 16)) // 平滑
C  = h
u_turn  = min(|C| / 180, 1)
u_phase = C >= 0 ? 1 : 0
```

## 5. 默认参数（Object.freeze，网页端设置面板的初始值）

```
strength:        0.0315    // u_blurAngle.x  最大模糊（占图像宽度比例）
fullAngle:       90        // u_blurAngle.y  达到最大模糊的物理角度（度）
anglePower:      0.5       // u_blurAngle.z  角度响应指数
fullDistance:    0.7       // u_blurSpread.x 距铰链多远处达到全模糊
distancePower:   1.45      // u_blurSpread.y 距离响应指数
edgeBlur:        0.18      // u_blurSpread.z 铰链处本身的模糊量
gradientOpacity: 0.7       // u_blackGradient.x 黑边不透明度
gradientStart:   0.26      // u_blackGradient.y 黑边起始
gradientEnd:     1.0       // u_blackGradient.z 黑边结束
```

## 6. 模糊金字塔（Gaussian pyramid）

- 上传图片时构建：level0 = 原图（居中裁切到屏幕比例）；level k+1 = 上一级经可分离 5-tap 高斯模糊（gauss5.frag.glsl，权重 0.227027/0.316216/0.0702703）渲染到半分辨率 FBO。
- 采样时 `textureLod(uv, max(0, log2(max(sigma,1))))`，minFilter = LINEAR_MIPMAP_LINEAR。

## 7. 交互细节（网页端）

- 打开后需先"允许运动权限"，然后把手机**屏幕朝上平放**（gravity ≈ 朝上）。
- 任意方向倾斜 → 折叠量随倾角增大（0~90° 到全折）；折向由倾斜方向决定。
- 无运动数据时自动演示：`u_phase` 每 180° 相位自动往复（一面从正对到侧立再回正）。
- 全屏按钮、设置（语言/换图/全屏）、社交悬浮球等为附属 UI。

## 8. Android 移植要点（本仓库）

- GLES 3.0（`#version 300 es`），`layout(location=…)`、`textureLod`、`fwidth` 均可用。
- 传感器：`SensorManager.TYPE_GRAVITY`（等价于网页端重力矢量），SENSOR_DELAY_GAME。
- 贴图：居中裁切到屏幕宽高比 → 构建 Gaussian 金字塔。
- 交互：全屏沉浸 → 倾斜即折叠；轻点 3 次退出。
