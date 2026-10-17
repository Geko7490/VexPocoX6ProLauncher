import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Macrobenchmark modülü: uygulamayı fiziksel cihazda ölçmek için ayrı bir test APK'sı üretir.
// Kaynak: https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
//
// com.android.test modülü kullanılır; böylece test APK'sı hedef uygulamayı kendisi
// yükleyebilir (self-instrumenting) ve "benchmark" build type'ı gerekmez.
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.vex.launcher.benchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    // Test APK'sının hedef uygulamayı kurabilmesi için gerekli.
    experimentalProperties["android.experimental.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
