package com.kamsiob.steadyhealth.ui.nav

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * Every route has a screen, and every screen has a route.
 *
 * This exists because it did not, and a route was declared and navigated to with
 * no `composable` registered for it, which is a crash the compiler cannot see and
 * no other test would have caught. It reads the source rather than the running
 * app, which is crude and is also the only way to check it without a device.
 */
class RoutesTest {

    @Test
    fun everyRouteHasAScreenBehindIt() {
        val declared = Regex("""const val (\w+) = "([^"]+)"""")
            .findAll(source("ui/nav/Tabs.kt"))
            .map { it.groupValues[1] }
            .filterNot { it == "TABS" }
            .toList()

        assertThat(declared).isNotEmpty()

        val app = source("ui/SteadyApp.kt")
        declared.forEach { name ->
            assertWithMessage("Route.$name is navigated to; something must draw it")
                .that(app)
                .contains("composable(Route.$name)")
        }
    }

    @Test
    fun nothingNavigatesToARouteThatDoesNotExist() {
        val declared = Regex("""const val (\w+) = """")
            .findAll(source("ui/nav/Tabs.kt"))
            .map { it.groupValues[1] }
            .toSet()

        val used = Regex("""Route\.(\w+)""")
            .findAll(source("ui/SteadyApp.kt"))
            .map { it.groupValues[1] }
            .toSet()

        assertThat(declared).containsAtLeastElementsIn(used)
    }

    @Test
    fun thereAreOnlyEverThreeTabs() {
        // MASTER_SPEC section 5 makes this a rule rather than a layout. A fourth
        // would be somewhere for a feature to hide instead of belonging.
        assertThat(Tab.entries).hasSize(3)
    }

    private fun source(relative: String): String {
        val path = "src/main/java/com/kamsiob/steadyhealth/$relative"
        var dir: File? = File(".").absoluteFile
        while (dir != null) {
            listOf(File(dir, path), File(dir, "app/$path")).forEach {
                if (it.exists()) return it.readText()
            }
            dir = dir.parentFile
        }
        error("could not find $path")
    }
}
