plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

android {
    namespace = "com.v7878.fee0"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.v7878.fee0"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            optimization {
                enable = false
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}