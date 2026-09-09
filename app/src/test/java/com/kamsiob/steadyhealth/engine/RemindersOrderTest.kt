package com.kamsiob.steadyhealth.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The ceiling, and which one of them wins when several are due at once.
 *
 * The order is a decision rather than a detail. Every capped kind except one can
 * simply go out tomorrow, and the app sends at most two things in a week, so the
 * order is what decides which of them somebody never hears.
 */
class RemindersOrderTest {

    @Test
    fun theAppointmentGoesBeforeEverythingElseThatIsCapped() {
        assertThat(Reminders.CAPPED.first()).isEqualTo(ReminderKind.Review)
    }

    @Test
    fun theAppointmentWinsAgainstEveryOtherKindOnItsOwn() {
        Reminders.CAPPED.drop(1).forEach { other ->
            assertThat(Reminders.pick(setOf(other, ReminderKind.Review)))
                .isEqualTo(ReminderKind.Review)
        }
    }

    @Test
    fun theDailyPromptIsNotCappedAndNeverPicked() {
        assertThat(Reminders.CAPPED).doesNotContain(ReminderKind.Daily)
        assertThat(Reminders.pick(setOf(ReminderKind.Daily))).isNull()
    }

    @Test
    fun everyCappedKindIsOneOfTheKindsThereAre() {
        assertThat(ReminderKind.entries).containsAtLeastElementsIn(Reminders.CAPPED)
    }

    @Test
    fun theCeilingCountsTheAppointmentLikeAnythingElse() {
        val sunday = A_DAY * 2
        val alreadySent = listOf(NOW - sunday, NOW - A_DAY)

        assertThat(Reminders.maySend(alreadySent, NOW)).isFalse()
    }

    @Test
    fun aWeekOldReminderIsOutOfTheWindow() {
        val alreadySent = listOf(NOW - A_DAY * 8, NOW - A_DAY * 9)

        assertThat(Reminders.maySend(alreadySent, NOW)).isTrue()
        assertThat(Reminders.leftThisWeek(alreadySent, NOW)).isEqualTo(Reminders.MOST_IN_A_WEEK)
    }

    private companion object {
        const val A_DAY = 86_400_000L
        const val NOW = 1_700_000_000_000L
    }
}
