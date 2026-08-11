plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.videoresizer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.videoresizer"
        minSdk = 24
        targetSdk = 34
        versionCode = 13
        versionName = "1.13"
    }

    // IMPORTANT: this keystore (release.keystore, committed at the project root)
    // is the ONLY thing that lets Android treat a newly-built APK as an "update"
    // to a previously-installed one instead of a conflicting different app.
    // Losing/regenerating it means every future install requires uninstalling
    // the old copy first. Do not delete release.keystore or change the alias.
    //
    // The password itself is NOT stored here — this repo may be public, so it's
    // read from an environment variable instead, which GitHub Actions populates
    // from the repository Secret RELEASE_KEYSTORE_PASSWORD (Settings > Secrets
    // and variables > Actions). PKCS12 keystores use one password for both the
    // store and the key, so storePassword and keyPassword are intentionally the
    // same value here. This mirrors exactly what was already set up in this
    // repo's history (commits e5807ce / 4001440) — restored here after it was
    // accidentally getting overwritten back to debug-signing on every update.
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("release.keystore")
            val releasePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                ?: "not-set-see-github-secrets"
            storePassword = releasePassword
            keyAlias = "videoresizer"
            keyPassword = releasePassword
        }
    }

    buildTypes {
        release {
            // Deliberately left off (isMinifyEnabled/isShrinkResources = true
            // was tried in this repo's history but reverted here for now — R8
            // shrinking is a real risk of stripping something Media3's
            // Transformer needs via reflection, and that's not something that
            // can be verified without a real device test pass). Safe to turn
            // on later once a shrunk build has actually been test-installed.
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // Build-speed tuning (see CHANGELOG.md, Batch 4): `assembleRelease`
    // otherwise drags in AGP's lintVital analysis pass before it'll produce
    // an APK, which re-parses/re-analyzes the whole module on every build.
    // This CI's only job is producing an installable signed APK, not
    // gating on lint findings, so skip it rather than pay for it every push.
    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Media3 Transformer for actual video resizing / re-encoding
    implementation("androidx.media3:media3-transformer:1.3.1")
    implementation("androidx.media3:media3-effect:1.3.1")
    implementation("androidx.media3:media3-common:1.3.1")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // Guava — explicit because ImmutableList is referenced transitively by
    // Media3's OverlayEffect API (used for the watermark feature). Likely
    // already pulled in transitively via media3-effect, but a prior real
    // build in this repo's own history (commit dba46ae) hit this as an
    // actual unresolved-reference failure, so it's declared explicitly here
    // too rather than relying on the transitive dependency.
    implementation("com.google.guava:guava:33.2.1-android")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
