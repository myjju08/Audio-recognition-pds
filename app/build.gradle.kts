import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    // Firebase - uncomment when google-services.json is added
    // alias(libs.plugins.google.services)
    // alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.earbrief.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.earbrief.app"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val props = Properties()
            val localProps = rootProject.file("local.properties")
            if (localProps.exists()) props.load(localProps.inputStream())

            val releaseStoreFile =
                System.getenv("RELEASE_STORE_FILE")
                    ?: props.getProperty("RELEASE_STORE_FILE", "earbrief-debug.keystore")
            storeFile = file(releaseStoreFile)
            storePassword =
                System.getenv("RELEASE_STORE_PASSWORD")
                    ?: props.getProperty("RELEASE_STORE_PASSWORD", "earbrief123")
            keyAlias =
                System.getenv("RELEASE_KEY_ALIAS")
                    ?: props.getProperty("RELEASE_KEY_ALIAS", "earbrief")
            keyPassword =
                System.getenv("RELEASE_KEY_PASSWORD")
                    ?: props.getProperty("RELEASE_KEY_PASSWORD", "earbrief123")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // API keys from local.properties (not committed)
            buildConfigField("String", "DEEPGRAM_API_KEY", "\"${findProperty("DEEPGRAM_API_KEY") ?: ""}\"")
            buildConfigField("String", "ELEVENLABS_API_KEY", "\"${findProperty("ELEVENLABS_API_KEY") ?: ""}\"")
            buildConfigField("String", "CLAUDE_API_KEY", "\"${findProperty("CLAUDE_API_KEY") ?: ""}\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "DEEPGRAM_API_KEY", "\"${findProperty("DEEPGRAM_API_KEY") ?: ""}\"")
            buildConfigField("String", "ELEVENLABS_API_KEY", "\"${findProperty("ELEVENLABS_API_KEY") ?: ""}\"")
            buildConfigField("String", "CLAUDE_API_KEY", "\"${findProperty("CLAUDE_API_KEY") ?: ""}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
}

ksp {
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime)

    // Lifecycle
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.lifecycle.service)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // DataStore
    implementation(libs.androidx.datastore)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.bundles.ktor)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)

    // ML / AI
    implementation(libs.onnxruntime.android)

    // Firebase (uncomment when google-services.json is added)
    // implementation(platform(libs.firebase.bom))
    // implementation(libs.firebase.analytics)
    // implementation(libs.firebase.crashlytics)
    // implementation(libs.firebase.auth)
    // implementation(libs.firebase.config)
    // implementation(libs.firebase.perf)

    // Image
    implementation(libs.coil.compose)

    // Security
    implementation(libs.security.crypto)

    // Testing
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit5.engine)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.uiautomator)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
