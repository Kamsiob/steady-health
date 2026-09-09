package com.kamsiob.steadyhealth.share

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * What may never go on a card, and what the app itself suggests.
 *
 * ADDENDUM-03 Part 11: "NEVER ON A CARD: weight, any clinically named measure, any
 * rating out of ten, anything about a condition, anything they did not choose."
 *
 * The last clause is why the second half of this file exists. A person can type
 * whatever they like and the app only says what it noticed; the app's own suggested
 * lines are different, because a suggestion is the app choosing, and a suggestion
 * carrying a rating out of ten would put one on a card without anybody asking.
 */
class CardRulesTest {

    @Test
    fun weightIsNoticedHoweverItIsWritten() {
        listOf(
            "I am down 14 lbs",
            "Three kilos lighter",
            "Two stone since March",
            "I lost 6 kg",
        ).forEach { line ->
            assertWithMessage(line).that(CardRules.worthAWord(line)).isNotEmpty()
        }
    }

    @Test
    fun aClinicalWordIsNoticed() {
        listOf(
            "My blood pressure is better",
            "Since my diagnosis",
            "Living with arthritis",
            "Off one medication",
        ).forEach { line ->
            assertWithMessage(line).that(CardRules.worthAWord(line)).isNotEmpty()
        }
    }

    @Test
    fun aRatingOutOfTenIsNoticedHoweverItIsWritten() {
        listOf("I put it at 7/10", "It was 3 out of 10", "10 out of 10 now").forEach { line ->
            assertWithMessage(line).that(CardRules.worthAWord(line)).contains("a rating out of ten")
        }
    }

    @Test
    fun anOrdinarySentenceIsNotNoticed() {
        // A check that fires on ordinary sentences is a check people stop reading.
        listOf(
            "I got off the floor without my hands today",
            "Four sessions this week and the stairs are easier",
            "I walked to the shop and back",
            "A month of this now",
            "I can carry the shopping in one trip",
        ).forEach { line ->
            assertWithMessage(line).that(CardRules.worthAWord(line)).isEmpty()
        }
    }

    @Test
    fun aLineThatWouldNotFitIsTooLong() {
        assertThat(CardRules.tooLong("Short")).isFalse()
        assertThat(CardRules.tooLong("a".repeat(CardRules.MOST_CHARACTERS))).isFalse()
        assertThat(CardRules.tooLong("a".repeat(CardRules.MOST_CHARACTERS + 1))).isTrue()
    }

    @Test
    fun everyLineTheAppSuggestsPassesTheSameCheck() {
        val suggested = suggestedLines()
        assertWithMessage("the suggested lines were found at all").that(suggested).isNotEmpty()
        suggested.forEach { (name, line) ->
            // The placeholders are replaced with something ordinary, because a
            // suggestion is only safe if it is safe once it is filled in.
            val filled = line
                .replace(Regex("""%\d\$[sd]"""), "the stairs")
                .replace("\\'", "'")
            assertWithMessage("$name: $filled").that(CardRules.worthAWord(filled)).isEmpty()
            assertWithMessage("$name is short enough").that(CardRules.tooLong(filled)).isFalse()
        }
    }

    @Test
    fun onlyTheMonthCardIsAlsoADocument() {
        assertThat(CardKind.entries.filter { it.alsoAPdf }).containsExactly(CardKind.Month)
    }

    @Test
    fun everyKindSurvivesBeingWrittenDownAndReadBack() {
        CardKind.entries.forEach { assertThat(CardKind.fromId(it.id)).isEqualTo(it) }
        assertThat(CardKind.fromId("postcard")).isNull()
    }

    /** Every card_ string and plural item, read out of the resource file itself. */
    private fun suggestedLines(): List<Pair<String, String>> {
        val text = File("src/main/res/values/strings.xml").readText()
        val singles = Regex("""<string name="(card_(?:week|milestone|month))">(.*?)</string>""")
            .findAll(text)
            .map { it.groupValues[1] to it.groupValues[2] }
        val plurals = Regex("""<plurals name="(card_week)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(text)
            .flatMap { block ->
                Regex("""<item quantity="\w+">(.*?)</item>""")
                    .findAll(block.groupValues[2])
                    .map { block.groupValues[1] to it.groupValues[1] }
            }
        return (singles + plurals).toList()
    }
}
