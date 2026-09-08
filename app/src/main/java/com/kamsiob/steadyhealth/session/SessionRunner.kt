package com.kamsiob.steadyhealth.session

/** Where the session is. The seven screens of ADDENDUM-03 Part 1, as states. */
sealed interface Stage {
    /** S2. The movement's name, its setup, the target, the stop rule. */
    data object Ready : Stage

    /** S3. Three, two, one, go. */
    data class CountIn(val secondsLeft: Int) : Stage

    /** S4. The dominant screen of the app. */
    data object Live : Stage

    /** S5. A shrinking arc, with the next movement named underneath. */
    data class Rest(val secondsLeft: Int) : Stage

    /** S7. What was done, and how it felt. */
    data object Done : Stage
}

/** One movement, as it was actually done. */
data class Result(
    val movementId: String,
    val target: Int,
    val count: Int,
    /** True when the person tapped rather than the phone counting. */
    val selfReported: Boolean,
    /** True when they dropped to the easier variant part way through. */
    val madeEasier: Boolean = false,
    val skipped: Boolean = false,
)

/** Why a session ended, which decides what the done screen says. */
enum class Ending { Finished, EnoughForToday, Hurt }

/**
 * The live session.
 *
 * A pure state machine. It holds no Android types, starts no timers and speaks
 * nothing; the view model drives it with `tick` and the screens read it. That split
 * is what lets every rule in ADDENDUM-03 Parts 1 and 2 be tested without a device,
 * including the ones that only happen on a bad day.
 *
 * Nothing here can fail in a way that loses what somebody already did. Every exit
 * keeps the results so far, and there is no path through this class that discards
 * them.
 */
data class SessionRunner(
    val plan: SessionPlan,
    val at: Int = 0,
    val stage: Stage = Stage.Ready,
    val count: Int = 0,
    /** Seconds elapsed in the current set, for holds and walks. */
    val elapsed: Int = 0,
    val paused: Boolean = false,
    val results: List<Result> = emptyList(),
    val ending: Ending? = null,
    /** Set when the person pressed "Something hurts" and named an area. */
    val hurtArea: Area? = null,
    /** Movements swapped to their easier variant mid set. */
    val easedIds: Set<String> = emptySet(),
) {
    val step: Step? get() = plan.steps.getOrNull(at)
    val movement: Movement? get() = step?.movement
    val next: Step? get() = plan.steps.getOrNull(at + 1)
    val finished: Boolean get() = stage == Stage.Done

    /** How far through the target, nought to one, for the ring. */
    val progress: Float
        get() {
            val target = step?.target ?: return 0f
            if (target <= 0) return 0f
            return (reached.toFloat() / target).coerceIn(0f, 1f)
        }

    /** What the big number shows: reps counted, or seconds for a hold or a walk. */
    val reached: Int
        get() = when (movement?.counted) {
            Counted.Hold -> elapsed
            Counted.Minutes -> elapsed / SECONDS_PER_MINUTE
            else -> count
        }

    /** True when the set has reached what it was asked for. */
    val reachedTarget: Boolean get() = step?.let { reached >= it.target } ?: false

    // --- moving through the session ------------------------------------------

    fun ready(): SessionRunner = copy(stage = Stage.CountIn(COUNT_IN), count = 0, elapsed = 0)

    /** Skip the count in, which S6 allows between movements. */
    fun go(): SessionRunner = copy(stage = Stage.Live)

    /**
     * One second of clock.
     *
     * The only thing that moves time in this class, so a test can run a whole session
     * in a loop and a paused session cannot advance by accident.
     */
    fun tick(): SessionRunner {
        if (paused) return this
        return when (val stage = stage) {
            is Stage.CountIn -> {
                if (stage.secondsLeft <= 1) {
                    copy(stage = Stage.Live)
                } else {
                    copy(stage = Stage.CountIn(stage.secondsLeft - 1))
                }
            }

            Stage.Live -> {
                val next = copy(elapsed = elapsed + 1)
                // A hold or a walk ends itself at its target; reps wait for the person,
                // because somebody who can do two more should be allowed to.
                if (next.timed && next.reachedTarget) next.endSet() else next
            }

            is Stage.Rest -> {
                if (stage.secondsLeft <= 1) {
                    advance()
                } else {
                    copy(stage = Stage.Rest(stage.secondsLeft - 1))
                }
            }

            else -> this
        }
    }

    /** One repetition, from the camera, the motion sensor, or a tap. */
    fun rep(): SessionRunner = if (paused || stage != Stage.Live) this else copy(count = count + 1)

    fun pause(): SessionRunner = copy(paused = true)

    fun resume(): SessionRunner = copy(paused = false)

    /** End the current set, keeping what was counted. */
    fun endSet(): SessionRunner = record(skipped = false).rest()

    fun skipRest(): SessionRunner = advance()

    // --- the three exits, and the pain button ---------------------------------

    /**
     * "Make it easier": drop to the easier variant mid set and carry on.
     *
     * What was counted so far is kept, because it happened. The set continues under
     * the easier movement's name and the result is recorded against it.
     */
    fun makeItEasier(): SessionRunner {
        val current = movement ?: return this
        val easier = current.easier?.let(Movements::byId) ?: return this
        val steps = plan.steps.toMutableList()
        steps[at] = Step(easier, target = easier.startTarget, lastResult = step?.lastResult)
        return copy(
            plan = plan.copy(steps = steps),
            easedIds = easedIds + easier.id,
        )
    }

    /** "Skip this one": on to the next movement, nothing recorded for this one. */
    fun skipThis(): SessionRunner = record(skipped = true).advance()

    /** "That's enough for today": end, keeping everything done. */
    fun enough(): SessionRunner =
        record(skipped = false).copy(stage = Stage.Done, ending = Ending.EnoughForToday)

    /**
     * "Something hurts": stop now, no confirmation.
     *
     * The area is asked for afterwards on its own screen, so one press is genuinely
     * one press. Everything done so far is kept.
     */
    fun hurts(): SessionRunner =
        record(skipped = false).copy(stage = Stage.Done, ending = Ending.Hurt)

    fun hurtsIn(area: Area): SessionRunner = copy(hurtArea = area)

    // --- internals ------------------------------------------------------------

    private val timed: Boolean
        get() = movement?.counted == Counted.Hold || movement?.counted == Counted.Minutes

    private fun rest(): SessionRunner = if (next == null) {
        copy(stage = Stage.Done, ending = ending ?: Ending.Finished)
    } else {
        copy(stage = Stage.Rest(REST_SECONDS))
    }

    private fun advance(): SessionRunner {
        val to = at + 1
        return if (to >= plan.steps.size) {
            copy(stage = Stage.Done, ending = ending ?: Ending.Finished)
        } else {
            copy(at = to, stage = Stage.Ready, count = 0, elapsed = 0)
        }
    }

    /** Keep what happened, once, whatever the reason the set ended. */
    private fun record(skipped: Boolean): SessionRunner {
        val step = step ?: return this
        if (results.any { it.movementId == step.movement.id }) return this
        return copy(
            results = results + Result(
                movementId = step.movement.id,
                target = step.target,
                count = if (skipped) 0 else reached,
                selfReported = step.movement.counted == Counted.Taps,
                madeEasier = step.movement.id in easedIds,
                skipped = skipped,
            ),
        )
    }

    companion object {
        const val COUNT_IN = 3
        const val REST_SECONDS = 30
        private const val SECONDS_PER_MINUTE = 60
    }
}
