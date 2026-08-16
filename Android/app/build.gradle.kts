import java.util.Properties

plugins {
    id("com.android.application")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val handwrittenRoot = project.findProperty("handwrittenRoot")?.let { file(it.toString()) }
    ?: System.getenv("HANDWRITTEN_ROOT")?.let(::file)
    ?: rootProject.file("../../Handwritten")
// F-Droid builds disable this to omit the CASIA-trained handwriting model.
// The runtime source stays a normal build dependency, but the UI cannot
// reach it and no handwriting model is bundled or loaded.
val includeHandwriting = providers.gradleProperty("includeHandwriting")
    .map { it.toBoolean() }
    .orElse(true)
    .get()
require(handwrittenRoot.resolve("android/runtime/src/main/java").isDirectory
        && handwrittenRoot.resolve("android/app/src/main/assets").isDirectory) {
    "Handwritten runtime not found at $handwrittenRoot; set HANDWRITTEN_ROOT or -PhandwrittenRoot."
}

android {
    namespace = "com.shiyu.sime"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shiyu.sime"
        minSdk = 24
        targetSdk = 35
        versionCode = 69
        versionName = "0.17.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                arguments(
                    "-DSIME_NCNN_SOURCE_DIR=${rootProject.layout.buildDirectory.get().asFile}/ncnn-20260526",
                    "-DHANDWRITTEN_ROOT=${handwrittenRoot.absolutePath}"
                )
            }
        }
        buildConfigField("boolean", "INCLUDE_HANDWRITING", includeHandwriting.toString())
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProperties.getProperty("storeFile")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Use Release optimization for native C++ even in debug APK
            externalNativeBuild {
                cmake {
                    arguments("-DCMAKE_BUILD_TYPE=Release")
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
            version = "3.22.1+"
        }
    }

    ndkVersion = "30.0.14904198"

    buildFeatures {
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            java.srcDir(handwrittenRoot.resolve("android/runtime/src/main/java"))
            if (includeHandwriting) {
                assets.srcDir(handwrittenRoot.resolve("android/app/src/main/assets"))
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core:1.15.0")
    testImplementation("junit:junit:4.13.2")
}
