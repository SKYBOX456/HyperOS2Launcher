plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.skybox.hyperoslauncher"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.skybox.hyperoslauncher"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
