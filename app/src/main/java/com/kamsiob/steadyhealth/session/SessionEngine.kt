package com.kamsiob.steadyhealth.session

import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import kotlin.math.roundToInt

/** How the last session felt. The one question, and what progression turns on. */
enum class Felt(val id: String) { Easy("easy"), AboutRight("about_right"), Hard("hard") }

/** One movement done, once. */
data class Done(
    val movementId: String,
    val epochDay: Long,
    val result: Int,
    val target: Int,
    /** True when the person typed the number rather than the phone counting it. */
    val selfReported: Boolean = false,
)

/** One movement in a planned session, with the number it is asking for. */
data class Step(
    val movement: Movement,
    val target: Int,
    /** The last result for this movement, for the live screen and the done screen. */
    val lastResult: Int? = null,
)

/** Why the session looks the way it does, in one sentence, or nothing. */
data class Adaptation(val kind: Kind, val movementName: String? = null) {
    enum class Kind { Lighter, Swapped, Shorter, OneMore, EasyDay, AfterUnwell, None }
}

/** A whole session, planned. */
data class SessionPlan(
    val steps: List<Step>,
    val adaptation: Adaptation,
    /** True when this is the short, easy version. */
    val easy: Boolean = false,
    /** True when this is the ninety second version somebody asked for. */
    val small: Boolean = false,
    /** The abilities this session feeds, for the one line under the name. */
    val feeds: List<AbilityDomain> = emptyList(),
) {
    val seconds: Int get() = steps.sumOf { it.movement.seconds } + REST * (steps.size - 1)
    val minutes: Int get() = ((seconds + HALF_A_MINUTE) / SECONDS_PER_MINUTE).coerceAtLeast(1)
    val main: List<Step> get() = steps.filter { it.movement.piece == Piece.Main }
    val hasWarmUp: Boolean get() = steps.any { it.movement.piece == Piece.WarmUp }
    val hasCoolDown: Boolean get() = steps.any { it.movement.piece == Piece.CoolDown }

    private companion object {
        const val REST = 30
        const val SECONDS_PER_MINUTE = 60
        const val HALF_A_MINUTE = 30
    }
}

/** Everything the engine needs to plan one session. */
data class SessionInputs(
    val way: GettingAround,
    val exclusions: Set<Exclusion> = emptySet(),
    val kit: Set<Kit> = setOf(Kit.None, Kit.Chair, Kit.Wall),
    /** Areas suppressed by "Something hurts", still inside their seven days. */
    val sore: Set<Area> = emptySet(),
    val history: List<Done> = emptyList(),
    /** How the last session felt, if there was one. */
    val lastFelt: Felt? = null,
    /** How the one before that felt, for the two-easy rule. */
    val feltBefore: Felt? = null,
    val today: Long = 0,
    val lastSessionDay: Long? = null,
    /** The last two sessions were strength, so the next one is easy. */
    val strengthRunLength: Int = 0,
    /** A body tag logged within a day of a session, and the area it points at. */
    val soreMentioned: Area? = null,
    /** Set when somebody asked for the ninety second version. */
    val wantSmall: Boolean = false,
    /** Two weeks of easier sessions after "I was unwell". */
    val rampingAfterUnwell: Boolean = false,
)

/**
 * Planning a session. ADDENDUM-03 Part 1, folded into LOGIC.md 15.
 *
 * Everything here is deterministic and every adaptation comes back as a named kind
 * that the screen turns into one sentence. The model never chooses a movement, never
 * sets a target, and never writes an adaptation line, and there is nowhere in these
 * types for it to.
 *
 * The shape of a session is four to eight minutes: a warm up over three minutes, a
 * cool down over five, and two or three main movements between them. The constraint
 * is not arbitrary. It is how long somebody will actually do something in their
 * kitchen, every day, for a year.
 */
object SessionEngine {

    /** A session over this many minutes opens with a warm up. */
    const val WARM_UP_OVER_MINUTES = 3

    /** A session over this many minutes ends with a cool down. */
    const val COOL_DOWN_OVER_MINUTES = 5

    /** Rated hard: targets come down by about this much. */
    const val LIGHTER = 0.10

    /** Two consecutive strength sessions, then an easy one. */
    const val STRENGTH_RUN_BEFORE_EASY = 2

    /** This many days without a session and the next one is shorter. */
    const val SHORTER_AFTER_DAYS = 4

    /** How many main movements an ordinary session has. */
    const val MAIN_MOVEMENTS = 3

    /** Managing less than this share of the ask brings the ask down to meet it. */
    const val WELL_SHORT = 0.6

    fun plan(inputs: SessionInputs): SessionPlan {
        val available = Movements.available(inputs.way, inputs.exclusions, inputs.kit, inputs.sore)
        if (available.none { it.piece == Piece.Main }) {
            return SessionPlan(emptyList(), Adaptation(Adaptation.Kind.None))
        }

        if (inputs.wantSmall) return small(available, inputs)
        if (easyDayDue(inputs)) return easyDay(available, inputs)

        // Both thresholds are read against what the session will actually come to,
        // not against the middle of it. Deciding the cool down from the body alone
        // produced sessions over five minutes with no cool down in them, which is the
        // rule saying one thing and the plan doing another.
        val chosen = chooseMain(available, inputs)
        val body = chosen.map { step(it, inputs) }
        val warmUp = warmUp(available)
            ?.takeIf { seconds(body) > WARM_UP_OVER_MINUTES * SECONDS_PER_MINUTE }
            ?.let { step(it, inputs) }
        val opening = listOfNotNull(warmUp) + body
        val cool = if (seconds(opening) > COOL_DOWN_OVER_MINUTES * SECONDS_PER_MINUTE) {
            coolDown(available).map { step(it, inputs) }
        } else {
            emptyList()
        }
        val steps = opening + cool
        return SessionPlan(
            steps = steps,
            adaptation = adaptation(inputs, chosen),
            feeds = chosen.map { it.domain }.distinct(),
        )
    }

    /**
     * Whether today is an easy one.
     *
     * After two strength sessions in a row, or after any session that felt hard.
     * Never a blank screen, and never the words "rest day", which read as permission
     * to skip two.
     */
    fun easyDayDue(inputs: SessionInputs): Boolean =
        inputs.lastFelt == Felt.Hard || inputs.strengthRunLength >= STRENGTH_RUN_BEFORE_EASY

    private fun easyDay(available: List<Movement>, inputs: SessionInputs): SessionPlan {
        val gentle = available.filter { it.piece == Piece.WarmUp } +
            available.filter { it.piece == Piece.Main && it.counted == Counted.Minutes }
        val steps = gentle.take(2).map { step(it, inputs) }
        return SessionPlan(
            steps = steps,
            adaptation = Adaptation(
                if (inputs.lastFelt == Felt.Hard) Adaptation.Kind.Lighter else Adaptation.Kind.EasyDay,
            ),
            easy = true,
            feeds = steps.map { it.movement.domain }.distinct(),
        )
    }

    /** Ninety seconds, and it counts as a session. */
    private fun small(available: List<Movement>, inputs: SessionInputs): SessionPlan {
        val one = available.firstOrNull { it.piece == Piece.Main && it.seconds <= NINETY }
            ?: available.first { it.piece == Piece.Main }
        return SessionPlan(
            steps = listOf(step(one, inputs)),
            adaptation = Adaptation(Adaptation.Kind.None),
            small = true,
            feeds = listOf(one.domain),
        )
    }

    /**
     * Which movements today.
     *
     * One per ability where possible, so a week of sessions covers all four without
     * anybody having to think about it, and the least recently done first so nothing
     * is neglected. Deliberately not random: two people with the same history get the
     * same session, and so does the same person twice, which is what makes a bug
     * reproducible.
     */
    private fun chooseMain(available: List<Movement>, inputs: SessionInputs): List<Movement> {
        val main = available.filter { it.piece == Piece.Main }
        val lastDone = inputs.history.groupBy { it.movementId }
            .mapValues { (_, done) -> done.maxOf { it.epochDay } }

        val swapped = inputs.soreMentioned
        return AbilityDomain.entries
            .mapNotNull { domain ->
                main.filter { it.domain == domain }
                    .filter { swapped == null || it.area != swapped }
                    .map { atTheRightLevel(it, inputs) }
                    .minByOrNull { lastDone[it.id] ?: Long.MIN_VALUE }
            }
            .distinctBy { it.id }
            .take(if (inputs.rampingAfterUnwell) MAIN_MOVEMENTS - 1 else MAIN_MOVEMENTS)
    }

    /**
     * The variant this person is on, from the ceiling rule.
     *
     * Reps are the progression up to the ceiling. Past it the movement itself changes,
     * which is what stops the app asking anybody for forty chair stands.
     */
    fun atTheRightLevel(movement: Movement, inputs: SessionInputs): Movement {
        val best = inputs.history.filter { it.movementId == movement.id }.maxOfOrNull { it.result }
        val ceiling = movement.ceiling
        if (best == null || ceiling == null || best < ceiling) return movement
        val harder = movement.harder?.let(Movements::byId) ?: return movement
        val allowed = Movements.available(inputs.way, inputs.exclusions, inputs.kit, inputs.sore)
        return if (harder in allowed) harder else movement
    }

    /**
     * The target for one movement.
     *
     * It moves from what was asked last time, not from what was managed. Those are
     * different numbers and the difference matters: somebody who stopped a
     * thirty-second warm up after five seconds because the doorbell went should not
     * be asked for five seconds next time, and then four, and then three.
     *
     * The exception is falling a long way short. If what somebody actually managed
     * was well under what was asked, the ask comes down to meet it, because a target
     * nobody can reach is not a target, it is a reminder of what they cannot do.
     */
    fun target(movement: Movement, inputs: SessionInputs): Int {
        val last = inputs.history.filter { it.movementId == movement.id }.maxByOrNull { it.epochDay }
        val asked = last?.target ?: movement.startTarget
        val managed = last?.result

        val adjusted = when {
            inputs.rampingAfterUnwell -> (asked * (1 - LIGHTER * 2)).roundToInt()
            inputs.lastFelt == Felt.Hard -> (asked * (1 - LIGHTER)).roundToInt()
            inputs.lastFelt == Felt.Easy && inputs.feltBefore == Felt.Easy -> asked + 1
            daysAway(inputs) >= SHORTER_AFTER_DAYS -> (asked * (1 - LIGHTER)).roundToInt()
            else -> asked
        }

        val metReality = if (managed != null && managed < asked * WELL_SHORT) {
            minOf(adjusted, managed.coerceAtLeast(1))
        } else {
            adjusted
        }

        val ceiling = movement.ceiling ?: Int.MAX_VALUE
        return metReality.coerceIn(1, ceiling)
    }

    private fun step(movement: Movement, inputs: SessionInputs) = Step(
        movement = movement,
        target = target(movement, inputs),
        lastResult = inputs.history
            .filter { it.movementId == movement.id }
            .maxByOrNull { it.epochDay }
            ?.result,
    )

    /**
     * Which adaptation to say out loud, and only one.
     *
     * In the order they matter to somebody reading the card: a swap because they said
     * something hurts beats a lighter session, which beats a shorter one, which beats
     * one more rep. Two sentences on an offer card is one too many.
     */
    private fun adaptation(inputs: SessionInputs, chosen: List<Movement>): Adaptation = when {
        inputs.rampingAfterUnwell -> Adaptation(Adaptation.Kind.AfterUnwell)
        inputs.soreMentioned != null -> Adaptation(
            Adaptation.Kind.Swapped,
            chosen.firstOrNull()?.name,
        )

        inputs.lastFelt == Felt.Hard -> Adaptation(Adaptation.Kind.Lighter)
        daysAway(inputs) >= SHORTER_AFTER_DAYS -> Adaptation(Adaptation.Kind.Shorter)
        inputs.lastFelt == Felt.Easy && inputs.feltBefore == Felt.Easy ->
            Adaptation(Adaptation.Kind.OneMore)

        else -> Adaptation(Adaptation.Kind.None)
    }

    private fun daysAway(inputs: SessionInputs): Long =
        inputs.lastSessionDay?.let { inputs.today - it } ?: 0

    private fun warmUp(available: List<Movement>): Movement? =
        available.firstOrNull { it.piece == Piece.WarmUp }

    private fun coolDown(available: List<Movement>): List<Movement> =
        available.filter { it.piece == Piece.CoolDown }.take(2)

    /** How long a run of steps takes, movements plus the rests between them. */
    private fun seconds(steps: List<Step>): Int =
        steps.sumOf { it.movement.seconds } + REST_BETWEEN * (steps.size - 1).coerceAtLeast(0)

    private const val REST_BETWEEN = 30
    private const val SECONDS_PER_MINUTE = 60
    private const val NINETY = 90
}
