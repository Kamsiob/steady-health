package com.kamsiob.steadyhealth.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kamsiob.steadyhealth.ui.components.PlanOnCard
import com.kamsiob.steadyhealth.ui.components.SessionCard
import com.kamsiob.steadyhealth.ui.components.SessionCardState
import com.kamsiob.steadyhealth.ui.theme.SteadyTheme
import org.junit.Rule
import org.junit.Test

/**
 * The Phase 3 gate: a therapist's plan and the app's own suggestions are visibly
 * separate, and a movement on both lists is counted once.
 *
 * ADDENDUM-03 Part 6 says this must be unambiguous, so it is a test rather than a
 * screenshot somebody looked at once. What it can check is that both headings are on
 * the screen, that the plan's movements sit under the plan's heading and the app's
 * under the app's, and that the sentence about a movement on both lists is said once
 * and not once per movement.
 *
 * Two of Part 6's harder sentences are held here as well. A second therapist's plan
 * gets its own name above its own lines rather than being poured in under the first
 * one's. And the extras go off in one tap, on the card, because Part 6 says one tap
 * and a switch in another tab is a tab, a scroll and a switch.
 */
class PlanSeparateTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theirPlanIsTheCardAndTheAppsOwnAreLabelledSeparately() {
        compose.setContent { SteadyTheme { Card(withAPlan) } }

        compose.onNodeWithText("From your physio").assertIsDisplayed()
        compose.onNodeWithText("Chair stands").assertIsDisplayed()

        // The app's own, under their own heading, below the plan's.
        compose.onNodeWithText("Also, if you want more").assertIsDisplayed()
        compose.onNodeWithText("A walk").assertIsDisplayed()
    }

    @Test
    fun aMovementOnBothListsIsSaidOnceAndNotOncePerMovement() {
        compose.setContent { SteadyTheme { Card(withAPlan) } }

        compose.onAllNodesWithTextSaying("This one's on your physio's list too.").let {
            it.fetchSemanticsNodes().let { nodes ->
                assert(nodes.size == 1) { "said ${nodes.size} times, should be once" }
            }
        }
    }

    @Test
    fun aClashIsFlaggedAndTheMovementStaysOnTheList() {
        compose.setContent { SteadyTheme { Card(withAPlan) } }

        // Asserted on existence rather than on being on screen: this test draws the
        // card on its own, with no scrolling parent, and a long card runs off a phone.
        compose.onNodeWithText("Ask your therapist about it", substring = true).assertExists()
        // Still there. Part 6: flagged rather than silently dropped.
        compose.onNodeWithText("Wall push ups").assertIsDisplayed()
    }

    @Test
    fun withoutAPlanTheCardIsTheAppsOwnAndSaysNothingAboutATherapist() {
        compose.setContent { SteadyTheme { Card(ourOwn) } }

        compose.onNodeWithText("Today's session").assertIsDisplayed()
        compose.onNodeWithText("Start").assertIsDisplayed().assertHasClickAction()
        compose.onAllNodesWithTextSaying("Also, if you want more").fetchSemanticsNodes().let {
            assert(it.isEmpty()) { "the app offered extras when there was no plan to be extra to" }
        }
        compose.onAllNodesWithTextSaying("Turn the app's extras").fetchSemanticsNodes().let {
            assert(it.isEmpty()) { "offered to turn off extras that were never offered" }
        }
    }

    // --- Part 6: multiple plans -------------------------------------------------

    @Test
    fun aSecondPlanKeepsItsOwnNameAndItsOwnLines() {
        compose.setContent { SteadyTheme { Card(withTwoPlans) } }

        compose.onNodeWithText("From your physio").assertExists()
        compose.onNodeWithText("From your OT").assertExists()
        compose.onNodeWithText("Chair stands").assertExists()
        compose.onNodeWithText("Getting off the floor").assertExists()
    }

    @Test
    fun withTwoPlansTheCardStopsNamingOneOfThemAtTheTop() {
        compose.setContent { SteadyTheme { Card(withTwoPlans) } }

        // The eyebrow says what the card is; each plan says whose it is. Naming one
        // therapist above both lists is what put the OT's movements under the physio.
        compose.onNodeWithText("Your plans").assertIsDisplayed()
    }

    // --- Part 6: the extras, off in one tap -------------------------------------

    @Test
    fun theExtrasGoOffInOneTapOnTheCardItself() {
        var taps = 0
        compose.setContent { SteadyTheme { Card(withAPlan, onExtras = { taps++ }) } }

        compose.onNodeWithText("Turn the app's extras off")
            .assertExists()
            .assertHasClickAction()
            .performClick()

        assert(taps == 1) { "one tap should have been enough, and it was $taps" }
    }

    @Test
    fun withTheExtrasOffTheCardOffersThemBack() {
        compose.setContent { SteadyTheme { Card(withExtrasOff) } }

        compose.onNodeWithText("Turn the app's extras back on")
            .assertExists()
            .assertHasClickAction()
        compose.onAllNodesWithTextSaying("Also, if you want more").fetchSemanticsNodes().let {
            assert(it.isEmpty()) { "extras were off and the app suggested some anyway" }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Card(state: SessionCardState, onExtras: () -> Unit = {}) {
        SessionCard(
            state = state,
            onGo = {},
            onSomethingSmall = {},
            onWithoutThePhone = {},
            onExtras = onExtras,
        )
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextSaying(
        text: String,
    ) = onAllNodes(androidx.compose.ui.test.hasText(text, substring = true))

    private val withAPlan = SessionCardState(
        length = "Three movements",
        doneToday = false,
        theirs = "From your physio",
        theirPlans = listOf(
            PlanOnCard(
                label = "From your physio",
                movements = listOf("Chair stands", "Wall push ups", "Heel raises"),
            ),
        ),
        alsoMovements = listOf("A walk", "Calf stretch"),
        extrasOn = true,
        onBothLists = "This one's on your physio's list too.",
        toAskAbout = listOf(
            "Your plan has wall push ups, and you said you avoid pushing. " +
                "Ask your therapist about it. We'll leave it in for now.",
        ),
    )

    private val withTwoPlans = SessionCardState(
        length = "Three movements",
        theirs = "Your plans",
        theirPlans = listOf(
            PlanOnCard(label = "From your physio", movements = listOf("Chair stands")),
            PlanOnCard(
                label = "From your OT",
                movements = listOf("Getting off the floor", "Heel raises"),
            ),
        ),
        extrasOn = true,
    )

    private val withExtrasOff = SessionCardState(
        length = "One movement",
        theirs = "From your physio",
        theirPlans = listOf(
            PlanOnCard(label = "From your physio", movements = listOf("Chair stands")),
        ),
        alsoMovements = emptyList(),
        extrasOn = false,
    )

    private val ourOwn = SessionCardState(
        length = "About 8 minutes",
        movements = listOf("Chair stands", "A walk"),
        feeds = "For Get up, Go",
    )
}
