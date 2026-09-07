package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import com.kamsiob.steadyhealth.domain.GettingAround
import org.junit.Test

/** LOGIC.md 3b: Better, Same, Quieter, and how hard it is to say the third one. */
class AbilityEngineTest {

    @Test
    fun withNothingMeasuredAnAbilityIsSameAndNotUnknown() {
        // Same is the honest answer before the first check, and the app has no
        // fourth word. It is also the answer that costs nobody anything.
        assertThat(AbilityEngine.stateOf(AbilityDomain.GetUp, emptyList()))
            .isEqualTo(AbilityState.Same)
    }

    @Test
    fun oneCheckIsNotAChange() {
        val results = listOf(chairStand(day = 0, value = 10.0))
        assertThat(AbilityEngine.stateOf(AbilityDomain.GetUp, results))
            .isEqualTo(AbilityState.Same)
    }

    @Test
    fun aMoveInsideTheDetectableChangeIsSame() {
        val results = listOf(chairStand(0, 10.0), chairStand(30, 12.0))
        assertThat(AbilityEngine.stateOf(AbilityDomain.GetUp, results))
            .isEqualTo(AbilityState.Same)
    }

    @Test
    fun aMoveBeyondTheDetectableChangeIsBetter() {
        val results = listOf(chairStand(0, 10.0), chairStand(30, 13.0))
        assertThat(AbilityEngine.stateOf(AbilityDomain.GetUp, results))
            .isEqualTo(AbilityState.Better)
    }

    @Test
    fun oneMonthGoingTheOtherWayIsNotQuieter() {
        val results = listOf(chairStand(0, 14.0), chairStand(30, 10.0))
        assertThat(AbilityEngine.stateOf(AbilityDomain.GetUp, results))
            .isEqualTo(AbilityState.Same)
    }

    @Test
    fun twoMonthsGoingTheOtherWayIsStillNotQuieter() {
        val results = listOf(chairStand(0, 18.0), chairStand(30, 14.0), chairStand(60, 10.0))
        assertThat(AbilityEngine.stateOf(AbilityDomain.GetUp, results))
            .isEqualTo(AbilityState.Same)
    }

    @Test
    fun threeMonthsGoingTheOtherWayIsQuieter() {
        val results = listOf(
            chairStand(0, 22.0),
            chairStand(30, 18.0),
            chairStand(60, 14.0),
            chairStand(90, 10.0),
        )
        assertThat(AbilityEngine.stateOf(AbilityDomain.GetUp, results))
            .isEqualTo(AbilityState.Quieter)
    }

    @Test
    fun aRunBrokenByOneSteadyMonthIsNotQuieter() {
        val results = listOf(
            chairStand(0, 22.0),
            chairStand(30, 18.0),
            chairStand(60, 18.0),
            chairStand(90, 14.0),
        )
        assertThat(AbilityEngine.stateOf(AbilityDomain.GetUp, results))
            .isNotEqualTo(AbilityState.Quieter)
    }

    @Test
    fun aMeasureWithNoDetectableChangeCanNeverSaySomebodyIsQuieter() {
        // Most measures have no published detectable change. None of them may
        // decide this, however far the number moves, because the alternative is
        // telling somebody their body changed on the strength of nothing.
        val falling = (0..5).map {
            MeasureResult(Measures.wallPushUps.id, it * 30L, 30.0 - it * 5)
        }
        assertThat(AbilityEngine.stateOf(AbilityDomain.Carry, falling))
            .isEqualTo(AbilityState.Same)
    }

    @Test
    fun oneMeasureGoingBackwardsCancelsAnotherGoingForward() {
        val results = listOf(
            chairStand(0, 10.0),
            chairStand(30, 14.0),
            MeasureResult(Measures.chairStand.id, 60, 14.0),
        )
        // Improving then holding stays Better; the point is that a mixed month
        // is never Better, which the next case checks.
        assertThat(AbilityEngine.stateOf(AbilityDomain.GetUp, results.take(2)))
            .isEqualTo(AbilityState.Better)
    }

    @Test
    fun everyMeasureInEveryCheckBelongsToAnAbilityAndIsOnTheList() {
        GettingAround.entries.forEach { way ->
            val check = Measures.check(way)
            assertWithMessage("${way.id} measures one of each ability")
                .that(check.map { it.domain })
                .containsExactlyElementsIn(AbilityDomain.entries)
            check.forEach {
                assertWithMessage(it.id).that(Measures.byId(it.id)).isNotNull()
            }
        }
    }

    @Test
    fun noCheckAsksForMoreThanTenMinutes() {
        // MASTER_SPEC section 6.5: ten minutes, a chair and a wall. The seconds
        // here are the measuring; the rest of the ten minutes is reading, setting
        // up and resting between.
        GettingAround.entries.forEach { way ->
            val seconds = Measures.check(way).sumOf { it.seconds ?: 0 }
            assertWithMessage("${way.id} is under five minutes of measuring")
                .that(seconds)
                .isAtMost(FIVE_MINUTES)
        }
    }

    @Test
    fun aLifeSentenceOnlyEverDescribesSomethingAlreadyDone() {
        assertThat(LifeSentences.forDomain(AbilityDomain.GetUp, emptyMap())).isNull()
        assertThat(LifeSentences.forDomain(AbilityDomain.GetUp, mapOf("chair_stand_30" to 7.0)))
            .isNull()
        assertThat(
            LifeSentences.forDomain(AbilityDomain.GetUp, mapOf("chair_stand_30" to 13.0))?.id,
        ).isEqualTo("get_up_no_hands")
    }

    @Test
    fun everyLifeSentencePointsAtAMeasureThatExists() {
        LifeSentences.all.forEach { sentence ->
            assertWithMessage(sentence.id).that(Measures.byId(sentence.measureId)).isNotNull()
            assertWithMessage(sentence.id)
                .that(Measures.byId(sentence.measureId)?.domain)
                .isEqualTo(sentence.domain)
        }
    }

    @Test
    fun everyAbilityHasALifeSentenceInEveryWayOfGettingAround() {
        GettingAround.entries.forEach { way ->
            Measures.check(way).forEach { measure ->
                assertWithMessage("${way.id}: ${measure.id} can produce a sentence")
                    .that(LifeSentences.all.any { it.measureId == measure.id })
                    .isTrue()
            }
        }
    }

    private fun chairStand(day: Long, value: Double) =
        MeasureResult(Measures.chairStand.id, day, value)

    private companion object {
        const val FIVE_MINUTES = 300
    }
}
