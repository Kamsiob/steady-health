package com.kamsiob.steadyhealth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kamsiob.steadyhealth.plan.HowMany
import com.kamsiob.steadyhealth.plan.HowOften
import com.kamsiob.steadyhealth.plan.PlanItem
import com.kamsiob.steadyhealth.plan.Sureness
import com.kamsiob.steadyhealth.session.Movements
import com.kamsiob.steadyhealth.ui.scan.PlanConfirmScreen
import com.kamsiob.steadyhealth.ui.scan.PlanDraftUiState
import com.kamsiob.steadyhealth.ui.theme.SteadyTheme
import org.junit.Rule
import org.junit.Test

/**
 * The one door all four ways in end at. ADDENDUM-03 Part 6.
 *
 * Two of Part 6's sentences live on this screen and neither of them did before. A plan
 * movement that clashes with something the person said they avoid is flagged here,
 * where they can still ask about it, rather than only on tomorrow's card. And "This is
 * your therapist's, not ours" is said once and then not again, which is the word Part 6
 * uses and which a block drawn on every confirmation is not.
 */
class PlanConfirmTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aClashIsSaidOnTheScreenWhereSomethingCanStillBeDoneAboutIt() {
        compose.setContent { SteadyTheme { Confirm(clashing) } }

        compose.onNodeWithText("you said you avoid pushing", substring = true).assertExists()
        // Flagged, never dropped. The line and the way to keep it are both still here.
        compose.onNodeWithText("3 sets of 8 wall push ups").assertExists()
        compose.onNodeWithText("Save the plan").assertExists().assertHasClickAction()
    }

    @Test
    fun theLineAboutWhoseThisIsIsSaidOnceAndCanBeReadAndPutAway() {
        var read = 0
        compose.setContent { SteadyTheme { Confirm(firstEver, onTheirsRead = { read++ }) } }

        compose.onNodeWithText("This is your therapist's", substring = true).assertExists()
        // The dismiss carries its label as a description, because what it draws is a
        // multiplication sign and nothing reads that out loud.
        compose.onNodeWithContentDescription("Got it")
            .assertExists()
            .assertHasClickAction()
            .performClick()

        assert(read == 1) { "the dismiss should say it has been read, and said it $read times" }
    }

    @Test
    fun aSecondPlanIsNotToldAgainWhoseItIs() {
        compose.setContent { SteadyTheme { Confirm(saidAlready) } }

        compose.onAllNodes(hasText("This is your therapist's", substring = true))
            .fetchSemanticsNodes()
            .let { assert(it.isEmpty()) { "said again on a later plan, and Part 6 says once" } }
    }

    @Composable
    private fun Confirm(state: PlanDraftUiState, onTheirsRead: () -> Unit = {}) {
        PlanConfirmScreen(
            state = state,
            onLabel = {},
            onDrop = {},
            onReviewDay = {},
            onTheirsRead = onTheirsRead,
            onSave = {},
            onBack = {},
        )
    }

    private fun item(id: String, line: String) = PlanItem(
        line = line,
        movement = Movements.byId(id),
        howMany = HowMany.Unsaid,
        howOften = HowOften.Unsaid,
        sureness = Sureness.Named,
    )

    private val clashing = PlanDraftUiState(
        items = listOf(item("wall_push_up", "3 sets of 8 wall push ups")),
        toAskAbout = listOf(
            "Your plan has wall push ups, and you said you avoid pushing. " +
                "Ask your therapist about it. We'll leave it in for now.",
        ),
    )

    private val firstEver = PlanDraftUiState(
        items = listOf(item("sit_to_stand", "10 chair stands twice a day")),
        sayTheirs = true,
    )

    private val saidAlready = PlanDraftUiState(
        items = listOf(item("sit_to_stand", "10 chair stands twice a day")),
        sayTheirs = false,
    )
}
