package com.kamsiob.steadyhealth.session

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import org.junit.Test

/**
 * Planning a session. ADDENDUM-03 Part 1.
 *
 * The rules worth holding are the ones about not asking too much: a hard session
 * makes the next one lighter, a ceiling stops reps climbing forever, and an area
 * somebody said hurts disappears rather than being worked around.
 */
class SessionEngineTest {

    @Test
    fun anOrdinarySessionIsBetweenFourAndEightMinutes() {
        val plan = SessionEngine.plan(onFeet())
        assertThat(plan.minutes).isAtLeast(FOUR)
        assertThat(plan.minutes).isAtMost(EIGHT)
    }

    @Test
    fun aSessionOverThreeMinutesOpensWithAWarmUp() {
        val plan = SessionEngine.plan(onFeet())
        assertThat(plan.minutes).isGreaterThan(SessionEngine.WARM_UP_OVER_MINUTES)
        assertThat(plan.hasWarmUp).isTrue()
        assertThat(plan.steps.first().movement.piece).isEqualTo(Piece.WarmUp)
    }

    @Test
    fun aSessionOverFiveMinutesEndsWithACoolDown() {
        val plan = SessionEngine.plan(onFeet())
        if (plan.minutes > SessionEngine.COOL_DOWN_OVER_MINUTES) {
            assertThat(plan.hasCoolDown).isTrue()
            assertThat(plan.steps.last().movement.piece).isEqualTo(Piece.CoolDown)
        }
    }

    @Test
    fun theSameHistoryAlwaysPlansTheSameSession() {
        // Not random. Two people with the same history get the same session, and so
        // does the same person twice, which is what makes a bug reproducible.
        val inputs = onFeet()
        assertThat(SessionEngine.plan(inputs).steps.map { it.movement.id })
            .isEqualTo(SessionEngine.plan(inputs).steps.map { it.movement.id })
    }

    @Test
    fun anOrdinarySessionSpreadsAcrossTheAbilities() {
        val fed = SessionEngine.plan(onFeet()).main.map { it.movement.domain }
        assertThat(fed).containsNoDuplicates()
        assertThat(fed.size).isAtLeast(2)
    }

    @Test
    fun aHardSessionMakesTheNextOneLighter() {
        val history = listOf(Done("sit_to_stand", 10, result = 10, target = 10))
        val steady = SessionEngine.target(chairStands(), onFeet().copy(history = history))
        val lighter = SessionEngine.target(
            chairStands(),
            onFeet().copy(history = history, lastFelt = Felt.Hard),
        )
        assertThat(lighter).isLessThan(steady)
    }

    @Test
    fun twoEasySessionsAddOne() {
        val history = listOf(Done("sit_to_stand", 10, result = 10, target = 10))
        val more = SessionEngine.target(
            chairStands(),
            onFeet().copy(history = history, lastFelt = Felt.Easy, feltBefore = Felt.Easy),
        )
        assertThat(more).isEqualTo(11)
    }

    @Test
    fun oneEasySessionDoesNotAddOne() {
        val history = listOf(Done("sit_to_stand", 10, result = 10, target = 10))
        val same = SessionEngine.target(
            chairStands(),
            onFeet().copy(history = history, lastFelt = Felt.Easy, feltBefore = Felt.Hard),
        )
        assertThat(same).isAtMost(10)
    }

    @Test
    fun daysAwayMakeTheNextOneShorter() {
        val history = listOf(Done("sit_to_stand", 10, result = 10, target = 10))
        val shorter = SessionEngine.target(
            chairStands(),
            onFeet().copy(history = history, today = 20, lastSessionDay = 10),
        )
        assertThat(shorter).isLessThan(10)
    }

    @Test
    fun aTargetNeverExceedsTheCeiling() {
        val movement = chairStands()
        val ceiling = checkNotNull(movement.ceiling)
        val history = (1..40).map { Done(movement.id, it.toLong(), result = 99, target = 99) }
        val target = SessionEngine.target(
            movement,
            onFeet().copy(history = history, lastFelt = Felt.Easy, feltBefore = Felt.Easy),
        )
        assertThat(target).isAtMost(ceiling)
    }

    @Test
    fun atTheCeilingTheMovementChangesRatherThanTheNumber() {
        val movement = chairStands()
        val ceiling = checkNotNull(movement.ceiling)
        val history = listOf(Done(movement.id, 10, result = ceiling, target = ceiling))
        val next = SessionEngine.atTheRightLevel(movement, onFeet().copy(history = history))
        assertThat(next.id).isNotEqualTo(movement.id)
        assertThat(next.id).isEqualTo(movement.harder)
    }

    @Test
    fun belowTheCeilingTheMovementStaysTheSame() {
        val movement = chairStands()
        val history = listOf(Done(movement.id, 10, result = 5, target = 8))
        assertThat(SessionEngine.atTheRightLevel(movement, onFeet().copy(history = history)).id)
            .isEqualTo(movement.id)
    }

    @Test
    fun aHardSessionMakesTheNextOneEasy() {
        val plan = SessionEngine.plan(onFeet().copy(lastFelt = Felt.Hard))
        assertThat(plan.easy).isTrue()
        assertThat(plan.minutes).isAtMost(FOUR)
    }

    @Test
    fun twoStrengthSessionsInARowMakeTheNextOneEasy() {
        val plan = SessionEngine.plan(onFeet().copy(strengthRunLength = 2))
        assertThat(plan.easy).isTrue()
        assertThat(plan.adaptation.kind).isEqualTo(Adaptation.Kind.EasyDay)
    }

    @Test
    fun anEasyDayIsNeverEmpty() {
        // Never a blank screen, and never the words "rest day", which read as
        // permission to skip two.
        val plan = SessionEngine.plan(onFeet().copy(lastFelt = Felt.Hard))
        assertThat(plan.steps).isNotEmpty()
    }

    @Test
    fun anAreaThatHurtsDisappearsRatherThanBeingWorkedAround() {
        val plan = SessionEngine.plan(onFeet().copy(sore = setOf(Area.Shoulder)))
        assertThat(plan.steps.map { it.movement.area }).doesNotContain(Area.Shoulder)
    }

    @Test
    fun everythingLeftOutIsLeftOut() {
        Exclusion.entries.forEach { exclusion ->
            val plan = SessionEngine.plan(onFeet().copy(exclusions = setOf(exclusion)))
            plan.steps.forEach { step ->
                assertWithMessage("${step.movement.id} survived $exclusion")
                    .that(step.movement.excludedBy)
                    .doesNotContain(exclusion)
            }
        }
    }

    @Test
    fun thereIsAlwaysSomethingToDoWhateverIsLeftOut() {
        val plan = SessionEngine.plan(onFeet().copy(exclusions = Exclusion.entries.toSet()))
        assertThat(plan.steps).isNotEmpty()
    }

    @Test
    fun nothingIsOfferedThatNeedsEquipmentTheRoomDoesNotHave() {
        val plan = SessionEngine.plan(onFeet().copy(kit = setOf(Kit.None)))
        plan.steps.forEach { step ->
            assertWithMessage(step.movement.id)
                .that(step.movement.kit)
                .containsExactly(Kit.None)
        }
    }

    @Test
    fun theSmallVersionIsOneMovementAndCounts() {
        val plan = SessionEngine.plan(onFeet().copy(wantSmall = true))
        assertThat(plan.steps).hasSize(1)
        assertThat(plan.small).isTrue()
    }

    @Test
    fun afterBeingUnwellTheSessionIsShorterAndSaysSo() {
        val plan = SessionEngine.plan(onFeet().copy(rampingAfterUnwell = true))
        assertThat(plan.adaptation.kind).isEqualTo(Adaptation.Kind.AfterUnwell)
        assertThat(plan.main.size).isLessThan(SessionEngine.MAIN_MOVEMENTS)
    }

    @Test
    fun everyWayOfGettingAroundGetsASession() {
        GettingAround.entries.forEach { way ->
            val plan = SessionEngine.plan(onFeet().copy(way = way))
            assertWithMessage("${way.id} has a session").that(plan.steps).isNotEmpty()
        }
    }

    @Test
    fun everyMovementInTheLibraryIsReachableAndWellFormed() {
        Movements.all.forEach { movement ->
            assertWithMessage("${movement.id} has a setup line").that(movement.setup).isNotEmpty()
            assertWithMessage("${movement.id} has a stop rule").that(movement.stopRule).isNotEmpty()
            assertWithMessage("${movement.id} starts somewhere").that(movement.startTarget)
                .isGreaterThan(0)
            movement.easier?.let {
                assertWithMessage("${movement.id} easier is real").that(Movements.byId(it)).isNotNull()
            }
            movement.harder?.let {
                assertWithMessage("${movement.id} harder is real").that(Movements.byId(it)).isNotNull()
            }
        }
        assertThat(Movements.all.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun everyStopRuleIsAPermissionRatherThanAWarning() {
        // "Stop whenever" and never "do not overdo it". A stop rule that sounds like a
        // warning is one people ignore.
        val warnings = listOf("don't", "do not", "never let", "avoid ", "careful", "warning")
        Movements.all.forEach { movement ->
            warnings.forEach { word ->
                assertWithMessage("${movement.id}: ${movement.stopRule}")
                    .that(movement.stopRule.lowercase())
                    .doesNotContain(word)
            }
        }
    }

    @Test
    fun stoppingOneSetEarlyDoesNotDragTheNextTargetDownToIt() {
        // Somebody who stopped a thirty second warm up after five seconds because the
        // doorbell went should not be asked for five next time, and then four.
        val movement = chairStands()
        val history = listOf(Done(movement.id, 10, result = 9, target = 10))
        assertThat(SessionEngine.target(movement, onFeet().copy(history = history)))
            .isEqualTo(10)
    }

    @Test
    fun fallingAWayShortBringsTheAskDownToMeetIt() {
        // A target nobody can reach is not a target, it is a reminder of what they
        // cannot do.
        val movement = chairStands()
        val history = listOf(Done(movement.id, 10, result = 2, target = 10))
        assertThat(SessionEngine.target(movement, onFeet().copy(history = history)))
            .isEqualTo(2)
    }

    @Test
    fun theTargetNeverFallsBelowOne() {
        val movement = chairStands()
        val history = listOf(Done(movement.id, 10, result = 0, target = 10))
        assertThat(SessionEngine.target(movement, onFeet().copy(history = history)))
            .isAtLeast(1)
    }

    private fun onFeet() = SessionInputs(
        way = GettingAround.OnFeet,
        kit = setOf(Kit.None, Kit.Chair, Kit.Wall),
        today = 20_000,
    )

    private fun chairStands() = checkNotNull(Movements.byId("sit_to_stand"))

    private companion object {
        const val FOUR = 4
        const val EIGHT = 8
    }
}
