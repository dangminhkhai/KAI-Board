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

    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }
        // AGP requires a release type; this project is debug-only (no minify/signing).
        getByName("release") {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
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
