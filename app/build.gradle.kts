plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.vellum.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.vellum.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2"
        // Коммит сборки: по нему приложение понимает, есть ли новее версия
        buildConfigField("String", "COMMIT", "\"${System.getenv("GITHUB_SHA") ?: "dev"}\"")
    }
    // Один ключ для всех сборок, иначе обновление поверх установленной версии не встанет
    signingConfigs {
        create("shared") {
            storeFile = file("vellum.jks")
            storePassword = "vellum-app"
            keyAlias = "vellum"
            keyPassword = "vellum-app"
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("shared") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}
