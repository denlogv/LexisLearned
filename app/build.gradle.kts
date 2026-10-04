import java.util.Properties
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    jacoco
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

// ---- Release signing ----------------------------------------------------------------------------
// Nothing secret lives in the repository. keystore.properties (git-ignored) only names the keystore file
// and key alias. The passwords are read at signing time from environment variables or, on macOS, from
// the Keychain, and only when a release task is actually requested. See the README, "Signing a release".
val releaseRequested = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true) || it.contains("bundle", ignoreCase = true)
}
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun secret(envName: String, keychainService: String): String? {
    System.getenv(envName)?.takeIf { it.isNotEmpty() }?.let { return it }
    return runCatching {
        val p = ProcessBuilder("security", "find-generic-password", "-s", keychainService, "-w").start()
        val out = p.inputStream.bufferedReader().readText().trim()
        if (p.waitFor() == 0 && out.isNotEmpty()) out else null
    }.getOrNull()
}

base.archivesName.set("LexisLearned")

android {
    namespace = "dev.denlogv.lexislearned"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.denlogv.lexislearned"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-pre1"
    }

    signingConfigs {
        if (releaseRequested) {
            val storePath = keystoreProps.getProperty("storeFile")
            val alias = keystoreProps.getProperty("keyAlias")
            if (storePath == null || alias == null) {
                throw GradleException(
                    "Release signing is not set up: create keystore.properties with storeFile and keyAlias " +
                        "(see keystore.properties.example and docs/RELEASING.md).",
                )
            }
            val storePassword = secret("LEXIS_STORE_PASSWORD", "lexislearned-keystore")
                ?: throw GradleException(
                    "No keystore password found. Set LEXIS_STORE_PASSWORD, or store it in the macOS Keychain " +
                        "with tools/setup-release-signing.sh (see docs/RELEASING.md).",
                )
            create("release") {
                storeFile = file(storePath.replaceFirst(Regex("^~"), System.getProperty("user.home")))
                keyAlias = alias
                this.storePassword = storePassword
                // Same as the store password unless a separate key password was provided.
                keyPassword = secret("LEXIS_KEY_PASSWORD", "lexislearned-key") ?: storePassword
            }
        }
    }

    buildTypes {
        debug {
            // A separate application id lets a debug build live next to the signed release on one phone, with its own data.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all { test ->
                // Robolectric loads classes without source locations; JaCoCo must still see them.
                test.extensions.configure<JacocoTaskExtension> {
                    isIncludeNoLocationClasses = true
                    excludes = listOf("jdk.internal.*")
                }
            }
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)
    debugImplementation(libs.compose.ui.tooling)

    detektPlugins(project(":detekt-rules"))

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.coroutines.test)
}

ktlint {
    version.set("1.5.0")
    android.set(true)
    ignoreFailures.set(false)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt.yml"))
    parallel = true
}

// ---- Test coverage --------------------------------------------------------------------------------
// The 80% line-coverage gate (AGENTS.md) covers all code except what cannot run in a JVM unit test:
// generated code, Compose screens and theme, the Activity and Application classes, and the Keystore-backed
// secret store. UI logic therefore lives in view models and plain functions, which are covered.
jacoco {
    toolVersion = "0.8.12"
}

val coverageExclusions = listOf(
    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    "**/*_Impl*.*", "**/*\$\$serializer.class", "**/ComposableSingletons*.*",
    "**/ui/**/*Screen*.*", "**/ui/**/*Components*.*", "**/ui/**/*Navigation*.*", "**/ui/Theme*.*",
    "**/MainActivity*.*", "**/LexisLearnedApp*.*", "**/data/KeystoreSecrets*.*",
)
val debugClasses = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) { exclude(coverageExclusions) }
val coverageData = layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates the HTML and XML coverage report for the unit tests."
    dependsOn("testDebugUnitTest")
    executionData.setFrom(coverageData)
    classDirectories.setFrom(debugClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Fails if unit-test line coverage is below 80%."
    dependsOn("testDebugUnitTest")
    executionData.setFrom(coverageData)
    classDirectories.setFrom(debugClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
