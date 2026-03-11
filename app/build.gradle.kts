import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// 1. Load project-level properties (Versioning, Names, Paths)
val localPropsFile = project.file("keystore/keystore.properties")
val localProps = Properties()
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}

// 2. Resolve organization-level paths and credentials
val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
val sdkParent = if (androidHome != null) file(androidHome).parentFile else null

// Organization directory (D:\AppDevops\airobot)
val orgDir = if (sdkParent != null && localProps.containsKey("org_path")) {
    sdkParent.resolve(localProps.getProperty("org_path"))
} else null

// Organization properties (Passwords)
val orgProps = Properties()
if (orgDir != null && localProps.containsKey("props_relative_path")) {
    val orgPropsFile = orgDir.resolve(localProps.getProperty("props_relative_path"))
    if (orgPropsFile.exists()) {
        orgProps.load(orgPropsFile.inputStream())
    }
}

// Organization keystore file
val orgKeystoreFile = if (orgDir != null && localProps.containsKey("keystore_relative_path")) {
    orgDir.resolve(localProps.getProperty("keystore_relative_path"))
} else null

// 3. Project Metadata Constants
val appName = localProps.getProperty("app_name") ?: "AiRobot-Assistant"
val verName = localProps.getProperty("version_name") ?: "1.0.0"
val verCode = (localProps.getProperty("version_code") ?: "10020").toInt()

android {
    namespace = "com.airobot.assistant"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.airobot.assistant"
        minSdk = 29
        targetSdk = 36
        versionCode = verCode
        versionName = verName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        // Only create release config if both local and org properties exist
        if (localPropsFile.exists() && orgProps.containsKey("storePassword")) {
            create("release") {
                storeFile = orgKeystoreFile
                storePassword = orgProps.getProperty("storePassword")
                keyAlias = orgProps.getProperty("keyAlias")
                keyPassword = orgProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Apply signing if the keystore file exists
            if (orgKeystoreFile?.exists() == true && orgProps.containsKey("storePassword")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        prefab = true
    }
}

// 4. APK Renaming via PackageApplication task
tasks.withType<com.android.build.gradle.tasks.PackageApplication>().configureEach {
    val task = this
    doLast {
        if (task.name.contains("Release")) {
            val outputDir = task.outputDirectory.get().asFile
            outputDir.walk().filter { it.extension == "apk" }.forEach { file ->
                val abi = when {
                    file.name.contains("arm64-v8a") -> "arm64-v8a"
                    file.name.contains("armeabi-v7a") -> "armeabi-v7a"
                    file.name.contains("x86_64") -> "x86_64"
                    file.name.contains("x86") -> "x86"
                    else -> "universal"
                }
                
                val newName = "${appName}_v${verName}_${verCode}_${abi}_release.apk"
                val destination = file.parentFile.resolve(newName)
                if (file.name != newName) {
                    file.renameTo(destination)
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(files("libs/sherpa-onnx-1.12.28.aar"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.icons.extended)
    implementation(libs.opus.v131)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.accompanist.permissions)
    implementation(libs.noise)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
