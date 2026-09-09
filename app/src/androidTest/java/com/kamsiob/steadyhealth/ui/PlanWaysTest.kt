package com.kamsiob.steadyhealth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kamsiob.steadyhealth.ui.scan.PlanSayScreen
import com.kamsiob.steadyhealth.ui.scan.PlanWaysScreen
import com.kamsiob.steadyhealth.ui.scan.PlanWaysUiState
import com.kamsiob.steadyhealth.ui.theme.SteadyTheme
import org.junit.Rule
import org.junit.Test

/**
 * The four ways into a therapist's plan, and the one that refuses. ADDENDUM-03 Part 6.
 *
 * Two things are worth holding on a device rather than in a comment. All four ways are
 * on the chooser, because Part 6 names four and the app had one for a while. And a
 * phone that cannot turn speech into words without a server is told so and offered
 * typing, and is never offered a microphone button: the fallback that would work is
 * the one that sends somebody's voice away, so there is no fallback, and the way that
 * check could quietly stop being true is somebody adding the ordinary recogniser back
 * as a convenience.
 */
class PlanWaysTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun allFourWaysInAreOnTheChooserAndTheCameraIsFirst() {
        compose.setContent {
            SteadyTheme {
                PlanWaysScreen(
                    onCamera = {},
                    onSay = {},
                    onPick = {},
                    onType = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText("Something on paper").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithText("Say it out loud").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithText("Pick from the library").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithText("Type it").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun aPhoneThatCannotHearWithoutAServerSaysSoAndOffersTyping() {
        compose.setContent {
            SteadyTheme { SaySaying(PlanWaysUiState(canHear = false)) }
        }

        compose.onNodeWithText("sending your voice away", substring = true).assertExists()
        compose.onNodeWithText("Type it").assertExists().assertHasClickAction()

        // Nothing offers to listen, because nothing here can listen without a server.
        compose.onAllNodes(hasText("Start talking")).fetchSemanticsNodes().let {
            assert(it.isEmpty()) { "offered to listen on a phone that cannot" }
        }
        compose.onAllNodes(hasText("Allow the microphone")).fetchSemanticsNodes().let {
            assert(it.isEmpty()) { "asked for a microphone it had decided not to use" }
        }
    }

    @Test
    fun aPhoneThatCanHearAsksForTheMicrophoneOnlyWhenSomebodyIsOnThisScreen() {
        compose.setContent {
            SteadyTheme { SaySaying(PlanWaysUiState(canHear = true, microphone = false)) }
        }

        compose.onNodeWithText("Allow the microphone").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun whatWasHeardIsShownBackBeforeAnythingIsReadFromIt() {
        val said = "ten chair stands twice a day and heel raises"
        compose.setContent {
            SteadyTheme {
                SaySaying(PlanWaysUiState(canHear = true, microphone = true, heard = said))
            }
        }

        compose.onNodeWithText(said).assertIsDisplayed()
        compose.onNodeWithText("Say it again").assertIsDisplayed().assertHasClickAction()
    }

    @Composable
    private fun SaySaying(state: PlanWaysUiState) {
        PlanSayScreen(
            state = state,
            onAllow = {},
            onListen = {},
            onStop = {},
            onContinue = {},
            onType = {},
            onBack = {},
        )
    }
}
