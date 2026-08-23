import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/** Upload-key credentials, absent on a fresh clone and in CI. */
val uploadKey = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "io.github.rastislavsk.solarcast"

    // Google Play requires new apps and updates to target API 36 from 31 Aug 2026.
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "io.github.rastislavsk.solarcast.native"
        // Compose's baseline, and the level adaptive icons start at.
        minSdk = 26
        targetSdk = 36

        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        /*
         * Release signing is driven by keystore.properties, which is never
         * committed. Without it the release build still runs and produces an
         * unsigned bundle, so CI and a fresh clone are never blocked on secrets.
         */
        if (uploadKey.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(uploadKey.getProperty("storeFile"))
                storePassword = uploadKey.getProperty("storePassword")
                keyAlias = uploadKey.getProperty("keyAlias")
                keyPassword = uploadKey.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // Emits the locale config that puts SolarCast under
        // Settings > System > Languages > App languages, and that the in-app
        // language picker drives through AppCompatDelegate.
        generateLocaleConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    bundle {
        // The in-app picker can switch to any of the four languages at runtime,
        // so none of them may be split out of the base APK.
        language { enableSplit = false }
    }

    lint {
        abortOnError = true
        // targetSdk 36 is the level Play requires as of 31 Aug 2026. Moving to
        // 37 is a decision for a later release, not an oversight.
        disable += "OldTargetApi"
        // The splash icon is 288dp because the splash screen spec fixes that size.
        disable += "VectorRaster"
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // AppCompat is here for one thing: AppCompatDelegate's per-app locales,
    // which is what the in-app language picker sets.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.google.fonts)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
