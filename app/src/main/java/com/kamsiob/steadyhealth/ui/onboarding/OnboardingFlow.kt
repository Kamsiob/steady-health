package com.kamsiob.steadyhealth.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kamsiob.steadyhealth.domain.GettingAround

/**
 * Setup, in order, with the back button working at every step.
 *
 * The order is ONBOARDING.md's, with two conditional skips it specifies: the
 * readiness screen goes straight on when every answer is no, and naming the first
 * walk only happens when the walking ladder is visible, which it is not for
 * somebody who is mostly in bed.
 */
@Composable
fun OnboardingFlow(onFinished: () -> Unit) {
    val viewModel: OnboardingViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state.step) {
        OnboardingStep.Welcome -> WelcomeScreen(
            state = state,
            onLanguage = viewModel::setLanguage,
            onNext = { viewModel.go(OnboardingStep.ThreeThings) },
        )

        OnboardingStep.ThreeThings -> ThreeThingsScreen(
            onNext = { viewModel.go(OnboardingStep.HowYouGetAround) },
        )

        OnboardingStep.HowYouGetAround -> HowYouGetAroundScreen(
            state = state,
            onChoose = viewModel::setGettingAround,
            onTherapist = viewModel::setWithTherapist,
            onBack = { viewModel.go(OnboardingStep.ThreeThings) },
            onNext = { viewModel.go(OnboardingStep.AboutYou) },
        )

        OnboardingStep.AboutYou -> AboutYouScreen(
            state = state,
            onUnits = viewModel::setUnits,
            onHeight = viewModel::setHeight,
            onAge = viewModel::setAge,
            onBack = { viewModel.go(OnboardingStep.HowYouGetAround) },
            onNext = { viewModel.go(OnboardingStep.WhereYouAreStarting) },
        )

        OnboardingStep.WhereYouAreStarting -> WhereYouAreStartingScreen(
            state = state,
            onChair = viewModel::setChair,
            onStairs = viewModel::setStairs,
            onWalk = viewModel::setWalk,
            onFloor = viewModel::setFloor,
            onPem = viewModel::setPem,
            onBack = { viewModel.go(OnboardingStep.AboutYou) },
            onNext = { viewModel.go(OnboardingStep.LeaveOut) },
        )

        OnboardingStep.LeaveOut -> LeaveOutScreen(
            state = state,
            onToggle = viewModel::toggleExclusion,
            onNothing = {
                viewModel.clearExclusions()
                viewModel.go(OnboardingStep.Readiness)
            },
            onBack = { viewModel.go(OnboardingStep.WhereYouAreStarting) },
            onNext = { viewModel.go(OnboardingStep.Readiness) },
        )

        OnboardingStep.Readiness -> ReadinessScreen(
            state = state,
            onAnswer = viewModel::setReadiness,
            onBack = { viewModel.go(OnboardingStep.LeaveOut) },
            onNext = { viewModel.go(OnboardingStep.WhatYouWant) },
        )

        OnboardingStep.WhatYouWant -> WhatYouWantScreen(
            state = state,
            onTyped = viewModel::setTyped,
            onAdd = viewModel::addWanted,
            onRemove = viewModel::removeWanted,
            onBack = { viewModel.go(OnboardingStep.Readiness) },
            onNext = { viewModel.go(OnboardingStep.RateThem) },
        )

        OnboardingStep.RateThem -> RateThemScreen(
            state = state,
            onRate = viewModel::rateWanted,
            onBack = { viewModel.go(OnboardingStep.WhatYouWant) },
            onNext = { viewModel.go(OnboardingStep.Anchor) },
        )

        OnboardingStep.Anchor -> AnchorScreen(
            state = state,
            onAnchor = viewModel::setAnchor,
            onBack = { viewModel.go(OnboardingStep.RateThem) },
            onNext = { viewModel.go(OnboardingStep.FirstWeighIn) },
        )

        OnboardingStep.FirstWeighIn -> FirstWeighInScreen(
            state = state,
            onWeight = viewModel::setFirstWeight,
            onBack = { viewModel.go(OnboardingStep.Anchor) },
            onSave = {
                viewModel.saveFirstWeighIn {
                    // Naming a walk only makes sense where there is one to name.
                    if (state.gettingAround == GettingAround.InBed) {
                        viewModel.finish(onFinished)
                    } else {
                        viewModel.go(OnboardingStep.NameFirstWalk)
                    }
                }
            },
        )

        OnboardingStep.NameFirstWalk -> NameFirstWalkScreen(
            state = state,
            onName = viewModel::setFirstWalkName,
            onBack = { viewModel.go(OnboardingStep.FirstWeighIn) },
            onSave = { viewModel.finish(onFinished) },
        )
    }
}
