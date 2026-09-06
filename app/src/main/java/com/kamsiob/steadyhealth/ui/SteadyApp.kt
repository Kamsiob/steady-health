package com.kamsiob.steadyhealth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.ui.components.SteadyTabBar
import com.kamsiob.steadyhealth.ui.nav.Tab
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * The shell: three tabs over one background, with the tab bar pinned.
 *
 * Phase 0 builds the frame and nothing inside it. The three screens below are
 * placeholders that say so, rather than half-built versions of the real ones,
 * because a screen that looks finished and is not is the thing a later session
 * trusts by mistake.
 */
@Composable
fun SteadyApp() {
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SteadyPalette.Ground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .statusBarsPadding(),
        ) {
            when (tab) {
                Tab.Today -> Placeholder(Tab.Today)
                Tab.Move -> Placeholder(Tab.Move)
                Tab.Abilities -> Placeholder(Tab.Abilities)
            }
        }
        SteadyTabBar(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}

/**
 * What a tab looks like before its screen exists.
 *
 * It names the phase that fills it in, so anybody who installs this build knows
 * what they are looking at rather than wondering whether something failed.
 */
@Composable
private fun Placeholder(tab: Tab) {
    Box(
        modifier = Modifier.fillMaxSize().padding(SteadySpacing.Screen),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SteadyText(
                text = stringResource(tab.label),
                style = SteadyType.ScreenTitleBig,
                color = SteadyPalette.Navy,
            )
            SteadyText(
                text = stringResource(com.kamsiob.steadyhealth.R.string.placeholder_phase),
                style = SteadyType.Body,
                color = SteadyPalette.Ink2,
            )
        }
    }
}
