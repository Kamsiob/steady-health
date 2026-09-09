package com.kamsiob.steadyhealth.scan

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The five outcomes in ADDENDUM-03 Part 5, held against pages that look like the
 * real thing rather than against one line each.
 *
 * Three rules are the ones worth holding and the rest of this file is in service
 * of them.
 *
 * A blood test, an imaging page and a medication list are never anything but out
 * of scope. Part 7 reads and explains whatever it is handed, so a miss here is
 * the app explaining somebody's cholesterol to them, and no amount of accuracy
 * elsewhere buys that back.
 *
 * Thin evidence comes back Unclear. Part 5 has a screen for not knowing that
 * offers both paths and "Just keep it", so the honest answer costs one tap and
 * the confident wrong one costs a flow that cannot work.
 *
 * A letterhead is not a letter. Nearly every exercise sheet in the world has a
 * clinic name and a therapist's initials at the top of it, and if that were
 * enough to make a page a report then no sheet would ever be a sheet.
 *
 * The fixtures are multi-line on purpose. Classifying a page is partly about how
 * the page is laid out, and a single line cannot go wrong in the ways a page can.
 */
class PageKindTest {

    @Test
    fun aNumberedHomeProgrammeIsExercises() {
        assertThat(PageKind.of(NUMBERED_SHEET)).isInstanceOf(PageKind.Exercises::class.java)
    }

    @Test
    fun aBulletedListOfMovementsIsExercises() {
        assertThat(PageKind.of(BULLETED_SHEET)).isInstanceOf(PageKind.Exercises::class.java)
    }

    @Test
    fun aSheetUnderAClinicLetterheadIsStillExercises() {
        // The letterhead rule. This page has a clinic name and a therapist's
        // letters after her name and it is still a sheet, because nobody wrote a
        // sentence on it.
        assertThat(PageKind.of(LETTERHEAD_SHEET)).isInstanceOf(PageKind.Exercises::class.java)
    }

    @Test
    fun aProgressNoteIsAReportOrLetter() {
        assertThat(PageKind.of(PROGRESS_NOTE)).isInstanceOf(PageKind.ReportOrLetter::class.java)
    }

    @Test
    fun aReferralLetterIsAReportOrLetter() {
        assertThat(PageKind.of(REFERRAL_LETTER)).isInstanceOf(PageKind.ReportOrLetter::class.java)
    }

    @Test
    fun anInsurerLetterAboutVisitsIsAReportOrLetter() {
        assertThat(PageKind.of(INSURER_LETTER)).isInstanceOf(PageKind.ReportOrLetter::class.java)
    }

    @Test
    fun aDischargeSummaryWithAProgrammeOnItIsBoth() {
        assertThat(PageKind.of(DISCHARGE_WITH_PROGRAMME)).isInstanceOf(PageKind.Both::class.java)
    }

    @Test
    fun aNoteThatEndsInAnUpdatedProgrammeIsBoth() {
        assertThat(PageKind.of(NOTE_WITH_PROGRAMME)).isInstanceOf(PageKind.Both::class.java)
    }

    @Test
    fun aFormWrittenInShorthandWithNoSentencesOnItIsStillAReport() {
        // The other half of the letterhead rule, and the thing that rule used to cost.
        // Prose is one way a page shows somebody wrote it and the shorthand is the
        // other, and there is not a full stop anywhere on this one.
        val kind = PageKind.of(EVALUATION_FORM)
        assertWithMessage("signals were ${kind.signals}")
            .that(kind)
            .isInstanceOf(PageKind.ReportOrLetter::class.java)
    }

    @Test
    fun anAppointmentCardIsUnclear() {
        assertThat(PageKind.of(APPOINTMENT_CARD)).isInstanceOf(PageKind.Unclear::class.java)
    }

    @Test
    fun aHandwrittenReminderIsUnclear() {
        assertThat(PageKind.of(HANDWRITTEN_NOTE)).isInstanceOf(PageKind.Unclear::class.java)
    }

    @Test
    fun aPageWithNothingOnItIsUnclearAndSaysSoWithNoSignals() {
        val kind = PageKind.of("")
        assertThat(kind).isInstanceOf(PageKind.Unclear::class.java)
        assertThat(kind.signals).isEmpty()
    }

    @Test
    fun aFewWordsAreNotEnoughToNameAPage() {
        // Enough movement names to clear the two clue bar, and still too little
        // page to be sure of anything.
        assertThat(PageKind.of("Squats and bridges")).isInstanceOf(PageKind.Unclear::class.java)
    }

    @Test
    fun aBloodTestIsOutOfScopeAndLabelledAsLabWork() {
        val kind = PageKind.of(LAB_PAGE)
        assertThat(kind).isInstanceOf(PageKind.OutOfScope::class.java)
        assertThat((kind as PageKind.OutOfScope).pointsAt).isEqualTo(Pointing.LabWork)
    }

    @Test
    fun anImagingPageIsOutOfScopeAndLabelledAsImaging() {
        val kind = PageKind.of(IMAGING_PAGE)
        assertThat(kind).isInstanceOf(PageKind.OutOfScope::class.java)
        assertThat((kind as PageKind.OutOfScope).pointsAt).isEqualTo(Pointing.Imaging)
    }

    @Test
    fun aMedicationListIsOutOfScopeAndLabelledAsMedicines() {
        val kind = PageKind.of(MEDICATION_LIST)
        assertThat(kind).isInstanceOf(PageKind.OutOfScope::class.java)
        assertThat((kind as PageKind.OutOfScope).pointsAt).isEqualTo(Pointing.Medicines)
    }

    @Test
    fun nothingOutOfScopeIsEverSomethingTheAppWouldExplain() {
        // The rule that matters most. Whatever else these three are, they are
        // never a report, never both, and never merely unclear, because two of
        // those three offer to explain the page.
        val pages = mapOf(
            "a blood test" to LAB_PAGE,
            "an imaging page" to IMAGING_PAGE,
            "a medication list" to MEDICATION_LIST,
            "a medication list inside a letter" to LETTER_CARRYING_MEDICINES,
        )
        pages.forEach { (name, page) ->
            assertWithMessage("$name is out of scope")
                .that(PageKind.of(page))
                .isInstanceOf(PageKind.OutOfScope::class.java)
        }
    }

    @Test
    fun oneOutOfScopeClueIsEnoughWhenThePageHasNothingElseToSay() {
        // An imaging word and nothing else. Unclear would offer to explain it, so
        // the tie goes the other way on purpose.
        val kind = PageKind.of(
            """
            MRI of the left knee, 12 January 2026
            Please bring this with you to your next appointment
            """.trimIndent(),
        )
        assertThat(kind).isInstanceOf(PageKind.OutOfScope::class.java)
    }

    @Test
    fun oneBorrowedClinicalWordDoesNotMakeASheetIntoAReport() {
        // "Gait training" is a line on plenty of sheets. Two distinct pieces of
        // shorthand are needed before the shorthand counts at all.
        val kind = PageKind.of(
            """
            Gait training: walk 10 minutes on level ground, once a day
            Sit to stands from a dining chair, 3 sets of 10, twice a day
            Heel raises, 2 sets of 12 each side
            """.trimIndent(),
        )
        assertThat(kind).isInstanceOf(PageKind.Exercises::class.java)
    }

    @Test
    fun theResultCarriesTheCluesThatFired() {
        val sheet = PageKind.of(NUMBERED_SHEET)
        assertThat(sheet.signals)
            .containsAtLeast(Signal.RepsAndSets, Signal.MovementName, Signal.NumberedSteps)

        val note = PageKind.of(PROGRESS_NOTE)
        assertThat(note.signals)
            .containsAtLeast(Signal.ClinicalShorthand, Signal.ProseParagraphs, Signal.DateOfService)
    }

    @Test
    fun everyClueThatFiresPointsWhereTheResultWent() {
        val sheet = PageKind.of(BULLETED_SHEET)
        // The emptiness check is not decoration. Without it the line below holds
        // for a page that fired nothing at all, and a test that passes on silence
        // is not testing the thing it names.
        assertThat(sheet.signals).isNotEmpty()
        assertThat(sheet.signals.map(Signal::points).toSet()).containsExactly(Pointing.Exercises)
    }

    @Test
    fun aPageTheAppCannotNameIsOutOfScopeIfAnythingOnItWasOutOfScope() {
        // The half read lab page. Two clues point at a report and neither is prose,
        // so on its own this page is Unclear, and Part 5's unclear screen offers to
        // explain it. One laboratory is enough to take that offer away.
        val kind = PageKind.of(HALF_READ_LAB_PAGE)
        assertWithMessage("signals were ${PageKind.of(HALF_READ_LAB_PAGE).signals}")
            .that(kind)
            .isInstanceOf(PageKind.OutOfScope::class.java)
    }

    @Test
    fun twoBorrowedWordsFromTwoDifferentPlacesDoNotCondemnASheet() {
        // "Technique" is an imaging heading and "as needed for" is how a medicine is
        // taken, and both are ordinary English on a physio handout. Two clues are
        // needed from one family, because one of each is a coincidence and this page
        // is a sheet by every other measure on it.
        val kind = PageKind.of(SHEET_WITH_BORROWED_WORDS)
        assertWithMessage("signals were ${PageKind.of(SHEET_WITH_BORROWED_WORDS).signals}")
            .that(kind)
            .isInstanceOf(PageKind.Exercises::class.java)
    }

    @Test
    fun bothCarriesTheCluesFromBothHalvesOfThePage() {
        val signals = PageKind.of(DISCHARGE_WITH_PROGRAMME).signals.map(Signal::points).toSet()
        assertThat(signals).containsAtLeast(Pointing.Exercises, Pointing.ReportOrLetter)
    }

    @Test
    fun noOutOfScopeClueFiresOnAnyPageTheAppIsSupposedToRead() {
        val readable = mapOf(
            "the numbered sheet" to NUMBERED_SHEET,
            "the bulleted sheet" to BULLETED_SHEET,
            "the letterhead sheet" to LETTERHEAD_SHEET,
            "the progress note" to PROGRESS_NOTE,
            "the referral letter" to REFERRAL_LETTER,
            "the insurer letter" to INSURER_LETTER,
            "the discharge summary" to DISCHARGE_WITH_PROGRAMME,
            "the note with a programme" to NOTE_WITH_PROGRAMME,
        )
        readable.forEach { (name, page) ->
            assertWithMessage("$name has no out of scope clue on it")
                .that(PageKind.of(page).signals.filter { it.points.outOfScope })
                .isEmpty()
        }
    }

    @Test
    fun theSameTextAlwaysGivesTheSameAnswer() {
        // Nothing in here is allowed to depend on a model, a clock or a random
        // number, so this is checkable rather than hopeful.
        assertThat(PageKind.of(DISCHARGE_WITH_PROGRAMME))
            .isEqualTo(PageKind.of(DISCHARGE_WITH_PROGRAMME))
    }

    private companion object {

        val NUMBERED_SHEET = """
            Riverside Physical Therapy
            Home programme for Margaret Doyle
            Prepared 14 March 2026

            1. Sit to stands from a dining chair. 3 sets of 10, twice a day.
            2. Heel raises holding the counter. 2 sets of 12, each side.
            3. Wall push ups at arm's length. 10 reps, once a day.
            4. Standing balance by the sink, hold for 30 seconds. 3 times per day.
        """.trimIndent()

        val BULLETED_SHEET = """
            Things to do each day

            - Ankle pumps, 20 reps, three times a day
            - Heel slides, hold for 5 seconds, 10 reps each leg
            - Sit to stands from the armchair, 2 sets of 8
            - Marching on the spot for 30 seconds, twice a day
        """.trimIndent()

        val LETTERHEAD_SHEET = """
            Ridgeway Physical Therapy
            Home programme prepared 14 March 2026 by Ellen Voss, PT, DPT

            Sit to stand from a dining chair     3 sets of 10     twice a day
            Wall push ups                        2 sets of 12     once a day
            Standing balance, hold for 30 seconds                 3 times per day
            Heel raises, 10 reps each side, twice a day
        """.trimIndent()

        val PROGRESS_NOTE = """
            Ridgeway Physical Therapy
            Progress note
            Date of service: 2 March 2026

            Mrs Doyle was seen today for her sixth visit following her left knee replacement
            in January and she arrived without her stick for the first time. She tells me the
            stairs at home are becoming easier and that she no longer holds the rail going up.
            AROM of the left knee has improved to 0 to 112 degrees and MMT of the quadriceps
            is graded 4 out of 5 on the left. Gait is steady over level ground without a
            device, and transfers from a low chair remain effortful for her. We will continue
            with the current plan of care and look at it again in three weeks.

            Sincerely,
            Ellen Voss, PT, DPT
        """.trimIndent()

        val REFERRAL_LETTER = """
            Dear Dr Aylmer,

            Thank you for referring Mr Patrick Nolan, who I saw in clinic on 18 February for
            ongoing difficulty with balance and with getting out of a low chair at home. He
            describes two near stumbles in the last six months, both of them indoors, and he
            has stopped walking to the shops on his own since the second one. On examination
            his gait is wide based and slow, and he needs both hands to rise from a standard
            dining chair. Transfers are otherwise independent with supervision at home.
            I have started him on a course of eight sessions and will write again after the
            sixth of those.

            Kind regards,
            Ruth Amara
            Consultant Physiotherapist
        """.trimIndent()

        val INSURER_LETTER = """
            To whom it may concern

            This letter confirms that outpatient physical therapy has been authorised for
            Margaret Doyle under policy 44-11982 for a total of twelve visits, beginning on
            1 March 2026 and ending on 31 May 2026. Any visit beyond that number will need a
            fresh authorisation from the treating clinician before it is provided, and we
            cannot backdate one. If you have a question about the dates above, please feel
            free to contact the benefits office on the number printed at the foot of the page.

            Yours sincerely
            Benefits Administration
        """.trimIndent()

        val DISCHARGE_WITH_PROGRAMME = """
            Hollis Rehabilitation
            Discharge summary
            Date of service: 20 April 2026

            Mrs Okafor completed twelve sessions of physical therapy following her hip
            fracture in January and she is discharged today at her own request. Her gait is
            steady indoors without a device and transfers from a standard chair are
            independent with no support from her arms. AROM at the right hip is within
            functional limits and MMT of the hip abductors is graded 4 out of 5. She has been
            given the programme below to carry on with at home.

            1. Bridges. 2 sets of 10, once a day.
            2. Sit to stands from a kitchen chair. 3 sets of 8, twice a day.
            3. Side lying hip abduction. 10 reps each side, once a day.

            Sincerely,
            Daniel Reyes, PT
        """.trimIndent()

        val NOTE_WITH_PROGRAMME = """
            Fenwick Physiotherapy
            Re: Alan Whitfield

            We saw Alan again on 6 May and his shoulder is moving a good deal more freely
            than it was at the initial evaluation in February. He can now reach the top shelf
            of his kitchen cupboard without any discomfort at all, which he could not do when
            we first met him. He still avoids lifting anything heavy with that arm and we
            have left that out of the programme for the time being.

            His programme from here is:
            - Wall slides, 3 sets of 10, twice a day
            - Shoulder flexion with a resistance band, 2 sets of 12, each side
            - Pendulum swings, hold for 30 seconds, 3 times per day
        """.trimIndent()

        /**
         * A re-evaluation, in the shape most of a therapy file is actually in: field
         * names down the left, shorthand down the right, and nobody writing sentences
         * to anybody.
         */
        val EVALUATION_FORM = """
            Fenwick Physiotherapy
            Re-evaluation, visit 8
            Date seen: 6 May 2026

            AROM right shoulder: 0 to 140        MMT: 4/5 throughout
            Gait: independent with a stick outdoors
            Transfers: independent
            ADLs: independent except overhead reaching
            Plan: 4 more visits, HEP updated
        """.trimIndent()

        val APPOINTMENT_CARD = """
            Fenwick Physiotherapy
            14 Bridge Street
            Appointment: Thursday 12 June at 10:15
            Please bring flat shoes and this card with you.
        """.trimIndent()

        val HANDWRITTEN_NOTE = """
            Notes from Thursday
            Ask about the knee before starting squats again
            Bring the sheet from last time
        """.trimIndent()

        /**
         * A lab page whose results column did not photograph. What is left is a
         * header that looks like any clinic's, and one word that says where it came
         * from.
         */
        val HALF_READ_LAB_PAGE = """
            Meridian Clinical Laboratory
            Referring physician: Dr H Aylmer
            Date of service: 4 March 2026
            Sample received in good condition and processed the same day
        """.trimIndent()

        val SHEET_WITH_BORROWED_WORDS = """
            Riverside Physical Therapy
            Home programme for Margaret Doyle

            Technique: keep your knee over your toes and go slowly
            1. Sit to stands from a dining chair. 3 sets of 10, twice a day.
            2. Heel raises holding the counter. 2 sets of 12, each side.
            3. Ice the knee afterwards as needed for soreness.
        """.trimIndent()

        val LAB_PAGE = """
            Meridian Clinical Laboratory
            Specimen collected 4 March 2026, fasting
            Ordering provider: Dr H Aylmer

            Haemoglobin          13.4 g/dL       (12.0 - 15.5)
            White blood cells    6.2 x10^9/L     (4.0 - 11.0)
            Creatinine           71 umol/L       (45 - 84)
            HbA1c                41 mmol/mol     (20 - 41)
            Vitamin D            58 nmol/L       Reference range 50 to 125
        """.trimIndent()

        val IMAGING_PAGE = """
            Northgate Radiology
            MRI of the left knee without contrast
            Clinical indication: ongoing pain on stairs

            TECHNIQUE: Multiplanar multisequence images were obtained of the left knee
            without intravenous contrast. Comparison is made with the radiographs of
            12 January 2026.

            IMPRESSION: Moderate degenerative changes of the medial compartment with joint
            space narrowing. No acute fracture. Small joint effusion.

            Reported by A. Sandhu, Consultant Radiologist
        """.trimIndent()

        val MEDICATION_LIST = """
            Current medications for Margaret Doyle
            Reviewed 2 March 2026 at Elm Road Pharmacy

            Amlodipine 5 mg tablet, one by mouth every morning. 2 refills.
            Atorvastatin 20 mg tablet, one by mouth at bedtime. 3 refills.
            Paracetamol 500 mg tablets, two by mouth up to four times a day as needed for pain.
            Levothyroxine 75 mcg tablet, one by mouth before food. Quantity 90.
        """.trimIndent()

        val LETTER_CARRYING_MEDICINES = """
            Dear Mrs Doyle,

            Following your visit on 2 March we have made a change to what you take in the
            morning and the pharmacy has the new one ready for you to collect this week.
            Please carry on with everything else exactly as it is written below and bring
            this page with you when you come in again in April.

            Amlodipine 5 mg tablet, one by mouth every morning
            Atorvastatin 20 mg tablet, one by mouth at bedtime

            Kind regards,
            Elm Road Surgery
        """.trimIndent()
    }
}
