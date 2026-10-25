plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.mysmartclass.smartexam"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.mysmartclass.smartexam"
        minSdk = 28
        targetSdk = 35
        versionCode = 4
        versionName = "0.3.0"
    }

    buildTypes {
        debug {
            // Pertahankan applicationIdSuffix pilot agar APK uji v0.3 dapat dipasang berdampingan dengan APK uji lama.
            applicationIdSuffix = ".v03test"
        }
        release {
            isMinifyEnabled = false
            // Untuk rilis ke siswa, buat keystore resmi di komputer pengembang.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
    }
}
