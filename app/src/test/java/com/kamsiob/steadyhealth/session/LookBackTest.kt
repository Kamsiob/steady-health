package com.kamsiob.steadyhealth.session

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The look back card. ADDENDUM-03 Part 9.
 *
 * The rules worth holding are the ones about silence: nothing until there is a real
 * then to look back at, and never a card that says somebody got worse.
 */
class LookBackTest {

    @Test
    fun saysNothingUntilThereIsSomethingToLookBackAt() {
        val recent = listOf(done("a", TODAY - 3, 10), done("a", TODAY, 12))
        assertThat(LookBacks.of(recent, TODAY)).isNull()
    }

    @Test
    fun itIsTheirOwnFirstAgainstTheirOwnLast() {
        val history = listOf(
            done("heel_raises", TODAY - 60, 8),
            done("heel_raises", TODAY - 30, 11),
            done("heel_raises", TODAY, 15),
        )
        val back = LookBacks.of(history, TODAY)
        assertThat(back?.then).isEqualTo(8)
        assertThat(back?.now).isEqualTo(15)
    }

    @Test
    fun aMovementThatWentDownIsNeverTheCard() {
        val history = listOf(done("a", TODAY - 60, 15), done("a", TODAY, 8))
        assertWithMessage("the app told somebody they got worse")
            .that(LookBacks.of(history, TODAY)).isNull()
    }

    @Test
    fun itChangesFromOneWeekToTheNext() {
        val history = listOf("a", "b", "c", "d").flatMap {
            listOf(done(it, TODAY - 90, 5), done(it, TODAY, 9))
        }
        val weeks = (0 until 4).map { LookBacks.of(history, TODAY + it * Week.DAYS)?.movementId }
        assertThat(weeks.toSet().size).isGreaterThan(1)
    }

    private fun done(id: String, day: Long, result: Int) =
        Done(movementId = id, epochDay = day, result = result, target = result)

    private companion object {
        const val TODAY = 20_000L
    }
}
