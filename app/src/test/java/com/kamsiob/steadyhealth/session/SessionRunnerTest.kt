package com.kamsiob.steadyhealth.session

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kamsiob.steadyhealth.domain.GettingAround
import org.junit.Test

/**
 * The live session, and the bad day.
 *
 * ADDENDUM-03 Part 21 requires that each of the three exits works from every point in
 * a session and saves what was done. That is checked here exhaustively rather than by
 * hand on the phone, because "every point" is more points than anybody will tap
 * through and it is exactly where a state machine goes wrong.
 */
class SessionRunnerTest {

    @Test
    fun aSessionRunsFromReadyToDone() {
        var runner = start()
        var guard = 0
        while (!runner.finished && guard++ < FOREVER) {
            runner = when (runner.stage) {
                Stage.Ready -> runner.ready()
                Stage.Live -> if (runner.reachedTarget) runner.endSet() else runner.rep().tick()
                else -> runner.tick()
            }
        }
        assertThat(runner.finished).isTrue()
        assertThat(runner.ending).isEqualTo(Ending.Finished)
        assertThat(runner.results).hasSize(runner.plan.steps.size)
    }

    @Test
    fun theReadyScreenStartsOnItsOwnSoTheSessionCanBeDoneFaceDown() {
        var runner = start()
        repeat(SessionRunner.FIRST_READY_SECONDS - 1) { runner = runner.tick() }
        assertWithMessage("started before the wait was up").that(runner.stage).isEqualTo(Stage.Ready)

        runner = runner.tick()
        assertThat(runner.stage).isInstanceOf(Stage.CountIn::class.java)
    }

    @Test
    fun theWaitIsShorterAfterTheFirstMovement() {
        var runner = start().ready().go().endSet().skipRest()
        assertThat(runner.stage).isEqualTo(Stage.Ready)
        assertThat(runner.readySeconds).isEqualTo(SessionRunner.READY_SECONDS)

        repeat(SessionRunner.READY_SECONDS) { runner = runner.tick() }
        assertThat(runner.stage).isInstanceOf(Stage.CountIn::class.java)
    }

    @Test
    fun aPausedReadyScreenNeverStartsOnItsOwn() {
        var runner = start().pause()
        repeat(SessionRunner.FIRST_READY_SECONDS * 2) { runner = runner.tick() }
        assertThat(runner.stage).isEqualTo(Stage.Ready)
    }

    @Test
    fun aSetThePhoneCountsEndsItselfOnceThePersonStops() {
        var runner = liveOf("sit_to_stand")
        val target = runner.step?.target ?: 0
        repeat(target) { runner = runner.rep() }
        assertThat(runner.stage).isEqualTo(Stage.Live)

        repeat(SessionRunner.STOPPED_SECONDS - 1) { runner = runner.tick() }
        assertWithMessage("ended before the person had stopped")
            .that(runner.stage).isEqualTo(Stage.Live)

        runner = runner.tick()
        assertThat(runner.stage).isNotEqualTo(Stage.Live)
        assertThat(runner.results.single().count).isEqualTo(target)
    }

    @Test
    fun moreRepetitionsKeepACountedSetGoing() {
        var runner = liveOf("sit_to_stand")
        val target = runner.step?.target ?: 0
        repeat(target) { runner = runner.rep() }

        repeat(SessionRunner.STOPPED_SECONDS - 1) { runner = runner.tick() }
        runner = runner.rep()
        repeat(SessionRunner.STOPPED_SECONDS - 1) { runner = runner.tick() }

        assertWithMessage("stopped somebody who was still going")
            .that(runner.stage).isEqualTo(Stage.Live)
        assertThat(runner.count).isEqualTo(target + 1)
    }

    @Test
    fun aSetCountedByHandWaitsForThePerson() {
        var runner = liveOf("heel_raises")
        val target = runner.step?.target ?: 0
        repeat(target) { runner = runner.rep() }
        repeat(SessionRunner.STOPPED_SECONDS * 2) { runner = runner.tick() }

        assertWithMessage("ended a set nobody asked it to end")
            .that(runner.stage).isEqualTo(Stage.Live)
    }

    @Test
    fun aSetWithNothingHappeningAtAllDoesNotWaitForever() {
        var runner = liveOf("heel_raises")
        runner = runner.rep().rep()
        repeat(SessionRunner.STALLED_SECONDS) { runner = runner.tick() }

        assertThat(runner.stage).isNotEqualTo(Stage.Live)
        assertWithMessage("lost what was already counted")
            .that(runner.results.single().count).isEqualTo(2)
    }

    @Test
    fun awholeSessionRunsToTheEndWithNobodyTouchingTheScreen() {
        // The Phase 1 gate's face-down session, as a rule rather than as a run. The
        // only input is the clock and the accelerometer; nothing here presses
        // anything, and the session still has to reach the end.
        var runner = SessionRunner(plan())
        var seconds = 0
        while (!runner.finished && seconds < AN_HOUR) {
            runner = runner.tick()
            // The accelerometer counting somebody who stops when the app says the
            // number is reached, which is what somebody doing this actually does.
            val sensed = runner.movement?.sensedBy
            val counting = sensed != null && sensed != Sensed.None
            if (runner.stage == Stage.Live && counting && !runner.reachedTarget) {
                runner = runner.rep()
            }
            seconds++
        }

        assertWithMessage("a session with nobody touching it never ended")
            .that(runner.finished).isTrue()
        assertWithMessage("it ended without recording anything")
            .that(runner.results).isNotEmpty()
        assertWithMessage("nothing was marked as left out when nothing was skipped")
            .that(runner.results.none { it.skipped }).isTrue()
    }

    @Test
    fun theCountInIsThreeSeconds() {
        val runner = start().ready()
        assertThat(runner.stage).isEqualTo(Stage.CountIn(SessionRunner.COUNT_IN))
        assertThat(runner.tick().tick().tick().stage).isEqualTo(Stage.Live)
    }

    @Test
    fun repsOnlyCountWhileTheSetIsLive() {
        assertThat(start().rep().count).isEqualTo(0)
        assertThat(live().rep().rep().count).isEqualTo(2)
    }

    @Test
    fun aPausedSessionDoesNotMoveAtAll() {
        val paused = live().rep().pause()
        val later = (1..100).fold(paused) { runner, _ -> runner.tick() }
        assertThat(later.elapsed).isEqualTo(paused.elapsed)
        assertThat(later.stage).isEqualTo(paused.stage)
        assertThat(later.rep().count).isEqualTo(paused.count)
        assertThat(later.resume().tick().elapsed).isEqualTo(paused.elapsed + 1)
    }

    @Test
    fun aHoldEndsItselfAtItsTargetAndRepsDoNot() {
        // Somebody who can do two more reps than asked should be allowed to. A hold
        // has no such argument: the target is the number.
        val hold = liveOf("one_leg")
        val ended = (1..hold.step!!.target).fold(hold) { runner, _ -> runner.tick() }
        assertThat(ended.stage).isNotEqualTo(Stage.Live)

        val reps = liveOf("sit_to_stand")
        val over = (1..reps.step!!.target + 5).fold(reps) { runner, _ -> runner.rep() }
        assertThat(over.stage).isEqualTo(Stage.Live)
        assertThat(over.count).isGreaterThan(over.step!!.target)
    }

    @Test
    fun everyExitWorksFromEveryPointAndKeepsWhatWasDone() {
        atEveryPoint { runner, where ->
            listOf<Pair<String, (SessionRunner) -> SessionRunner>>(
                "make it easier" to { it.makeItEasier() },
                "skip this one" to { it.skipThis() },
                "that's enough" to { it.enough() },
                "something hurts" to { it.hurts() },
            ).forEach { (name, exit) ->
                val after = exit(runner)
                assertWithMessage("$name at $where kept what was counted")
                    .that(after.results.sumOf { it.count } + after.count)
                    .isAtLeast(runner.results.sumOf { it.count })
            }
        }
    }

    @Test
    fun enoughForTodayEndsTheSessionFromAnywhere() {
        atEveryPoint { runner, where ->
            val after = runner.enough()
            assertWithMessage("enough at $where ended it").that(after.finished).isTrue()
            assertWithMessage("enough at $where said why")
                .that(after.ending)
                .isEqualTo(Ending.EnoughForToday)
        }
    }

    @Test
    fun theHurtButtonStopsTheSessionInOnePress() {
        atEveryPoint { runner, where ->
            val after = runner.hurts()
            assertWithMessage("hurts at $where stopped it").that(after.finished).isTrue()
            assertWithMessage("hurts at $where said why").that(after.ending).isEqualTo(Ending.Hurt)
        }
    }

    @Test
    fun theAreaIsAskedForAfterTheSessionHasAlreadyStopped() {
        // One press is one press. The list of areas comes afterwards, on its own
        // screen, so nothing stands between somebody in pain and the session ending.
        val stopped = live().rep().hurts()
        assertThat(stopped.finished).isTrue()
        assertThat(stopped.hurtArea).isNull()
        assertThat(stopped.hurtsIn(Area.Shoulder).hurtArea).isEqualTo(Area.Shoulder)
    }

    @Test
    fun makingItEasierKeepsWhatWasAlreadyCounted() {
        val started = liveOf("sit_to_stand").rep().rep().rep()
        val easier = started.makeItEasier()
        assertThat(easier.count).isEqualTo(3)
        assertThat(easier.movement?.id).isEqualTo("sit_to_stand_high")
    }

    @Test
    fun makingItEasierWithNoEasierMovementBringsTheAskDownInstead() {
        val runner = liveOf("heel_raises")
        val asked = runner.step?.target ?: 0
        val eased = runner.makeItEasier()

        assertWithMessage("the movement changed when there was nothing to change to")
            .that(eased.movement?.id).isEqualTo("heel_raises")
        assertThat(eased.step?.target).isLessThan(asked)
        assertThat(eased.step?.target).isAtLeast(1)
    }

    @Test
    fun makingItEasierNeverAsksForLessThanIsAlreadyDone() {
        var runner = liveOf("heel_raises")
        val asked = runner.step?.target ?: 0
        repeat(asked - 1) { runner = runner.rep() }

        val eased = runner.makeItEasier()
        assertThat(eased.step?.target).isEqualTo(asked - 1)
    }

    @Test
    fun skippingRecordsNothingForThatMovementButKeepsTheRest() {
        val runner = live().rep().rep().skipThis()
        val skipped = runner.results.single()
        assertThat(skipped.skipped).isTrue()
        assertThat(skipped.count).isEqualTo(0)
        assertThat(runner.finished).isFalse()
    }

    @Test
    fun nothingIsEverRecordedTwiceForOneMovement() {
        val runner = live().rep().endSet()
        assertThat(runner.enough().results.map { it.movementId }).containsNoDuplicates()
    }

    @Test
    fun restIsSkippableAndMovesOn() {
        val resting = live().rep().endSet()
        assertThat(resting.stage).isInstanceOf(Stage.Rest::class.java)
        assertThat(resting.skipRest().stage).isEqualTo(Stage.Ready)
        assertThat(resting.skipRest().at).isEqualTo(resting.at + 1)
    }

    @Test
    fun theLastSetEndsTheSessionRatherThanResting() {
        var runner = start()
        while (runner.at < runner.plan.steps.size - 1) {
            runner = runner.ready().go().endSet().skipRest()
        }
        assertThat(runner.ready().go().endSet().finished).isTrue()
    }

    @Test
    fun theRingNeverGoesPastFull() {
        val over = (1..99).fold(liveOf("sit_to_stand")) { runner, _ -> runner.rep() }
        assertThat(over.progress).isAtMost(1f)
        assertThat(over.progress).isAtLeast(0f)
    }

    // --- helpers --------------------------------------------------------------

    private fun plan() = SessionEngine.plan(
        SessionInputs(way = GettingAround.OnFeet, kit = setOf(Kit.None, Kit.Chair, Kit.Wall)),
    )

    private fun start() = SessionRunner(plan())

    private fun live() = start().ready().go()

    /** A runner sitting live on one particular movement, for the rules that differ. */
    private fun liveOf(movementId: String): SessionRunner {
        val movement = checkNotNull(Movements.byId(movementId))
        val single = SessionPlan(
            steps = listOf(Step(movement, target = movement.startTarget)),
            adaptation = Adaptation(Adaptation.Kind.None),
        )
        return SessionRunner(single).ready().go()
    }

    /**
     * Every state a session passes through, once each, for the rules that have to hold
     * everywhere.
     */
    private fun atEveryPoint(check: (SessionRunner, String) -> Unit) {
        var runner = start()
        var guard = 0
        val seen = mutableSetOf<String>()
        while (!runner.finished && guard++ < FOREVER) {
            val where = "step ${runner.at} ${runner.stage::class.simpleName}"
            if (seen.add(where)) check(runner, where)
            runner = when (runner.stage) {
                Stage.Ready -> runner.ready()
                Stage.Live -> if (runner.reachedTarget) runner.endSet() else runner.rep().tick()
                else -> runner.tick()
            }
        }
        assertThat(seen).isNotEmpty()
    }

    private companion object {
        const val AN_HOUR = 3600
        const val FOREVER = 10_000
    }
}
