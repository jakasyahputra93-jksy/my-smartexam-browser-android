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
        versionCode = 2
        versionName = "0.1.2"
    }

    buildTypes {
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
