plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.devpro58.hnem06.moneysnap"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.devpro58.hnem06.moneysnap"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // No R8 yet, so there is no mapping file to upload. Flip this to true in the
            // same change that sets isMinifyEnabled = true — enabling R8 without symbol
            // upload leaves obfuscated, unusable stack traces in Crashlytics.
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Backports java.time to minSdk 24. Required for recurrence date math
        // (month-end clamping, DST) and month-scoped stats aggregation.
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        viewBinding = true
        // BuildConfig.VERSION_NAME for the Profile "About" row, BuildConfig.DEBUG for Timber.
        buildConfig = true
    }

    // Room exports its schema JSON here so migrations are verifiable by MigrationTestHelper.
    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// room-testing 2.8.3 pulls room-migration, which parses the exported schema JSON with
// kotlinx-serialization 1.8.x. The Kotlin Gradle Plugin pins 1.7.3 `strictly` on every
// configuration, and GeneratedSerializer.typeParametersSerializers() no longer exists in
// 1.8 — so MigrationTestHelper dies with AbstractMethodError at runtime rather than at
// build time. Force the version Room was compiled against, on the test classpath only.
configurations.matching { it.name.contains("AndroidTest") }.configureEach {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
        force("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1")
        force("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
        force("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1")
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Unit tests (JVM)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.mockk)

    // Instrumented tests (device/emulator)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    // Firebase Auth
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    // Logging
    implementation(libs.timber)

    // Image loading (receipt thumbnails, incl. remote Storage URLs)
    implementation("io.coil-kt:coil:2.7.0")

    // On-device receipt OCR (bundled Latin model, available immediately offline)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation Component
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Background sync
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
}
