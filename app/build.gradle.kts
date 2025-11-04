@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.tasks.PackageAndroidArtifact
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = if (keystorePropertiesFile.exists() && keystorePropertiesFile.isFile) {
    Properties().apply {
        load(FileInputStream(keystorePropertiesFile))
    }
} else null

fun String.execute(currentWorkingDir: File = file("./")): String {
    val parts = this.split("\\s+".toRegex()).filter { it.isNotBlank() }
    val pb = ProcessBuilder(parts)
        .directory(currentWorkingDir)
        .redirectErrorStream(true)
    val proc = pb.start()
    val output = proc.inputStream.readBytes().toString(Charsets.UTF_8).trim()
    proc.waitFor()
    return output
}

val gitCommitCount = "git rev-list HEAD --count".execute().toInt()

android {
    namespace = "io.github.a13e300.ksuwebui"
    compileSdk = 36

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "io.github.a13e300.ksuwebui"
        minSdk = 26
        targetSdk = 36
        versionCode = gitCommitCount
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSig = signingConfigs.findByName("release")
            signingConfig = if (releaseSig != null) releaseSig else {
                println("use debug signing config")
                signingConfigs["debug"]
            }
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    // https://stackoverflow.com/a/77745844
    tasks.withType<PackageAndroidArtifact> {
        doFirst { appMetadata.asFile.orNull?.writeText("") }
    }
    androidResources {
        generateLocaleConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_23
        targetCompatibility = JavaVersion.VERSION_23
    }
    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(23))
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "**"
        }
    }
}

val vName = android.defaultConfig.versionName
val vCode = android.defaultConfig.versionCode

tasks.configureEach {
    if ((this as? AbstractArchiveTask) != null && (name.contains("package", ignoreCase = true) || name.contains("bundle", ignoreCase = true))) {
        (this as AbstractArchiveTask).archiveBaseName.set("KsuWebUI-$vName-$vCode")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.webkit)
    implementation(libs.material)

    implementation(libs.com.github.topjohnwu.libsu.core)
    implementation(libs.com.github.topjohnwu.libsu.service)
    implementation(libs.com.github.topjohnwu.libsu.io)
}
