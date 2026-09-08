package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/** The ceiling, which is the whole of the reminder design. LOGIC.md section 12. */
class RemindersTest {

    @Test
    fun withNothingSentYetOneMayBeSent() {
        assertThat(Reminders.maySend(emptyList(), now = DAY * 10)).isTrue()
    }

    @Test
    fun afterTwoInTheWeekNoMoreGoOut() {
        val sent = listOf(DAY * 8, DAY * 9)
        assertThat(Reminders.maySend(sent, now = DAY * 10)).isFalse()
    }

    @Test
    fun theWindowRollsRatherThanResetting() {
        // Two sent eight days ago do not count. Two sent six days ago do. There is
        // no Monday on which somebody's allowance comes back all at once.
        assertThat(Reminders.maySend(listOf(DAY * 2, DAY * 3), now = DAY * 11)).isTrue()
        assertThat(Reminders.maySend(listOf(DAY * 5, DAY * 6), now = DAY * 11)).isFalse()
    }

    @Test
    fun turningEveryTypeOnDoesNotRaiseTheCeiling() {
        // "Regardless of switches" is the word that matters in LOGIC.md.
        val sent = listOf(DAY * 9, DAY * 10)
        ReminderKind.entries.forEach {
            assertWithMessage("with ${it.id} due as well")
                .that(Reminders.maySend(sent, now = DAY * 10))
                .isFalse()
        }
    }

    @Test
    fun theCountLeftIsNeverBelowZero() {
        val many = List(9) { DAY * 10 }
        assertThat(Reminders.leftThisWeek(many, now = DAY * 10)).isEqualTo(0)
        assertThat(Reminders.usedThisWeek(many, now = DAY * 10)).isEqualTo(9)
    }

    @Test
    fun goodNewsGoesFirstWhenTwoAreDueAtOnce() {
        assertThat(Reminders.pick(setOf(ReminderKind.Photo, ReminderKind.StepReady)))
            .isEqualTo(ReminderKind.StepReady)
        assertThat(Reminders.pick(setOf(ReminderKind.Photo, ReminderKind.Walk)))
            .isEqualTo(ReminderKind.Walk)
        assertThat(Reminders.pick(setOf(ReminderKind.Photo, ReminderKind.WeekNote)))
            .isEqualTo(ReminderKind.WeekNote)
    }

    @Test
    fun nothingDueMeansNothingPicked() {
        assertThat(Reminders.pick(emptySet())).isNull()
    }

    @Test
    fun everyCappedKindCanBePickedWhenItIsTheOnlyOneDue() {
        Reminders.CAPPED.forEach {
            assertWithMessage(it.id).that(Reminders.pick(setOf(it))).isEqualTo(it)
        }
    }

    @Test
    fun theDailyPromptIsNotOneOfTheTwoAWeek() {
        // ADDENDUM-03 Part 13: one a day, and everything else capped at two a week
        // combined. Counting the daily one against the two would mean an app that
        // says its one useful thing twice a week.
        assertThat(Reminders.CAPPED).doesNotContain(ReminderKind.Daily)
        assertThat(Reminders.pick(setOf(ReminderKind.Daily))).isNull()
    }

    private companion object {
        const val DAY = 86_400_000L
    }
}
