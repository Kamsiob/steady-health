package com.kamsiob.steadyhealth.ai

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.domain.DayRating
import com.kamsiob.steadyhealth.domain.TalkTest
import org.junit.Test

/**
 * The Sunday write-up without the model, which for most people is the Sunday
 * write-up.
 */
class WeekWriterTest {

    @Test
    fun aQuietWeekSaysSoAndDoesNotApologise() {
        val note = WeekWriter.write(empty)
        assertThat(note.paragraphs).hasSize(1)
        assertThat(note.paragraphs.first()).contains("nothing wrong with that")
    }

    @Test
    fun theNoteNeverStatesAWeightNumber() {
        val note = WeekWriter.write(busy.copy(weightDirection = WeightDirection.ALittleLower))
        val text = note.paragraphs.joinToString(" ")
        assertThat(text).contains("a little lower")
        assertThat(text).doesNotContain("kg")
        assertThat(text).doesNotContain("lb")
    }

    @Test
    fun noNoteEverUsesABannedWord() {
        everyShapeOfWeek().map(WeekWriter::write).forEach { note ->
            note.paragraphs.forEach { paragraph ->
                assertWithMessage(paragraph).that(Words.bannedWordsIn(paragraph)).isEmpty()
            }
        }
    }

    @Test
    fun noNoteEverPutsFoodAndWeightInOneSentence() {
        everyShapeOfWeek().map(WeekWriter::write).forEach { note ->
            note.paragraphs.forEach { paragraph ->
                Words.sentences(paragraph).forEach { sentence ->
                    assertWithMessage(sentence).that(WeekFilter.offends(sentence)).isFalse()
                }
            }
        }
    }

    @Test
    fun noNoteRunsPastEightyWords() {
        everyShapeOfWeek().map(WeekWriter::write).forEach { note ->
            assertWithMessage(note.paragraphs.joinToString(" "))
                .that(note.words)
                .isAtMost(WeekWriter.MOST_WORDS)
        }
    }

    @Test
    fun withNumbersOffTheNoteHasNoMinutesInIt() {
        val note = WeekWriter.write(busy.copy(minutes = null))
        assertThat(note.paragraphs.joinToString(" ")).doesNotContain("minutes in all")
    }

    @Test
    fun aRestrictionTagIsNeverSpokenAbout() {
        val note = WeekWriter.write(
            busy.copy(
                tags = listOf(TagCount("ate_light", 5), TagCount("late_night", 4)),
                weightDirection = WeightDirection.ALittleLower,
            ),
        )
        val text = note.paragraphs.joinToString(" ")
        assertThat(text).doesNotContain("light")
        assertThat(text).doesNotContain("late")
    }

    @Test
    fun aHardWeekIsSaidPlainlyAndIsNotAFailure() {
        val note = WeekWriter.write(busy.copy(dayRatings = List(5) { DayRating.Rough }))
        assertThat(note.paragraphs.joinToString(" ")).contains("hard week")
    }

    @Test
    fun theFilterTakesOutASentenceThatJoinsFoodToWeight() {
        val bad = "You ate light on four days and your weight is a little lower."
        assertThat(WeekFilter.offends(bad)).isTrue()
        assertThat(WeekFilter.clean("$bad You moved on three days.").trim())
            .isEqualTo("You moved on three days.")
    }

    @Test
    fun theFilterLeavesAnHonestSentenceAlone() {
        val fine = "Your weight is about the same as last week."
        assertThat(WeekFilter.offends(fine)).isFalse()
        assertThat(WeekFilter.clean(fine)).isEqualTo(fine)
    }

    private val empty = WeekBrief(
        daysMoved = 0,
        minutes = 0,
        tags = emptyList(),
        dayRatings = emptyList(),
        sleepAverageHours = null,
        walkName = "A 2 minute walk",
        stepOffered = false,
        talkResults = emptyList(),
        weightDirection = null,
        whatTheyWant = "Carry the groceries in one trip",
    )

    private val busy = empty.copy(
        daysMoved = 4,
        minutes = 22,
        tags = listOf(TagCount("slept_badly", 4), TagCount("sore", 3)),
        dayRatings = listOf(DayRating.Okay, DayRating.Good, DayRating.Okay),
        talkResults = listOf(TalkTest.YesEasily, TalkTest.JustAbout),
    )

    /** Enough shapes of week that the rules above are tested against all of them. */
    private fun everyShapeOfWeek(): List<WeekBrief> = buildList {
        add(empty)
        add(busy)
        WeightDirection.entries.forEach { add(busy.copy(weightDirection = it)) }
        add(busy.copy(stepOffered = true))
        add(busy.copy(minutes = null))
        add(busy.copy(talkResults = List(3) { TalkTest.No }))
        add(busy.copy(talkResults = List(3) { TalkTest.YesEasily }))
        add(busy.copy(dayRatings = List(4) { DayRating.Good }))
        add(busy.copy(dayRatings = List(4) { DayRating.Rough }))
        add(busy.copy(daysMoved = 1, minutes = 2))
        add(busy.copy(daysMoved = 7, minutes = 210, stepOffered = true))
        Tags.all.forEach { tag ->
            add(
                busy.copy(
                    tags = listOf(TagCount(tag.id, 5)),
                    weightDirection = WeightDirection.ALittleLower,
                ),
            )
        }
    }
}
