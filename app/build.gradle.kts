import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Signing key (keystore.properties dijana oleh workflow "bootstrap signing")
val keystoreProps = Properties()
val keystorePropsFile = rootProject.file("keystore.properties")
if (keystorePropsFile.exists()) {
    keystorePropsFile.inputStream().use { keystoreProps.load(it) }
}

android {
    namespace = "com.bangnyok.mp3nyok"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bangnyok.mp3nyok"
        minSdk = 24
        targetSdk = 35
        // versionName ikut git tag (v1.2.3 -> "1.2.3"); versionCode naik ikut CI run
        versionName = (System.getenv("GITHUB_REF_NAME") ?: "1.0").removePrefix("v")
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 1

        // youtubedl-android ships python + ffmpeg for all ABIs (~150MB APK).
        // Uncomment to shrink the APK to specific devices:
        // ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = rootProject.file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Kalau keystore belum di-bootstrap, guna debug signing supaya build tetap jalan
            signingConfig = if (keystorePropsFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
        buildConfig = true
    }
    defaultConfig {
        buildConfigField(
            "String", "UPDATE_TOKEN",
            "\"${(project.findProperty("updateToken") as String?) ?: ""}\""
        )
    }
    packaging {
        // Required by youtubedl-android (bundled python/ffmpeg native libs)
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Playback (mp3 + test streaming)
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")

    // Persistence (JSON file store)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // yt-dlp + ffmpeg bundled for Android (downloads YouTube / YT Music -> mp3)
    val youtubedlAndroid = "0.18.1"
    implementation("io.github.junkfood02.youtubedl-android:library:$youtubedlAndroid")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$youtubedlAndroid")

    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")
}
