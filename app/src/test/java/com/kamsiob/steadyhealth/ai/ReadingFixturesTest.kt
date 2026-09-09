package com.kamsiob.steadyhealth.ai

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * The job 9 validator, against a corpus of readings a model would plausibly write.
 *
 * AI.md asks job 6's validator for "a corpus of at least 30 deliberately bad outputs,
 * one per failure mode" and asks job 9 for the five adversarial cases by name:
 * characterising progress, naming a condition absent from the text, recommending
 * anything, inventing a measurement, and answering a question the document raises.
 * This is that corpus for job 9, and it came before the model, for the reason
 * ADDENDUM-01 gives: a sentence somebody might act on is not something a prompt
 * instruction prevents.
 *
 * Every case is checked against one document, because that is the shape of this job.
 * There is no brief and no facts, only the text that came off the photograph, so a
 * piece of a reading is right or wrong only in relation to it. The good cases are the
 * other half of the test: a validator that rejects everything protects nobody, and a
 * reading that never appears is a feature that does not exist.
 */
class ReadingFixturesTest {

    @Test
    fun theCorpusIsAtLeastThirtyCasesAndCoversEveryFault() {
        assertThat(cases().size + good().size).isAtLeast(THIRTY)
        assertThat(cases().map { it.fault }.toSet()).containsExactlyElementsIn(ReadingFault.entries)
    }

    @Test
    fun everyPartOfAReadingIsAttacked() {
        // A corpus that only tries paragraphs leaves the kind, the terms and the
        // questions untested, and three of the five named cases live in those.
        assertThat(cases().map { it.part }.toSet())
            .containsExactly("kind", "paragraph", "term", "question")
    }

    @Test
    fun everyDeliberatelyBadPieceIsCaught() {
        cases().forEach { case ->
            assertWithMessage("${case.why}: ${case.text}")
                .that(check(case.part, case.text, case.plain).passed)
                .isFalse()
        }
    }

    @Test
    fun everyBadPieceIsCaughtForTheRightReason() {
        cases().forEach { case ->
            val verdict = check(case.part, case.text, case.plain)
            assertWithMessage("${case.why}: ${case.text} -> ${verdict.detail}")
                .that(verdict.faults)
                .contains(case.fault)
        }
    }

    @Test
    fun everyHonestPieceSurvives() {
        good().forEach { case ->
            val verdict = check(case.part, case.text, case.plain)
            assertWithMessage("${case.why}: ${case.text} -> ${verdict.detail}")
                .that(verdict.passed)
                .isTrue()
        }
    }

    @Test
    fun theCorpusHoldsAsManyHonestReadingsAsTheOtherKind() {
        // The half of this that is easy to forget. A validator can be made to pass
        // every bad case by rejecting everything, and the only thing standing in the
        // way of that is a corpus of honest readings large enough to hurt.
        assertThat(good().size).isAtLeast(FIFTEEN)
        assertThat(good().map { it.part }.toSet())
            .containsExactly("kind", "paragraph", "term", "question")
    }

    @Test
    fun theHonestPiecesAssembleIntoAReadingThatSurvivesWhole() {
        // As many paragraphs as a reading may carry. The corpus holds more honest
        // ones than that, and everyHonestPieceSurvives is where the rest are checked.
        val reading = Reading(
            kind = good().first { it.part == "kind" }.text,
            paragraphs = good().filter { it.part == "paragraph" }
                .map { it.text }
                .take(Reading.MOST_PARAGRAPHS),
            terms = good().filter { it.part == "term" }.map { ReadingTerm(it.text, it.plain) },
            questions = good().filter { it.part == "question" }.map { it.text },
        )
        val verdict = ReadingValidator.check(reading, document())
        assertThat(verdict.faults).isEmpty()
        assertThat(verdict.kept).isEqualTo(reading)
        assertThat(verdict.trimmedParagraphs).isEqualTo(0)
        assertThat(verdict.paragraphsAllFailed).isFalse()
    }

    @Test
    fun aReadingOfNothingButTheCorpusLosesEveryParagraphAndKeepsTheHonestTerms() {
        // What the screen has to cope with: AI.md says that when the plain words
        // section fails entirely the terms and the photo are shown on their own.
        val reading = Reading(
            kind = "",
            paragraphs = cases().filter { it.part == "paragraph" }.map { it.text }.take(THREE),
            terms = good().filter { it.part == "term" }.map { ReadingTerm(it.text, it.plain) },
            questions = emptyList(),
        )
        val verdict = ReadingValidator.check(reading, document())
        assertThat(verdict.paragraphsAllFailed).isTrue()
        assertThat(verdict.keptTerms).hasSize(reading.terms.size)
    }

    // --- the fixture ---------------------------------------------------------

    private data class Case(
        val why: String,
        val part: String,
        val text: String,
        val plain: String,
        val fault: ReadingFault,
    )

    private data class Good(
        val why: String,
        val part: String,
        val text: String,
        val plain: String,
    )

    /** One piece of a reading, checked the way the screen's own section would be. */
    private fun check(part: String, text: String, plain: String): PieceVerdict<*> = when (part) {
        "kind" -> ReadingValidator.checkKind(text, document())
        "paragraph" -> ReadingValidator.checkParagraph(text, document())
        "term" -> ReadingValidator.checkTerm(ReadingTerm(text, plain), document())
        "question" -> ReadingValidator.checkQuestion(text, document())
        else -> error("the corpus has a case with an unknown part \"$part\"")
    }

    private fun corpus(): JsonObject = Json.parseToJsonElement(
        checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/job9-bad-readings.json"))
            .bufferedReader()
            .use { it.readText() },
    ).jsonObject

    private fun document(): String =
        corpus().getValue("document").jsonObject.getValue("text").jsonPrimitive.content

    private fun cases(): List<Case> = corpus().getValue("cases").jsonArray.map {
        val row = it.jsonObject
        Case(
            why = row.getValue("why").jsonPrimitive.content,
            part = row.getValue("part").jsonPrimitive.content,
            text = row.getValue("text").jsonPrimitive.content,
            plain = row["plain"]?.jsonPrimitive?.content.orEmpty(),
            fault = ReadingFault.valueOf(row.getValue("fault").jsonPrimitive.content),
        )
    }

    private fun good(): List<Good> = corpus().getValue("good").jsonArray.map {
        val row = it.jsonObject
        Good(
            why = row.getValue("why").jsonPrimitive.content,
            part = row.getValue("part").jsonPrimitive.content,
            text = row.getValue("text").jsonPrimitive.content,
            plain = row["plain"]?.jsonPrimitive?.content.orEmpty(),
        )
    }

    private companion object {
        const val THIRTY = 30
        const val FIFTEEN = 15
        const val THREE = 3
    }
}
