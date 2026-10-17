// Vex Launcher - kök Gradle build dosyası
// Tüm modüller burada tanımlanan eklentileri paylaşır; sürümler gradle/libs.versions.toml içinde sabitlenmiştir.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
