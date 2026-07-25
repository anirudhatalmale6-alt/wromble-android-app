plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dk.wromble.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wromble.order"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "2.0.1"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("upload") {
            // Registreret upload-noegle for com.wromble.order (Play kraever SHA1 61:3F:84...),
            // samme noegle som den oprindelige app blev signeret med (alias wromble-key).
            val ks = file("wromble-release.keystore")
            if (ks.exists()) {
                storeFile = ks
                storePassword = "Wromble2024Release"
                keyAlias = "wromble-key"
                keyPassword = "Wromble2024Release"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val upload = signingConfigs.getByName("upload")
            if (upload.storeFile != null) signingConfig = upload
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        // 16 KB-sidestoerrelse: udpak native libs saa de kan laegges 16KB-justeret (Android 15+ krav)
        jniLibs { useLegacyPackaging = false }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.02")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Maps (OpenStreetMap – ingen API-noegle noedvendig)
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // QR-scanner (CameraX + ML Kit barcode)
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Biometrisk app-laas (Face/fingeraftryk)
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Placering (platform LocationManager bruges – ingen ekstra dep)

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
