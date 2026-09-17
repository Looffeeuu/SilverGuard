import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val silverGuardLocalProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

// Cloud AI is paused for the offline 0.5 release; existing local proxy settings are preserved.
val silverGuardAiEnabled = silverGuardLocalProperties.getProperty("SILVERGUARD_AI_ENABLED", "false") == "true"

android {
    namespace = "com.silverguard.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.silverguard.app"
        minSdk = 23
        targetSdk = 37
        versionCode = 13
        versionName = "0.5.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "SILVERGUARD_AI_ENABLED", silverGuardAiEnabled.toString())
        buildConfigField(
            "String",
            "SILVERGUARD_AI_PROXY_URL",
            (if (silverGuardAiEnabled) silverGuardLocalProperties
                .getProperty("SILVERGUARD_AI_PROXY_URL", "")
                .trim() else "")
                .asBuildConfigString()
        )
        buildConfigField(
            "String",
            "SILVERGUARD_AI_PROXY_TOKEN",
            (if (silverGuardAiEnabled) silverGuardLocalProperties
                .getProperty("SILVERGUARD_AI_PROXY_TOKEN", "")
                .trim() else "")
                .asBuildConfigString()
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Bundled Chinese OCR model: can work on-device after the app is installed.
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    implementation("com.squareup.okhttp3:okhttp:5.3.0")
    implementation("org.jsoup:jsoup:1.23.2")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
