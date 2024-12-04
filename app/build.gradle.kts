import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

/**
 * The package name of the application.
 *
 * This constant holds the package name used throughout the application.
 * It is primarily used for identifying the application's namespace in
 * Android and is essential for intents, broadcasting, and other system
 * interactions.
 */
private val packageName = "com.hifnawy.circulardurationview.demo"

/**
 * A DateTimeFormatter for formatting the current date and time.
 *
 * This formatter is used to generate a string representation of the current date and time
 * in the format "dd/MMM/yyyy_hh:mm:ss.S a". This format is used to log the start of the build
 * process.
 *
 * @see LocalDateTime
 * @see DateTimeFormatter
 */
private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy_hh:mm:ss.S a")

/**
 * The current date and time at the start of the build process.
 *
 * This field is used to generate a string representation of the current date and time
 * in the format "dd/MMM/yyyy_hh:mm:ss.S a". This format is used to log the start of the build
 * process.
 *
 * @see LocalDateTime
 * @see DateTimeFormatter
 */
private val buildDateAndTime = LocalDateTime.now()

/**
 * The file object representing the local.properties file in the root project directory.
 *
 * This file is used to store custom configuration properties for the project, such as
 * signing configurations, debugging options, and other local settings. The properties
 * are loaded during the build process to configure the build environment.
 *
 * @see Properties
 * @see FileInputStream
 */
private val localPropertiesFile = rootProject.file("local.properties")

/**
 * A flag indicating whether debugging is enabled in the release variant.
 *
 * @see ApplicationBuildType.isDebuggable
 */
private var isDebuggingEnabled = false

/**
 * A flag indicating whether signing is enabled in the release variant.
 *
 * @see ApplicationBuildType.signingConfig
 */
private var isSigningConfigEnabled = false

project.logger.lifecycle("INFO: Build Started at: ${buildDateAndTime.format(dateTimeFormatter)}")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    if (localPropertiesFile.exists()) {
        val keystoreProperties = Properties().apply { load(FileInputStream(localPropertiesFile)) }
        val signingProperties = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")

        isSigningConfigEnabled =
                signingProperties.all { property -> property in keystoreProperties.keys } &&
                rootProject.file(keystoreProperties["storeFile"] as String).exists()

        when {
            !isSigningConfigEnabled -> {
                signingProperties
                    .filter { property -> property !in keystoreProperties.keys }
                    .forEach { missingKey -> project.logger.warn("WARNING: missing key in '${localPropertiesFile.absolutePath}': $missingKey") }
            }

            else                    -> {
                signingConfigs {
                    project.logger.lifecycle("INFO: keystore: ${rootProject.file(keystoreProperties["storeFile"] as String).absolutePath}")

                    create("release") {
                        keyAlias = keystoreProperties["keyAlias"] as String
                        keyPassword = keystoreProperties["keyPassword"] as String
                        storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                        storePassword = keystoreProperties["storePassword"] as String
                    }
                }
            }
        }

        isDebuggingEnabled =
                keystoreProperties.getProperty("isDebuggingEnabled")?.toBoolean() ?: false
    } else {
        project.logger.warn("WARNING: local.properties not found, add local.properties in root directory to enable signing.")
    }

    defaultConfig {
        namespace = packageName
        applicationId = namespace

        minSdk = 24
        compileSdk = 35
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
        versionCode = 32
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BUILD_DATE_AND_TIME", "\"${buildDateAndTime.format(dateTimeFormatter)}\"")
    }

    sourceSets.forEach { sourceSet ->
        sourceSet.java.srcDir("src/$sourceSet.name")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = isDebuggingEnabled

            project.logger.lifecycle("INFO: $name isDebuggable: $isDebuggingEnabled")

            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )

            signingConfigs.findByName("release")?.also { signingConfiguration ->
                when {
                    isSigningConfigEnabled -> {
                        signingConfig = signingConfiguration
                        project.logger.lifecycle("INFO: $name buildType is signed with release signing config.")
                        project.logger.lifecycle("INFO: $name signing config is located in ${signingConfiguration.storeFile?.absolutePath}")
                    }

                    else                   -> project.logger.warn(
                            "WARNING: $name buildType is not signed, " +
                            "add signing config in local.properties to enable signing."
                    )
                }
            } ?: project.logger.error("ERROR: $name signing config not found, add signing config in local.properties")
        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )

            signingConfigs.findByName("release")?.also { signingConfiguration ->
                when {
                    isSigningConfigEnabled -> {
                        signingConfig = signingConfiguration
                        project.logger.lifecycle("INFO: $name buildType is signed with release signing config.")
                        project.logger.lifecycle("INFO: $name signing config is located in ${signingConfiguration.storeFile?.absolutePath}")
                    }

                    else                   -> project.logger.lifecycle(
                            "INFO: $name buildType is signed with default signing config, " +
                            "add signing config in local.properties to enable signing."
                    )
                }
            } ?: project.logger.lifecycle(
                    "INFO: $name buildType is signed with default signing config, " +
                    "add signing config in local.properties to enable signing."
            )

            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            this as BaseVariantOutputImpl
            val applicationId =
                    if (variant.buildType.name == "release") "$applicationId.release" else applicationId
            val apkName =
                    "${applicationId}_${variant.buildType.name}_v${android.defaultConfig.versionName}.apk"

            outputFileName = apkName
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    dataBinding {
        enable = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(project(":CircularDurationView"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}