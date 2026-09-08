package com.kamsiob.steadyhealth.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.session.Felt
import com.kamsiob.steadyhealth.session.Stage

/**
 * One route for the whole session.
 *
 * The seven screens are one destination whose content changes, not seven destinations.
 * That is deliberate: a back gesture in the middle of a set should not drop somebody
 * onto the count-in screen of the movement before, and a session is one thing that is
 * happening rather than a place somebody navigated to.
 *
 * The exits are how a session ends. There is no back button on any of these screens.
 */
@Composable
fun SessionHost(
    viewModel: SessionViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A locked screen or an incoming call pauses the session rather than letting it
    // run on behind them, and coming back picks it up where it stopped.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.interrupted()
                Lifecycle.Event.ON_START -> viewModel.returned()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val runner by viewModel.runner.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val asking by viewModel.askingWhereItHurts.collectAsStateWithLifecycle()
    val current = runner ?: return

    if (asking) {
        HurtScreen(onArea = viewModel::hurtsIn, modifier = modifier)
        return
    }

    if (current.finished) {
        SessionDoneScreen(
            state = done,
            onFelt = viewModel::setFelt,
            onCorrect = viewModel::correct,
            onSave = onFinished,
            onChangeNext = onFinished,
            modifier = modifier,
        )
        return
    }

    val state = current.toUiState(
        speaking = viewModel.speaking,
        unit = "",
        stepOf = stringResource(
            R.string.check_step,
            current.at + 1,
            current.plan.steps.size,
        ),
    )
    val actions = SessionActions(
        onReady = viewModel::ready,
        onSkipCountIn = viewModel::skipCountIn,
        onTap = viewModel::rep,
        onEndSet = viewModel::endSet,
        onSkipRest = viewModel::skipRest,
        onPause = viewModel::pause,
        onEasier = viewModel::makeItEasier,
        onSkip = viewModel::skipThis,
        onEnough = viewModel::enough,
        onHurts = viewModel::hurts,
        onSpeaker = viewModel::toggleSpeaker,
    )

    when (current.stage) {
        Stage.Ready -> ReadyScreen(state, actions, modifier)
        is Stage.CountIn -> CountInScreen(state, actions, modifier)
        Stage.Live -> LiveScreen(state, actions, modifier)
        is Stage.Rest -> RestScreen(state, actions, modifier)
        Stage.Done -> SessionDoneScreen(
            state = done,
            onFelt = viewModel::setFelt,
            onCorrect = viewModel::correct,
            onSave = onFinished,
            onChangeNext = onFinished,
            modifier = modifier,
        )
    }
}

/** Used by the done screen's copy, kept here so the host owns the whole flow. */
internal fun Felt.id(): String = name
