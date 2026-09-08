package com.kamsiob.steadyhealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType

/**
 * L1: the sand block a screen uses to explain itself once. DESIGN.md 4b.
 *
 * Never a modal, never a tour, never a dark overlay. It sits at the top of the
 * content, says one sentence, and the small dismiss ends it for good.
 */
@Composable
fun SaysOnce(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteadySpacing.Inside))
            .background(SteadyPalette.Sand)
            .padding(SteadySpacing.InsideTight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteadySpacing.Tight),
    ) {
        SteadyText(
            text = text,
            style = SteadyType.Body,
            color = SteadyPalette.Ink2,
            modifier = Modifier.weight(1f),
        )
        val dismiss = stringResource(R.string.says_dismiss)
        Box(
            modifier = Modifier
                .heightIn(min = SteadySpacing.TapTarget)
                .clip(RoundedCornerShape(SteadySpacing.Tight))
                .clickable(role = Role.Button, onClick = onDismiss)
                .semantics { contentDescription = dismiss }
                .padding(horizontal = SteadySpacing.Tight),
            contentAlignment = Alignment.Center,
        ) {
            SteadyText(
                text = stringResource(R.string.says_close_mark),
                style = SteadyType.SectionTitle,
                color = SteadyPalette.Ink3Text,
            )
        }
    }
}
