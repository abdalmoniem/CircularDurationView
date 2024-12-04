import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)

    id("maven-publish")
}

val packageName = "com.hifnawy.circulardurationview"
val githubProperties = Properties()
val githubPropertiesFile = rootProject.file("github.properties")

android {
    namespace = packageName
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        version = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    libraryVariants.all {
        outputs.all {
            this as BaseVariantOutputImpl
            val apkName = "${packageName}_${buildType.name}_v${version}.aar"

            outputFileName = apkName
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.hifnawy"
            artifactId = "circulardurationview"
            version = project.version as String

            afterEvaluate {
                artifact(layout.buildDirectory.file("outputs/aar/${packageName}_release_v${version}.aar"))
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/abdalmoniem/CircularDurationView")

            if (githubPropertiesFile.exists()) {
                githubProperties.load(FileInputStream(githubPropertiesFile))

                credentials {
                    val usr = githubProperties["gpr.usr"] as? String
                    val key = githubProperties["gpr.key"] as? String

                    if (usr == null && key == null) throw GradleException("gpr.usr and gpr.key are not set in the github.properties file")

                    username = usr
                    password = key
                }
            }
        }
    }
}

tasks.named("publishMavenPublicationToMavenLocal").configure {
    dependsOn(tasks.named("bundleReleaseAar"))
}