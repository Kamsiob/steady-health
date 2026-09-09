package com.kamsiob.steadyhealth.scan

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * The corpus of real pages for jobs 8 and 9, held against the thing that decides
 * what happens to them.
 *
 * AI.md asks both jobs for "at least thirty documents, including a progress note,
 * a plan of care, a discharge summary, an insurance letter, a referral, a
 * handwritten programme, a badly photographed one, one in Spanish, one out of
 * scope, and one blank". The corpus comes before the model for the same reason
 * job 6's did: it is what the model will be measured against, and a corpus written
 * afterwards is an exam written to fit the answers.
 *
 * None of it needs a model to earn its place today. PageKind.of ships already, it
 * is deterministic, and it is the gate in front of both jobs: it is what decides
 * whether a page is offered to the reader at all. So every document carries the
 * outcome ADDENDUM-03 Part 5 has to reach on it, and this file is the whole corpus
 * put through that one question.
 *
 * The pages are written the way extraction hands text over rather than the way
 * somebody would type them out: line breaks in the wrong places, table columns
 * collapsed into a run of numbers, letters dropped where the glare was, 0 for O
 * and l for 1, and no punctuation at all on the handwritten one. A fixture that
 * reads cleanly is a fixture for a page nobody will ever photograph.
 *
 * The out of scope ones are the reason to have the file at all. Part 5's
 * unclear screen offers the reader as one of its two buttons, so a blood test that
 * came back Unclear is a blood test somebody gets explained to them, and that is
 * the failure this corpus exists to catch.
 */
class DocumentCorpusTest {

    @Test
    fun theCorpusIsAtLeastThirtyDocumentsAndReachesEveryOutcome() {
        assertThat(corpus().size).isAtLeast(THIRTY)
        assertThat(corpus().map { it.kind }.toSet()).containsExactlyElementsIn(EVERY_OUTCOME)
    }

    @Test
    fun theCorpusHoldsEveryPageTheSpecNames() {
        // Pinned by name, so that removing the Spanish one or the blank one to make
        // a test go green breaks a different test instead.
        assertThat(corpus().map { it.name }).containsAtLeast(
            "progress-note",
            "plan-of-care",
            "discharge-summary",
            "insurance-benefits-letter",
            "gp-referral",
            "handwritten-programme",
            "glare-damaged-note",
            "spanish-home-programme",
            "blood-test-panel",
            "blank-page",
        )
    }

    @Test
    fun everyDocumentClassifiesTheWayTheCorpusSaysItDoes() {
        corpus().forEach { document ->
            val kind = PageKind.of(document.text)
            assertWithMessage("${document.name}: ${document.why} -> ${kind.signals}")
                .that(kind)
                .isInstanceOf(classOf(document.kind))
        }
    }

    @Test
    fun nothingOutOfScopeIsEverOfferedToTheReader() {
        // The same assertion as above for seven of the documents, kept apart because
        // this is the one that matters and a failure should say which rule broke.
        // Exercises, ReportOrLetter and Both all offer something, and Unclear offers
        // both, so anything short of OutOfScope hands the page to job 9.
        corpus().filter { it.kind == OUT_OF_SCOPE }.forEach { document ->
            assertWithMessage("${document.name} would have been read: ${document.why}")
                .that(PageKind.of(document.text))
                .isInstanceOf(PageKind.OutOfScope::class.java)
        }
    }

    @Test
    fun everyDocumentGivesTheSameAnswerTwice() {
        // Nothing in the classifier is allowed to depend on a model, a clock or a
        // random number, and the whole corpus is a better place to check that than
        // one page.
        corpus().forEach { document ->
            assertWithMessage(document.name)
                .that(PageKind.of(document.text))
                .isEqualTo(PageKind.of(document.text))
        }
    }

    // --- the fixture ---------------------------------------------------------

    private data class Document(
        val name: String,
        val why: String,
        val text: String,
        val kind: String,
    )

    private fun file(): JsonObject = Json.parseToJsonElement(
        checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/job8-9-documents.json"))
            .bufferedReader()
            .use { it.readText() },
    ).jsonObject

    private fun corpus(): List<Document> = file().getValue("documents").jsonArray.map {
        val row = it.jsonObject
        Document(
            name = row.getValue("name").jsonPrimitive.content,
            why = row.getValue("why").jsonPrimitive.content,
            text = row.getValue("text").jsonPrimitive.content,
            kind = row.getValue("kind").jsonPrimitive.content,
        )
    }

    /**
     * The name in the fixture, and the result it stands for.
     *
     * Written out rather than looked up by reflection, so that a document naming an
     * outcome that does not exist fails here with the name it used instead of
     * quietly matching nothing.
     */
    private fun classOf(kind: String): Class<out PageKind> = when (kind) {
        "Exercises" -> PageKind.Exercises::class.java
        "ReportOrLetter" -> PageKind.ReportOrLetter::class.java
        "Both" -> PageKind.Both::class.java
        "Unclear" -> PageKind.Unclear::class.java
        OUT_OF_SCOPE -> PageKind.OutOfScope::class.java
        else -> error("no such page kind: $kind")
    }

    private companion object {
        const val THIRTY = 30
        const val OUT_OF_SCOPE = "OutOfScope"

        val EVERY_OUTCOME = listOf(
            "Exercises",
            "ReportOrLetter",
            "Both",
            "Unclear",
            OUT_OF_SCOPE,
        )
    }
}
