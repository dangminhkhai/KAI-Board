plugins {
    id("com.android.application")
}

android {
    namespace = "vn.kai.board"
    compileSdk = 37

    defaultConfig {
        applicationId = "vn.kai.board"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-debug"
    }

    // Shared debug keystore so every machine signs the same (adb install -r without uninstall).
    // File: keystore/android-debug.keystore (checked in; debug-only, not for Play Store).
    signingConfigs {
        create("sharedDebug") {
            storeFile = rootProject.file("keystore/android-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("sharedDebug")
        }
        // AGP requires a release type; this project is debug-only (no minify/signing).
        getByName("release") {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            // Same cert as debug so accidental release builds still match device installs.
            signingConfig = signingConfigs.getByName("sharedDebug")
        }
    }

    packaging {
        jniLibs {
            // Keep ML Kit's native translation engine compressed in the APK.
            // Android extracts it at install time, reducing download/APK size
            // without changing typing latency or translation behavior.
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("com.google.mlkit:translate:17.0.3")
    testImplementation("junit:junit:4.13.2")
}
