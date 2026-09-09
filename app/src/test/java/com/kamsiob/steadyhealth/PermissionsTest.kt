package com.kamsiob.steadyhealth

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File
import java.util.Properties

/**
 * What the finished app asks the phone for, read off the manifest the build produced
 * rather than off the one somebody typed.
 *
 * The app's whole position is that nothing leaves the phone, and the screens say so at
 * the moment a person is pointing a camera at their own letter or saying out loud what
 * their therapist asked of them. A promise made in a sentence on a screen is worth
 * whatever the system underneath it allows, and INTERNET is the one permission that
 * decides whether it was ever possible to keep. So this reads the merged manifest,
 * which is the one that goes into the APK, and fails the build if it is there.
 *
 * It has been there. ML Kit's text recognition reads a page with a model that ships
 * inside this APK, but it arrives with Google's usage telemetry transport attached,
 * and that library declares INTERNET and sends events of its own accord. Nobody typed
 * it and nobody would have seen it. That is the case this test exists for: the
 * permissions worth checking are the ones that arrive without being asked for.
 */
class PermissionsTest {

    private val requested: List<String> by lazy { permissionsIn(mergedManifest()) }

    @Test
    fun theBuiltAppCannotOpenANetworkConnection() {
        // Every byte an Android app sends over a network goes through this one
        // permission, whoever wrote the code that sends it. Without it the promise is
        // held by the system rather than by everybody remembering.
        assertWithMessage("something has put a network permission back into the app")
            .that(requested)
            .doesNotContain("android.permission.INTERNET")
    }

    @Test
    fun theWaysInAreStillAskedFor() {
        // Pinned the other way as well, so that a test which quietly stopped finding
        // any permissions at all could not go on passing the one above.
        assertThat(requested).containsAtLeast(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.VIBRATE",
        )
    }

    @Test
    fun thereIsSomethingToCheck() {
        assertThat(requested.size).isAtLeast(FOUR)
    }

    /**
     * The manifest the build actually produced, permissions from libraries included.
     *
     * Reading the file in src/main would check only what somebody typed, and the
     * permission this test exists for was never typed by anybody. The Android plugin
     * writes the merged manifest's path into a properties file on the unit test
     * classpath, the same file Robolectric reads, so that is where the path comes from
     * rather than from a guess at the build layout.
     */
    private fun mergedManifest(): File {
        val properties = Properties()
        val stream = javaClass.classLoader?.getResourceAsStream(CONFIG)
            ?: error("$CONFIG is not on the test classpath")
        stream.use { properties.load(it) }
        val relative = properties.getProperty("android_merged_manifest")
            ?: error("$CONFIG names no merged manifest")
        return findFromModule(relative)
    }

    /** Walk up from wherever the test runs until the module is found. */
    private fun findFromModule(relative: String): File {
        var dir: File? = File(".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, relative)
            if (candidate.exists()) return candidate
            val inModule = File(dir, "app/$relative")
            if (inModule.exists()) return inModule
            dir = dir.parentFile
        }
        error("could not find $relative from ${File(".").absolutePath}")
    }

    /**
     * Comments come out first. The merged manifest keeps them, and the block above the
     * removal explains itself using the name of the permission it removes.
     */
    private fun permissionsIn(file: File): List<String> {
        val text = file.readText().replace(COMMENTS, "")
        return NAMED.findAll(text).map { it.groupValues[1] }.toList()
    }

    private companion object {
        const val CONFIG = "com/android/tools/test_config.properties"
        const val FOUR = 4
        val COMMENTS = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
        val NAMED = Regex("""<uses-permission[^>]*android:name="([^"]+)"""")
    }
}
