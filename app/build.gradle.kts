plugins {
    alias(libs.plugins.android.application)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

android {
    namespace = "com.v7878.samik"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.v7878.samik"
        minSdk = 26
        targetSdk = 37
        versionCode = 100_000_000
        versionName = "1.0.0"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.recyclerview)
}