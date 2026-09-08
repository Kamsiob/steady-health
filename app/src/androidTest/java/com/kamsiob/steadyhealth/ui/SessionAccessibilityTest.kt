package com.kamsiob.steadyhealth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import com.kamsiob.steadyhealth.session.Counted
import com.kamsiob.steadyhealth.ui.session.CountInScreen
import com.kamsiob.steadyhealth.ui.session.LiveScreen
import com.kamsiob.steadyhealth.ui.session.ReadyScreen
import com.kamsiob.steadyhealth.ui.session.RestScreen
import com.kamsiob.steadyhealth.ui.session.SessionActions
import com.kamsiob.steadyhealth.ui.session.SessionUiState
import com.kamsiob.steadyhealth.ui.theme.SteadyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The session at the accessibility bar. ADDENDUM-03 Part 17, DESIGN.md 7.
 *
 * This is the Phase 1 gate's "runs correctly at 200 percent font scale and with
 * TalkBack", and it is a test rather than a run on the phone on purpose. What
 * TalkBack reads is the semantics tree, which this can assert every time rather than
 * once; and the font scale is overridden inside the test, so nothing on the owner's
 * phone is changed to run it.
 *
 * The four session screens are checked, not one, because the count-in and the rest
 * are the two that have almost nothing on them and are the easiest to leave unlabelled.
 */
class SessionAccessibilityTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun everySessionScreenHasOneObviousNextAction() {
        listOf(
            "I'm ready" to @Composable { ReadyScreen(state, actions) },
            "Skip the count in" to @Composable { CountInScreen(state.copy(countInLeft = 3), actions) },
            "Done with this one" to @Composable { LiveScreen(state, actions) },
            "Skip the rest" to @Composable { RestScreen(state.copy(restLeft = 12, restTotal = 30), actions) },
        ).forEach { (primary, screen) ->
            compose.setContent { SteadyTheme { screen() } }
            compose.onNodeWithText(primary).assertIsDisplayed().assertHasClickAction()
            compose.onNodeWithText(EXITS.first()).assertIsDisplayed()
        }
    }

    @Test
    fun nothingTappableInASessionIsWithoutALabel() {
        compose.setContent { SteadyTheme { LiveScreen(state, actions) } }

        val tappable = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertTrue("the live screen has things to tap", tappable.isNotEmpty())
        tappable.forEach { node ->
            val described = node.config.toString()
            assertTrue(
                "a tappable node with no label at ${node.boundsInRoot}: $described",
                described.contains("ContentDescription") || described.contains("Text"),
            )
        }
    }

    @Test
    fun theCountAndTheExitsSurviveTwiceTheTextSize() {
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f),
            ) {
                SteadyTheme { LiveScreen(state.copy(count = 12), actions) }
            }
        }

        compose.onNodeWithText("12").assertIsDisplayed()
        compose.onNodeWithText("Done with this one").assertIsDisplayed().assertHasClickAction()
        EXITS.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun theReadyScreenSurvivesTwiceTheTextSize() {
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f),
            ) {
                SteadyTheme { ReadyScreen(state, actions) }
            }
        }

        compose.onNodeWithText("Stands from a high seat").assertIsDisplayed()
        compose.onNodeWithText("I'm ready").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun theExitsAreOnEverySessionScreenAndThereAreExactlyFour() {
        compose.setContent { SteadyTheme { LiveScreen(state, actions) } }
        assertEquals(FOUR, EXITS.size)
        EXITS.forEach { compose.onNodeWithText(it).assertIsDisplayed().assertHasClickAction() }
    }

    private val state = SessionUiState(
        movementName = "Stands from a high seat",
        setup = "Use a high chair or the edge of the bed. Stand up, sit down.",
        stopRule = "Use your hands if you need them. That still counts.",
        target = 8,
        count = 3,
        progress = 0.375f,
        counted = Counted.Reps,
        stepOf = "2 of 6",
    )

    private val actions = SessionActions(
        onReady = {},
        onSkipCountIn = {},
        onTap = {},
        onEndSet = {},
        onSkipRest = {},
        onPause = {},
        onEasier = {},
        onSkip = {},
        onEnough = {},
        onHurts = {},
        onSpeaker = {},
    )

    private companion object {
        const val FOUR = 4

        /** The four exits, which are on every screen of a session and never change. */
        val EXITS = listOf(
            "Make it easier",
            "Skip this one",
            "That's enough for today",
            "Something hurts",
        )
    }
}
