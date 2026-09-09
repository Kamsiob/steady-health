package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.TagPill
import com.kamsiob.steadyhealth.ui.components.TextEntry
import com.kamsiob.steadyhealth.ui.onboarding.Starters
import com.kamsiob.steadyhealth.ui.onboarding.starterLabel
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing

/**
 * Add something you would like to be able to do. ADDENDUM-03 Part 18.
 *
 * The same question as O3 and deliberately the same shape: the same words, the same
 * hint, the same six chips for this way of getting around, and the sentence kept
 * exactly as it was typed. Setup asks it once and this asks it whenever, so the two
 * cannot end up reading as different questions about the same thing.
 *
 * It asks for what O3 asks for and no more. No rating, no date, no ability to choose
 * from: the rating is what the monthly check is for, and the ability comes from the
 * chip or, for a typed sentence, from where O3 puts one.
 */
@Composable
fun AddSomethingScreen(
    way: GettingAround,
    onAdd: (String, AbilityDomain) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var typed by rememberSaveable { mutableStateOf("") }

    SteadyScreen(
        title = stringResource(R.string.add_item_title),
        onBack = onBack,
        modifier = modifier,
        footer = {
            if (typed.isNotBlank()) {
                PrimaryButton(
                    label = stringResource(R.string.o3_add),
                    onClick = { onAdd(typed, AbilityDomain.Go) },
                )
            }
        },
    ) {
        SectionTitle(stringResource(R.string.o3_question))

        TextEntry(
            value = typed,
            onValue = { typed = it },
            hint = stringResource(R.string.o3_hint),
            imeAction = ImeAction.Done,
            onSubmit = { onAdd(typed, AbilityDomain.Go) },
        )

        SectionTitle(stringResource(R.string.o3_or))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
            verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
        ) {
            Starters.forWay(way).forEach { starter ->
                val label = stringResource(starterLabel(starter.id))
                TagPill(
                    label = label,
                    selected = false,
                    onClick = { onAdd(label, starter.domain) },
                )
            }
        }

        Paragraph(stringResource(R.string.add_item_why))
    }
}
