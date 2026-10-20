plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.daijinduo.github"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.daijinduo.github"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "v0.3"
    }

    buildTypes {
        release {
            // CI 中预生成 debug keystore，用 debug 签名保证 APK 可直接安装
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // 液态玻璃（iOS 26 Liquid Glass 风格）：背景模糊 + 玻璃透镜效果（Chris Banes / Haze）
    implementation("dev.chrisbanes.haze:haze:1.5.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
