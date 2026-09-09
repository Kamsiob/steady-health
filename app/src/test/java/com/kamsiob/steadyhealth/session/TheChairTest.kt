package com.kamsiob.steadyhealth.session

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The chair, and whether it is still the same one. ADDENDUM-03 Part 15.
 *
 * The rules worth holding are the quiet ones: the first answer is not a change, the
 * same answer twice is not a change, the note is said once and then never again, and
 * a movement that merely leans on a chair is told nothing about its height.
 */
class TheChairTest {

    @Test
    fun nobodyIsToldAnythingBeforeAnybodyHasBeenAsked() {
        val chair = TheChair()
        assertThat(chair.known).isFalse()
        assertThat(chair.lineFor(movement("sit_to_stand"))).isEqualTo(ChairLine.AskOnce)
        assertThat(chair.noteToSay(DAY_ZERO)).isNull()
        assertThat(chair.differentChair(DAY_ZERO)).isNull()
    }

    @Test
    fun theHeightOnRecordGoesInEveryChairStandSetupLine() {
        val chair = TheChair().changedTo(ChairHeight.Level, TODAY)
        val stands = Movements.all.filter { TheChair.standsFromTheChair(it) }

        assertThat(stands).isNotEmpty()
        stands.forEach {
            assertThat(chair.lineFor(it)).isEqualTo(ChairLine.SameAgain(ChairHeight.Level))
        }
    }

    @Test
    fun movementsThatOnlyLeanOnAChairAreToldNothingAboutItsHeight() {
        val chair = TheChair().changedTo(ChairHeight.Level, TODAY)
        val leaning = listOf("warm_seated_march", "seated_forward_lean", "half_kneel_to_stand")

        leaning.forEach { assertThat(chair.lineFor(movement(it))).isNull() }
    }

    @Test
    fun theFirstAnswerIsNotAChangeAndIsNeverWorthANote() {
        val chair = TheChair().changedTo(ChairHeight.Level, TODAY)

        assertThat(chair.known).isTrue()
        assertThat(chair.change).isNull()
        assertThat(chair.noteToSay(DAY_ZERO)).isNull()
    }

    @Test
    fun answeringTheSameHeightAgainIsNotAChange() {
        val chair = TheChair()
            .changedTo(ChairHeight.Level, TODAY)
            .changedTo(ChairHeight.Level, TODAY + A_FORTNIGHT)

        assertThat(chair.change).isNull()
        assertThat(chair.noteToSay(DAY_ZERO)).isNull()
    }

    @Test
    fun aChangedChairIsAPlainFactAboutBothChairs() {
        val chair = TheChair()
            .changedTo(ChairHeight.Level, TODAY)
            .changedTo(ChairHeight.Low, TODAY + A_FORTNIGHT)

        val note = chair.noteToSay(TODAY)

        assertThat(note).isEqualTo(
            DifferentChair(ChairHeight.Level, ChairHeight.Low, TODAY + A_FORTNIGHT),
        )
        assertThat(note?.lower).isTrue()
    }

    @Test
    fun aHigherChairIsTheSameFactTheOtherWayRound() {
        val chair = TheChair()
            .changedTo(ChairHeight.Low, TODAY)
            .changedTo(ChairHeight.High, TODAY + A_FORTNIGHT)

        assertThat(chair.noteToSay(TODAY)?.lower).isFalse()
    }

    @Test
    fun theNoteIsSaidOnceAndThenNotAgain() {
        val chair = TheChair()
            .changedTo(ChairHeight.Level, TODAY)
            .changedTo(ChairHeight.Low, TODAY + A_FORTNIGHT)

        assertThat(chair.noteToSay(TODAY)).isNotNull()
        assertThat(chair.noted().noteToSay(TODAY)).isNull()
    }

    @Test
    fun theFactOutlivesTheNoteThatWasSaidAboutIt() {
        val chair = TheChair()
            .changedTo(ChairHeight.Level, TODAY)
            .changedTo(ChairHeight.Low, TODAY + A_FORTNIGHT)
            .noted()

        assertThat(chair.differentChair(TODAY)).isNotNull()
    }

    @Test
    fun numbersRecordedSinceTheChangeAreOnTheChairInTheRoomNow() {
        val changedOn = TODAY + A_FORTNIGHT
        val chair = TheChair()
            .changedTo(ChairHeight.Level, TODAY)
            .changedTo(ChairHeight.Low, changedOn)

        assertThat(chair.noteToSay(changedOn)).isNull()
        assertThat(chair.noteToSay(changedOn - 1)).isNotNull()
    }

    @Test
    fun twoChangesBeforeAnythingIsSaidAreStillOneNoteFromTheOldestChair() {
        val chair = TheChair()
            .changedTo(ChairHeight.Level, TODAY)
            .changedTo(ChairHeight.High, TODAY + A_FORTNIGHT)
            .changedTo(ChairHeight.Low, TODAY + A_MONTH)

        assertThat(chair.noteToSay(TODAY)).isEqualTo(
            DifferentChair(ChairHeight.Level, ChairHeight.Low, TODAY + A_FORTNIGHT),
        )
    }

    @Test
    fun aChangeAfterTheNoteWasSaidEarnsItsOwnNote() {
        val chair = TheChair()
            .changedTo(ChairHeight.Level, TODAY)
            .changedTo(ChairHeight.High, TODAY + A_FORTNIGHT)
            .noted()
            .changedTo(ChairHeight.Low, TODAY + A_MONTH)

        assertThat(chair.noteToSay(TODAY)).isEqualTo(
            DifferentChair(ChairHeight.High, ChairHeight.Low, TODAY + A_MONTH),
        )
    }

    @Test
    fun onlyMovementsDoneFromTheChairMakeTheChangeWorthMentioning() {
        val chair = TheChair()
            .changedTo(ChairHeight.Level, TODAY)
            .changedTo(ChairHeight.Low, TODAY + A_FORTNIGHT)
        val elsewhere = listOf(
            done("heel_raises", TODAY),
            done("heel_raises", TODAY + A_MONTH),
        )

        assertThat(chair.noteToSay(elsewhere)).isNull()
        assertThat(chair.differentChair(elsewhere)).isNull()
    }

    @Test
    fun historyReachingBackPastTheChangeIsWorthMentioningOnce() {
        val chair = TheChair()
            .changedTo(ChairHeight.Level, TODAY)
            .changedTo(ChairHeight.Low, TODAY + A_FORTNIGHT)
        val history = listOf(
            done("heel_raises", TODAY + A_MONTH),
            done("sit_to_stand", TODAY),
            done("sit_to_stand", TODAY + A_MONTH),
        )

        assertThat(chair.noteToSay(history)).isNotNull()
        assertThat(chair.noted().noteToSay(history)).isNull()
        assertThat(chair.noted().differentChair(history)).isNotNull()
    }

    @Test
    fun chairStandsDoneOnlySinceTheChangeAreComparableWithEachOther() {
        val chair = TheChair()
            .changedTo(ChairHeight.Level, TODAY)
            .changedTo(ChairHeight.Low, TODAY + A_FORTNIGHT)
        val history = listOf(
            done("heel_raises", TODAY),
            done("sit_to_stand", TODAY + A_MONTH),
        )

        assertThat(chair.noteToSay(history)).isNull()
    }

    @Test
    fun emptyHistoryHasNothingToCompareAndSoNothingToSay() {
        val chair = TheChair()
            .changedTo(ChairHeight.Level, TODAY)
            .changedTo(ChairHeight.Low, TODAY + A_FORTNIGHT)

        assertThat(chair.noteToSay(emptyList())).isNull()
    }

    @Test
    fun theHeightsAreOrderedLowToHighSoAChangeHasADirection() {
        assertThat(ChairHeight.Low).isLessThan(ChairHeight.Level)
        assertThat(ChairHeight.Level).isLessThan(ChairHeight.High)
        assertThat(ChairHeight.entries.map { it.id })
            .containsExactly("low", "level", "high")
            .inOrder()
    }

    private fun movement(id: String) = requireNotNull(Movements.byId(id)) { "no movement $id" }

    private fun done(movementId: String, epochDay: Long) =
        Done(movementId = movementId, epochDay = epochDay, result = A_RESULT, target = A_RESULT)

    private companion object {
        const val DAY_ZERO = 0L
        const val TODAY = 20_000L
        const val A_FORTNIGHT = 14L
        const val A_MONTH = 30L
        const val A_RESULT = 8
    }
}
