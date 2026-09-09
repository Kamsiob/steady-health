package com.kamsiob.steadyhealth.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.engine.ReminderKind
import com.kamsiob.steadyhealth.remind.Reminding
import com.kamsiob.steadyhealth.share.CardKind
import com.kamsiob.steadyhealth.ui.components.SteadyTabBar
import com.kamsiob.steadyhealth.ui.model.ModelsActions
import com.kamsiob.steadyhealth.ui.model.ModelsScreen
import com.kamsiob.steadyhealth.ui.model.ModelsViewModel
import com.kamsiob.steadyhealth.ui.nav.Route
import com.kamsiob.steadyhealth.ui.nav.Tab
import com.kamsiob.steadyhealth.ui.onboarding.OnboardingFlow
import com.kamsiob.steadyhealth.ui.places.PlacesViewModel
import com.kamsiob.steadyhealth.ui.scan.Camera
import com.kamsiob.steadyhealth.ui.scan.CameraPreview
import com.kamsiob.steadyhealth.ui.scan.DocumentsScreen
import com.kamsiob.steadyhealth.ui.scan.PageFoundScreen
import com.kamsiob.steadyhealth.ui.scan.PlanConfirmScreen
import com.kamsiob.steadyhealth.ui.scan.PlanPickScreen
import com.kamsiob.steadyhealth.ui.scan.PlanSayScreen
import com.kamsiob.steadyhealth.ui.scan.PlanTypeScreen
import com.kamsiob.steadyhealth.ui.scan.PlanWaysScreen
import com.kamsiob.steadyhealth.ui.scan.PlanWaysViewModel
import com.kamsiob.steadyhealth.ui.scan.ScanScreen
import com.kamsiob.steadyhealth.ui.scan.ScanViewModel
import com.kamsiob.steadyhealth.ui.screens.AbilitiesScreen
import com.kamsiob.steadyhealth.ui.screens.AbilityDetailScreen
import com.kamsiob.steadyhealth.ui.screens.AboutScreen
import com.kamsiob.steadyhealth.ui.screens.AddSomethingScreen
import com.kamsiob.steadyhealth.ui.screens.AskScreen
import com.kamsiob.steadyhealth.ui.screens.CardScreen
import com.kamsiob.steadyhealth.ui.screens.CheckDoneScreen
import com.kamsiob.steadyhealth.ui.screens.CheckIntroScreen
import com.kamsiob.steadyhealth.ui.screens.CheckMeasureScreen
import com.kamsiob.steadyhealth.ui.screens.DataRoute
import com.kamsiob.steadyhealth.ui.screens.GettingAroundScreen
import com.kamsiob.steadyhealth.ui.screens.KitScreen
import com.kamsiob.steadyhealth.ui.screens.LeaveOutSettingsScreen
import com.kamsiob.steadyhealth.ui.screens.LogPastScreen
import com.kamsiob.steadyhealth.ui.screens.MonthsScreen
import com.kamsiob.steadyhealth.ui.screens.OfferScreen
import com.kamsiob.steadyhealth.ui.screens.PacingScreen
import com.kamsiob.steadyhealth.ui.screens.PastSessionScreen
import com.kamsiob.steadyhealth.ui.screens.PatternScreen
import com.kamsiob.steadyhealth.ui.screens.PlacesScreen
import com.kamsiob.steadyhealth.ui.screens.PlansScreen
import com.kamsiob.steadyhealth.ui.screens.PrivacyScreen
import com.kamsiob.steadyhealth.ui.screens.QuieterScreen
import com.kamsiob.steadyhealth.ui.screens.RateAgainScreen
import com.kamsiob.steadyhealth.ui.screens.RemindersScreen
import com.kamsiob.steadyhealth.ui.screens.SayHowScreen
import com.kamsiob.steadyhealth.ui.screens.SessionsScreen
import com.kamsiob.steadyhealth.ui.screens.SettingsActions
import com.kamsiob.steadyhealth.ui.screens.SettingsScreen
import com.kamsiob.steadyhealth.ui.screens.SummaryScreen
import com.kamsiob.steadyhealth.ui.screens.SundayScreen
import com.kamsiob.steadyhealth.ui.screens.TodayScreen
import com.kamsiob.steadyhealth.ui.screens.TryOfferScreen
import com.kamsiob.steadyhealth.ui.screens.TryResultScreen
import com.kamsiob.steadyhealth.ui.screens.WalkDoneScreen
import com.kamsiob.steadyhealth.ui.screens.WalkingScreen
import com.kamsiob.steadyhealth.ui.screens.WeighInScreen
import com.kamsiob.steadyhealth.ui.screens.WeightPageScreen
import com.kamsiob.steadyhealth.ui.screens.YourItemScreen
import com.kamsiob.steadyhealth.ui.screens.YourListScreen
import com.kamsiob.steadyhealth.ui.session.PhoneFreeScreen
import com.kamsiob.steadyhealth.ui.session.SessionHost
import com.kamsiob.steadyhealth.ui.session.SessionViewModel
import com.kamsiob.steadyhealth.ui.share.CardScreen
import com.kamsiob.steadyhealth.ui.share.CardViewModel
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
fun SteadyApp(openSession: Boolean = false) {
    val viewModel: SteadyViewModel = viewModel()
    val askViewModel: AskViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val checkViewModel: CheckViewModel = viewModel()
    val abilityViewModel: AbilityViewModel = viewModel()
    val summaryViewModel: SummaryViewModel = viewModel()
    val tryViewModel: TryViewModel = viewModel()
    val sessionViewModel: SessionViewModel = viewModel()
    val sessionsViewModel: SessionsViewModel = viewModel()
    val scanViewModel: ScanViewModel = viewModel()
    val modelsViewModel: ModelsViewModel = viewModel()
    val cardViewModel: CardViewModel = viewModel()
    val placesViewModel: PlacesViewModel = viewModel()
    val therapistPageViewModel: TherapistPageViewModel = viewModel()
    val onboarded by viewModel.onboardingComplete.collectAsStateWithLifecycle()

    when (onboarded) {
        null -> Box(Modifier.fillMaxSize().background(SteadyPalette.Ground))
        false -> OnboardingFlow(onFinished = viewModel::onboardingFinished)
        true -> Tabs(
            openSession,
            viewModel,
            askViewModel,
            settingsViewModel,
            checkViewModel,
            abilityViewModel,
            summaryViewModel,
            tryViewModel,
            sessionViewModel,
            sessionsViewModel,
            scanViewModel,
            modelsViewModel,
            therapistPageViewModel,
            cardViewModel,
            placesViewModel,
        )
    }
}

@Composable
@Suppress("LongParameterList") // Four tabs, nine view models, one place.
private fun Tabs(
    openSession: Boolean,
    viewModel: SteadyViewModel,
    askViewModel: AskViewModel,
    settingsViewModel: SettingsViewModel,
    checkViewModel: CheckViewModel,
    abilityViewModel: AbilityViewModel,
    summaryViewModel: SummaryViewModel,
    tryViewModel: TryViewModel,
    sessionViewModel: SessionViewModel,
    sessionsViewModel: SessionsViewModel,
    scanViewModel: ScanViewModel,
    modelsViewModel: ModelsViewModel,
    therapistPageViewModel: TherapistPageViewModel,
    cardViewModel: CardViewModel,
    placesViewModel: PlacesViewModel,
) {
    val navController = rememberNavController()
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    val back: () -> Unit = { navController.popBackStack() }

    // The widget was tapped. Straight into the session, which is the whole point of
    // a widget: the thing it names is one tap away and not four.
    LaunchedEffect(openSession) {
        if (openSession) {
            sessionViewModel.startTodays()
            navController.navigate(Route.SESSION)
        }
    }
    val offer by viewModel.offer.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(SteadyPalette.Ground),
        ) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                NavHost(navController = navController, startDestination = Route.TABS) {
                    composable(Route.TABS) {
                        TabBody(
                            tab = tab,
                            viewModel = viewModel,
                            askViewModel = askViewModel,
                            settingsViewModel = settingsViewModel,
                            checkViewModel = checkViewModel,
                            abilityViewModel = abilityViewModel,
                            summaryViewModel = summaryViewModel,
                            tryViewModel = tryViewModel,
                            sessionViewModel = sessionViewModel,
                            sessionsViewModel = sessionsViewModel,
                            scanViewModel = scanViewModel,
                            modelsViewModel = modelsViewModel,
                            therapistPageViewModel = therapistPageViewModel,
                            cardViewModel = cardViewModel,
                            navController = navController,
                        )
                    }

                    dailyRoutes(viewModel, navController, back)

                    sessionsRoutes(
                        viewModel = sessionsViewModel,
                        sessionViewModel = sessionViewModel,
                        navController = navController,
                        back = back,
                        onCard = { line ->
                            cardViewModel.open(CardKind.Week, line)
                            navController.navigate(Route.SEND_CARD)
                        },
                    )
                    scanRoutes(scanViewModel, modelsViewModel, navController, back)

                    asideRoutes(cardViewModel, placesViewModel, back)
                    askRoutes(askViewModel, navController, back)
                    checkRoutes(
                        viewModel = checkViewModel,
                        navController = navController,
                        back = back,
                        onCard = { line ->
                            cardViewModel.open(CardKind.Milestone, line)
                            navController.navigate(Route.SEND_CARD)
                        },
                    ) {
                        summaryViewModel.open()
                        navController.navigate(Route.SUMMARY)
                    }
                    abilityRoutes(abilityViewModel, navController, back)
                    summaryRoutes(summaryViewModel, back)
                    tryRoutes(tryViewModel, back)
                    sessionRoutes(sessionViewModel, back)

                    youRoutes(
                        viewModel = settingsViewModel,
                        steadyViewModel = viewModel,
                        therapistPageViewModel = therapistPageViewModel,
                        navController = navController,
                        back = back,
                    )

                    settingsRoutes(
                        viewModel = settingsViewModel,
                        askViewModel = askViewModel,
                        scanViewModel = scanViewModel,
                        modelsViewModel = modelsViewModel,
                        navController = navController,
                        back = back,
                        onSummary = {
                            summaryViewModel.open()
                            navController.navigate(Route.SUMMARY)
                        },
                        onDeleted = {
                            navController.popBackStack(Route.TABS, inclusive = false)
                            viewModel.startAgain()
                        },
                    )

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

            // Not during a session. ADDENDUM-03 Part 1 makes the session screens
            // full screen, and a tab bar under a live set is a fourth way out that
            // is not one of the three exits and does not save what was done.
            val entry by navController.currentBackStackEntryAsState()
            if (entry?.destination?.route != Route.SESSION) {
                SteadyTabBar(
                    selected = tab,
                    onSelect = {
                        tab = it
                        navController.popBackStack(Route.TABS, inclusive = false)
                    },
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
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

/**
 * The settings routes, in their own function.
 *
 * Not a separate graph, just a separate place to read them: settings is a list of
 * small screens that all pop back to the same place, and having them inline made
 * the one composable that holds the whole app twice as long as anything else.
 */
/**
 * The daily three and the walk, in their own function, for the same reason as
 * [settingsRoutes]: they are a list of small screens that all end where they
 * started.
 */
private fun NavGraphBuilder.dailyRoutes(
    viewModel: SteadyViewModel,
    navController: NavHostController,
    back: () -> Unit,
) {
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
            onTag = viewModel::toggleTag,
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
}

/**
 * Ask a question and one card.
 *
 * Their own function and their own view model, because the cards are constants
 * and the search is a word match: nothing here touches the database.
 */
/**
 * The monthly check, from the offer to the result.
 *
 * A flow with a beginning and an end rather than a place, so it lives outside the
 * tabs and pops back to where it started. Its view model holds the accelerometer,
 * and leaving takes it with them.
 */
/**
 * One ability in depth, and the weight behind it. Grid screens 9 and 19.
 *
 * Reached from a tile on Today and from a row on Abilities, which are the two
 * places the four are shown, so tapping one anywhere goes to the same page.
 */
/**
 * The visit summary. Never a fourth tab, and reached from the places DESIGN.md
 * names: the Abilities tab, an ability page, and one row in settings.
 */
/**
 * Try it and see. Grid screens 14 and 15.
 *
 * Reached from a row on Abilities that only appears when the engine has something
 * to offer or something to report. It is never announced and never badged.
 */
/**
 * Whichever of the three tabs is showing.
 *
 * Its own composable because there are three of them and each needs a handful of
 * view models, and inlining all of that made the one function that holds the app
 * longer than any screen in it.
 */
@Composable
@Suppress("LongParameterList", "LongMethod") // Four tabs, one when, one place.
private fun TabBody(
    tab: Tab,
    viewModel: SteadyViewModel,
    askViewModel: AskViewModel,
    settingsViewModel: SettingsViewModel,
    checkViewModel: CheckViewModel,
    abilityViewModel: AbilityViewModel,
    summaryViewModel: SummaryViewModel,
    tryViewModel: TryViewModel,
    sessionViewModel: SessionViewModel,
    sessionsViewModel: SessionsViewModel,
    scanViewModel: ScanViewModel,
    modelsViewModel: ModelsViewModel,
    therapistPageViewModel: TherapistPageViewModel,
    cardViewModel: CardViewModel,
    navController: NavHostController,
) {
    when (tab) {
        Tab.Today -> {
            val state by viewModel.today.collectAsStateWithLifecycle()
            // On every return rather than once: the date changes overnight, a session
            // may have been done since, and a screen that only ever loads once has no
            // way back if that one load did not finish.
            LifecycleResumeEffect(Unit) {
                viewModel.refresh()
                onPauseOrDispose { }
            }
            TodayScreen(
                state = state,
                onAbility = {
                    abilityViewModel.open(it)
                    navController.navigate(Route.ABILITY)
                },
                onSayHow = {
                    viewModel.openSayHow()
                    navController.navigate(Route.SAY_HOW)
                },
                onGo = {
                    sessionViewModel.startTodays()
                    navController.navigate(Route.SESSION)
                },
                onSomethingSmall = {
                    sessionViewModel.startSomethingSmall()
                    navController.navigate(Route.SESSION)
                },
                onWithoutThePhone = {
                    sessionViewModel.openPhoneFree()
                    navController.navigate(Route.PHONE_FREE)
                },
                onExtras = viewModel::toggleExtras,
                onBringBack = { yes ->
                    state.bringBack?.let { viewModel.bringBack(it.area, yes) }
                },
                onSunday = {
                    sessionsViewModel.openSunday()
                    navController.navigate(Route.SUNDAY)
                },
                onWhyAway = viewModel::answerWhyAway,
                onTherapistPage = therapistPageViewModel::share,
                onNotice = viewModel::dismissNotice,
            )
        }

        Tab.Sessions -> {
            val state by sessionsViewModel.state.collectAsStateWithLifecycle()
            LifecycleResumeEffect(Unit) {
                sessionsViewModel.refresh()
                onPauseOrDispose { }
            }
            SessionsScreen(
                state = state,
                onGo = {
                    sessionViewModel.startTodays()
                    navController.navigate(Route.SESSION)
                },
                onSomethingSmall = {
                    sessionViewModel.startSomethingSmall()
                    navController.navigate(Route.SESSION)
                },
                onWithoutThePhone = {
                    sessionViewModel.openPhoneFree()
                    navController.navigate(Route.PHONE_FREE)
                },
                onExtras = sessionsViewModel::toggleExtras,
                onMovement = { id: String ->
                    sessionViewModel.startOne(id)
                    navController.navigate(Route.SESSION)
                },
                onRepeat = { runId: Long ->
                    sessionsViewModel.openPast(runId)
                    navController.navigate(Route.PAST_SESSION)
                },
                onLogPast = {
                    sessionsViewModel.openLogPast()
                    navController.navigate(Route.LOG_PAST)
                },
                onScan = { navController.navigate(Route.PLAN_WAYS) },
            )
        }

        Tab.You -> {
            val state by settingsViewModel.settings.collectAsStateWithLifecycle()
            LifecycleResumeEffect(Unit) {
                settingsViewModel.openSettings()
                onPauseOrDispose { }
            }
            SettingsScreen(
                state = state,
                actions = settingsActions(
                    settingsViewModel,
                    askViewModel,
                    scanViewModel,
                    modelsViewModel,
                    navController,
                ),
                onBack = null,
            )
        }

        Tab.Progress -> {
            val state by viewModel.abilitiesState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { viewModel.refresh() }
            val tryOffer by tryViewModel.offer.collectAsStateWithLifecycle()
            val tryResult by tryViewModel.result.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { tryViewModel.refresh() }
            AbilitiesScreen(
                state = state,
                tryOffer = tryOffer != null,
                tryResult = tryResult != null,
                onTry = {
                    navController.navigate(
                        if (tryResult != null) Route.TRY_RESULT else Route.TRY_OFFER,
                    )
                },
                onSummary = {
                    summaryViewModel.open()
                    navController.navigate(Route.SUMMARY)
                },
                onAbility = {
                    abilityViewModel.open(it)
                    navController.navigate(Route.ABILITY)
                },
                onCheck = {
                    checkViewModel.open()
                    navController.navigate(Route.CHECK)
                },
                onCard = {
                    // Blank from here. The kinds with a line already in them are
                    // offered where that line is, on the Sunday review and after a
                    // crossing, and this is the door for somebody who just wants to
                    // say something.
                    cardViewModel.open(CardKind.Blank, "")
                    navController.navigate(Route.SEND_CARD)
                },
                onMonths = {
                    viewModel.openMonths()
                    navController.navigate(Route.MONTHS)
                },
                onAdd = { navController.navigate(Route.ADD_ITEM) },
                onWeight = {
                    abilityViewModel.openWeight()
                    navController.navigate(Route.WEIGHT)
                },
                onWeighIn = {
                    viewModel.openWeighIn()
                    navController.navigate(Route.WEIGH_IN)
                },
                onTherapistPage = therapistPageViewModel::share,
            )
        }
    }
}

/**
 * The session. One route for all seven of its screens.
 *
 * No back button anywhere inside it: the exits are how a session ends, and a back
 * gesture in the middle of a set should not drop somebody onto the screen before.
 */
private fun NavGraphBuilder.sessionRoutes(viewModel: SessionViewModel, back: () -> Unit) {
    composable(Route.SESSION) {
        SessionHost(viewModel = viewModel, onFinished = back)
    }
}

private fun NavGraphBuilder.tryRoutes(viewModel: TryViewModel, back: () -> Unit) {
    composable(Route.TRY_OFFER) {
        val state by viewModel.offer.collectAsStateWithLifecycle()
        state?.let {
            TryOfferScreen(
                state = it,
                onStart = {
                    viewModel.start()
                    back()
                },
                onNotNow = {
                    viewModel.notNow()
                    back()
                },
            )
        }
    }

    composable(Route.TRY_RESULT) {
        val state by viewModel.result.collectAsStateWithLifecycle()
        state?.let {
            TryResultScreen(
                state = it,
                onSuggest = {
                    viewModel.finish(keep = false)
                    back()
                },
                onKeep = {
                    viewModel.finish(keep = true)
                    back()
                },
            )
        }
    }
}

private fun NavGraphBuilder.summaryRoutes(viewModel: SummaryViewModel, back: () -> Unit) {
    composable(Route.SUMMARY) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        SummaryScreen(
            state = state,
            onWindow = viewModel::setWindow,
            onExport = viewModel::export,
            onRegenerate = viewModel::regenerate,
            onBack = back,
        )
    }
}

private fun NavGraphBuilder.abilityRoutes(
    viewModel: AbilityViewModel,
    navController: NavHostController,
    back: () -> Unit,
) {
    composable(Route.ABILITY) {
        val state by viewModel.detail.collectAsStateWithLifecycle()
        AbilityDetailScreen(
            state = state,
            onWeight = {
                viewModel.openWeight()
                navController.navigate(Route.WEIGHT)
            },
            onBack = back,
        )
    }

    composable(Route.WEIGHT) {
        val state by viewModel.weightPage.collectAsStateWithLifecycle()
        WeightPageScreen(state = state, onBack = back)
    }
}

private fun NavGraphBuilder.checkRoutes(
    viewModel: CheckViewModel,
    navController: NavHostController,
    back: () -> Unit,
    onCard: (String) -> Unit,
    onSummary: () -> Unit,
) {
    composable(Route.CHECK) {
        val state by viewModel.intro.collectAsStateWithLifecycle()
        CheckIntroScreen(
            state = state,
            onStart = {
                viewModel.start()
                navController.navigate(Route.CHECK_MEASURE)
            },
            onLater = back,
        )
    }

    composable(Route.CHECK_MEASURE) {
        val state by viewModel.measure.collectAsStateWithLifecycle()
        val finished by viewModel.finished.collectAsStateWithLifecycle()
        val rate by viewModel.rateAgain.collectAsStateWithLifecycle()
        LaunchedEffect(finished, rate) {
            when {
                finished -> navController.navigate(Route.CHECK_DONE)
                rate.isNotEmpty() -> navController.navigate(Route.CHECK_RATE)
            }
        }
        CheckMeasureScreen(
            state = state,
            onTap = viewModel::tap,
            onStop = viewModel::stop,
            onSkip = viewModel::skip,
            onConfirmSeen = viewModel::confirmSeen,
            onBack = { navController.popBackStack(Route.TABS, inclusive = false) },
        )
    }

    composable(Route.CHECK_RATE) {
        val items by viewModel.rateAgain.collectAsStateWithLifecycle()
        val finished by viewModel.finished.collectAsStateWithLifecycle()
        LaunchedEffect(finished) {
            if (finished) navController.navigate(Route.CHECK_DONE)
        }
        RateAgainScreen(
            items = items,
            onRate = viewModel::rate,
            onSureness = viewModel::sureness,
            onDone = viewModel::ratingsDone,
            onBack = { navController.popBackStack(Route.TABS, inclusive = false) },
        )
    }

    composable(Route.CHECK_DONE) {
        val state by viewModel.done.collectAsStateWithLifecycle()
        CheckDoneScreen(
            state = state,
            onCard = onCard,
            onSave = {
                viewModel.save { quieter ->
                    if (quieter) {
                        navController.navigate(Route.QUIETER)
                    } else {
                        navController.popBackStack(Route.TABS, inclusive = false)
                    }
                }
            },
            onBack = { navController.popBackStack(Route.TABS, inclusive = false) },
        )
    }

    composable(Route.QUIETER) {
        val state by viewModel.quieter.collectAsStateWithLifecycle()
        state?.let {
            QuieterScreen(
                state = it,
                onSummary = {
                    viewModel.dismissQuieter()
                    onSummary()
                },
                onOkay = {
                    viewModel.dismissQuieter()
                    navController.popBackStack(Route.TABS, inclusive = false)
                },
            )
        }
    }
}

private fun NavGraphBuilder.askRoutes(
    viewModel: AskViewModel,
    navController: NavHostController,
    back: () -> Unit,
) {
    composable(Route.ASK) {
        val state by viewModel.ask.collectAsStateWithLifecycle()
        AskScreen(
            state = state,
            onQuestion = viewModel::setQuestion,
            onCard = {
                viewModel.openCard(it)
                navController.navigate(Route.CARD)
            },
            onBack = back,
        )
    }

    composable(Route.CARD) {
        val state by viewModel.card.collectAsStateWithLifecycle()
        CardScreen(state = state, onBack = back)
    }
}

/**
 * The ways into a therapist's plan, and the page a plan can come off.
 *
 * ADDENDUM-03 Parts 5 and 6. The chooser is the only door: everywhere in the app that
 * used to open the camera opens this instead, so nothing reaches the camera without
 * passing the four ways, and all four ways reach one confirmation screen. Its own
 * builder because it is one flow rather than seven places, and because nothing else in
 * the app can reach the middle of it.
 *
 * The three ways that are not the camera each hold their own [PlanWaysViewModel],
 * scoped to their own entry on the back stack. Each is one screen with one answer on
 * it, so there is nothing to share between them, and leaving a screen is then what
 * lets go of what it was holding, which for the spoken way is the microphone.
 */
private fun NavGraphBuilder.scanRoutes(
    viewModel: ScanViewModel,
    modelsViewModel: ModelsViewModel,
    navController: NavHostController,
    back: () -> Unit,
) {
    composable(Route.PLAN_WAYS) {
        PlanWaysScreen(
            onCamera = {
                viewModel.open()
                navController.navigate(Route.SCAN)
            },
            onSay = { navController.navigate(Route.PLAN_SAY) },
            onPick = { navController.navigate(Route.PLAN_PICK) },
            onType = { navController.navigate(Route.PLAN_TYPE) },
            onBack = back,
        )
    }

    planWaysRoutes(viewModel, navController, back)

    composable(Route.SCAN) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val camera = remember { Camera(context) }
        val ask = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { viewModel.allowedNow() }

        LifecycleResumeEffect(Unit) {
            viewModel.allowedNow()
            onPauseOrDispose { }
        }

        ScanScreen(
            state = state,
            onTake = { camera.take(viewModel::took) },
            onAllow = { ask.launch(android.Manifest.permission.CAMERA) },
            onFinish = {
                viewModel.finished()
                navController.navigate(Route.PAGE_FOUND)
            },
            onBack = back,
            preview = { CameraPreview(camera) },
        )
    }

    composable(Route.PAGE_FOUND) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        PageFoundScreen(
            state = state,
            onWho = viewModel::setWho,
            onAddExercises = {
                viewModel.proposePlan()
                navController.navigate(Route.PLAN_CONFIRM)
            },
            // Part 7's reading is Phase 4 and behind a flag. Until then this keeps the
            // page, which is what every path here does anyway, rather than offering
            // something the app cannot do yet.
            onExplain = { viewModel.keep { navController.popBackStack(Route.TABS, false) } },
            onKeep = { viewModel.keep { navController.popBackStack(Route.TABS, false) } },
            onBack = back,
        )
    }

    composable(Route.MODELS) {
        val state by modelsViewModel.state.collectAsStateWithLifecycle()
        ModelsScreen(
            state = state,
            actions = ModelsActions(
                onAdd = modelsViewModel::showTerms,
                onRemove = modelsViewModel::remove,
                onTerms = modelsViewModel::showTerms,
                onAgree = modelsViewModel::agree,
                onCloseTerms = modelsViewModel::closeTerms,
                onByHandSeen = modelsViewModel::byHandSeen,
            ),
            onBack = back,
        )
    }

    composable(Route.DOCUMENTS) {
        val state by viewModel.documents.collectAsStateWithLifecycle()
        DocumentsScreen(
            state = state,
            onOpen = viewModel::openDocument,
            onRemove = viewModel::removeDocument,
            onBack = back,
        )
    }

    composable(Route.PLAN_CONFIRM) {
        val draft by viewModel.draft.collectAsStateWithLifecycle()
        PlanConfirmScreen(
            state = draft,
            onLabel = viewModel::setPlanLabel,
            onDrop = viewModel::dropItem,
            onReviewDay = viewModel::setPlanReviewDay,
            onTheirsRead = viewModel::theirsRead,
            onSave = { viewModel.savePlan { navController.popBackStack(Route.TABS, false) } },
            onBack = back,
        )
    }
}

/**
 * The three ways into a plan that have no page behind them. ADDENDUM-03 Part 6.
 *
 * All three end on the same confirmation screen the photographed sheet ends on, with
 * the same draft behind it, so "nothing is saved unconfirmed" is one screen's job and
 * not four.
 */
private fun NavGraphBuilder.planWaysRoutes(
    scan: ScanViewModel,
    navController: NavHostController,
    back: () -> Unit,
) {
    composable(Route.PLAN_TYPE) {
        val ways: PlanWaysViewModel = viewModel()
        val state by ways.state.collectAsStateWithLifecycle()
        PlanTypeScreen(
            state = state,
            onTyped = ways::setTyped,
            onContinue = {
                scan.propose(ways.typedItems())
                navController.navigate(Route.PLAN_CONFIRM)
            },
            onBack = back,
        )
    }

    composable(Route.PLAN_PICK) {
        val ways: PlanWaysViewModel = viewModel()
        val state by ways.state.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { ways.open() }
        PlanPickScreen(
            state = state,
            onMovement = ways::togglePicked,
            onContinue = {
                scan.propose(ways.pickedItems())
                navController.navigate(Route.PLAN_CONFIRM)
            },
            onBack = back,
        )
    }

    composable(Route.PLAN_SAY) {
        val ways: PlanWaysViewModel = viewModel()
        val state by ways.state.collectAsStateWithLifecycle()
        // The microphone is asked for on the tap that needs it, never on the way in,
        // and only on a phone that has already said it can do this without a server.
        val ask = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { ways.microphoneNow() }

        LaunchedEffect(Unit) { ways.open() }
        // Whether the permission is held is asked again on every return, because the
        // person may have just granted it in Settings. The recogniser is let go on the
        // way out rather than merely stopped, so that the app is not holding the
        // microphone off screen even for as long as one last result would take.
        LifecycleResumeEffect(Unit) {
            ways.microphoneNow()
            onPauseOrDispose { ways.release() }
        }

        PlanSayScreen(
            state = state,
            onAllow = { ask.launch(android.Manifest.permission.RECORD_AUDIO) },
            onListen = ways::listen,
            onStop = ways::stopListening,
            onContinue = {
                scan.propose(ways.heardItems())
                navController.navigate(Route.PLAN_CONFIRM)
            },
            onType = { navController.navigate(Route.PLAN_TYPE) },
            onBack = back,
        )
    }
}

/**
 * The screens that hang off the Sessions tab.
 *
 * Their own builder because Tabs was one line past detekt's length rule, and because
 * these four belong together: a week read back, a session read back, a session done
 * away from the phone, and one added after the fact.
 */
private fun NavGraphBuilder.sessionsRoutes(
    viewModel: SessionsViewModel,
    sessionViewModel: SessionViewModel,
    navController: NavHostController,
    back: () -> Unit,
    onCard: (String) -> Unit,
) {
    composable(Route.SUNDAY) {
        val state by viewModel.sundayReview.collectAsStateWithLifecycle()
        SundayScreen(state = state, onCard = onCard, onBack = back)
    }

    composable(Route.PAST_SESSION) {
        val state by viewModel.past.collectAsStateWithLifecycle()
        PastSessionScreen(
            state = state,
            onCount = viewModel::correctPast,
            onRepeat = {
                sessionViewModel.repeat(state.runId)
                navController.navigate(Route.SESSION)
            },
            onRemove = { viewModel.removePast(back) },
            onBack = back,
        )
    }

    composable(Route.PHONE_FREE) {
        val state by sessionViewModel.phoneFree.collectAsStateWithLifecycle()
        PhoneFreeScreen(
            state = state,
            onRead = sessionViewModel::readPhoneFree,
            onDidIt = sessionViewModel::phoneFreeDone,
            onManaged = sessionViewModel::phoneFreeManaged,
            onSave = { sessionViewModel.savePhoneFree(back) },
            onBack = back,
        )
    }

    composable(Route.LOG_PAST) {
        val state by viewModel.logPast.collectAsStateWithLifecycle()
        LogPastScreen(
            state = state,
            onDay = viewModel::chooseLogDay,
            onMovement = viewModel::toggleLogMovement,
            onSave = { viewModel.saveLogPast(back) },
            onBack = back,
        )
    }
}

/**
 * What Settings can do, in one place.
 *
 * Built here rather than inline because the You tab and the older Settings route both
 * show the same screen, and two copies of eleven callbacks is how one of them ends up
 * missing a row.
 */
private fun settingsActions(
    viewModel: SettingsViewModel,
    askViewModel: AskViewModel,
    scanViewModel: ScanViewModel,
    modelsViewModel: ModelsViewModel,
    navController: NavHostController,
) = SettingsActions(
    onGettingAround = { navController.navigate(Route.GETTING_AROUND) },
    onTherapist = viewModel::setTherapist,
    onWeighsIn = viewModel::setWeighsIn,
    onShowNumbers = viewModel::setShowNumbers,
    onExclusions = { navController.navigate(Route.LEAVE_OUT) },
    onPattern = { navController.navigate(Route.PATTERN) },
    onPacing = { navController.navigate(Route.PACING) },
    onData = { navController.navigate(Route.DATA) },
    onReminders = { navController.navigate(Route.REMINDERS) },
    onTryItAndSee = viewModel::setTryItAndSee,
    onAsk = {
        askViewModel.openAsk()
        navController.navigate(Route.ASK)
    },
    onWeekTarget = viewModel::setWeekTarget,
    onDaily = { viewModel.setReminder(ReminderKind.Daily, it) },
    onScan = { navController.navigate(Route.PLAN_WAYS) },
    onDocuments = {
        scanViewModel.openDocuments()
        navController.navigate(Route.DOCUMENTS)
    },
    onModels = {
        modelsViewModel.open()
        navController.navigate(Route.MODELS)
    },
    onExtras = viewModel::setExtras,
    onList = {
        viewModel.openList()
        navController.navigate(Route.YOUR_LIST)
    },
    onKit = {
        viewModel.openKit()
        navController.navigate(Route.KIT)
    },
    onPlans = {
        viewModel.openPlans()
        navController.navigate(Route.PLANS)
    },
    // ADDENDUM-03 Part 20 moves the walkthrough here from Progress. The route
    // opens it for itself, so this only has to be the door.
    onPlaces = { navController.navigate(Route.PLACES) },
    onAudio = viewModel::setAudio,
    onAbout = { navController.navigate(Route.ABOUT) },
)

/**
 * The screens ADDENDUM-03 Part 20 adds to You, and the two Progress shares with it.
 *
 * Their own builder rather than more of [settingsRoutes], because none of them is a
 * setting: they are the person's own list, what is in their room, whose plans they
 * are on, and what this app is. The add screen and the months are here rather than
 * with Progress because both are reached from both tabs, and a screen with two ways
 * in should be declared once.
 */
private fun NavGraphBuilder.youRoutes(
    viewModel: SettingsViewModel,
    steadyViewModel: SteadyViewModel,
    therapistPageViewModel: TherapistPageViewModel,
    navController: NavHostController,
    back: () -> Unit,
) {
    composable(Route.YOUR_LIST) {
        val state by viewModel.yourList.collectAsStateWithLifecycle()
        LifecycleResumeEffect(Unit) {
            viewModel.openList()
            onPauseOrDispose { }
        }
        YourListScreen(
            state = state,
            onOpen = {
                viewModel.openItem(it)
                navController.navigate(Route.YOUR_ITEM)
            },
            onAdd = { navController.navigate(Route.ADD_ITEM) },
            onBack = back,
        )
    }

    composable(Route.YOUR_ITEM) {
        val state by viewModel.yourList.collectAsStateWithLifecycle()
        YourItemScreen(
            state = state,
            onTyped = viewModel::setItemText,
            onSave = { viewModel.saveItem(back) },
            onRemove = { viewModel.removeItem(back) },
            onBack = back,
        )
    }

    composable(Route.ADD_ITEM) {
        val state by viewModel.settings.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { viewModel.openSettings() }
        // Nothing until the profile is read: the six chips depend on how somebody
        // gets around, and the wrong six for a frame is the app describing a life
        // that is not theirs.
        if (state.loaded) {
            AddSomethingScreen(
                way = state.gettingAround,
                onAdd = { text, domain -> viewModel.addItem(text, domain) { back() } },
                onBack = back,
            )
        }
    }

    composable(Route.KIT) {
        val state by viewModel.kit.collectAsStateWithLifecycle()
        KitScreen(
            state = state,
            onKit = viewModel::toggleKit,
            onArms = viewModel::setChairArms,
            onHeight = viewModel::setChairHeight,
            onBack = back,
        )
    }

    composable(Route.PLANS) {
        val state by viewModel.plansState.collectAsStateWithLifecycle()
        PlansScreen(
            state = state,
            onReviewDay = viewModel::setReviewDay,
            onArchive = viewModel::archivePlan,
            onExport = therapistPageViewModel::share,
            onBack = back,
        )
    }

    composable(Route.MONTHS) {
        val state by steadyViewModel.months.collectAsStateWithLifecycle()
        MonthsScreen(
            state = state,
            onEarlier = steadyViewModel::earlierMonth,
            onLater = steadyViewModel::laterMonth,
            onBack = back,
        )
    }

    composable(Route.ABOUT) {
        AboutScreen(onPrivacy = { navController.navigate(Route.PRIVACY) }, onBack = back)
    }

    composable(Route.PRIVACY) {
        PrivacyScreen(onBack = back)
    }
}

/**
 * The card and the places walkthrough. ADDENDUM-03 Parts 11 and 8.
 *
 * Their own group rather than inside the settings routes, because neither is a
 * setting: both are things somebody does once and both are reached from Progress.
 */
private fun NavGraphBuilder.asideRoutes(
    cardViewModel: CardViewModel,
    placesViewModel: PlacesViewModel,
    back: () -> Unit,
) {
    composable(Route.SEND_CARD) {
        val state by cardViewModel.state.collectAsStateWithLifecycle()
        CardScreen(
            state = state,
            onLine = cardViewModel::setLine,
            onSend = cardViewModel::send,
            onBack = back,
        )
    }

    composable(Route.PLACES) {
        val state by placesViewModel.state.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { placesViewModel.open() }
        PlacesScreen(
            state = state,
            onSaid = placesViewModel::said,
            onStartAgain = placesViewModel::startAgain,
            onBack = back,
        )
    }
}

private fun NavGraphBuilder.settingsRoutes(
    viewModel: SettingsViewModel,
    askViewModel: AskViewModel,
    scanViewModel: ScanViewModel,
    modelsViewModel: ModelsViewModel,
    navController: NavHostController,
    back: () -> Unit,
    onSummary: () -> Unit,
    onDeleted: () -> Unit,
) {
    composable(Route.SETTINGS) {
        val state by viewModel.settings.collectAsStateWithLifecycle()
        SettingsScreen(
            state = state,
            actions = settingsActions(viewModel, askViewModel, scanViewModel, modelsViewModel, navController),
            onBack = back,
        )
    }

    composable(Route.GETTING_AROUND) {
        val state by viewModel.settings.collectAsStateWithLifecycle()
        GettingAroundScreen(
            selected = state.gettingAround,
            onChoose = {
                viewModel.setGettingAround(it)
                back()
            },
            onBack = back,
        )
    }

    composable(Route.LEAVE_OUT) {
        val state by viewModel.settings.collectAsStateWithLifecycle()
        LeaveOutSettingsScreen(
            selected = state.exclusions,
            labels = Exclusion.entries.map {
                it to stringResource(viewModel.labelFor(it))
            },
            onToggle = viewModel::toggleExclusion,
            onBack = back,
        )
    }

    composable(Route.PATTERN) {
        val pem by viewModel.pattern.collectAsStateWithLifecycle()
        PatternScreen(
            selected = pem,
            onChoose = {
                viewModel.setPattern(it)
                back()
            },
            onBack = back,
        )
    }

    composable(Route.REMINDERS) {
        val state by viewModel.settings.collectAsStateWithLifecycle()
        val ask = rememberLauncherForActivityResult(RequestPermission()) { viewModel.refresh() }
        RemindersScreen(
            on = state.remindersOn,
            left = state.remindersLeft,
            blocked = state.remindersBlocked,
            hasAppointment = state.hasAppointment,
            onToggle = { kind, on ->
                // The one moment this app asks for the notification permission,
                // and only because somebody just asked for something that needs it.
                if (on) ask.launch(Reminding.POST_NOTIFICATIONS)
                viewModel.setReminder(kind, on)
            },
            onBack = back,
        )
    }

    composable(Route.DATA) {
        DataRoute(
            viewModel = viewModel,
            onSummary = onSummary,
            onDeleted = onDeleted,
            onBack = back,
        )
    }

    composable(Route.PACING) {
        val state by viewModel.settings.collectAsStateWithLifecycle()
        PacingScreen(
            minutes = state.envelopeMinutes,
            days = state.envelopeDays,
            onMinutes = viewModel::setEnvelopeMinutes,
            onDays = viewModel::setEnvelopeDays,
            onStop = {
                // LOGIC.md section 7: the app re-asks the pattern question when
                // somebody leaves, so leaving lands on that question rather than
                // back in a list of switches.
                viewModel.stopPacing()
                back()
                navController.navigate(Route.PATTERN)
            },
            onBack = back,
        )
    }
}
