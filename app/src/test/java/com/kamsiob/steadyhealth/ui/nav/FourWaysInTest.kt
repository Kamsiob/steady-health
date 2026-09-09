package com.kamsiob.steadyhealth.ui.nav

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * Four ways in, one door out. ADDENDUM-03 Part 6.
 *
 * "All four end in the same confirmation screen where every item is shown with what
 * the app matched it to, and nothing is saved unconfirmed." Both halves of that are
 * arrangement rather than logic, so there is nothing an ordinary test can call. This
 * reads the source instead, which is crude and is the only way to check it without a
 * device, and it is the same trick [RoutesTest] uses for the same reason.
 *
 * The failure it is here to catch is a fifth way in, or a shortcut added later, that
 * writes a plan and skips the screen where the person agrees to it. That would be
 * invisible in every other test: the plan would save, the card would show it, and
 * nobody would have confirmed a line of it.
 */
class FourWaysInTest {

    @Test
    fun allFourWaysEndOnTheConfirmationScreen() {
        val app = source("ui/SteadyApp.kt")

        // The camera's way in is the page it has just read; the other three are their
        // own screens. Each one leaves for the same route and no other.
        listOf("PAGE_FOUND", "PLAN_TYPE", "PLAN_PICK", "PLAN_SAY").forEach { route ->
            assertWithMessage("$route must end where the other ways in end")
                .that(routeBlock(app, route))
                .contains("Route.PLAN_CONFIRM")
        }
    }

    @Test
    fun thereIsOnlyOneConfirmationScreenForAllOfThem() {
        val app = source("ui/SteadyApp.kt")

        assertThat(app.windowed("composable(Route.PLAN_CONFIRM)")).isEqualTo(1)
        assertThat(app.windowed("PlanConfirmScreen(")).isEqualTo(1)
    }

    @Test
    fun nothingWritesAPlanExceptTheSaveOnThatScreen() {
        val scan = source("ui/scan/ScanViewModel.kt")

        // One call, and it is the one the save button makes. A second would be a way
        // in that had decided it did not need confirming.
        assertWithMessage("a plan is written in exactly one place")
            .that(scan.windowed("plans.save("))
            .isEqualTo(1)
        assertThat(scan.substringAfter("fun savePlan")).contains("plans.save(")

        val app = source("ui/SteadyApp.kt")
        assertThat(app.windowed("savePlan")).isEqualTo(1)
        assertThat(routeBlock(app, "PLAN_CONFIRM")).contains("savePlan")
    }

    @Test
    fun theThreeWaysWithNoPageBehindThemHoldNothingThatCouldWrite() {
        val ways = source("ui/scan/PlanWaysViewModel.kt")

        // They build items and hand them over. Giving this one a plan repository is
        // how "nothing is saved unconfirmed" would quietly stop being true.
        assertThat(ways).doesNotContain("PlanRepository")
        assertThat(ways).doesNotContain("PlanItemEntity")
    }

    /** One `composable(Route.X)` block, up to wherever the next one starts. */
    private fun routeBlock(source: String, route: String): String {
        val from = source.indexOf("composable(Route.$route)")
        assertWithMessage("Route.$route has no screen").that(from).isAtLeast(0)
        val next = source.indexOf("composable(Route.", from + 1)
        return if (next < 0) source.substring(from) else source.substring(from, next)
    }

    private fun String.windowed(text: String): Int = split(text).size - 1

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
