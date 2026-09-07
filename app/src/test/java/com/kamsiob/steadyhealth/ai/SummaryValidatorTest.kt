package com.kamsiob.steadyhealth.ai

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * The validator, against a corpus of things a model would plausibly write.
 *
 * ADDENDUM-01: "the validator in AI.md Job 6 is not optional: build it before the
 * model is wired in, with the adversarial fixture corpus, because an unvalidated
 * sentence in this feature is the one failure that actually matters."
 *
 * So the corpus came first and the model has not been wired at all. Every case in
 * job6-bad-outputs.json is a sentence a language model writes readily against
 * this brief, and every one of them has to be caught. The six good ones are the
 * other half of the test: a validator that rejects everything is not a validator.
 */
class SummaryValidatorTest {

    @Test
    fun theCorpusIsAtLeastThirtyCasesAndCoversEveryFault() {
        assertThat(cases().size).isAtLeast(THIRTY)
        assertThat(cases().map { it.fault }.toSet()).containsExactlyElementsIn(Fault.entries)
    }

    @Test
    fun everyDeliberatelyBadParagraphIsCaught() {
        cases().forEach { case ->
            val verdict = SummaryValidator.check(
                SummaryParagraph(case.text, case.cites),
                brief(),
            )
            assertWithMessage("${case.why}: ${case.text}")
                .that(verdict.passed)
                .isFalse()
        }
    }

    @Test
    fun everyBadParagraphIsCaughtForTheRightReason() {
        cases().forEach { case ->
            val verdict = SummaryValidator.check(
                SummaryParagraph(case.text, case.cites),
                brief(),
            )
            assertWithMessage("${case.why}: ${case.text} -> ${verdict.detail}")
                .that(verdict.faults)
                .contains(case.fault)
        }
    }

    @Test
    fun everyHonestParagraphSurvives() {
        // The other half. A validator that rejects everything protects nobody,
        // because the summary then always falls back and the feature is dead.
        good().forEach { case ->
            val verdict = SummaryValidator.check(
                SummaryParagraph(case.text, case.cites),
                brief(),
            )
            assertWithMessage("${case.why}: ${case.text} -> ${verdict.detail}")
                .that(verdict.passed)
                .isTrue()
        }
    }

    @Test
    fun aQuestionWithoutACandidateIsDropped() {
        val summary = VisitSummary(
            paragraphs = emptyList(),
            questions = listOf(
                SummaryQuestion("Is the soreness worth looking at?", "c_sore"),
                SummaryQuestion("Should you have a scan?", "c_invented"),
            ),
            fromModel = true,
        )
        val verdict = SummaryValidator.check(summary, brief())
        assertThat(verdict.questions.map { it.candidateId }).containsExactly("c_sore")
        assertThat(verdict.droppedQuestions).isEqualTo(1)
    }

    @Test
    fun neverMoreThanFourQuestionsAndNeverMoreThanThreeParagraphs() {
        val many = VisitSummary(
            paragraphs = List(6) { SummaryParagraph("Go is Same.", listOf("f_go")) },
            questions = List(9) { SummaryQuestion("Worth asking?", "c_sore") },
            fromModel = true,
        )
        val verdict = SummaryValidator.check(many, brief())
        assertThat(verdict.paragraphs).hasSize(SummaryValidator.MOST_PARAGRAPHS)
        assertThat(verdict.questions).hasSize(VisitBrief.MOST_QUESTIONS)
    }

    @Test
    fun aSummaryWhereEverythingFailsSaysSo() {
        val summary = VisitSummary(
            paragraphs = List(3) {
                SummaryParagraph("Get up improved because of the walks.", listOf("f_getup"))
            },
            questions = emptyList(),
            fromModel = true,
        )
        val verdict = SummaryValidator.check(summary, brief())
        assertThat(verdict.allFailed).isTrue()
        assertThat(verdict.kept).isEmpty()
    }

    @Test
    fun numbersAreFoundAsDigitsAndAsWords() {
        assertThat(SummaryValidator.numbersIn("14 stands, up from nine"))
            .containsExactly(14.0, 9.0)
        assertThat(SummaryValidator.numbersIn("nothing numeric here")).isEmpty()
    }

    @Test
    fun quotesAreFoundInsideEitherKindOfQuotationMark() {
        assertThat(SummaryValidator.quotesIn("""You wrote "the knees" today."""))
            .containsExactly("the knees")
        assertThat(SummaryValidator.quotesIn("You wrote “the knees” today."))
            .containsExactly("the knees")
    }

    @Test
    fun aNumberInACitedFactIsAllowedAndTheSameNumberElsewhereIsNot() {
        val allowed = SummaryValidator.check(
            SummaryParagraph("Sore appeared on 6 days.", listOf("f_sore")),
            brief(),
        )
        val notAllowed = SummaryValidator.check(
            SummaryParagraph("Sore appeared on 6 days.", listOf("f_go")),
            brief(),
        )
        assertThat(allowed.passed).isTrue()
        assertThat(notAllowed.faults).contains(Fault.InventedNumber)
    }

    // --- the fixture ---------------------------------------------------------

    private data class Case(
        val why: String,
        val text: String,
        val cites: List<String>,
        val fault: Fault,
    )

    private data class Good(val why: String, val text: String, val cites: List<String>)

    private fun corpus(): JsonObject = Json.parseToJsonElement(
        checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/job6-bad-outputs.json"))
            .bufferedReader()
            .use { it.readText() },
    ).jsonObject

    private fun cases(): List<Case> = corpus().getValue("cases").jsonArray.map {
        val row = it.jsonObject
        Case(
            why = row.getValue("why").jsonPrimitive.content,
            text = row.getValue("text").jsonPrimitive.content,
            cites = row.getValue("cites").jsonArray.map { c -> c.jsonPrimitive.content },
            fault = Fault.valueOf(row.getValue("fault").jsonPrimitive.content),
        )
    }

    private fun good(): List<Good> = corpus().getValue("good").jsonArray.map {
        val row = it.jsonObject
        Good(
            why = row.getValue("why").jsonPrimitive.content,
            text = row.getValue("text").jsonPrimitive.content,
            cites = row.getValue("cites").jsonArray.map { c -> c.jsonPrimitive.content },
        )
    }

    private fun brief(): VisitBrief {
        val raw = corpus().getValue("brief").jsonObject
        val window = raw.getValue("window").jsonObject
        return VisitBrief(
            window = VisitWindow(
                fromDay = window.getValue("fromDay").jsonPrimitive.content.toLong(),
                toDay = window.getValue("toDay").jsonPrimitive.content.toLong(),
                label = window.getValue("label").jsonPrimitive.content,
            ),
            facts = raw.getValue("facts").jsonArray.map { element ->
                val row = element.jsonObject
                Fact(
                    id = row.getValue("id").jsonPrimitive.content,
                    kind = row.getValue("kind").jsonPrimitive.content,
                    text = row.getValue("text").jsonPrimitive.content,
                    numbers = row["numbers"]?.jsonArray?.map { it.jsonPrimitive.double }.orEmpty(),
                    months = row["months"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
                    quotes = row["quotes"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
                )
            },
            candidates = raw.getValue("candidates").jsonArray.map { element ->
                val row = element.jsonObject
                QuestionCandidate(
                    id = row.getValue("id").jsonPrimitive.content,
                    rule = row.getValue("rule").jsonPrimitive.content.toInt(),
                    text = row.getValue("text").jsonPrimitive.content,
                    evidence = row.getValue("evidence").jsonArray.map { it.jsonPrimitive.content },
                )
            },
        )
    }

    private companion object {
        const val THIRTY = 30
    }
}
