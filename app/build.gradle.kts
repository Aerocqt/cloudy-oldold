plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.aerocat.cloudy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aerocat.cloudy"
        minSdk = 26
        targetSdk = 34            // A32 4G shipped OneUI on Android 11/12/13; kept modest on purpose
        versionCode = 80604
        versionName = "8.6.4 Beta"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true }
}

dependencies {
    // ---- tribalfs SESL fork (OneUI look) ----
    // These REPLACE the standard AndroidX + Material artifacts. Do NOT also pull the
    // upstream androidx.appcompat / com.google.android.material or you'll get duplicate classes.
    // Coordinates below follow the JitPack pattern for these repos; confirm the newest
    // tag from each repo's README as they are versioned frequently.
    implementation("io.github.tribalfs:oneui-design:0.4.0+oneui7")
    implementation("com.github.tribalfs.sesl-androidx:appcompat:1.7.0+sesl7")
    implementation("com.github.tribalfs.sesl-androidx:preference:1.2.1+sesl7")
    implementation("com.github.tribalfs.sesl-androidx:recyclerview:1.4.0+sesl7")
    implementation("com.github.tribalfs:sesl-material-components-android:1.12.0+sesl7")
    implementation("com.github.tribalfs.sesl-androidx:viewpager2:1.1.0+sesl7")

    // ---- root execution ----
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("com.github.topjohnwu.libsu:service:6.0.0")

    // ---- networking + json ----
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
}
