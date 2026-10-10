plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.ricci.garminsleep"
    compileSdk = 36

    signingConfigs {
        getByName("debug") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "de.ricci.sleepsync.dev"
        minSdk = 28
        targetSdk = 35
        // CI builds get a monotonically increasing version so SleepSync can compare updates reliably.
        val ciRun = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
        versionCode = ciRun ?: 1
        versionName = if (ciRun != null) "0.1.0-v2-dev.$ciRun" else "0.1.0-v2-dev"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui-viewbinding")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("com.joaomgcd:taskerpluginlibrary:0.4.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.Dimezis:BlurView:version-3.2.0")
}
