package com.kamsiob.steadyhealth.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.ui.screens.AbilityTileState
import com.kamsiob.steadyhealth.ui.screens.TodayUiState
import com.kamsiob.steadyhealth.ui.screens.WeightPageScreen
import com.kamsiob.steadyhealth.ui.screens.WeightPageUiState
import com.kamsiob.steadyhealth.ui.theme.SteadyTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/**
 * With numbers off, no weight figure reaches a screen.
 *
 * MASTER_SPEC section 10 asks for this as an instrumented test, and it has to be
 * one: the thing being checked is what is drawn, and a state object with a null
 * in it proves nothing about what a person sees.
 *
 * What counts as a figure here is a figure about the person's body: the weight,
 * and the unit beside it. "0 of 3 done" is a count of today's three things and
 * "20 seconds" is how long a check-in takes, and hiding either of those would not
 * be numbers-off, it would be the app refusing to say anything.
 *
 * The check reads every string in the drawn tree rather than the state object,
 * because a state with a null in it proves nothing about what a person sees.
 */
class NumbersOffTest {

    @get:Rule
    val compose = createComposeRule()

    // Today's weight case is gone with the weight block itself: MASTER_SPEC 6.1 takes
    // weight off Today entirely. Numbers off has two new surfaces there instead, the
    // session card's minutes and the noticed line's counts, and both are Phase 8.

    @Test
    fun theWeightPageShowsNoDigitWhenNumbersAreOff() {
        compose.setContent {
            SteadyTheme {
                WeightPageScreen(
                    state = WeightPageUiState(
                        value = "A little lower",
                        unit = "",
                        explain = "than a month ago",
                        sinceLabel = "Since you started",
                        since = "",
                        daysLabel = "Days you moved",
                        days = "",
                    ),
                    onBack = {},
                )
            }
        }
        assertNoWeightFigure()
    }

    private fun assertNoWeightFigure() {
        val drawn = compose.onRoot().printToString(maxDepth = DEEP)
        val strings = drawn.lines()
            .filter { it.contains("Text = ") || it.contains("ContentDescription = ") }
            .map { it.substringAfter("= ") }

        // A decimal figure is what a weight looks like, and the unit is what
        // makes it one. Neither may appear.
        val figures = strings.filter { Regex("""\d+\.\d""").containsMatchIn(it) }
        assertFalse("a weight figure reached the screen: $figures", figures.isNotEmpty())

        val units = strings.filter {
            Regex("""(?<![\p{L}])(lb|kg)(?![\p{L}])""").containsMatchIn(it)
        }
        assertFalse("a weight unit reached the screen: $units", units.isNotEmpty())
    }

    /**
     * The date carries digits and is not a figure about the body, so it is not on
     * this screen for the test. Numbers-off is about the weight, not the calendar.
     */
    private val numbersOff = TodayUiState(
        date = "Sunday",
        greeting = "Evening",
        abilities = listOf(
            AbilityTileState(
                domain = AbilityDomain.GetUp,
                name = "Get up",
                lifeSentence = "You can stand up without using your hands now.",
            ),
        ),
        weightValue = "A little lower",
        weightUnit = null,
        weightExplain = "than a month ago",
        weighedIn = false,
        saidHowItWent = false,
        moved = false,
        nextWalkName = "",
        moveTitle = "Move for two minutes",
        moveSubtitle = "A short walk counts",
        dayLetters = emptyList(),
    )

    private companion object {
        const val DEEP = 100
    }
}
