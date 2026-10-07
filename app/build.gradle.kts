
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.witty.securevault"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.witty.securevault"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }
    
    chaquopy {
        defaultConfig {
            version = "3.11"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}

// Auto-copy the Debug APK to the root project directory and rename it
tasks.whenTaskAdded {
    if (name == "assembleDebug") {
        doLast {
            copy {
                // Takes the APK from app/build/outputs/apk/debug
                from("${layout.buildDirectory.get()}/outputs/apk/debug")
                // Drops it in the main project folder
                into(project.rootDir)
                include("*.apk")
                // Renames it to SERET.apk
                rename { "SERET.apk" }
            }
        }
    }
}
