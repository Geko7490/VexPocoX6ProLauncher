# Vex Launcher - R8 / ProGuard kuralları
# Compose, AndroidX ve DataStore kendi kurallarını taşır; ek olarak sadece model sınıfları korunur.
-keepclassmembers class com.vex.launcher.core.model.** { *; }
-dontwarn org.jetbrains.annotations.**
