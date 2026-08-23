import java.io.File
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
}

/*
 * The web app is the single source of truth: it lives at the repository root
 * as index.html and is copied into the bundle assets at build time, so the
 * Android build can never ship a stale fork of it.
 */
abstract class BundleWebApp : DefaultTask() {
    @get:InputFile
    abstract val source: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun bundle() {
        val root = outputDir.get().asFile
        root.deleteRecursively()
        val www = File(root, "www")
        www.mkdirs()
        source.get().asFile.copyTo(File(www, "index.html"), overwrite = true)
    }
}

val bundleWebApp = tasks.register<BundleWebApp>("bundleWebApp") {
    description = "Copies the standalone index.html from the repository root into the app assets."
    source.fileValue(rootProject.projectDir.parentFile.resolve("index.html").canonicalFile)
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
        applicationId = "io.github.rastislavsk.solarcast"
        // Adaptive icons and the modern WebView APIs start at 26; anything older
        // is well under a percent of Play devices in 2026.
        minSdk = 26
        targetSdk = 36

        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        /*
         * Release signing is driven by keystore.properties, which is never
         * committed. Play App Signing re-signs the artifact for distribution,
         * but the upload key must be ours and must stay stable.
         *
         * Without that file the release build still runs and produces an
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
        buildConfig = true
    }

    lint {
        abortOnError = true
        // targetSdk 36 is the level Play requires as of 31 Aug 2026. Moving to
        // 37 is a decision for a later release, not an oversight.
        disable += "OldTargetApi"
        // The splash icon is 288dp because the splash screen spec fixes that
        // size; the 200dp advice does not apply to it.
        disable += "VectorRaster"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // Emits res/xml/_generated_res_locale_config.xml, which is what makes the
        // app show up under Settings > Apps > SolarCast > Language on Android 13+.
        generateLocaleConfig = true
    }

    dependenciesInfo {
        // Play does not need the dependency blob and it is not reproducible.
        includeInApk = false
        includeInBundle = false
    }

    bundle {
        // The in-app switcher can pick any of the four languages at runtime,
        // so none of them may be split out of the base APK.
        language { enableSplit = false }
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin")
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(bundleWebApp, BundleWebApp::outputDir)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.material)
}
