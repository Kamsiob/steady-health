package com.kamsiob.steadyhealth.engine

import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
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
     * Go, for somebody in a wheelchair.
     *
     * Deliberately the same numbers as walking. A minute is a minute, the talk
     * test works the same way, and giving this ladder its own shorter shape would
     * be the interface saying something about the person that it has no business
     * saying.
     */
    val wheeling: List<Step> = listOf(
        2, 4, 6, 8, 11, 14, 16, 20, 25, 30,
    ).mapIndexed { index, minutes ->
        Step(
            index = index,
            ladder = Ladder.Wheeling,
            domain = AbilityDomain.Go,
            amount = minutes,
            measure = StepMeasure.Minutes,
            name = "$minutes minutes wheeling",
            instruction = "At a pace you could talk right through.",
        )
    }

    /**
     * Seated strength, transfers, and pressure relief.
     *
     * This is the wheelchair user's strength ladder, and it is also what LOGIC.md
     * section 5 puts in front of anybody who left pushing out: the band press in
     * range replaces the push-up ladder rather than leaving a hole where it was.
     */
    val seated: List<Step> = listOf(
        SeatedStep(
            reps = 8,
            name = "Seated band rows",
            instruction = "Band round your feet or a table leg, pull back, breathe out as you pull.",
            domain = AbilityDomain.Carry,
        ),
        SeatedStep(
            reps = 8,
            name = "Seated band press",
            instruction = "Band behind you, press forward at chest height. Breathe out as you press.",
            domain = AbilityDomain.Carry,
        ),
        SeatedStep(
            reps = 5,
            name = "Pressure relief lifts",
            instruction = "Hands on the armrests, lift clear for a few seconds, down with control.",
            domain = AbilityDomain.GetUp,
        ),
        SeatedStep(
            reps = 10,
            name = "Forward lean and back",
            instruction = "Lean forward over your knees, pause, come back up tall.",
            domain = AbilityDomain.GetUp,
        ),
        SeatedStep(
            reps = 8,
            name = "Seated rotation",
            instruction = "Hands on your chest, turn slowly one way, then the other.",
            domain = AbilityDomain.Steady,
        ),
        SeatedStep(
            reps = 10,
            name = "Overhead reach",
            instruction = "Reach both arms up as far as is comfortable. Slowly.",
            domain = AbilityDomain.Steady,
        ),
        SeatedStep(
            reps = 5,
            name = "Transfers",
            instruction = "Chair to bed and back, however you do it. Rest between.",
            domain = AbilityDomain.GetUp,
        ),
        SeatedStep(
            reps = 12,
            name = "Seated band press",
            instruction = "Same band, a few more.",
            domain = AbilityDomain.Carry,
        ),
        SeatedStep(
            reps = 12,
            name = "Seated band rows",
            instruction = "Same band, a few more.",
            domain = AbilityDomain.Carry,
        ),
    ).mapIndexed { index, step ->
        Step(
            index = index,
            ladder = Ladder.Seated,
            domain = step.domain,
            amount = step.reps,
            measure = StepMeasure.Repetitions,
            name = step.name,
            instruction = step.instruction,
        )
    }

    /**
     * The bed set, from screen 18 of the grid.
     *
     * Unlike every other ladder these are not done one at a time. A session is the
     * first [BED_SET_PARTS] of them run back to back, and progression adds a part
     * rather than making a part longer, which is why [bedSet] takes the step index
     * and returns a list.
     */
    val inBed: List<Step> = listOf(
        BedStep(
            seconds = 40,
            name = "Ankle pumps",
            instruction = "Point and flex, 30 each side. Keeps the blood moving.",
            domain = AbilityDomain.Steady,
        ),
        BedStep(
            seconds = 60,
            name = "Towel squeeze",
            instruction = "As hard as is comfortable, 5 seconds, 6 times each hand.",
            domain = AbilityDomain.Carry,
        ),
        BedStep(
            seconds = 40,
            name = "Ten slow breaths",
            instruction = "In through the nose, out slowly. The phone counts.",
            domain = AbilityDomain.Go,
        ),
        BedStep(
            seconds = 60,
            name = "Sit to the edge",
            instruction = "Roll onto your side, push up with the lower arm. Rest there.",
            domain = AbilityDomain.GetUp,
        ),
        BedStep(
            seconds = 60,
            name = "Heel slides",
            instruction = "One heel at a time, up towards you and back down.",
            domain = AbilityDomain.Go,
        ),
        BedStep(
            seconds = 45,
            name = "Arms overhead",
            instruction = "Reach both arms up as far as is comfortable. Slowly.",
            domain = AbilityDomain.Carry,
        ),
    ).mapIndexed { index, step ->
        Step(
            index = index,
            ladder = Ladder.InBed,
            domain = step.domain,
            amount = step.seconds,
            measure = StepMeasure.Seconds,
            name = step.name,
            instruction = step.instruction,
        )
    }

    /** How many parts a bed session starts with. Step one adds a fourth. */
    const val BED_SET_PARTS = 3

    /**
     * The parts of one bed session at [stepIndex], and nothing else.
     *
     * [exclusions] applies here as it does everywhere: somebody who left lying
     * flat out does not get sit to the edge offered from lying down, and the set
     * is still a set.
     */
    fun bedSet(stepIndex: Int, exclusions: Set<Exclusion> = emptySet()): List<Step> {
        val available = inBed.filterNot { hiddenInBed(it, exclusions) }
        return available.take(BED_SET_PARTS + stepIndex.coerceAtLeast(0))
    }

    private fun hiddenInBed(step: Step, exclusions: Set<Exclusion>): Boolean = when {
        Exclusion.LiftingOverhead in exclusions && step.name == "Arms overhead" -> true
        Exclusion.StomachStrain in exclusions && step.name == "Sit to the edge" -> true
        else -> false
    }

    private data class SeatedStep(
        val reps: Int,
        val name: String,
        val instruction: String,
        val domain: AbilityDomain,
    )

    private data class BedStep(
        val seconds: Int,
        val name: String,
        val instruction: String,
        val domain: AbilityDomain,
    )

    /**
     * What this person can see, given how they get around and what they said to
     * leave out.
     *
     * LOGIC.md section 5's visibility table, plus the rule from section 3b that
     * the way of getting around selects which exercises exist at all. An
     * exclusion hides a ladder or caps it, and something is always left.
     */
    fun visibleLadders(
        way: GettingAround = GettingAround.OnFeet,
        exclusions: Set<Exclusion> = emptySet(),
    ): LadderPlan {
        val ladders = when (way) {
            GettingAround.OnFeet, GettingAround.Walker -> onFootLadders(exclusions)
            GettingAround.Wheelchair -> wheelchairLadders(exclusions)
            GettingAround.InBed -> listOf(VisibleLadder(Ladder.InBed, bedSet(LAST_STEP, exclusions)))
        }
        return LadderPlan(ladders.filter { it.steps.isNotEmpty() })
    }

    private fun onFootLadders(exclusions: Set<Exclusion>): List<VisibleLadder> = buildList {
        // Walking is always visible. LOGIC.md hides jogging and intervals under
        // the impact exclusion, and this ladder stops at thirty minutes, so there
        // is nothing yet for that rule to hide.
        add(VisibleLadder(Ladder.Walking, walking))
        add(VisibleLadder(Ladder.ChairAndStanding, chairFor(exclusions)))
        if (Exclusion.GettingOnTheFloor !in exclusions) {
            add(VisibleLadder(Ladder.Floor, floorFor(exclusions)))
        }
        if (Exclusion.Pushing in exclusions) {
            // Pushing is not simply removed. LOGIC.md replaces it with the band
            // press in range, so the ability it fed still has something in it.
            add(VisibleLadder(Ladder.Seated, seatedFor(exclusions).filter { it.name.contains("press") }))
        } else {
            add(VisibleLadder(Ladder.Pushing, pushing))
        }
        add(VisibleLadder(Ladder.Balance, balance))
    }

    /**
     * The pushing ladder is not here on purpose. Every one of its steps is
     * written for somebody standing at a wall or a counter, and rewriting the
     * instructions to mean something else would be describing a movement the app
     * has not thought about. The seated ladder carries Carry instead, and the
     * band press is in it twice.
     */
    private fun wheelchairLadders(exclusions: Set<Exclusion>): List<VisibleLadder> = listOf(
        VisibleLadder(Ladder.Wheeling, wheeling),
        VisibleLadder(Ladder.Seated, seatedFor(exclusions)),
    )

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

    private fun seatedFor(exclusions: Set<Exclusion>): List<Step> = seated.filterNot { step ->
        when {
            Exclusion.LiftingOverhead in exclusions && step.name == "Overhead reach" -> true
            Exclusion.TwistingBack in exclusions && step.name == "Seated rotation" -> true
            Exclusion.DeepForwardBending in exclusions && step.name == "Forward lean and back" -> true
            else -> false
        }
    }

    /** Enough to ask for the whole bed set rather than a session's worth of it. */
    private const val LAST_STEP = 99

    private val LYING_STEPS = setOf("Glute bridge")
}
