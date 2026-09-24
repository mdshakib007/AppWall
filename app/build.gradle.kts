plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.mdshakib007.appwall"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.mdshakib007.appwall"
        minSdk = 29
        targetSdk = 36
        // CI passes these from the git tag (see .github/workflows/release.yml); local builds use the defaults.
        versionCode = (project.findProperty("appVersionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("appVersionName") as String?) ?: "1.0.0"
        vectorDrawables.useSupportLibrary = true
        // English only for now; keeps resources small. Add locales here when translations land.
        resourceConfigurations += listOf("en")
    }

    signingConfigs {
        // Real releases: put keystore details in ~/.gradle/gradle.properties or the environment
        // (APPWALL_KEYSTORE, APPWALL_KEYSTORE_PASSWORD, APPWALL_KEY_ALIAS, APPWALL_KEY_PASSWORD).
        // Without them, release builds are signed with the debug key so they still install for local testing.
        create("release") {
            val ks = (project.findProperty("APPWALL_KEYSTORE") as String?) ?: System.getenv("APPWALL_KEYSTORE")
            if (ks != null && file(ks).exists()) {
                storeFile = file(ks)
                storePassword = (project.findProperty("APPWALL_KEYSTORE_PASSWORD") as String?) ?: System.getenv("APPWALL_KEYSTORE_PASSWORD")
                keyAlias = (project.findProperty("APPWALL_KEY_ALIAS") as String?) ?: System.getenv("APPWALL_KEY_ALIAS")
                keyPassword = (project.findProperty("APPWALL_KEY_PASSWORD") as String?) ?: System.getenv("APPWALL_KEY_PASSWORD")
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/*.version",
                "META-INF/versions/**",
                "kotlin/**",
                "DebugProbesKt.bin",
            )
        }
    }
    dependenciesInfo {
        // Don't embed the Play Store dependency metadata blob (binary, unreadable) — F-Droid friendly.
        includeInApk = false
        includeInBundle = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
