import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Play-kaupan upload-avain: keystore.properties (ei versionhallinnassa), ks. docs/play/JULKAISU.md
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "fi.heatcall"
    compileSdk = 37

    defaultConfig {
        applicationId = "fi.heatcall"
        minSdk = 29
        targetSdk = 36
        versionCode = 6
        versionName = "1.2"
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("upload") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Sivuladattava APK (GitHub-julkaisut): debug-avain, jotta päivitykset asentuvat vanhan päälle.
            signingConfig = signingConfigs.getByName("debug")
        }
        // Play-kauppa: ./gradlew bundlePlay → app/build/outputs/bundle/play/app-play.aab
        create("play") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload")
            matchingFallbacks += listOf("release")
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

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
