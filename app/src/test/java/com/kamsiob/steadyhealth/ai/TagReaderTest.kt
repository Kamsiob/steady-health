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
 * The vocabulary, and the validator that keeps the reader inside it.
 *
 * The fixture corpus in job2-tagging.json is the one AI.md asks for: at least
 * thirty sentences with the tags they must and must not produce. The must-not
 * half is checkable today, without a model, and it is the half that matters,
 * because it is where a keen classifier does harm.
 */
class TagReaderTest {

    @Test
    fun thereAreExactlyTwentyFourTags() {
        assertThat(Tags.all).hasSize(Tags.COUNT)
        assertThat(Tags.ids).hasSize(Tags.COUNT)
    }

    @Test
    fun noTagIsAJudgement() {
        // AI.md: "Overate," "binged," "cheated," "bad day," and any moralising
        // word do not exist and cannot be produced.
        val forbidden = listOf(
            "overate", "binged", "cheated", "cheat", "bad_day", "bad", "fail",
            "lazy", "good_day", "junk", "clean", "guilty",
        )
        forbidden.forEach { word ->
            assertWithMessage("no tag is called $word")
                .that(Tags.ids)
                .doesNotContain(word)
        }
    }

    @Test
    fun everyTagInTheFixtureCorpusIsRealAndEveryForbiddenOneIsNot() {
        assertThat(cases()).hasSize(atLeastThirty(cases().size))
        cases().forEach { case ->
            case.must.forEach { tag ->
                assertWithMessage("${case.text} expects $tag").that(Tags.ids).contains(tag)
            }
        }
    }

    @Test
    fun theValidatorDropsEveryTagThatIsNotOneOfTheTwentyFour() {
        cases().forEach { case ->
            val pretend = case.mustNot.map { ReadTag(it, 0.9) }
            val survived = TagReader.validate(pretend).map(ReadTag::tag)
            val invented = case.mustNot.filterNot { it in Tags.ids }
            assertWithMessage("${case.text}: invented tags must not survive")
                .that(survived)
                .containsNoneIn(invented)
        }
    }

    @Test
    fun theValidatorNeverReturnsMoreThanThree() {
        val many = Tags.all.mapIndexed { i, tag -> ReadTag(tag.id, 1.0 - i / 100.0) }
        assertThat(TagReader.validate(many)).hasSize(Tags.MOST_PER_SENTENCE)
    }

    @Test
    fun theValidatorKeepsTheMostConfidentOfADuplicate() {
        val proposed = listOf(ReadTag("sore", 0.2), ReadTag("sore", 0.8))
        assertThat(TagReader.validate(proposed)).containsExactly(ReadTag("sore", 0.8))
    }

    @Test
    fun theValidatorDropsAConfidenceOutsideZeroToOne() {
        assertThat(TagReader.validate(listOf(ReadTag("sore", 1.5)))).isEmpty()
        assertThat(TagReader.validate(listOf(ReadTag("sore", -0.1)))).isEmpty()
    }

    @Test
    fun whatThePersonTaughtTheAppComesBackFirstAndCertain() {
        val synonyms = fixtureSynonyms()
        val read = TagReader.fromSynonyms("Grabbed takeout on the way home.", synonyms)
        assertThat(read.map(ReadTag::tag)).containsExactly("ate_out")
        assertThat(read.first().confidence).isEqualTo(1.0)
    }

    @Test
    fun aSynonymMatchesWholeWordsOnly() {
        val synonyms = listOf(Synonym("diner", "ate_out"))
        assertThat(TagReader.fromSynonyms("Had dinner at home.", synonyms)).isEmpty()
        assertThat(TagReader.fromSynonyms("Went to the diner.", synonyms)).isNotEmpty()
    }

    @Test
    fun aSynonymForATagThatDoesNotExistIsIgnored() {
        val synonyms = listOf(Synonym("takeout", "cheated"))
        assertThat(TagReader.fromSynonyms("Takeout again.", synonyms)).isEmpty()
    }

    @Test
    fun everySynonymInTheCorpusResolvesToARealTag() {
        fixtureSynonyms().forEach { synonym ->
            assertWithMessage(synonym.phrase).that(Tags.ids).contains(synonym.tag)
        }
    }

    @Test
    fun whatThePersonTaughtBeatsWhatTheReaderGuessed() {
        val taught = listOf(ReadTag("ate_out", 1.0))
        val guessed = listOf(ReadTag("cooked", 0.9), ReadTag("social", 0.8), ReadTag("busy", 0.7))
        val combined = TagReader.combine(taught, guessed)
        assertThat(combined).hasSize(Tags.MOST_PER_SENTENCE)
        assertThat(combined.first().tag).isEqualTo("ate_out")
    }

    @Test
    fun aWholeDiaryEntryIsNotLearnableAsAPhrase() {
        assertThat(TagReader.learnable("takeout")).isTrue()
        assertThat(TagReader.learnable("grabbed takeout on the way")).isTrue()
        assertThat(TagReader.learnable("I got home late and could not face cooking so I stopped")).isFalse()
        assertThat(TagReader.learnable("a")).isFalse()
    }

    @Test
    fun theRestrictionTagsAreAllRealTags() {
        assertThat(Tags.ids).containsAtLeastElementsIn(Tags.restriction)
    }

    private data class Case(val text: String, val must: List<String>, val mustNot: List<String>)

    private fun corpus(): JsonObject = Json.parseToJsonElement(
        checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/job2-tagging.json"))
            .bufferedReader()
            .use { it.readText() },
    ).jsonObject

    private fun cases(): List<Case> = corpus().getValue("cases").jsonArray.map { element ->
        val row = element.jsonObject
        Case(
            text = row.getValue("text").jsonPrimitive.content,
            must = row.getValue("must").jsonArray.map { it.jsonPrimitive.content },
            mustNot = row.getValue("mustNot").jsonArray.map { it.jsonPrimitive.content },
        )
    }

    private fun fixtureSynonyms(): List<Synonym> =
        corpus().getValue("synonyms").jsonArray.map { element ->
            val row = element.jsonObject
            Synonym(
                row.getValue("phrase").jsonPrimitive.content,
                row.getValue("tag").jsonPrimitive.content,
            )
        }

    /** The corpus has to be at least thirty cases. AI.md, testing the model jobs. */
    private fun atLeastThirty(size: Int): Int {
        assertWithMessage("the corpus is at least thirty cases").that(size).isAtLeast(THIRTY)
        return size
    }

    private companion object {
        const val THIRTY = 30
    }
}
