package com.kamsiob.steadyhealth.ai

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The job 9 validator, against small hand written readings of one letter.
 *
 * The fixture corpus AI.md asks for is a separate job and lives elsewhere. These are
 * the checks themselves, one case each, so that a corpus failure can be read against
 * a test that says which rule it was.
 *
 * Every case is checked against the same document, because that is the shape of job
 * 9: there is no brief, only the text that came off the photograph, and a reading is
 * right or wrong only in relation to it.
 */
class ReadingValidatorTest {

    @Test
    fun anHonestReadingSurvivesWhole() {
        // The other half of the job. A validator that rejects everything protects
        // nobody, because the reading then never appears and the feature is dead.
        val verdict = ReadingValidator.check(honest(), DOCUMENT)
        assertThat(verdict.faults).isEmpty()
        assertThat(verdict.kept).isEqualTo(honest())
        assertThat(verdict.paragraphsAllFailed).isFalse()
        assertThat(verdict.trimmedParagraphs).isEqualTo(0)
    }

    @Test
    fun aBannedWordSurvivesInATermTheDocumentItselfUsed() {
        val term = ReadingTerm("condition", "The word the report used for the shoulder.")
        val verdict = ReadingValidator.checkTerm(term, DOCUMENT)
        assertThat(verdict.passed).isTrue()
        assertThat(ReadingValidator.bannedWordsIn("condition")).contains("condition")
    }

    @Test
    fun theSameWordInAPlainLineIsCaughtEvenThoughTheDocumentUsesIt() {
        val term = ReadingTerm("condition", "The report says this about your condition.")
        val verdict = ReadingValidator.checkTerm(term, DOCUMENT)
        assertThat(verdict.faults).containsExactly(ReadingFault.ForbiddenWord)
    }

    @Test
    fun aTermTheDocumentNeverUsedIsDropped() {
        val term = ReadingTerm("HEP", "Home exercise programme.")
        val verdict = ReadingValidator.checkTerm(term, DOCUMENT)
        assertThat(verdict.faults).containsExactly(ReadingFault.UnsourcedTerm)
    }

    @Test
    fun aQuestionWithoutAQuestionMarkFails() {
        val finding = "You could ask about the 140 degrees goal."
        assertThat(ReadingValidator.checkQuestion(finding, DOCUMENT).faults)
            .containsExactly(ReadingFault.NotAQuestion)
        val asked = "Could you ask about the 140 degrees goal?"
        assertThat(ReadingValidator.checkQuestion(asked, DOCUMENT).passed).isTrue()
    }

    @Test
    fun aQuestionAboutNothingInTheDocumentFails() {
        val verdict = ReadingValidator.checkQuestion(
            "What happens at the appointment tomorrow?",
            DOCUMENT,
        )
        assertThat(verdict.faults).containsExactly(ReadingFault.UngroundedQuestion)
    }

    @Test
    fun aSixthParagraphIsTrimmedAndNotFaulted() {
        val many = honest().copy(paragraphs = List(7) { PARAGRAPHS.first() })
        val verdict = ReadingValidator.check(many, DOCUMENT)
        assertThat(verdict.paragraphs).hasSize(Reading.MOST_PARAGRAPHS)
        assertThat(verdict.trimmedParagraphs).isEqualTo(2)
        assertThat(verdict.faults).isEmpty()
        assertThat(verdict.keptParagraphs).hasSize(Reading.MOST_PARAGRAPHS)
    }

    @Test
    fun aShortReadingIsShortAndNotWrong() {
        val two = honest().copy(paragraphs = PARAGRAPHS.take(2))
        val verdict = ReadingValidator.check(two, DOCUMENT)
        assertThat(verdict.faults).isEmpty()
        assertThat(verdict.paragraphsAllFailed).isFalse()
    }

    @Test
    fun aReadingWhereEveryParagraphFailsSaysSoAndKeepsTheTerms() {
        val opinionated = honest().copy(
            paragraphs = List(3) { "This is good news, and your condition is improving." },
        )
        val verdict = ReadingValidator.check(opinionated, DOCUMENT)
        assertThat(verdict.paragraphsAllFailed).isTrue()
        assertThat(verdict.keptParagraphs).isEmpty()
        assertThat(verdict.kept.terms).isEqualTo(honest().terms)
        assertThat(verdict.faults).contains(ReadingFault.ForbiddenWord)
    }

    @Test
    fun aReadingWithNoParagraphsAtAllReportsTheSameThing() {
        // Deliberately unlike job 6's allFailed. The screen's question is whether
        // there is a plain words section to show, and none and none left look alike.
        val verdict = ReadingValidator.check(honest().copy(paragraphs = emptyList()), DOCUMENT)
        assertThat(verdict.paragraphsAllFailed).isTrue()
    }

    @Test
    fun aNameTheDocumentDoesNotCarryIsCaught() {
        val invented = "Your visits at Northgate Clinic continue twice weekly."
        assertThat(ReadingValidator.checkParagraph(invented, DOCUMENT).faults)
            .containsExactly(ReadingFault.InventedName)
        val real = "Your visits at Riverside Physical Therapy continue twice weekly."
        assertThat(ReadingValidator.checkParagraph(real, DOCUMENT).passed).isTrue()
    }

    @Test
    fun aNumberSpelledOutTracesBackToTheDigitsOnThePage() {
        val honestTerm = ReadingTerm("MMT 4/5", "A clinician graded that muscle four out of five.")
        assertThat(ReadingValidator.checkTerm(honestTerm, DOCUMENT).passed).isTrue()
        val invented = ReadingTerm("MMT 4/5", "A clinician graded that muscle seven out of ten.")
        assertThat(ReadingValidator.checkTerm(invented, DOCUMENT).faults)
            .containsExactly(ReadingFault.InventedNumber)
    }

    @Test
    fun aSeasonTheDocumentNeverNamedIsCaught() {
        val guessed = "The goal is 140 degrees by winter."
        val verdict = ReadingValidator.checkParagraph(guessed, DOCUMENT)
        assertThat(verdict.faults).containsExactly(ReadingFault.InventedDate)
    }

    @Test
    fun aUnitTheDocumentNeverUsesIsCaught() {
        val verdict = ReadingValidator.checkParagraph("Your grip was 30 kg on the right.", DOCUMENT)
        assertThat(verdict.faults).containsExactly(ReadingFault.InventedMeasurement)
    }

    @Test
    fun aSentenceThatSaysWhyIsRejected() {
        val verdict = ReadingValidator.checkParagraph(
            "Your flexion reached 120 degrees because the visits continued.",
            DOCUMENT,
        )
        assertThat(verdict.faults).containsExactly(ReadingFault.CausalClaim)
    }

    @Test
    fun shouldFiresOnItsOwnAndNeverInsideShoulder() {
        assertThat(ReadingValidator.bannedWordsIn("your shoulder was measured")).isEmpty()
        assertThat(ReadingValidator.bannedWordsIn("you should ask about it")).contains("should")
    }

    @Test
    fun theStemsCatchTheEndingsAndTheWholeWordsDoNot() {
        assertThat(ReadingValidator.bannedWordsIn("it recommends rest")).contains("recommend")
        assertThat(ReadingValidator.bannedWordsIn("it recommended rest")).contains("recommend")
        assertThat(ReadingValidator.bannedWordsIn("nothing was diagnosed")).contains("diagnos")
        assertThat(ReadingValidator.bannedWordsIn("that is better")).contains("better")
        assertThat(ReadingValidator.bannedWordsIn("a betterment fund")).isEmpty()
    }

    @Test
    fun theAppWideBannedListIsNotTheOneThisJobUses() {
        // DESIGN.md bans "report" and "goal" everywhere, and job 9's whole first line
        // is what kind of document this is. Running that list here would reject the
        // honest reading of nearly every letter.
        val plain = "The report says your goal is 140 degrees by October."
        assertThat(Words.bannedWordsIn(plain)).isNotEmpty()
        assertThat(ReadingValidator.checkParagraph(plain, DOCUMENT).passed).isTrue()
    }

    @Test
    fun aKindTheDocumentDoesNotSupportIsDroppedAndComesBackBlank() {
        assertThat(ReadingValidator.checkKind("", DOCUMENT).passed).isTrue()
        val guessed = honest().copy(kind = "a discharge summary from Lakeside Hospital")
        val verdict = ReadingValidator.check(guessed, DOCUMENT)
        assertThat(verdict.kind.faults).containsExactly(ReadingFault.InventedName)
        assertThat(verdict.keptKind).isEmpty()
        assertThat(verdict.kept.paragraphs).isEqualTo(PARAGRAPHS)
    }

    @Test
    fun aVerdictWithNoBannedWordInItIsStillAVerdict() {
        // What a model writes once it has been told not to say "improving". No banned
        // word, no number, no name and no condition, so every other check here passes
        // it whole, and it is the same sentence.
        listOf(
            "Your shoulder is heading in the right direction.",
            "The plan of care is on track for October.",
            "Your walking has come along since the first visit.",
            "That is a big step from where you started.",
            "Your flexion is further than it was at the first visit.",
            "The report's 120 degrees is a significant change.",
        ).forEach { caught(it, ReadingFault.Judgement) }
    }

    @Test
    fun reassuranceIsCaughtWithoutTheWordWorrying() {
        listOf(
            "Nothing in this report is out of the ordinary.",
            "There is nothing here to be alarmed about.",
            "You can be confident about the plan of care.",
        ).forEach { caught(it, ReadingFault.Judgement) }
    }

    @Test
    fun adviceOutlivesTheLossOfShouldAndRecommend() {
        listOf(
            "Keep doing the exercises twice weekly until October.",
            "Carry on with the plan of care as written.",
            "It might be worth mentioning the cane to Dr Okafor at the next visit.",
            "You do not need to do anything differently.",
            "There is no need for anything extra before October.",
        ).forEach { caught(it, ReadingFault.Advice) }
    }

    @Test
    fun aQuestionMarkDoesNotTurnAdviceIntoAQuestion() {
        // Grounded in the document and asked as a question, and still the app saying
        // to keep going.
        val leading = "Could you ask whether to keep going with the twice weekly visits?"
        val verdict = ReadingValidator.checkQuestion(leading, DOCUMENT)
        assertThat(verdict.faults).contains(ReadingFault.Advice)
    }

    @Test
    fun aNumberReadAgainstPeopleIsCaughtWithoutTheWordNormal() {
        listOf(
            "Your flexion of 120 degrees is in the usual range for someone your age.",
            "Most people walking 30 metres with a cane manage about the same.",
            "An MMT of 4/5 is typical at this point.",
            "Few people walk 30 metres with a cane at this stage.",
            "Nothing in this report is unexpected.",
        ).forEach { caught(it, ReadingFault.PopulationClaim) }
    }

    @Test
    fun secondGuessingAClinicianIsHedgedAndIsStillSecondGuessing() {
        listOf(
            "The report gives 120 degrees, though that seems low for this stage.",
            "Dr Okafor might not have seen the earlier measurement.",
            "It could be worth another opinion on the plan of care.",
        ).forEach { caught(it, ReadingFault.SecondGuess) }
    }

    @Test
    fun foodSleepAndMedicineAreOutsideWhatAReadingHandles() {
        listOf(
            "Eating more protein would help the shoulder.",
            "A good night of sleep before the next visit will help.",
            "Ask whoever wrote this whether any medication would change things.",
        ).forEach { caught(it, ReadingFault.OutOfScope) }
    }

    @Test
    fun theOtherWordsForBecauseSayWhyJustAsPlainly() {
        listOf(
            "Your flexion reached 120 degrees thanks to the twice weekly visits.",
            "The visits continued and as a result flexion reached 120 degrees.",
            "You walk 30 metres with a cane, so the gait is independent.",
            "The MMT 4/5 reflects the work you have put in.",
        ).forEach { caught(it, ReadingFault.CausalClaim) }
    }

    @Test
    fun aConditionWithNoTelltaleEndingIsCaught() {
        listOf(
            "The report is about carpal tunnel syndrome.",
            "The report describes a torn rotator cuff.",
            "This is a note about whiplash.",
        ).forEach { caught(it, ReadingFault.InventedCondition) }
    }

    @Test
    fun theAddendumsOwnWorkedPlainLineStillPasses() {
        // ADDENDUM-03 Part 7 prints this line as what good looks like. A check that
        // widened "which means" into a pattern would reject the example.
        val term = ReadingTerm(
            "MMT 4/5",
            "A clinician graded that muscle four out of five, which usually means it " +
                "moves against resistance but not full resistance.",
        )
        assertThat(ReadingValidator.checkTerm(term, DOCUMENT).passed).isTrue()
    }

    @Test
    fun theWordsAnHonestReadingNeedsAreNotOnTheseLists() {
        // A step up is a movement before it is a metaphor, a note says continue, and
        // "without help" is what the ADLs line of a note actually says. A check that
        // took any of the three would reject the honest reading of an ordinary page.
        listOf(
            "The report lists step ups onto the bottom stair.",
            "The report says to continue twice weekly for 6 weeks.",
            "The report says you dress and wash without help.",
            "The report says your personal alarm is with you.",
        ).forEach {
            val verdict = ReadingValidator.checkParagraph(it, DOCUMENT)
            assertWithMessage("$it -> ${verdict.detail}").that(verdict.passed).isTrue()
        }
    }

    @Test
    fun aQuotationOfTheDocumentIsTheDocumentSpeaking() {
        // AI.md: "If a report says a range of motion decreased, the app says the
        // report says it, and nothing more." The banned word is in the sentence and
        // the sentence is still honest, because the words inside the marks are the
        // page's own. Without this the plain words section fails on any document
        // that characterises anything, which is most of them.
        val quoted = "The report reads \"Shoulder flexion improved to 120 degrees\"."
        assertThat(ReadingValidator.bannedWordsIn(quoted)).contains("improv")
        assertThat(ReadingValidator.checkParagraph(quoted, DOCUMENT).passed).isTrue()
    }

    @Test
    fun theSameWordOutsideTheMarksIsTheAppSayingIt() {
        val said = "Your shoulder flexion improved to 120 degrees."
        assertThat(ReadingValidator.checkParagraph(said, DOCUMENT).faults)
            .contains(ReadingFault.ForbiddenWord)
    }

    @Test
    fun marksAroundWordsThePageDoesNotCarryBuyNothing() {
        // The licence is only worth having while the quotation is real.
        val faked = "The report reads \"flexion improved beyond what was hoped\"."
        val verdict = ReadingValidator.checkParagraph(faked, DOCUMENT)
        assertThat(verdict.faults).contains(ReadingFault.FabricatedQuote)
        assertThat(verdict.faults).contains(ReadingFault.ForbiddenWord)
    }

    @Test
    fun aQuotationCoversItsOwnWordsAndNotTheSameWordsAgain() {
        // Quoting the page once does not license saying it afterwards, so the marks
        // are taken out where they stand and not looked for elsewhere in the piece.
        val both = "The report reads \"Shoulder flexion improved to 120 degrees\". " +
            "Your flexion improved to 120 degrees."
        assertThat(ReadingValidator.checkParagraph(both, DOCUMENT).faults)
            .contains(ReadingFault.ForbiddenWord)
    }

    @Test
    fun aNumberWrittenOutInWordsIsPutBackTogether() {
        // Read one word at a time, "hundred" and "twenty" are each a number the page
        // does not carry, and an honest sentence about the page's own measurement is
        // thrown away for its grammar.
        assertThat(SummaryValidator.spelledIn("one hundred and twenty").map { it.second })
            .contains(HUNDRED)
        val written = "The report gives your flexion as one hundred and twenty degrees."
        assertThat(ReadingValidator.checkParagraph(written, DOCUMENT).passed).isTrue()
        val invented = "The report gives your flexion as one hundred and thirty degrees."
        assertThat(ReadingValidator.checkParagraph(invented, DOCUMENT).faults)
            .containsExactly(ReadingFault.InventedNumber)
    }

    @Test
    fun aCapitalAfterAQuotationOpensASentenceAndIsNotAName() {
        val text = "The report ends with \"Follow up with Dr Okafor after the next visit.\" " +
            "Earlier it gives your flexion as 120 degrees."
        assertThat(Words.sentences(text)).hasSize(1)
        assertThat(ReadingValidator.checkParagraph(text, DOCUMENT).passed).isTrue()
    }

    @Test
    fun aPluralAbbreviationCanStillGroundAQuestion() {
        // "ADLs" is not every letter a capital, and under that reading the only word
        // in this question with anything in it was thrown out along with it.
        assertThat(ReadingValidator.contentWordsIn("What does the note mean by ADLs?"))
            .containsExactly("ADLs")
    }

    @Test
    fun aPlainLineMaySpellOutItsOwnAbbreviation() {
        // The expansion is the answer, and two of its three words are capitals that
        // are nobody's name.
        val plain = "Manual Muscle Testing, a way of grading strength out of five."
        assertThat(ReadingValidator.properNounsIn(plain)).contains("Muscle")
        assertThat(ReadingValidator.checkTerm(ReadingTerm("MMT", plain), DOCUMENT).passed).isTrue()
        val named = ReadingTerm("MMT", "The Northgate way of grading strength out of five.")
        assertThat(ReadingValidator.checkTerm(named, DOCUMENT).faults)
            .containsExactly(ReadingFault.InventedName)
    }

    private fun caught(paragraph: String, fault: ReadingFault) {
        val verdict = ReadingValidator.checkParagraph(paragraph, DOCUMENT)
        assertWithMessage("$paragraph -> ${verdict.detail}").that(verdict.faults).contains(fault)
    }

    private fun honest() = Reading(
        kind = "a progress report from a physical therapist",
        paragraphs = PARAGRAPHS,
        terms = listOf(
            ReadingTerm("MMT 4/5", "A clinician graded that muscle four out of five."),
            ReadingTerm("Gait", "The way you walk."),
        ),
        questions = listOf(
            "Could you ask about the goal of 140 degrees by October?",
            "What does MMT 4/5 mean here?",
        ),
    )

    private companion object {

        const val HUNDRED = 100.0

        val DOCUMENT = """
            Progress report from Riverside Physical Therapy, 4 September.
            Shoulder flexion improved to 120 degrees. MMT 4/5 on the right.
            Gait is independent with a cane over 30 metres.
            Plan of care: continue twice weekly for 6 weeks. Goal is 140 degrees by October.
            The referring physician asked about the condition of the rotator cuff.
            Follow up with Dr Okafor after the next visit.
        """.trimIndent()

        val PARAGRAPHS = listOf(
            "The report says your shoulder flexion reached 120 degrees.",
            "It says you walk 30 metres with a cane, on your own.",
            "The plan of care is twice a week for 6 weeks, with 140 degrees by October.",
        )
    }
}
