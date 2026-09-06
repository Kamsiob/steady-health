package com.kamsiob.steadyhealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.ui.nav.Tab
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * The tab bar from DESIGN.md section 3: white, 82 dp, a hairline on top, three
 * items, a filled icon over a label, navy when it is where you are.
 *
 * Two deliberate departures from the letter of that line, both recorded in
 * DECISIONS.md. The label is 11 sp as specified, which is small, but it is sp so
 * it grows with the system font setting. And the inactive colour is ink3-text
 * rather than ink3, because ink3 on white is 2.7:1 and the floor asks for 4.5:1;
 * ink3 stays exactly as it is everywhere it is a shape rather than a word.
 */
@Composable
fun SteadyTabBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().background(SteadyPalette.White)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HAIRLINE)
                .background(SteadyPalette.Hairline),
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(BAR_HEIGHT),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab ->
                TabItem(tab = tab, selected = tab == selected, onSelect = { onSelect(tab) })
            }
        }
    }
}

@Composable
private fun TabItem(tab: Tab, selected: Boolean, onSelect: () -> Unit) {
    val colour = if (selected) SteadyPalette.Navy else SteadyPalette.Ink3Text
    val label = stringResource(tab.label)

    Column(
        modifier = Modifier
            .selectable(selected = selected, role = Role.Tab, onClick = onSelect)
            .padding(horizontal = SteadySpacing.Screen, vertical = SteadySpacing.ListGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ICON_TO_LABEL),
    ) {
        Icon(
            painter = painterResource(tab.icon),
            // The label underneath says the same thing, and a screen reader that
            // reads both says everything twice.
            contentDescription = null,
            tint = colour,
            modifier = Modifier.size(ICON),
        )
        SteadyText(text = label, style = SteadyType.TabLabel, color = colour)
    }
}

private val BAR_HEIGHT = 82.dp
private val HAIRLINE = 1.dp
private val ICON = 26.dp
private val ICON_TO_LABEL = 3.dp
