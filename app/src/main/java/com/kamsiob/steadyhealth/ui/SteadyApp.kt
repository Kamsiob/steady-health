package com.kamsiob.steadyhealth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kamsiob.steadyhealth.ui.components.SteadyTabBar
import com.kamsiob.steadyhealth.ui.nav.Route
import com.kamsiob.steadyhealth.ui.nav.Tab
import com.kamsiob.steadyhealth.ui.onboarding.OnboardingFlow
import com.kamsiob.steadyhealth.ui.screens.AbilitiesScreen
import com.kamsiob.steadyhealth.ui.screens.MoveScreen
import com.kamsiob.steadyhealth.ui.screens.OfferScreen
import com.kamsiob.steadyhealth.ui.screens.SayHowScreen
import com.kamsiob.steadyhealth.ui.screens.TodayScreen
import com.kamsiob.steadyhealth.ui.screens.WalkDoneScreen
import com.kamsiob.steadyhealth.ui.screens.WalkingScreen
import com.kamsiob.steadyhealth.ui.screens.WeighInScreen
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import kotlinx.coroutines.delay

/**
 * The whole app: setup once, then three tabs and a small stack.
 *
 * Nothing is drawn until the app knows whether setup has been done, because a
 * flash of Today before onboarding is the sort of thing somebody remembers about
 * an app they used once.
 */
@Composable
fun SteadyApp() {
    val viewModel: SteadyViewModel = viewModel()
    val onboarded by viewModel.onboardingComplete.collectAsStateWithLifecycle()

    when (onboarded) {
        null -> Box(Modifier.fillMaxSize().background(SteadyPalette.Ground))
        false -> OnboardingFlow(onFinished = viewModel::onboardingFinished)
        true -> Tabs(viewModel)
    }
}

@Composable
private fun Tabs(viewModel: SteadyViewModel) {
    val navController = rememberNavController()
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    val back: () -> Unit = { navController.popBackStack() }
    val offer by viewModel.offer.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(SteadyPalette.Ground),
        ) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                NavHost(navController = navController, startDestination = Route.TABS) {
                    composable(Route.TABS) {
                        when (tab) {
                            Tab.Today -> {
                                val state by viewModel.today.collectAsStateWithLifecycle()
                                LaunchedEffect(Unit) { viewModel.refresh() }
                                TodayScreen(
                                    state = state,
                                    onAbility = { tab = Tab.Abilities },
                                    onWeighIn = {
                                        viewModel.openWeighIn()
                                        navController.navigate(Route.WEIGH_IN)
                                    },
                                    onSayHow = {
                                        viewModel.openSayHow()
                                        navController.navigate(Route.SAY_HOW)
                                    },
                                    onMove = { tab = Tab.Move },
                                    onAsk = {},
                                    onSettings = {},
                                )
                            }

                            Tab.Move -> {
                                val state by viewModel.move.collectAsStateWithLifecycle()
                                MoveScreen(
                                    state = state,
                                    onGo = {
                                        viewModel.startWalk()
                                        navController.navigate(Route.WALKING)
                                    },
                                )
                            }

                            Tab.Abilities -> {
                                val state by viewModel.abilitiesState.collectAsStateWithLifecycle()
                                AbilitiesScreen(state = state, onAbility = {})
                            }
                        }
                    }

                    composable(Route.WEIGH_IN) {
                        val state by viewModel.weighIn.collectAsStateWithLifecycle()
                        WeighInScreen(
                            state = state,
                            onWeight = viewModel::setWeight,
                            onSave = {
                                viewModel.saveWeighIn()
                                back()
                            },
                            onBack = back,
                        )
                    }

                    composable(Route.SAY_HOW) {
                        val state by viewModel.sayHow.collectAsStateWithLifecycle()
                        SayHowScreen(
                            state = state,
                            onSentence = viewModel::setSentence,
                            onSleep = viewModel::setSleep,
                            onRating = viewModel::setDayRating,
                            onSave = {
                                viewModel.saveDay()
                                back()
                            },
                            onBack = back,
                        )
                    }

                    composable(Route.WALKING) {
                        val state by viewModel.walking.collectAsStateWithLifecycle()
                        var seconds by remember { mutableIntStateOf(0) }

                        // One tick a second while this screen is on top. It stops when
                        // the screen leaves, so a walk somebody backed out of does not
                        // keep counting in the background.
                        LaunchedEffect(Unit) {
                            while (true) {
                                delay(ONE_SECOND)
                                seconds += 1
                                viewModel.tickWalk(seconds)
                            }
                        }

                        WalkingScreen(
                            state = state,
                            onStop = {
                                viewModel.stopWalk(seconds)
                                navController.navigate(Route.WALK_DONE)
                            },
                            onBack = back,
                        )
                    }

                    composable(Route.WALK_DONE) {
                        val state by viewModel.walkDone.collectAsStateWithLifecycle()
                        WalkDoneScreen(
                            state = state,
                            onTalkTest = viewModel::setTalkTest,
                            onSave = {
                                viewModel.saveWalkDone()
                                navController.popBackStack(Route.TABS, inclusive = false)
                            },
                            onBack = { navController.popBackStack(Route.TABS, inclusive = false) },
                        )
                    }
                }
            }

            SteadyTabBar(
                selected = tab,
                onSelect = {
                    tab = it
                    navController.popBackStack(Route.TABS, inclusive = false)
                },
                modifier = Modifier.navigationBarsPadding(),
            )
        }

        // The offer covers everything while it is up, because it is a thing the app
        // is saying rather than a place somebody navigated to, and because the only
        // two answers are on it. Both are buttons of equal weight: offered, never
        // assigned.
        offer?.let { ready ->
            OfferScreen(
                state = ready,
                onAccept = viewModel::acceptOffer,
                onNotYet = viewModel::declineOffer,
                modifier = Modifier.fillMaxSize().background(SteadyPalette.Ground),
            )
        }
    }
}

private const val ONE_SECOND = 1000L
