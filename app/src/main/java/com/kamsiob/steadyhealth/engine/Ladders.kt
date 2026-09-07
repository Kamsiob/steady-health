package com.kamsiob.steadyhealth.engine

import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.Ladder

/** One step on a ladder: what to do, for how long or how many. */
data class Step(
    val index: Int,
    val ladder: Ladder,
    val domain: AbilityDomain,
    /** Minutes for a walk, repetitions for a strength step, seconds for a hold. */
    val amount: Int,
    val measure: StepMeasure,
    val name: String,
    val instruction: String,
)

enum class StepMeasure { Minutes, Repetitions, Seconds }

/** A ladder the person can see, with the steps their exclusions leave in it. */
data class VisibleLadder(val ladder: Ladder, val steps: List<Step>)

/** Everything on offer right now. */
data class LadderPlan(val ladders: List<VisibleLadder>) {
    fun steps(ladder: Ladder): List<Step> =
        ladders.firstOrNull { it.ladder == ladder }?.steps.orEmpty()

    val stepCount: Int get() = ladders.sumOf { it.steps.size }

    /**
     * The floor LOGIC.md section 5 promises: when nearly everything is excluded
     * there is still something. It is never false, and a test holds it to that.
     */
    val hasSomethingToDo: Boolean get() = stepCount > 0
}

/**
 * The five ladders of LOGIC.md section 5, as a table.
 *
 * Deliberately data rather than an algorithm. Everything the app might ask of
 * somebody is written out here in order, so the whole of it can be read in one
 * sitting, by them or by anybody forking this. Nothing generates a workout and
 * nothing adapts a movement.
 *
 * Every step carries the ability it feeds, because MASTER_SPEC requires that
 * every exercise belongs to exactly one of the four and there is nowhere else to
 * put it.
 */
object Ladders {

    /** Any session at or above this counts as a day moved. LOGIC.md section 3. */
    const val COUNTS_AS_A_SESSION_SECONDS = 120

    val walking: List<Step> = listOf(
        2, 4, 6, 8, 11, 14, 16, 20, 25, 30,
    ).mapIndexed { index, minutes ->
        Step(
            index = index,
            ladder = Ladder.Walking,
            domain = AbilityDomain.Go,
            amount = minutes,
            measure = StepMeasure.Minutes,
            name = "A $minutes minute walk",
            instruction = "At a pace you could talk right through.",
        )
    }

    val chairAndStanding: List<Step> = listOf(
        Triple(1, "Seated marching", "Sit tall, march your knees up one at a time."),
        Triple(8, "Seated leg extensions", "Sit tall, straighten one knee, lower it with control."),
        Triple(
            8,
            "Seated band rows",
            "Band round your feet, pull your elbows back, breathe out as you pull.",
        ),
        Triple(5, "Stands from a high seat", "A high chair or a bed. Arms crossed if you can."),
        Triple(5, "Stands from a chair", "Chair against a wall, arms crossed, feet flat."),
        Triple(8, "Stands from a chair", "Same chair, a few more."),
        Triple(10, "Stands from a chair", "Same chair, ten of them."),
        Triple(10, "Heel raises", "Hold a counter, rise onto your toes, lower slowly."),
        Triple(8, "Step ups, low step", "One step, near a rail. Up, then down with control."),
        Triple(8, "Mini squats to a chair", "Chair behind you. Sit back until you touch, then stand."),
        Triple(10, "Stands with no hands", "Arms crossed the whole way, ten of them."),
    ).mapIndexed { index, (reps, name, instruction) ->
        Step(
            index = index,
            ladder = Ladder.ChairAndStanding,
            domain = AbilityDomain.GetUp,
            amount = reps,
            measure = if (index == 0) StepMeasure.Minutes else StepMeasure.Repetitions,
            name = name,
            instruction = instruction,
        )
    }

    val floor: List<Step> = listOf(
        Triple(1, "Kneeling, with support", "Hands on a chair, one knee down, then the other."),
        Triple(
            1,
            "Half kneel to stand",
            "One knee down, hand on a chair, drive up through the front foot.",
        ),
        Triple(1, "Floor to stand, side sit", "Down through a side sit, back up the same way."),
        Triple(1, "Floor to stand, no hands", "Whichever way works. Take your time."),
        Triple(8, "Glute bridge", "On your back, knees bent, lift your hips, lower slowly."),
        Triple(6, "Bird dog", "Hands and knees, opposite arm and leg, hold, swap."),
    ).mapIndexed { index, (reps, name, instruction) ->
        Step(
            index = index,
            ladder = Ladder.Floor,
            domain = AbilityDomain.GetUp,
            amount = reps,
            measure = StepMeasure.Repetitions,
            name = name,
            instruction = instruction,
        )
    }

    val pushing: List<Step> = listOf(
        Triple(
            8,
            "Wall push-ups",
            "Hands on the wall at chest height, body straight, breathe out as you push.",
        ),
        Triple(12, "Wall push-ups", "Same wall, a few more."),
        Triple(8, "Counter push-ups", "Hands on a kitchen counter. Body stays in one line."),
        Triple(12, "Counter push-ups", "Same counter, a few more."),
        Triple(8, "Low incline push-ups", "Hands on a low step or a sturdy chair."),
        Triple(6, "Push-ups from your knees", "Knees down, body straight from head to knees."),
        Triple(5, "Push-ups from your feet", "Body in one line, hips level."),
    ).mapIndexed { index, (reps, name, instruction) ->
        Step(
            index = index,
            ladder = Ladder.Pushing,
            domain = AbilityDomain.Carry,
            amount = reps,
            measure = StepMeasure.Repetitions,
            name = name,
            instruction = instruction,
        )
    }

    val balance: List<Step> = listOf(
        Triple(10, "Feet together", "Near a wall or counter. Feet touching, stand still."),
        Triple(10, "Semi-tandem", "Near a wall or counter. One foot half in front of the other."),
        Triple(10, "Tandem", "Near a wall or counter. Heel right in front of toe."),
        Triple(10, "One leg", "Near a wall or counter. One foot off the ground."),
        Triple(
            10,
            "One leg, looking around",
            "Near a wall or counter. Turn your head slowly, left and right.",
        ),
        Triple(5, "One leg, eyes closed", "Near a wall or counter, hand ready. Close your eyes."),
    ).mapIndexed { index, (seconds, name, instruction) ->
        Step(
            index = index,
            ladder = Ladder.Balance,
            domain = AbilityDomain.Steady,
            amount = seconds,
            measure = StepMeasure.Seconds,
            name = name,
            instruction = instruction,
        )
    }

    /**
     * What this person can see, given what they said to leave out.
     *
     * LOGIC.md section 5's visibility table. An exclusion hides a ladder or caps
     * it, and the seated ladder plus short walks are the floor that is never
     * taken away.
     */
    fun visibleLadders(exclusions: Set<Exclusion>): LadderPlan {
        val ladders = buildList {
            // Walking is always visible. LOGIC.md hides jogging and intervals
            // under the impact exclusion, and this ladder stops at thirty
            // minutes, so there is nothing yet for that rule to hide.
            add(VisibleLadder(Ladder.Walking, walking))
            add(VisibleLadder(Ladder.ChairAndStanding, chairFor(exclusions)))
            if (Exclusion.GettingOnTheFloor !in exclusions) {
                add(VisibleLadder(Ladder.Floor, floorFor(exclusions)))
            }
            if (Exclusion.Pushing !in exclusions) {
                add(VisibleLadder(Ladder.Pushing, pushing))
            }
            add(VisibleLadder(Ladder.Balance, balance))
        }
        return LadderPlan(ladders.filter { it.steps.isNotEmpty() })
    }

    private fun chairFor(exclusions: Set<Exclusion>): List<Step> =
        if (Exclusion.DeepKneeBending in exclusions) {
            // Deep knee bending caps sit-to-stand at partial range and hides the
            // mini squats, so what is left is the seated work and the high seat.
            chairAndStanding.filterNot { it.name.contains("Mini squats") }
                .filterNot { it.name == "Stands with no hands" }
        } else {
            chairAndStanding
        }

    private fun floorFor(exclusions: Set<Exclusion>): List<Step> =
        floor.filterNot { step ->
            Exclusion.LyingFlat in exclusions && step.name in LYING_STEPS
        }

    private val LYING_STEPS = setOf("Glute bridge")
}
