plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.finploit.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.finploit.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 50
        versionName = "2.16.0"

        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:5009/\"")
        buildConfigField(
            "String",
            "GOOGLE_CLIENT_ID",
            "\"${System.getenv("GOOGLE_CLIENT_ID") ?: findProperty("FINPLOIT_GOOGLE_CLIENT_ID") ?: ""}\"",
        )
    }

    signingConfigs {
        create("release") {
            // Credenciais via env var ou ~/.gradle/gradle.properties (FINPLOIT_*)
            storeFile = file("${rootProject.projectDir}/finploit-release.jks")
            storePassword = System.getenv("KEYSTORE_PASS")
                ?: (findProperty("FINPLOIT_KEYSTORE_PASS") as String? ?: "")
            keyAlias = System.getenv("KEY_ALIAS")
                ?: (findProperty("FINPLOIT_KEY_ALIAS") as String? ?: "finploit")
            keyPassword = System.getenv("KEY_PASS")
                ?: (findProperty("FINPLOIT_KEY_PASS") as String? ?: "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:5009/\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "API_BASE_URL", "\"https://api-finance.josefelipedev.com/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Storage
    implementation(libs.security.crypto)
    implementation(libs.datastore.preferences)

    // Image
    implementation(libs.coil.compose)

    // Google Sign-In
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.google.id)

    // Location
    implementation(libs.play.services.location)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // WorkManager + Hilt integration
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room (offline cache + budget limits)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // CameraX (receipt scan + barcode)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // ML Kit Barcode
    implementation(libs.mlkit.barcode)

    // Glance Widget
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Accompanist permissions
    implementation(libs.accompanist.permissions)

    debugImplementation(libs.androidx.ui.tooling)
}
