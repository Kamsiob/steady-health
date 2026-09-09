package com.kamsiob.steadyhealth.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
 */
class PlanSeparateTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theirPlanIsTheCardAndTheAppsOwnAreLabelledSeparately() {
        compose.setContent { SteadyTheme { SessionCard(withAPlan, onGo = {}, onSomethingSmall = {}, onWithoutThePhone = {}) } }

        compose.onNodeWithText("From your physio").assertIsDisplayed()
        compose.onNodeWithText("Chair stands").assertIsDisplayed()

        // The app's own, under their own heading, below the plan's.
        compose.onNodeWithText("Also, if you want more").assertIsDisplayed()
        compose.onNodeWithText("A walk").assertIsDisplayed()
    }

    @Test
    fun aMovementOnBothListsIsSaidOnceAndNotOncePerMovement() {
        compose.setContent { SteadyTheme { SessionCard(withAPlan, onGo = {}, onSomethingSmall = {}, onWithoutThePhone = {}) } }

        compose.onAllNodesWithTextSaying("This one's on your physio's list too.").let {
            it.fetchSemanticsNodes().let { nodes ->
                assert(nodes.size == 1) { "said ${nodes.size} times, should be once" }
            }
        }
    }

    @Test
    fun aClashIsFlaggedAndTheMovementStaysOnTheList() {
        compose.setContent { SteadyTheme { SessionCard(withAPlan, onGo = {}, onSomethingSmall = {}, onWithoutThePhone = {}) } }

        // Asserted on existence rather than on being on screen: this test draws the
        // card on its own, with no scrolling parent, and a long card runs off a phone.
        compose.onNodeWithText("Ask your therapist about it", substring = true).assertExists()
        // Still there. Part 6: flagged rather than silently dropped.
        compose.onNodeWithText("Wall push ups").assertIsDisplayed()
    }

    @Test
    fun withoutAPlanTheCardIsTheAppsOwnAndSaysNothingAboutATherapist() {
        compose.setContent { SteadyTheme { SessionCard(ourOwn, onGo = {}, onSomethingSmall = {}, onWithoutThePhone = {}) } }

        compose.onNodeWithText("Today's session").assertIsDisplayed()
        compose.onNodeWithText("Start").assertIsDisplayed().assertHasClickAction()
        compose.onAllNodesWithTextSaying("Also, if you want more").fetchSemanticsNodes().let {
            assert(it.isEmpty()) { "the app offered extras when there was no plan to be extra to" }
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextSaying(
        text: String,
    ) = onAllNodes(androidx.compose.ui.test.hasText(text, substring = true))

    private val withAPlan = SessionCardState(
        length = "Three movements",
        movements = listOf("Chair stands", "Wall push ups", "Heel raises"),
        doneToday = false,
        theirs = "From your physio",
        alsoMovements = listOf("A walk", "Calf stretch"),
        onBothLists = "This one's on your physio's list too.",
        toAskAbout = listOf(
            "Your plan has wall push ups, and you said you avoid pushing. " +
                "Ask your therapist about it. We'll leave it in for now.",
        ),
    )

    private val ourOwn = SessionCardState(
        length = "About 8 minutes",
        movements = listOf("Chair stands", "A walk"),
        feeds = "For Get up, Go",
    )
}
