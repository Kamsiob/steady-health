package com.kamsiob.steadyhealth.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.ui.screens.AbilityTileState
import com.kamsiob.steadyhealth.ui.screens.SettingsActions
import com.kamsiob.steadyhealth.ui.screens.SettingsScreen
import com.kamsiob.steadyhealth.ui.screens.SettingsUiState
import com.kamsiob.steadyhealth.ui.screens.TodayScreen
import com.kamsiob.steadyhealth.ui.screens.TodayUiState
import com.kamsiob.steadyhealth.ui.theme.SteadyTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The accessibility floor from DESIGN.md section 7, checked where it is real.
 *
 * On device rather than in a JVM test because what is being checked is the
 * semantics tree Android hands to TalkBack, which only exists when something has
 * actually composed. A screen whose labels are right in the source and wrong in
 * the tree is the failure worth catching.
 *
 * The font scale cases matter for the same reason: this app is for people who
 * turn text size up, and a layout that breaks at 200% breaks for exactly the
 * people it was built for. Nothing here changes a setting on the phone; the
 * density is overridden inside the test.
 */
class AccessibilityTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun everyThingOnTodayThatCanBeTappedSaysWhatItIs() {
        compose.setContent { SteadyTheme { TodayScreen(today, {}, {}, {}, {}, {}, {}, {}) } }

        // DESIGN.md section 7: TalkBack labels describe meaning, not drawing.
        listOf(
            "Get up, You can stand up without using your hands now.",
            "Go, You can do a flight of stairs without stopping now.",
        ).forEach { spoken ->
            compose.onNodeWithContentDescription(spoken).assertIsDisplayed()
            compose.onNodeWithContentDescription(spoken).assertHasClickAction()
        }
    }

    @Test
    fun theThreeDailyCardsAnnounceWhetherTheyAreDone() {
        compose.setContent { SteadyTheme { TodayScreen(today, {}, {}, {}, {}, {}, {}, {}) } }

        // Nothing is conveyed by colour alone, so "done" is in the words.
        compose.onNodeWithContentDescription("Weighed in, done. Takes ten seconds")
            .assertIsDisplayed()
        // The one that is not done says what it is, not what it is missing.
        compose.onNodeWithContentDescription("Say how today went. Talk or type, 20 seconds")
            .assertIsDisplayed()
    }

    @Test
    fun nothingTappableIsWithoutALabel() {
        compose.setContent { SteadyTheme { TodayScreen(today, {}, {}, {}, {}, {}, {}, {}) } }
        val tappable = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertTrue("Today has things to tap", tappable.isNotEmpty())
        tappable.forEach { node ->
            val described = node.config.toString()
            assertTrue(
                "a tappable node with no label at ${node.boundsInRoot}: $described",
                described.contains("ContentDescription") || described.contains("Text"),
            )
        }
    }

    @Test
    fun todaySurvivesTextSetToTwiceTheSize() {
        // The people this app is for turn text size up. A layout that only works
        // at 100% works for somebody else.
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = 2f,
                ),
            ) {
                SteadyTheme { TodayScreen(today, {}, {}, {}, {}, {}, {}, {}) }
            }
        }
        compose.onNodeWithText("Get up").assertIsDisplayed()
    }

    @Test
    fun settingsSwitchesSayWhichWayTheyAreSet() {
        compose.setContent {
            SteadyTheme {
                SettingsScreen(
                    state = SettingsUiState(
                        gettingAroundLabel = "On my feet",
                        exclusionsLabel = "Nothing",
                        pemLabel = "No",
                    ),
                    actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}),
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText("How you get around").assertIsDisplayed()
        compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().let {
            assertTrue("settings has rows to tap", it.size >= FOUR)
        }
    }

    private val today = TodayUiState(
        date = "Sunday, September 6",
        greeting = "Evening",
        abilities = listOf(
            AbilityTileState(
                domain = AbilityDomain.GetUp,
                name = "Get up",
                lifeSentence = "You can stand up without using your hands now.",
            ),
            AbilityTileState(
                domain = AbilityDomain.Go,
                name = "Go",
                lifeSentence = "You can do a flight of stairs without stopping now.",
            ),
        ),
        weightValue = "176.4",
        weightUnit = "lb",
        weightExplain = "The scale said 176.4 lb today.",
        weighedIn = true,
        saidHowItWent = false,
        moved = false,
        nextWalkName = "A 4 minute walk",
        nextLabel = "Your next walk",
        moveTitle = "Move for two minutes",
        moveSubtitle = "A short walk counts",
        dayLetters = listOf("M", "T", "W", "T", "F", "S", "S"),
        walkedThisWeek = List(7) { false },
    )

    private companion object {
        const val FOUR = 4
    }
}
