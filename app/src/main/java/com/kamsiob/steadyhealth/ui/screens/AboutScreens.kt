package com.kamsiob.steadyhealth.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kamsiob.steadyhealth.BuildConfig
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.ui.components.ListItem
import com.kamsiob.steadyhealth.ui.components.NoteBlock
import com.kamsiob.steadyhealth.ui.components.Paragraph
import com.kamsiob.steadyhealth.ui.components.SectionTitle
import com.kamsiob.steadyhealth.ui.components.SteadyScreen

/**
 * About: what this is, what it is not, the version and the licence.
 *
 * "What it is not" is the load-bearing paragraph and it is COMPLIANCE.md's own
 * sentence rather than a friendlier one written here. It is the standing position
 * the whole app is built inside, and a re-worded copy of it in one screen is how a
 * position drifts without anybody deciding to move it.
 *
 * It is a note block rather than a footnote, and it sits second rather than last,
 * because somebody who reads two things on this screen reads what the app is and
 * what it is not.
 */
@Composable
fun AboutScreen(
    onPrivacy: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SteadyScreen(
        title = stringResource(R.string.about_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        Paragraph(stringResource(R.string.about_what))

        NoteBlock(
            heading = stringResource(R.string.about_not_heading),
            text = stringResource(R.string.about_not),
        )

        ListItem(
            heading = stringResource(R.string.about_privacy),
            subtitle = stringResource(R.string.about_privacy_sub),
            onClick = onPrivacy,
        )

        Paragraph(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
        Paragraph(stringResource(R.string.about_licence))

        SectionTitle(stringResource(R.string.about_made_with))
        Paragraph(stringResource(R.string.about_made_with_body))
        Paragraph(stringResource(R.string.models_credit))

        Paragraph(stringResource(R.string.about_contact))
    }
}

/**
 * The privacy policy, in the app.
 *
 * PRIVACY.md promises the policy is published inside the app as well as at its
 * address, and this app has no network permission with which to fetch it, so the
 * document is here rather than behind a link to somewhere it cannot reach. The words
 * are the hosted document's own, shortened only where a sentence named something
 * this app calls something else.
 */
@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    SteadyScreen(
        title = stringResource(R.string.privacy_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        Paragraph(stringResource(R.string.privacy_effective))
        NoteBlock(stringResource(R.string.privacy_lede))

        SectionTitle(stringResource(R.string.privacy_collects_heading))
        Paragraph(stringResource(R.string.privacy_collects))

        SectionTitle(stringResource(R.string.privacy_never_heading))
        Paragraph(stringResource(R.string.privacy_never))

        SectionTitle(stringResource(R.string.privacy_optional_heading))
        Paragraph(stringResource(R.string.privacy_optional))

        SectionTitle(stringResource(R.string.privacy_permissions_heading))
        Paragraph(stringResource(R.string.privacy_permissions))

        SectionTitle(stringResource(R.string.privacy_yours_heading))
        Paragraph(stringResource(R.string.privacy_yours))

        Paragraph(stringResource(R.string.privacy_children))
        Paragraph(stringResource(R.string.privacy_changes))
        Paragraph(stringResource(R.string.about_contact))
    }
}
