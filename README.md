# DaijinDuo

> **利用 Android 陀螺仪实现 iPhone DUO 的偏转效果**

<p align="center">
  <strong>简体中文</strong> · <a href="README_EN.md"><strong>English</strong></a>
</p>

把手机桌面截图当作一块"会呼吸的玻璃"——屏幕朝上平放时图像平整，向任意方向倾斜时沿垂直铰链 90° 折叠，
带 **透视缩短、景深模糊（Gaussian 金字塔）、铰链黑边渐变与玻璃反光**，完整复刻 [solotilt.com](https://solotilt.com/) 的 iPhone DUO 折叠效果。

**v0.3 正式版** · 包名 `com.daijinduo.github`

## ✨ 特性

- **陀螺仪驱动**：原生加速度计经 JS 桥驱动 WebView 渲染内核，映射公式与网页端逐行一致（`sign=-1` + 指数平滑）
- **WebView / WebGL2 内核**：渲染完全内嵌浏览器内核，避开原生 GLES 兼容性差异，几乎全设备可用
- **🎛 效果调节（全屏内）**：进入全屏后**双击屏幕**呼出调节面板——**模糊强度** · **陀螺仪灵敏度** · **模糊触发角度**（本地持久化）；首次进入会提示进入方法，可勾选「不再提示」
- **复古简约 UI**：强制内置思源宋体（子集化打包）· 纯黑白 · 白描边品牌 Logo
- **液态玻璃底栏**：iOS 26 Liquid Glass 风格悬浮 Tab 栏（Haze 真实背景模糊 · 顶部渐变高光 · 弹性液态选中 Pill）
- **双页结构**：`Duo`（主要功能）/ `Show`（敬请期待）
- **轻点 3 次退出** 全屏效果
- **GitHub Actions 云构建**，APK 自动发布到 Release

## 🖼 交互流程

1. 打开 App：`Duo` 页（复古简约主页 + 品牌 Logo + 折叠预览）
2. **相册**（黑底白框）→ 申请相册权限 → 网格浏览最近图片
3. **文件**（白底黑字）→ 系统 Photo Picker 直接选择
4. 点击图片 → 全屏沉浸，开始折叠效果（首次进入会提示「双击呼出效果调节」，可勾选不再提示）
5. 倾斜手机：折叠量随倾角增大（0~90° 全折），方向决定折向
6. **双击屏幕** → 呼出效果调节面板（模糊强度 / 陀螺仪灵敏度 / 模糊触发角度，实时生效并持久化）；点遮罩或 ✕ 关闭
7. 轻点屏幕 3 次（800ms 窗口内）→ 退出回到主页
8. 底部玻璃栏切换 `Show` 页（敬请期待）

## 📁 目录结构

```
app/src/main/
├── assets/daijinduo/index.html     # WebView 渲染内核（WebGL2 折叠 shader 逐行移植）
├── java/com/daijinduo/github/
│   ├── MainActivity.kt             # 双页骨架（Duo/Show）+ 液态玻璃底栏
│   ├── WebViewActivity.kt          # 全屏效果页：HTML 注入 + JS 桥 + 原生加速度计 + 双击呼出调节
│   ├── SettingsStore.kt            # 效果设置持久化（SharedPreferences，含「不再提示」）
│   ├── ui/
│   │   ├── Theme.kt                # 思源宋体 · 复古简约色板
│   │   ├── HomeScreen.kt           # Duo 主页（Logo + 双入口）
│   │   ├── FoldAdjustOverlay.kt    # 全屏双击呼出的效果调节面板 + 首次进入提示横幅
│   │   ├── GalleryScreen.kt        # 相册权限 + 图片网格
│   │   ├── ShowPage.kt             # Show 页（敬请期待）
│   │   └── LiquidGlassBar.kt       # 液态玻璃悬浮底栏（Haze）
│   └── data/GalleryLoader.kt       # MediaStore 查询与缩略图
ref/                                # solotilt.com 逆向分析产物
```

## ☁️ 云构建（GitHub Actions）

推送到 `main` 分支自动触发（也可在 Actions 页手动 `workflow_dispatch`）：

- JDK 17 + Android SDK 35
- `gradle :app:assembleRelease`（debug 签名，可直接安装）
- 产物发布到 **GitHub Release**：`DaijinDuo-v0.3.apk`

## 📦 版本记录

| 版本 | 说明 |
|---|---|
| **v0.3** | 效果调节**移至全屏页**：进入全屏后**双击**呼出调节面板（模糊强度 / 陀螺仪灵敏度 / 模糊触发角度）；首次进入提示进入方法，可勾选「不再提示」；主页移除效果设置卡 · 签名不变 · 包名 `com.daijinduo.github` |
| v0.2 | 新增效果设置：模糊强度 / 陀螺仪灵敏度 / 模糊触发角度（主页滑块，本地持久化，注入渲染内核）· 签名不变 · 包名 `com.daijinduo.github` |
| v0.1.1 | 强制内置思源宋体（子集化打包，全设备渲染一致）· 主题色改纯黑白 · 包名 `com.daijinduo.github` |
| v0.1ZS | 首个正式版：复古简约 UI · 思源宋体 · 白描边 Logo · 液态玻璃底栏 · 双页结构（Duo/Show）· 包名 `com.daijinduo.github`（版本号重新开始） |

> 注：v1.0.0 ~ v1.1.2 为开发期迭代版本，已全部移除，正式版从 **v0.1ZS** 起算。

## 🛠 本地构建

```bash
./gradlew :app:assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

## ⚙️ 效果调节（v0.3 在全屏双击呼出，其余与网页端一致）

| 参数 | 默认值 | 调节范围 | 含义 |
|---|---|---|---|
| **模糊强度** 🎛 | 0.0315 | 0 ~ 0.08 | 折叠处最大模糊（占图像宽度） |
| **陀螺仪灵敏度** 🎛 | 1.0× | 0.5× ~ 2.0× | 折叠对倾斜角的响应增益 |
| **模糊触发角度** 🎛 | 90° | 30° ~ 120° | 达到最大模糊的倾角 |
| fullAngle（原） | 90° | — | 同上（由触发角度覆盖） |
| anglePower | 0.5 | — | 角度响应指数 |
| fullDistance | 0.7 | — | 距铰链全模糊距离 |
| distancePower | 1.45 | — | 距离响应指数 |
| edgeBlur | 0.18 | — | 铰链处模糊 |
| gradientOpacity | 0.7 | — | 黑边不透明度 |

> 🎛 = 全屏效果页双击呼出调节面板，实时生效并本地持久化（SharedPreferences），注入 WebView 渲染内核；「不再提示」勾选后下次进入不再弹提示。

## 📜 许可

仅供个人学习与体验使用；折叠效果逆向自 [solotilt.com](https://solotilt.com/)，版权归原作者所有。
