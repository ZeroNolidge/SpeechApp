import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

// One codebase, two apps: the Android APK and the web (PWA) version.
kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("talkandlearn")
        browser {
            commonWebpackConfig { outputFileName = "talkandlearn.js" }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation("androidx.core:core-ktx:1.15.0")
            implementation("androidx.activity:activity-compose:1.9.3")
        }
    }
}

compose.resources {
    packageOfResClass = "com.zeronolidge.speechapp.res"
    publicResClass = false
}

android {
    namespace = "com.zeronolidge.speechapp"
    compileSdk = 36

    defaultConfig {
        // Keep this ID: the installed app updates in place only if it matches.
        applicationId = "com.zeronolidge.speechapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }

    // CI signs every build with the same permanent key (kept in GitHub secrets), so a new
    // version installs over the old one and keeps the child's progress. Without the key,
    // e.g. on a contributor's machine, builds fall back to the debug key.
    val keystorePath = System.getenv("SIGNING_KEYSTORE_PATH")
    val keystorePassword = System.getenv("SIGNING_PASSWORD")
    val hasReleaseKey = !keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank() && file(keystorePath).exists()

    signingConfigs {
        create("release") {
            if (hasReleaseKey) {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                keyAlias = "talkandlearn"
                keyPassword = keystorePassword
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}
