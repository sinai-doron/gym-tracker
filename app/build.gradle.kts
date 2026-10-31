import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.doron.gymtracker"
    compileSdk = 37

    defaultConfig {
        // The published identity, fixed forever at the first Play upload: dev.neonsheep.<app>
        // for every app (decided 2026-10-31). The Kotlin namespace above is internal and
        // stays as it is. A new applicationId is a different app to Android, so moving
        // from com.doron.gymtracker means Settings → Backup there, Restore here.
        applicationId = "dev.neonsheep.gymtracker"
        minSdk = 26
        // Play requires API 36 for new apps and updates since 2026-08-31. Not 37 yet: that would
        // take on Android 17's behaviour changes untested.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // MapLibre ships native code per CPU type; keep phones (arm64) and the emulator (x86_64)
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }

        // doron-backend: optional sign-in and sync. Staging is apps.neonsheep.dev; for a backend on
        // this computer set backend.url=http://localhost:7300 in local.properties and run
        // `adb reverse tcp:7300 tcp:7300`.
        val backendUrl = localProps.getProperty("backend.url", "https://apps.neonsheep.dev")
        buildConfigField("String", "BACKEND_URL", "\"${backendUrl.trimEnd('/')}\"")
        // The backend's web OAuth client: the audience of every Google ID token. Not a secret.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"381579861019-jdv1n9tsqhlh40iius7liep7p6co8cun.apps.googleusercontent.com\""
        )
    }

    // One fixed key, so an install from any computer updates the app in place instead of
    // requiring an uninstall (which wipes the workout data). The key stays out of this public
    // repo: point signing.storeFile in local.properties at it; without it the default debug
    // key of the current machine is used.
    signingConfigs {
        getByName("debug") {
            localProps.getProperty("signing.storeFile")?.let { path ->
                storeFile = file(path)
                storePassword = localProps.getProperty("signing.storePassword", "android")
                keyAlias = localProps.getProperty("signing.keyAlias", "androiddebugkey")
                keyPassword = localProps.getProperty("signing.keyPassword", "android")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
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

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation
    implementation(libs.navigation.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Health Connect
    implementation(libs.health.connect)

    // Location
    implementation(libs.play.services.location)

    // Maps: MapLibre with OpenFreeMap vector tiles (no API key), styled in assets/map_style_run.json
    implementation(libs.maplibre)

    // Image loading
    implementation(libs.coil.compose)

    // Sign-in and sync with doron-backend
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.tink.android)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.androidx.compiler)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)

    // Instrumented tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// AGP 9 compiles Kotlin itself (no kotlin-android plugin); the JVM target follows compileOptions.
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
