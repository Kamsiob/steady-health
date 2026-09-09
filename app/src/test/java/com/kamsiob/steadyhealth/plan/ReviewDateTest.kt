package com.kamsiob.steadyhealth.plan

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The one prompt before the next appointment. ADDENDUM-03 Part 6.
 *
 * The rules worth holding are the ones somebody would notice were broken while they
 * were getting ready to see their physio. It arrives before the appointment and never
 * on the day or after it. It arrives once for a date and never again for that date,
 * however many times a day the worker wakes up. And it says nothing about anything but
 * the appointment being close.
 */
class ReviewDateTest {

    @Test
    fun threeDaysOutIsTooEarly() {
        assertThat(ReviewDate.due(TODAY + 3, TODAY)).isEqualTo(ReviewPrompt.Quiet)
    }

    @Test
    fun twoDaysOutIsTheDayItGoesOut() {
        assertThat(ReviewDate.due(TODAY + 2, TODAY)).isEqualTo(ReviewPrompt.Send(TODAY + 2))
    }

    @Test
    fun oneDayOutIsTheCatchUpWhenNothingWentOutTheDayBefore() {
        assertWithMessage("a day the phone was off is not the whole feature lost")
            .that(ReviewDate.due(TODAY + 1, TODAY))
            .isEqualTo(ReviewPrompt.Send(TODAY + 1))
    }

    @Test
    fun oneDayOutSaysNothingWhenItAlreadyWentOut() {
        val sent = setOf(TODAY + 1)
        assertThat(ReviewDate.due(TODAY + 1, TODAY, sent)).isEqualTo(ReviewPrompt.Quiet)
    }

    @Test
    fun theDayOfTheAppointmentIsNotTwoDaysBefore() {
        assertThat(ReviewDate.due(TODAY, TODAY)).isEqualTo(ReviewPrompt.Quiet)
    }

    @Test
    fun yesterdaysAppointmentIsNotAPrompt() {
        assertThat(ReviewDate.due(TODAY - 1, TODAY)).isEqualTo(ReviewPrompt.Quiet)
    }

    @Test
    fun anAppointmentLongPastIsNeverMentionedAgain() {
        assertThat(ReviewDate.due(TODAY - A_MONTH, TODAY)).isEqualTo(ReviewPrompt.Quiet)
    }

    @Test
    fun noDateSetIsNothingToSay() {
        assertThat(ReviewDate.due(null, TODAY)).isEqualTo(ReviewPrompt.Quiet)
    }

    @Test
    fun onePromptPerAppointmentAndNoMore() {
        val appointment = TODAY + A_MONTH
        val sent = mutableSetOf<Long>()
        var sends = 0

        for (day in TODAY..(appointment + A_MONTH)) {
            val answer = ReviewDate.due(appointment, day, sent)
            if (answer is ReviewPrompt.Send) {
                sends++
                sent += answer.onDay
            }
        }

        assertWithMessage("an appointment is worth exactly one prompt")
            .that(sends)
            .isEqualTo(1)
    }

    @Test
    fun itLandsTwoDaysBeforeWhenTheWorkerRunsEveryDay() {
        val appointment = TODAY + A_MONTH
        val sent = mutableSetOf<Long>()
        val days = mutableListOf<Long>()

        for (day in TODAY..(appointment + A_MONTH)) {
            val answer = ReviewDate.due(appointment, day, sent)
            if (answer is ReviewPrompt.Send) {
                days += day
                sent += answer.onDay
            }
        }

        assertThat(days).containsExactly(appointment - ReviewDate.DAYS_BEFORE)
    }

    @Test
    fun nothingEverGoesOutOnTheDayOrAfterIt() {
        val appointment = TODAY + A_MONTH

        for (day in TODAY..(appointment + A_MONTH)) {
            val answer = ReviewDate.due(appointment, day)
            if (day >= appointment) {
                assertWithMessage("day $day is not before the appointment")
                    .that(answer)
                    .isEqualTo(ReviewPrompt.Quiet)
            }
        }
    }

    @Test
    fun movingTheAppointmentEarnsTheNewDateItsOwnPrompt() {
        val first = TODAY + 2
        val moved = TODAY + 9
        val sent = setOf(first)

        assertThat(ReviewDate.due(first, TODAY, sent)).isEqualTo(ReviewPrompt.Quiet)
        assertWithMessage("a date that moved is a different date")
            .that(ReviewDate.due(moved, moved - ReviewDate.DAYS_BEFORE, sent))
            .isEqualTo(ReviewPrompt.Send(moved))
    }

    @Test
    fun theSoonestDueAppointmentWinsWhenTwoPlansHaveDates() {
        val physio = TODAY + 2
        val handTherapist = TODAY + 3

        assertThat(ReviewDate.due(listOf(handTherapist, physio), TODAY))
            .isEqualTo(ReviewPrompt.Send(physio))
    }

    @Test
    fun anAppointmentTodayDoesNotHideOneTwoDaysOut() {
        val physio = TODAY
        val occupationalTherapist = TODAY + 2

        assertWithMessage("today's appointment is past speaking about, the other is not")
            .that(ReviewDate.due(listOf(physio, occupationalTherapist), TODAY))
            .isEqualTo(ReviewPrompt.Send(occupationalTherapist))
    }

    @Test
    fun twoAppointmentsADayApartEachGetTheirOwn() {
        val nearer = TODAY + 2
        val later = TODAY + 3
        val sent = mutableSetOf<Long>()

        val first = ReviewDate.due(listOf(nearer, later), TODAY, sent)
        sent += (first as ReviewPrompt.Send).onDay
        val second = ReviewDate.due(listOf(nearer, later), TODAY + 1, sent)

        assertThat(first).isEqualTo(ReviewPrompt.Send(nearer))
        assertWithMessage("the second one takes its catch up the following day")
            .that(second)
            .isEqualTo(ReviewPrompt.Send(later))
    }

    @Test
    fun noPlanWithADateIsNothingToSay() {
        assertThat(ReviewDate.due(emptyList(), TODAY)).isEqualTo(ReviewPrompt.Quiet)
    }

    @Test
    fun theNextAppointmentIsTheSoonestOneNotBehindUs() {
        val dates = listOf(TODAY - 1, TODAY + 20, TODAY + 4)

        assertThat(ReviewDate.soonest(dates, TODAY)).isEqualTo(TODAY + 4)
    }

    @Test
    fun anAppointmentTodayIsStillTheNextOne() {
        assertThat(ReviewDate.soonest(listOf(TODAY, TODAY + 4), TODAY)).isEqualTo(TODAY)
    }

    @Test
    fun everyAppointmentBehindUsLeavesNoNextOne() {
        assertThat(ReviewDate.soonest(listOf(TODAY - 1, TODAY - A_MONTH), TODAY)).isNull()
    }

    private companion object {
        const val TODAY = 20_000L
        const val A_MONTH = 30L
    }
}
