package com.kamsiob.steadyhealth.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.session.SessionPlan
import com.kamsiob.steadyhealth.ui.session.SessionHost
import com.kamsiob.steadyhealth.ui.session.SessionViewModel

/**
 * The whole first run: five screens, one of which is a session.
 *
 * There is no navigation graph here on purpose. Onboarding is a straight line with a
 * stored position, so the step in the database is the only source of truth about
 * where somebody is, and closing the app halfway resumes exactly there rather than
 * starting again.
 */
@Composable
fun OnboardingFlow(onFinished: () -> Unit) {
    val viewModel: OnboardingViewModel = viewModel()
    val sessionViewModel: SessionViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plan by viewModel.firstPlan.collectAsStateWithLifecycle()
    val fallback = stringResource(R.string.o3_default)

    when (state.step) {
        Step.Hook -> HookScreen(
            state = state,
            onLanguage = viewModel::setLanguage,
            onNext = { viewModel.go(Step.HowYouGetAround) },
        )

        Step.HowYouGetAround -> HowYouGetAroundScreen(onChoose = viewModel::setGettingAround)

        Step.TheirWords -> TheirWordsScreen(
            state = state,
            onTyped = viewModel::setTyped,
            onKeep = viewModel::keep,
            onNext = { viewModel.go(Step.FirstSession) },
            onSkip = { viewModel.skipWanted(fallback) },
        )

        Step.FirstSession -> FirstSession(
            plan = plan,
            sessionViewModel = sessionViewModel,
            onDone = viewModel::sessionFinished,
        )

        Step.AfterTheSession -> AfterTheSessionScreen(
            state = state,
            onExclusion = viewModel::toggleExclusion,
            onNothing = viewModel::clearExclusions,
            onChair = viewModel::setChair,
            onDone = { viewModel.finish(onFinished) },
        )
    }
}

/**
 * O4, in two halves: what you are about to do, then doing it.
 *
 * The session itself is the ordinary session machinery with an ordinary plan of one
 * movement. Nothing about the first session is a special case in the engine, which is
 * why the first thing somebody ever does in this app is a real session and is saved
 * as one.
 */
@Composable
private fun FirstSession(
    plan: SessionPlan?,
    sessionViewModel: SessionViewModel,
    onDone: () -> Unit,
) {
    var running by rememberSaveable { mutableStateOf(false) }
    val started = remember { mutableStateOf(false) }

    if (!running) {
        FirstSessionScreen(
            movement = plan?.steps?.firstOrNull()?.movement,
            onStart = { running = true },
        )
        return
    }

    // Starting is a side effect. Doing it in the click handler would run it again on
    // every recomposition after a rotation.
    LaunchedEffect(plan) {
        val ready = plan ?: return@LaunchedEffect
        if (!started.value) {
            started.value = true
            sessionViewModel.start(ready, first = true)
        }
    }

    SessionHost(viewModel = sessionViewModel, onFinished = onDone)
}
