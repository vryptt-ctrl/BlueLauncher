plugins {
    id("com.android.application")
}

val blueNdkVersion = "27.2.12479018"
val rustTargets = linkedMapOf(
    "aarch64-linux-android" to "arm64-v8a",
    "armv7-linux-androideabi" to "armeabi-v7a",
    "x86_64-linux-android" to "x86_64",
)
val rustManifest = layout.projectDirectory.file("src/main/rust/Cargo.toml")
val rustSource = layout.projectDirectory.dir("src/main/rust/src")

fun ndkRoot(): File {
    val configured = providers.environmentVariable("ANDROID_NDK_HOME").orNull
        ?: providers.environmentVariable("ANDROID_NDK_ROOT").orNull
    if (configured != null) return file(configured)
    val sdk = providers.environmentVariable("ANDROID_HOME").orNull
        ?: providers.environmentVariable("ANDROID_SDK_ROOT").orNull
        ?: throw GradleException("Set ANDROID_HOME or ANDROID_NDK_HOME so the Rust build can find the NDK.")
    return file("$sdk/ndk/$blueNdkVersion")
}

fun toolchainBin(): File {
    val host = when {
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true) -> "windows-x86_64"
        System.getProperty("os.name").startsWith("Mac", ignoreCase = true) -> "darwin-x86_64"
        else -> "linux-x86_64"
    }
    return file("${ndkRoot()}/toolchains/llvm/prebuilt/$host/bin")
}

fun linkerFor(target: String, bin: File): String {
    val suffix = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) ".cmd" else ""
    return when (target) {
        "aarch64-linux-android" -> "${bin.absolutePath}/aarch64-linux-android23-clang$suffix"
        "armv7-linux-androideabi" -> "${bin.absolutePath}/armv7a-linux-androideabi23-clang$suffix"
        "x86_64-linux-android" -> "${bin.absolutePath}/x86_64-linux-android23-clang$suffix"
        else -> error("Unsupported Rust target: $target")
    }
}

val buildRust = tasks.register("buildRust") {
    group = "build"
    description = "Compiles the embedded Rust JNI core for each packaged TV ABI."
}

rustTargets.forEach { (target, abi) ->
    val outputLibrary = layout.buildDirectory.file("rust-jni/$abi/libblue_launcher.so")
    val compile = tasks.register<Exec>("buildRust-$abi") {
        group = "build"
        inputs.file(rustManifest)
        inputs.dir(rustSource)
        outputs.file(outputLibrary)
        doFirst {
            val bin = toolchainBin()
            if (!bin.isDirectory) {
                throw GradleException("Android NDK $blueNdkVersion is missing. Install it and retry:\n" +
                    "  sdkmanager \"ndk;$blueNdkVersion\"")
            }
            environment("CARGO_TARGET_DIR", layout.buildDirectory.dir("cargo").get().asFile.absolutePath)
            environment("CARGO_PROFILE_RELEASE_PANIC", "abort")
            environment("CARGO_PROFILE_RELEASE_LTO", "true")
            environment("CARGO_PROFILE_RELEASE_CODEGEN_UNITS", "1")
            environment("CARGO_PROFILE_RELEASE_STRIP", "symbols")
            environment("CARGO_TARGET_${target.uppercase().replace('-', '_')}_LINKER", linkerFor(target, bin))
            // Android 15+ devices may use 16 KB memory pages; 32-bit ARM keeps its 4 KB layout.
            environment(
                "RUSTFLAGS",
                if (target == "armv7-linux-androideabi") ""
                else "-C link-arg=-Wl,-z,max-page-size=16384",
            )
        }
        commandLine(
            "cargo", "build",
            "--manifest-path", rustManifest.asFile.absolutePath,
            "--target", target,
            "--release", "--locked",
        )
        doLast {
            val produced = layout.buildDirectory.file("cargo/$target/release/libblue_launcher.so").get().asFile
            val destination = outputLibrary.get().asFile
            destination.parentFile.mkdirs()
            produced.copyTo(destination, overwrite = true)
        }
    }
    buildRust.configure { dependsOn(compile) }
}

android {
    namespace = "com.bluelabs.bluelauncher"
    compileSdk = 36
    ndkVersion = blueNdkVersion

    defaultConfig {
        applicationId = "com.bluelabs.bluelauncher"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.1"
        ndk {
            abiFilters += rustTargets.values
        }
    }

    sourceSets.named("main") {
        jniLibs.directories.add("build/rust-jni")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs.useLegacyPackaging = false
        resources.excludes += setOf("META-INF/LICENSE*", "META-INF/NOTICE*")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = true
        warningsAsErrors = false
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }.configureEach {
    dependsOn(buildRust)
}

dependencies {
    // Platform dialogs and Views cover the entire UI; this is only needed for lifecycle-safe results.
    implementation("androidx.activity:activity:1.13.0")

    testImplementation("junit:junit:4.13.2")
}
