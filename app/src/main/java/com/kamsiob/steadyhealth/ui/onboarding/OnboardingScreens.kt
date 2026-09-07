package com.kamsiob.steadyhealth.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Anchor
import com.kamsiob.steadyhealth.domain.ChairEase
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.FloorAccess
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.domain.PemAnswer
import com.kamsiob.steadyhealth.domain.ReadinessFlag
import com.kamsiob.steadyhealth.domain.Stairs
import com.kamsiob.steadyhealth.domain.Units
import com.kamsiob.steadyhealth.domain.WalkTolerance
import com.kamsiob.steadyhealth.ui.components.Dial
import com.kamsiob.steadyhealth.ui.components.Hero
import com.kamsiob.steadyhealth.ui.components.HeroExplain
import com.kamsiob.steadyhealth.ui.components.HeroHeadline
import com.kamsiob.steadyhealth.ui.components.HeroLabel
import com.kamsiob.steadyhealth.ui.components.HeroSky
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.MoveGlyph
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.Pill
import com.kamsiob.steadyhealth.ui.components.PrimaryButton
import com.kamsiob.steadyhealth.ui.components.RatingRow
import com.kamsiob.steadyhealth.ui.components.SecondaryButton
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen
import com.kamsiob.steadyhealth.ui.components.Stepper
import com.kamsiob.steadyhealth.ui.components.TalkGlyph
import com.kamsiob.steadyhealth.ui.components.TextEntry
import com.kamsiob.steadyhealth.ui.components.ThreeUpChoice
import com.kamsiob.steadyhealth.ui.components.WeighGlyph
import com.kamsiob.steadyhealth.ui.theme.SteadyPalette
import com.kamsiob.steadyhealth.ui.theme.SteadyShapes
import com.kamsiob.steadyhealth.ui.theme.SteadySpacing
import com.kamsiob.steadyhealth.ui.theme.SteadyText
import com.kamsiob.steadyhealth.ui.theme.SteadyType
import com.kamsiob.steadyhealth.util.Convert

/**
 * Setup, screen by screen, with the copy ONBOARDING.md fixes word for word.
 *
 * Eleven screens, most of them one tap. The target the specification measures is
 * the first weigh-in and the first tracked ability both saved inside two minutes,
 * so nothing here asks twice and nothing waits for a Continue it does not need.
 */

/** Screen 1. The hero, the language, one sentence. */
@Composable
fun WelcomeScreen(state: OnboardingState, onLanguage: (String) -> Unit, onNext: () -> Unit) {
    SteadyScreen(
        title = null,
        onBack = null,
        footer = {
            PrimaryButton(label = stringResource(R.string.welcome_start), onClick = onNext)
            SteadyText(
                text = stringResource(R.string.welcome_caption),
                style = SteadyType.Caption,
                color = SteadyPalette.Ink3Text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
    ) {
        // Tall on purpose. DESIGN.md section 2 names the welcome hero as the one
        // place empty space has a reason.
        Hero(sky = HeroSky.Morning, minHeight = WELCOME_HERO) {
            HeroLabel(stringResource(R.string.app_name))
            HeroHeadline(stringResource(R.string.welcome_headline))
            HeroExplain(stringResource(R.string.welcome_line))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap)) {
            listOf(
                "en" to stringResource(R.string.language_english),
                "es" to stringResource(R.string.language_spanish),
                "zh" to stringResource(R.string.language_chinese),
                "ar" to stringResource(R.string.language_arabic),
            ).forEach { (tag, label) ->
                Pill(label = label, selected = state.language == tag, onClick = { onLanguage(tag) })
            }
        }
    }
}

/** Screen 2. Three lines, read in ten seconds. */
@Composable
fun ThreeThingsScreen(onNext: () -> Unit) {
    SteadyScreen(
        title = null,
        onBack = null,
        footer = { PrimaryButton(label = stringResource(R.string.three_okay), onClick = onNext) },
    ) {
        SteadyText(
            text = stringResource(R.string.three_title),
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.padding(vertical = SteadySpacing.Inside),
        )
        ThreeThingsRow(stringResource(R.string.three_weigh)) { WeighGlyph(Modifier.size(GLYPH)) }
        ThreeThingsRow(stringResource(R.string.three_say)) { TalkGlyph(Modifier.size(GLYPH)) }
        ThreeThingsRow(stringResource(R.string.three_move)) { MoveGlyph(Modifier.size(GLYPH)) }
    }
}

@Composable
private fun ThreeThingsRow(text: String, glyph: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = SteadySpacing.ListGap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteadySpacing.Inside),
    ) {
        Box(Modifier.size(GLYPH)) { glyph() }
        Paragraph(text = text, modifier = Modifier.weight(1f), color = SteadyPalette.Ink)
    }
}

/**
 * Screen 2b. The first real question, because it decides which version of the
 * app the person gets. It is never framed as a level.
 */
@Composable
fun HowYouGetAroundScreen(
    state: OnboardingState,
    onChoose: (GettingAround) -> Unit,
    onTherapist: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    SteadyScreen(
        title = stringResource(R.string.around_title),
        onBack = onBack,
        footer = {
            PrimaryButton(
                label = stringResource(R.string.continue_label),
                onClick = onNext,
                enabled = state.gettingAround != null,
            )
        },
    ) {
        SectionTitle(stringResource(R.string.around_question))

        listOf(
            GettingAround.OnFeet to stringResource(R.string.around_on_feet),
            GettingAround.Walker to stringResource(R.string.around_walker),
            GettingAround.Wheelchair to stringResource(R.string.around_wheelchair),
            GettingAround.InBed to stringResource(R.string.around_in_bed),
        ).forEach { (value, label) ->
            ListItem(
                heading = label,
                next = state.gettingAround == value,
                onClick = { onChoose(value) },
            )
        }

        Paragraph(stringResource(R.string.around_why))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SteadyShapes.Card)
                .background(SteadyPalette.White)
                .padding(SteadySpacing.InsideTight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SteadyText(
                text = stringResource(R.string.around_therapist),
                style = SteadyType.Body,
                color = SteadyPalette.Ink,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.withTherapist,
                onCheckedChange = onTherapist,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = SteadyPalette.Green,
                    checkedThumbColor = SteadyPalette.White,
                ),
            )
        }
    }
}

/** Screen 3. Height, age, units. Nothing else. */
@Composable
fun AboutYouScreen(
    state: OnboardingState,
    onUnits: (Units) -> Unit,
    onHeight: (Double) -> Unit,
    onAge: (Int?) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    SteadyScreen(
        title = stringResource(R.string.about_you_title),
        onBack = onBack,
        footer = { PrimaryButton(label = stringResource(R.string.continue_label), onClick = onNext) },
    ) {
        SectionTitle(stringResource(R.string.about_you_units))
        Row(horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap)) {
            Pill(
                label = stringResource(R.string.units_imperial),
                selected = state.units == Units.Imperial,
                onClick = { onUnits(Units.Imperial) },
            )
            Pill(
                label = stringResource(R.string.units_metric),
                selected = state.units == Units.Metric,
                onClick = { onUnits(Units.Metric) },
            )
        }

        SectionTitle(stringResource(R.string.about_you_height))
        Stepper(
            label = stringResource(R.string.about_you_height),
            value = Convert.heightLabel(state.heightCm, state.units),
            supporting = stringResource(R.string.about_you_height_why),
            onDown = { onHeight(state.heightCm - 1) },
            onUp = { onHeight(state.heightCm + 1) },
        )

        SectionTitle(stringResource(R.string.about_you_age))
        if (state.ageSkipped) {
            Paragraph(stringResource(R.string.about_you_age_why))
            SecondaryButton(
                label = stringResource(R.string.about_you_age),
                onClick = { onAge(DEFAULT_AGE) },
            )
        } else {
            Stepper(
                label = stringResource(R.string.about_you_age),
                value = (state.age ?: DEFAULT_AGE).toString(),
                supporting = stringResource(R.string.about_you_age_why),
                onDown = { onAge((state.age ?: DEFAULT_AGE) - 1) },
                onUp = { onAge((state.age ?: DEFAULT_AGE) + 1) },
            )
            SecondaryButton(label = stringResource(R.string.about_you_skip), onClick = { onAge(null) })
        }
    }
}

/** Screen 4. Four capability questions, then the one that matters more. */
@Composable
fun WhereYouAreStartingScreen(
    state: OnboardingState,
    onChair: (ChairEase) -> Unit,
    onStairs: (Stairs) -> Unit,
    onWalk: (WalkTolerance) -> Unit,
    onFloor: (FloorAccess) -> Unit,
    onPem: (PemAnswer) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    SteadyScreen(
        title = stringResource(R.string.start_title),
        onBack = onBack,
        gap = SteadySpacing.ListGap,
        footer = {
            PrimaryButton(
                label = stringResource(R.string.continue_label),
                onClick = onNext,
                enabled = state.startingAnswered,
            )
        },
    ) {
        SectionTitle(stringResource(R.string.start_chair))
        ThreeUpChoice(
            options = listOf(
                stringResource(R.string.start_chair_hard),
                stringResource(R.string.start_chair_okay),
                stringResource(R.string.start_chair_easy),
            ),
            selectedIndex = state.chair?.ordinal,
            onSelect = { onChair(ChairEase.entries[it]) },
        )
        Paragraph(stringResource(R.string.start_chair_why))

        SectionTitle(stringResource(R.string.start_stairs))
        ThreeUpChoice(
            options = listOf(
                stringResource(R.string.start_stairs_avoid),
                stringResource(R.string.start_stairs_stop),
                stringResource(R.string.start_stairs_fine),
            ),
            selectedIndex = state.stairs?.ordinal,
            onSelect = { onStairs(Stairs.entries[it]) },
        )

        SectionTitle(stringResource(R.string.start_walk))
        ThreeUpChoice(
            options = listOf(
                stringResource(R.string.start_walk_under5),
                stringResource(R.string.start_walk_5to15),
                stringResource(R.string.start_walk_over15),
            ),
            selectedIndex = state.walkTolerance?.ordinal,
            onSelect = { onWalk(WalkTolerance.entries[it]) },
        )

        SectionTitle(stringResource(R.string.start_floor))
        ThreeUpChoice(
            options = listOf(
                stringResource(R.string.start_floor_no),
                stringResource(R.string.start_floor_help),
                stringResource(R.string.start_floor_yes),
            ),
            selectedIndex = state.floor?.ordinal,
            onSelect = { onFloor(FloorAccess.entries[it]) },
        )

        SectionTitle(stringResource(R.string.start_pem))
        ThreeUpChoice(
            options = listOf(
                stringResource(R.string.start_pem_no),
                stringResource(R.string.start_pem_sometimes),
                stringResource(R.string.start_pem_yes),
            ),
            selectedIndex = state.pem?.ordinal,
            onSelect = { onPem(PemAnswer.entries[it]) },
        )
        Paragraph(stringResource(R.string.start_pem_why))
    }
}

/** Screen 5. Movement words only, and the app never asks why. */
@Composable
fun LeaveOutScreen(
    state: OnboardingState,
    onToggle: (Exclusion) -> Unit,
    onNothing: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    SteadyScreen(
        title = stringResource(R.string.leave_out_title),
        onBack = onBack,
        footer = {
            PrimaryButton(label = stringResource(R.string.continue_label), onClick = onNext)
            SecondaryButton(label = stringResource(R.string.leave_out_nothing), onClick = onNothing)
        },
    ) {
        SectionTitle(stringResource(R.string.leave_out_question))

        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
            verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
        ) {
            exclusionLabels().forEach { (value, label) ->
                Pill(
                    label = label,
                    selected = value in state.exclusions,
                    onClick = { onToggle(value) },
                )
            }
        }

        Paragraph(stringResource(R.string.leave_out_why))
    }
}

@Composable
private fun exclusionLabels(): List<Pair<Exclusion, String>> = listOf(
    Exclusion.Pushing to stringResource(R.string.leave_out_pushing),
    Exclusion.StomachStrain to stringResource(R.string.leave_out_stomach),
    Exclusion.GettingOnTheFloor to stringResource(R.string.leave_out_floor),
    Exclusion.Impact to stringResource(R.string.leave_out_impact),
    Exclusion.DeepKneeBending to stringResource(R.string.leave_out_knee),
    Exclusion.LiftingOverhead to stringResource(R.string.leave_out_overhead),
    Exclusion.TwistingBack to stringResource(R.string.leave_out_twisting),
    Exclusion.DeepForwardBending to stringResource(R.string.leave_out_forward),
    Exclusion.ArchingBack to stringResource(R.string.leave_out_arching),
    Exclusion.LyingFlat to stringResource(R.string.leave_out_lying),
    Exclusion.BreathHolding to stringResource(R.string.leave_out_breath),
)

/** Screen 6. Original wording, never the questionnaire itself. COMPLIANCE.md. */
@Composable
fun ReadinessScreen(
    state: OnboardingState,
    onAnswer: (ReadinessFlag, Boolean) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    SteadyScreen(
        title = stringResource(R.string.readiness_title),
        onBack = onBack,
        gap = SteadySpacing.ListGap,
        footer = {
            PrimaryButton(
                label = stringResource(R.string.continue_label),
                onClick = onNext,
                enabled = state.readinessAnswered,
            )
        },
    ) {
        readinessLabels().forEach { (flag, label) ->
            SectionTitle(label)
            ThreeUpChoice(
                options = listOf(
                    stringResource(R.string.readiness_no),
                    stringResource(R.string.readiness_yes),
                ),
                selectedIndex = state.readiness[flag]?.let { if (it) 1 else 0 },
                onSelect = { onAnswer(flag, it == 1) },
            )
        }

        if (state.anyReadinessFlag) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SteadyShapes.Card)
                    .background(SteadyPalette.Sand)
                    .padding(SteadySpacing.Inside),
                verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
            ) {
                Paragraph(text = stringResource(R.string.readiness_note), color = SteadyPalette.Navy)
                SteadyText(
                    text = stringResource(R.string.readiness_attribution),
                    style = SteadyType.Caption,
                    color = SteadyPalette.Ink2,
                )
            }
        }
    }
}

@Composable
private fun readinessLabels(): List<Pair<ReadinessFlag, String>> = listOf(
    ReadinessFlag.ChestPain to stringResource(R.string.readiness_chest),
    ReadinessFlag.Fainting to stringResource(R.string.readiness_fainting),
    ReadinessFlag.SupervisedOnly to stringResource(R.string.readiness_supervised),
    ReadinessFlag.Pregnant to stringResource(R.string.readiness_pregnant),
    ReadinessFlag.BoneOrJoint to stringResource(R.string.readiness_bone),
)

/** Screen 7. The centre of setup, and the person's own words. */
@Composable
fun WhatYouWantScreen(
    state: OnboardingState,
    onTyped: (String) -> Unit,
    onAdd: (String, AbilityDomain) -> Unit,
    onRemove: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    SteadyScreen(
        title = null,
        onBack = onBack,
        footer = {
            PrimaryButton(
                label = stringResource(R.string.continue_label),
                onClick = onNext,
                enabled = state.wanted.isNotEmpty(),
            )
        },
    ) {
        SteadyText(
            text = stringResource(R.string.want_title),
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.padding(top = SteadySpacing.Tight),
        )
        Paragraph(stringResource(R.string.want_prompt))

        TextEntry(
            value = state.typed,
            onValue = onTyped,
            hint = stringResource(R.string.want_hint),
            onSubmit = { onAdd(state.typed, AbilityDomain.GetUp) },
        )
        SecondaryButton(
            label = stringResource(R.string.want_add),
            onClick = { onAdd(state.typed, AbilityDomain.GetUp) },
        )

        if (state.wanted.isNotEmpty()) {
            SectionTitle(stringResource(R.string.want_tracking))
            state.wanted.forEach { item ->
                ListItem(
                    heading = item.text,
                    subtitle = abilityLabel(item.domain),
                    onClick = { onRemove(item.text) },
                )
            }
        }

        SectionTitle(stringResource(R.string.want_starters))
        starters().forEach { (text, domain) ->
            ListItem(heading = text, onClick = { onAdd(text, domain) })
        }
    }
}

@Composable
private fun starters(): List<Pair<String, AbilityDomain>> = listOf(
    stringResource(R.string.want_starter_groceries) to AbilityDomain.Carry,
    stringResource(R.string.want_starter_stairs) to AbilityDomain.Go,
    stringResource(R.string.want_starter_floor) to AbilityDomain.GetUp,
    stringResource(R.string.want_starter_kids) to AbilityDomain.Go,
    stringResource(R.string.want_starter_shelf) to AbilityDomain.Carry,
    stringResource(R.string.want_starter_mailbox) to AbilityDomain.Go,
    stringResource(R.string.want_starter_stiff) to AbilityDomain.Steady,
)

@Composable
fun abilityLabel(domain: AbilityDomain): String = stringResource(
    when (domain) {
        AbilityDomain.GetUp -> R.string.ability_get_up
        AbilityDomain.Go -> R.string.ability_go
        AbilityDomain.Carry -> R.string.ability_carry
        AbilityDomain.Steady -> R.string.ability_steady
    },
)

/** Screen 7b. Ten blocks per item, in the person's own hand. */
@Composable
fun RateThemScreen(
    state: OnboardingState,
    onRate: (String, Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    SteadyScreen(
        title = null,
        onBack = onBack,
        footer = { PrimaryButton(label = stringResource(R.string.continue_label), onClick = onNext) },
    ) {
        SteadyText(
            text = stringResource(R.string.rate_title),
            style = SteadyType.ScreenTitleBig,
            color = SteadyPalette.Navy,
            modifier = Modifier.padding(top = SteadySpacing.Tight),
        )
        Paragraph(stringResource(R.string.rate_scale))

        state.wanted.forEach { item ->
            SectionTitle(item.text)
            RatingRow(
                rating = item.rating,
                onRate = { onRate(item.text, it) },
                label = item.text,
            )
        }

        Paragraph(stringResource(R.string.rate_why))
    }
}

/** Screen 8. One habit question. */
@Composable
fun AnchorScreen(
    state: OnboardingState,
    onAnchor: (Anchor) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    SteadyScreen(
        title = stringResource(R.string.anchor_title),
        onBack = onBack,
        footer = {
            PrimaryButton(
                label = stringResource(R.string.anchor_done),
                onClick = onNext,
                enabled = state.anchor != null,
            )
        },
    ) {
        SectionTitle(stringResource(R.string.anchor_question))
        Paragraph(stringResource(R.string.anchor_why))

        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
            verticalArrangement = Arrangement.spacedBy(SteadySpacing.ListGap),
        ) {
            anchorLabels().forEach { (value, label) ->
                Pill(label = label, selected = state.anchor == value, onClick = { onAnchor(value) })
            }
        }

        Paragraph(stringResource(R.string.anchor_note))
    }
}

@Composable
private fun anchorLabels(): List<Pair<Anchor, String>> = listOf(
    Anchor.MorningCoffee to stringResource(R.string.anchor_coffee),
    Anchor.Lunch to stringResource(R.string.anchor_lunch),
    Anchor.Dinner to stringResource(R.string.anchor_dinner),
    Anchor.BrushingTeeth to stringResource(R.string.anchor_teeth),
    Anchor.WalkingTheDog to stringResource(R.string.anchor_dog),
    Anchor.SchoolRun to stringResource(R.string.anchor_school),
)

/** Screen 9. Straight to the dial. The first real action. */
@Composable
fun FirstWeighInScreen(
    state: OnboardingState,
    onWeight: (Double) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    SteadyScreen(
        title = stringResource(R.string.weigh_title),
        onBack = onBack,
        footer = {
            PrimaryButton(label = stringResource(R.string.weigh_save), onClick = onSave)
            SteadyText(
                text = stringResource(R.string.weigh_whenever),
                style = SteadyType.Caption,
                color = SteadyPalette.Ink3Text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
    ) {
        Spacer(Modifier.height(SteadySpacing.Inside))
        Dial(
            value = state.firstWeightKg,
            displayValue = Convert.weightLabel(state.firstWeightKg, state.units),
            unit = stringResource(
                if (state.units == Units.Imperial) R.string.unit_lb else R.string.unit_kg,
            ),
            onValue = onWeight,
            spoken = Convert.weightLabel(state.firstWeightKg, state.units),
        )
        SteadyText(
            text = stringResource(R.string.weigh_this_morning),
            style = SteadyType.Body,
            color = SteadyPalette.Ink2,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

/** Screen 10. The person names step one, and that name is reused for it. */
@Composable
fun NameFirstWalkScreen(
    state: OnboardingState,
    onName: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    SteadyScreen(
        title = stringResource(R.string.name_walk_title),
        onBack = onBack,
        footer = { PrimaryButton(label = stringResource(R.string.name_walk_save), onClick = onSave) },
    ) {
        SectionTitle(stringResource(R.string.name_walk_question))
        TextEntry(
            value = state.firstWalkName,
            onValue = onName,
            hint = stringResource(R.string.name_walk_hint),
            onSubmit = onSave,
        )
    }
}

/** What the first weigh-in says back. ONBOARDING.md screen 9. */
@Composable
fun FirstPointCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SteadyShapes.Card)
            .background(SteadyPalette.White)
            .padding(SteadySpacing.Inside),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SteadySpacing.Tight),
    ) {
        Box(
            modifier = Modifier
                .size(SteadySpacing.GlyphInCard)
                .clip(SteadyShapes.GlyphTile)
                .background(SteadyPalette.GreenL),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(SteadySpacing.GlyphInTile)) { WeighGlyph() }
        }
        Paragraph(
            text = stringResource(R.string.weigh_first_done),
            modifier = Modifier.weight(1f).wrapContentHeight(),
            color = SteadyPalette.Navy,
        )
    }
}

private val GLYPH = 52.dp
private val WELCOME_HERO = 420.dp
