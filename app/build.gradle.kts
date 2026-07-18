plugins {
    id("com.android.application")
}

val releaseStorePath = providers.environmentVariable("KAI_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("KAI_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("KAI_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("KAI_RELEASE_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    releaseStorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "vn.kai.board"
    compileSdk = 37

    defaultConfig {
        applicationId = "vn.kai.board"
        minSdk = 26
        targetSdk = 35
        versionCode = 120
        versionName = "1.2.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseStorePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
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
            include("arm64-v8a")
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
