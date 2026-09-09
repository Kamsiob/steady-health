package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.session.Done
import com.kamsiob.steadyhealth.session.Movements
import org.junit.Test

/**
 * What the app already watched somebody do, and the rules about offering it back.
 *
 * The tests that matter here are the ones that stop this becoming a measure the app
 * took on its own: nothing is written, a zero is never offered, and a number from
 * outside the window is not offered either.
 */
class PassiveTest {

    @Test
    fun everyWatchedMovementIsRealAndEveryWatchedMeasureIsReal() {
        // A mapping to an id that does not exist is a measure that silently never
        // gets offered, which looks exactly like a measure nobody does.
        Passive.watching.forEach { (measureId, movementId) ->
            assertWithMessage(measureId).that(Measures.byId(measureId)).isNotNull()
            assertWithMessage(movementId).that(Movements.byId(movementId)).isNotNull()
        }
    }

    @Test
    fun theWatchedMovementBelongsToTheSameAbilityAsItsMeasure() {
        Passive.watching.forEach { (measureId, movementId) ->
            val measure = Measures.byId(measureId)!!
            val movement = Movements.byId(movementId)!!
            assertWithMessage("$measureId watches $movementId")
                .that(movement.domain)
                .isEqualTo(measure.domain)
        }
    }

    @Test
    fun theBestInTheWindowIsWhatComesBack() {
        val history = listOf(
            done("sit_to_stand", day = TODAY - 20, result = 9),
            done("sit_to_stand", day = TODAY - 3, result = 14),
            done("sit_to_stand", day = TODAY - 1, result = 11),
        )

        val seen = Passive.seen(Measures.chairStand.id, history, TODAY)

        assertThat(seen?.value).isEqualTo(14.0)
        assertThat(seen?.onDay).isEqualTo(TODAY - 3)
    }

    @Test
    fun anythingOlderThanTheWindowIsNotOffered() {
        val history = listOf(done("sit_to_stand", day = TODAY - 40, result = 20))

        assertThat(Passive.seen(Measures.chairStand.id, history, TODAY)).isNull()
    }

    @Test
    fun aZeroIsNeverOffered() {
        // Zero is what an interrupted session leaves behind, not a thing to confirm.
        val history = listOf(done("sit_to_stand", day = TODAY - 1, result = 0))

        assertThat(Passive.seen(Measures.chairStand.id, history, TODAY)).isNull()
    }

    @Test
    fun aMeasureNothingWatchesIsNeverOffered() {
        val history = listOf(done("sit_to_stand", day = TODAY - 1, result = 12))

        assertThat(Passive.seen(Measures.twoMinuteStep.id, history, TODAY)).isNull()
    }

    @Test
    fun aNumberTheyTypedIsOfferedAndSaysSo() {
        val history = listOf(done("wall_push_up", day = TODAY - 2, result = 12, typed = true))

        val seen = Passive.seen(Measures.wallPushUps.id, history, TODAY)

        assertThat(seen?.counted).isFalse()
    }

    @Test
    fun aNumberThePhoneCountedSaysThatToo() {
        val history = listOf(done("wall_push_up", day = TODAY - 2, result = 12))

        assertThat(Passive.seen(Measures.wallPushUps.id, history, TODAY)?.counted).isTrue()
    }

    @Test
    fun anotherMovementsResultIsNotThisMeasuresResult() {
        // sit_to_stand_high is an easier chair stand and its number means something
        // else. Reading it as the chair stand would flatter somebody with a number
        // they did not earn.
        val history = listOf(done("sit_to_stand_high", day = TODAY - 1, result = 20))

        assertThat(Passive.seen(Measures.chairStand.id, history, TODAY)).isNull()
    }

    @Test
    fun awholeChecksWorthComesBackKeyedByMeasure() {
        val history = listOf(
            done("sit_to_stand", day = TODAY - 2, result = 12),
            done("one_leg", day = TODAY - 4, result = 18),
        )
        val measures = listOf(Measures.chairStand, Measures.twoMinuteStep, Measures.singleLegStance)

        val seen = Passive.seen(measures, history, TODAY)

        assertThat(seen.keys)
            .containsExactly(Measures.chairStand.id, Measures.singleLegStance.id)
    }

    @Test
    fun anEmptyHistoryOffersNothingAndDoesNotThrow() {
        assertThat(Passive.seen(Measures.all, emptyList(), TODAY)).isEmpty()
    }

    private fun done(movementId: String, day: Long, result: Int, typed: Boolean = false) =
        Done(
            movementId = movementId,
            epochDay = day,
            result = result,
            target = result,
            selfReported = typed,
        )

    private companion object {
        const val TODAY = 20_000L
    }
}
