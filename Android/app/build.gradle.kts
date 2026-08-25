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
// F-Droid builds disable this to omit the CASIA-trained handwriting model.
// The runtime source stays a normal build dependency, but the UI cannot
// reach it and no handwriting model is bundled or loaded.
val includeHandwriting = providers.gradleProperty("includeHandwriting")
    .map { it.toBoolean() }
    .orElse(true)
    .get()
val requiredHandwrittenRoot = rootProject.file("../require/Handwritten")
val requiredSimeRoot = rootProject.file("../require/Sime")
val simeRoot = project.findProperty("simeEngineRoot")?.let { file(it.toString()) }
    ?: System.getenv("SIME_ENGINE_ROOT")?.let(::file)
    ?: requiredSimeRoot.takeIf { it.isDirectory }
    ?: rootProject.file("../../Sime")
val simeAssetsRoot = simeRoot.resolve("save")
val requiredHandwrittenUsable = requiredHandwrittenRoot
    .resolve("android/runtime/src/main/java").isDirectory
    && (!includeHandwriting || requiredHandwrittenRoot
        .resolve("android/app/src/main/assets").isDirectory)
val handwrittenRoot = project.findProperty("handwrittenRoot")?.let { file(it.toString()) }
    ?: System.getenv("HANDWRITTEN_ROOT")?.let(::file)
    ?: requiredHandwrittenRoot.takeIf { requiredHandwrittenUsable }
    ?: rootProject.file("../../Handwritten")
val requiredNcnnRoot = rootProject.file("../require/ncnn")
require(simeAssetsRoot.resolve("sime.cnt").isFile
        && simeAssetsRoot.resolve("sime.dict").isFile) {
    "Sime runtime model bundle not found at $simeAssetsRoot."
}
require(handwrittenRoot.resolve("android/runtime/src/main/java").isDirectory) {
    "Handwritten runtime not found at $handwrittenRoot; set HANDWRITTEN_ROOT or -PhandwrittenRoot."
}
if (includeHandwriting) {
    require(handwrittenRoot.resolve("android/app/src/main/assets").isDirectory) {
        "Handwritten model assets not found at $handwrittenRoot."
    }
}

android {
    namespace = "com.shiyu.sime"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shiyu.sime"
        minSdk = 24
        targetSdk = 35
        versionCode = 71
        versionName = "0.17.2"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                arguments(
                    "-DSIME_NCNN_SOURCE_DIR=${if (requiredNcnnRoot.isDirectory) requiredNcnnRoot else rootProject.layout.buildDirectory.get().asFile.resolve("ncnn-20260526")}",
                    "-DSIME_ENGINE_ROOT=${simeRoot.absolutePath}",
                    "-DHANDWRITTEN_ROOT=${handwrittenRoot.absolutePath}"
                )
            }
        }
        buildConfigField("boolean", "INCLUDE_HANDWRITING", includeHandwriting.toString())
    }

    val storeFilePath = keystoreProperties.getProperty("storeFile")
    val hasKeystore = storeFilePath != null && file(storeFilePath).exists()

    signingConfigs {
        create("release") {
            if (hasKeystore) {
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
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
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
            assets.srcDir(simeAssetsRoot)
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
