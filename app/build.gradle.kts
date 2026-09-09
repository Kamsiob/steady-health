import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.kamsiob.steadyhealth"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.kamsiob.steadyhealth"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // arm64 for every phone this ships to, and x86_64 so the instrumented
        // tests can run on an ordinary emulator, which is where anything
        // destructive belongs. Play splits an app bundle by ABI, so a phone
        // downloads only its own.
        ndk { abiFilters += setOf("arm64-v8a", "x86_64") }
    }

    signingConfigs {
        // The real key, if it is on this machine, and nothing at all if it is not.
        //
        // Everything comes from a properties file OUTSIDE the repository, named by
        // the STEADY_KEYSTORE_PROPERTIES environment variable and defaulting to
        // ~/.steady/keystore.properties. The repository is public. No path, no
        // password and no alias is written down here, and there is nothing to
        // accidentally commit because there is no file in the tree to edit.
        //
        // A machine without that file still builds, tests and installs; it just
        // cannot produce a bundle Play will take, which is the correct outcome.
        val properties = Properties()
        val describedBy = providers.environmentVariable("STEADY_KEYSTORE_PROPERTIES").orNull
            ?: (System.getProperty("user.home") + "/.steady/keystore.properties")
        val file = File(describedBy)
        if (file.exists()) {
            FileInputStream(file).use { properties.load(it) }
            create("steady") {
                storeFile = File(properties.getProperty("storeFile"))
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ""
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // The real key when this machine has it, and the debug key otherwise so
            // that a release build can still be installed and tested. A debug signed
            // build cannot go to Play, which is the point: the only way to produce
            // something uploadable is to have the keystore, and the keystore lives
            // outside the repository. Creating it is an owner task on the BLOCKED
            // list, and LAUNCH.md says how.
            signingConfig = signingConfigs.findByName("steady")
                ?: signingConfigs.getByName("debug")
        }
    }

    bundle {
        // Every language ships inside the app.
        //
        // By default a bundle splits language resources and Play fetches the rest
        // on demand, which needs Play Core, a network connection, and a store
        // this app does not depend on. Steady Health has to work offline from the
        // moment it is installed, and somebody switching to their own language on
        // a plane should not find English.
        language {
            enableSplit = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Four languages from the first commit, because retrofitting right-to-left
    // is where layout assumptions surface, and they are cheaper to find now.
    androidResources {
        localeFilters += listOf("en", "es", "zh", "ar")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true
        disable += setOf(
            // targetSdk below compileSdk is deliberate; see the version catalog.
            "OldTargetApi",
            // Lint wants an en dash in page ranges and phone numbers. This app
            // ships neither an en dash nor an em dash anywhere a person reads.
            "TypographyDashes",
        )
        // Reported, not fatal, until Phase 7. Right now the unused ones are
        // colours and strings written for screens the next phases build, and
        // deleting work that is about to be used is worse than a check that
        // waits. warningsAsErrors would otherwise promote these back.
        informational += setOf("UnusedResources")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        allWarningsAsErrors.set(true)
    }
}

// The schema JSON is checked in so a migration can be written against exactly
// what shipped rather than against what the code says today.
room3 {
    schemaDirectory("$projectDir/schemas")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    parallel = true
}

dependencies {
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.profileinstaller)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)

    implementation(libs.androidx.datastore.preferences)

    // The widget. ADDENDUM-03 Part 13: two sizes, today's session, tap to start.
    implementation(libs.androidx.glance.appwidget)

    // ADDENDUM-03 Part 5: one button for every piece of paper. The camera takes the
    // photograph and ML Kit reads the text off it, both on device. Nothing about a
    // document ever leaves the phone, which is why the text recognition is the
    // bundled model rather than the Play Services one that downloads.
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    detektPlugins(libs.detekt.formatting)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.compose.ui.test.manifest)
}
