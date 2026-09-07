package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.ai.Tags
import org.junit.Test

/**
 * Try it and see, and the patterns behind it.
 *
 * The rules that matter most here are the ones about not finding things: no
 * pattern on a restriction tag, no winner without a real difference, and no offer
 * at all to somebody in pacing mode.
 */
class ExperimentEngineTest {

    @Test
    fun nothingIsOfferedBeforeSixWeeks() {
        assertThat(ExperimentEngine.mayOffer(5, pacing = false, turnedOn = true)).isFalse()
        assertThat(ExperimentEngine.mayOffer(6, pacing = false, turnedOn = true)).isTrue()
    }

    @Test
    fun nothingIsEverOfferedInPacingMode() {
        // LOGIC.md section 7. Varying what you do for a fortnight to see what
        // happens is the thing that causes harm for a post-exertional pattern, so
        // this is not a decision the person has to make while unwell.
        (0..52).forEach { weeks ->
            assertWithMessage("$weeks weeks in")
                .that(ExperimentEngine.mayOffer(weeks, pacing = true, turnedOn = true))
                .isFalse()
        }
    }

    @Test
    fun nothingIsOfferedWhenItIsSwitchedOff() {
        assertThat(ExperimentEngine.mayOffer(20, pacing = false, turnedOn = false)).isFalse()
    }

    @Test
    fun theTwoHalvesAreSplitByTheDayEachResultWasTakenOn() {
        val experiment = experiment()
        val results = listOf(
            MeasureResult(Measures.chairStand.id, START + 1, 11.0),
            MeasureResult(Measures.chairStand.id, START + 13, 11.0),
            MeasureResult(Measures.chairStand.id, START + 15, 13.0),
            MeasureResult(Measures.chairStand.id, START + 27, 13.0),
        )
        val outcome = ExperimentEngine.split(experiment, results)
        assertThat(outcome.aValues).containsExactly(11.0, 11.0)
        assertThat(outcome.bValues).containsExactly(13.0, 13.0)
    }

    @Test
    fun aDifferenceInsideTheNoiseIsNoDifference() {
        // Two more chair stands is inside the three-repetition detectable change,
        // so there is no winner, and "no difference" is the honest reading.
        val outcome = Outcome(listOf(11.0), listOf(13.0), Measures.chairStand)
        assertThat(outcome.realDifference).isFalse()
        assertThat(outcome.winner).isNull()
    }

    @Test
    fun aDifferenceBeyondTheNoiseHasAWinner() {
        assertThat(Outcome(listOf(11.0), listOf(15.0), Measures.chairStand).winner).isEqualTo(Arm.B)
        assertThat(Outcome(listOf(15.0), listOf(11.0), Measures.chairStand).winner).isEqualTo(Arm.A)
    }

    @Test
    fun aMeasureWithNoDetectableChangeNeverHasAWinner() {
        val outcome = Outcome(listOf(4.0), listOf(30.0), Measures.wallPushUps)
        assertThat(outcome.realDifference).isFalse()
        assertThat(outcome.winner).isNull()
    }

    @Test
    fun aTestWithOnlyOneHalfDoneCannotBeRead() {
        assertThat(Outcome(listOf(11.0), emptyList(), Measures.chairStand).enoughToRead).isFalse()
    }

    @Test
    fun everyPermittedVariableIsAboutHowNotAboutWhat() {
        // LOGIC.md 9b permits six and forbids everything else. The forbidden ones
        // are eating, restriction, medication and sleep duration, and the way this
        // app forbids them is by having nowhere to put them.
        val forbidden = listOf("eat", "food", "meal", "fast", "medic", "sleep", "calorie")
        Variable.entries.forEach { variable ->
            forbidden.forEach { word ->
                assertWithMessage("${variable.id} must not be about $word")
                    .that(variable.id)
                    .doesNotContain(word)
            }
        }
    }

    // --- patterns ------------------------------------------------------------

    @Test
    fun noPatternBeforeSixWeeks() {
        assertThat(Patterns.find(weeks(5, "sore"))).isEmpty()
    }

    @Test
    fun aRestrictionTagCanNeverProduceAPattern() {
        // The pattern this app must never find is "you ate light and your weight
        // went down", and the way to never find it is to never look.
        Tags.restriction.forEach { tag ->
            val found = Patterns.find(weeks(10, tag))
            assertWithMessage("$tag must never produce a pattern").that(found).isEmpty()
        }
    }

    @Test
    fun aSplitBelowFiveOfSevenIsNotAPattern() {
        val mixed = (0 until 8).map { week ->
            WeekOfDays(
                weekStartDay = week * 7L,
                tagsOnThreeDays = if (week % 2 == 0) setOf("busy") else emptySet(),
                sleepAverageHours = if (week == 0) 5.0 else 7.0,
                daysMoved = 3,
                weightChangeKg = 0.0,
            )
        }
        assertThat(Patterns.find(mixed).filter { it.measure == PatternMeasure.Sleep }).isEmpty()
    }

    @Test
    fun aClearSplitIsAPatternAndCarriesItsOwnNumbers() {
        val found = Patterns.find(weeks(12, "busy")).filter { it.measure == PatternMeasure.Sleep }
        assertThat(found).isNotEmpty()
        val pattern = found.first()
        assertThat(pattern.tag).isEqualTo("busy")
        assertThat(pattern.split).contains(" of ")
        assertThat(pattern.higherWithTag).isFalse()
    }

    private fun experiment() = Experiment(
        variable = Variable.TimeOfDay,
        conditionA = "Mornings",
        conditionB = "Evenings",
        measureId = Measures.chairStand.id,
        startedOnDay = START,
    )

    /** [count] weeks where the tag is on half of them and sleep is worse there. */
    private fun weeks(count: Int, tag: String) = (0 until count).map { week ->
        val withTag = week < count / 2
        WeekOfDays(
            weekStartDay = week * 7L,
            tagsOnThreeDays = if (withTag) setOf(tag) else emptySet(),
            sleepAverageHours = if (withTag) 5.5 else 7.5,
            daysMoved = if (withTag) 2 else 4,
            weightChangeKg = 0.0,
        )
    }

    private companion object {
        const val START = 20_000L
    }
}
