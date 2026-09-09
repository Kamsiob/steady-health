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
    val seconds: Int get() =
        steps.sumOf { it.movement.seconds } + REST * (steps.size - 1).coerceAtLeast(0)
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
    /**
     * The envelope from pacing mode, in minutes, or null when pacing mode is off.
     *
     * LOGIC.md section 7 is explicit that in pacing mode nothing increases and the
     * app only ever suggests staying at or below a number the person set for
     * themselves. Both halves of that are the engine's to keep: a session planned to
     * eight minutes for somebody who said five is the app overriding them, and one
     * more rep because the last two felt easy is graded exercise, which is the thing
     * pacing mode exists to not be.
     */
    val pacingMinutes: Int? = null,
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

    /**
     * The longest a session ever comes to. ADDENDUM-03 Part 1: four to eight minutes.
     *
     * Held by choosing movements that fit rather than by cutting a session short, so
     * nobody is ever offered something the app then takes away from them.
     */
    const val LONGEST_MINUTES = 8

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
        // not against the middle of it. The warm up is decided from the body, and the
        // cool down from the finished session, because the cool down is part of what
        // makes a session longer than five minutes. Reading it against the opening
        // alone left seven minute sessions ending on the last hard movement.
        val lastDone = lastDoneByMovement(inputs)
        val chosen = chooseMain(available, inputs, lastDone)
        val body = chosen.map { step(it, inputs) }
        val warmUp = warmUp(available, lastDone)
            ?.takeIf { seconds(body) > WARM_UP_OVER_MINUTES * SECONDS_PER_MINUTE }
            ?.let { step(it, inputs) }
        val opening = listOfNotNull(warmUp) + body
        val cool = coolDown(available, lastDone).map { step(it, inputs) }
        val whole = seconds(opening + cool)
        val room = longestMinutes(inputs) * SECONDS_PER_MINUTE
        val steps = if (whole > COOL_DOWN_OVER_MINUTES * SECONDS_PER_MINUTE && whole <= room) {
            opening + cool
        } else {
            opening
        }
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

    /**
     * Two to three minutes of mobility, breathing or a short walk. LOGIC.md 15.
     *
     * Least recently done first, like the rest of a session, because an easy day that
     * is the same two warm ups every time is the part people learn to skip. Breathing
     * counts as well as walking: reading only the walks left somebody in bed, who has
     * no walk, with warm ups and nothing else, and left everybody's easy day at eighty
     * seconds rather than the two to three minutes it is meant to be.
     *
     * It stops when the run is long enough rather than after a fixed number of
     * pieces, so three twenty second warm ups and one two minute walk both come to an
     * easy day instead of to whatever two of them happen to add up to.
     */
    private fun easyDay(available: List<Movement>, inputs: SessionInputs): SessionPlan {
        val lastDone = lastDoneByMovement(inputs)
        val gentle = available.filter { isGentle(it) }
            .sortedBy { lastDone[it.id] ?: Long.MIN_VALUE }
        val enough = minOf(EASY_DAY_SECONDS, longestMinutes(inputs) * SECONDS_PER_MINUTE)
        val chosen = mutableListOf<Movement>()
        gentle.forEach { movement ->
            if (runLength(chosen) < enough) chosen += movement
        }
        if (chosen.isEmpty()) {
            // Everything gentle was left out, by an exclusion or by a sore area. Never
            // a blank screen, so the lightest thing still on offer is the easy day.
            available.filter { it.piece == Piece.Main }.minByOrNull { it.seconds }
                ?.let { chosen += it }
        }
        val steps = chosen.map { step(it, inputs) }
        return SessionPlan(
            steps = steps,
            adaptation = Adaptation(
                if (inputs.lastFelt == Felt.Hard) Adaptation.Kind.Lighter else Adaptation.Kind.EasyDay,
            ),
            easy = true,
            feeds = steps.map { it.movement.domain }.distinct(),
        )
    }

    /**
     * The first session, run before anything optional has been asked.
     *
     * One movement, no warm up, no cool down. It deliberately needs nothing in the
     * room, because O5 asks about the chair afterwards and a first session that opens
     * with "find a sturdy chair" is a first session a lot of people never finish.
     */
    fun first(inputs: SessionInputs): SessionPlan {
        val available = Movements.available(inputs.way, inputs.exclusions, inputs.kit, inputs.sore)
        val main = available.filter { it.piece == Piece.Main }
        val bare = main.filter { it.kit == setOf(Kit.None) }
        val one = Movements.first(inputs.way)?.takeIf { it in available }
            ?: bare.minByOrNull { it.startTarget }
            ?: main.firstOrNull()
            ?: return SessionPlan(emptyList(), Adaptation(Adaptation.Kind.None))
        return SessionPlan(
            steps = listOf(step(one, inputs)),
            adaptation = Adaptation(Adaptation.Kind.None),
            small = true,
            feeds = listOf(one.domain),
        )
    }

    /**
     * One movement as a step, with the number today would ask for.
     *
     * Public so the library and "do this again" ask for what the engine would have
     * asked, rather than for whatever the movement starts everybody on.
     */
    fun stepFor(movement: Movement, inputs: SessionInputs): Step = step(movement, inputs)

    /**
     * Ninety seconds, and it counts as a session.
     *
     * Least recently done first, like everything else, because the person who presses
     * "not today, but something small" is often the person who presses it all week.
     */
    private fun small(available: List<Movement>, inputs: SessionInputs): SessionPlan {
        val lastDone = lastDoneByMovement(inputs)
        val main = available.filter { it.piece == Piece.Main }
        val one = main.filter { it.seconds <= NINETY }
            .minByOrNull { lastDone[it.id] ?: Long.MIN_VALUE }
            ?: main.first()
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
     * One per ability, least recently done first, so nothing is neglected and a week
     * of sessions covers all four without anybody having to think about it. The
     * abilities are taken least recently fed first rather than in the order the enum
     * declares them: taking them in declaration order meant the fourth, Steady, was
     * never reached at all, because three movements were always found before it.
     *
     * A movement is only taken if the session still has room for it and for what has
     * to come after it. A session is eight minutes at most and one of these is a ten
     * minute brisk walk, so choosing on recency alone produced sixteen minute
     * sessions. Deliberately not random: two people with the same history get the same
     * session, and so does the same person twice, which is what makes a bug
     * reproducible.
     *
     * One each is the first pass and not the whole rule. Somebody without a chair has
     * nothing under Get up, and a sore shoulder and a sore arm together can empty
     * Carry, and taking one per ability and stopping handed those people a two
     * movement session while five Steady movements sat there unused. So the pass runs
     * again over the abilities that still have something, which changes nothing on an
     * ordinary day and keeps the session whole on a hard one.
     */
    private fun chooseMain(
        available: List<Movement>,
        inputs: SessionInputs,
        lastDone: Map<String, Long>,
    ): List<Movement> {
        val main = available.filter { it.piece == Piece.Main }
        val most = if (inputs.rampingAfterUnwell) MAIN_MOVEMENTS - 1 else MAIN_MOVEMENTS
        val shortest = main.minOfOrNull { it.seconds } ?: 0
        val wanted = howMany(available, inputs, lastDone, most, shortest)
        val budget = mainBudget(available, inputs, lastDone, wanted)
        val abilities = byAbility(main, inputs, lastDone)

        val chosen = mutableListOf<Movement>()
        var taken = 0
        fun take(candidates: List<Movement>) {
            if (chosen.size >= wanted) return
            val room = budget - taken - shortest * (wanted - chosen.size - 1)
            val next = candidates.firstOrNull { movement ->
                movement.seconds <= room && chosen.none { it.id == movement.id }
            } ?: return
            chosen += next
            taken += next.seconds
        }

        repeat(wanted) { abilities.forEach { candidates -> take(candidates) } }
        if (chosen.isEmpty()) {
            // Nothing fitted, which happens only when the room is smaller than the
            // shortest movement there is: a one minute envelope in pacing mode. One
            // movement over the number beats a session with nothing in it.
            val lightest = compareBy<Movement>(
                { it.seconds },
                { lastDone[it.id] ?: Long.MIN_VALUE },
            )
            main.minWithOrNull(lightest)?.let { chosen += it }
        }
        return chosen
    }

    /**
     * The abilities in the order today should feed them, each with its own movements.
     *
     * The ability whose movements were done longest ago comes first, and inside each
     * one the movement done longest ago comes first, so both rotate on their own
     * without anything having to remember whose turn it is.
     */
    private fun byAbility(
        main: List<Movement>,
        inputs: SessionInputs,
        lastDone: Map<String, Long>,
    ): List<List<Movement>> {
        val swapped = inputs.soreMentioned
        return AbilityDomain.entries
            .map { domain ->
                main.filter { it.domain == domain }
                    .filter { swapped == null || it.area != swapped }
                    .map { atTheRightLevel(it, inputs) }
                    .distinctBy { it.id }
                    .sortedBy { lastDone[it.id] ?: Long.MIN_VALUE }
            }
            .filter { it.isNotEmpty() }
            .sortedBy { candidates -> candidates.maxOf { lastDone[it.id] ?: Long.MIN_VALUE } }
    }

    /**
     * How many seconds the main movements have between them.
     *
     * Whatever the warm up and the cool down take is not available to the body of the
     * session, so it is worked out before anything is chosen rather than discovered
     * afterwards. The warm up and the cool down may still be dropped once the body is
     * known, which only ever makes the session shorter than this allowed for.
     */
    private fun mainBudget(
        available: List<Movement>,
        inputs: SessionInputs,
        lastDone: Map<String, Long>,
        wanted: Int,
    ): Int {
        val longest = longestMinutes(inputs)
        val warmUp = warmUp(available, lastDone)?.seconds ?: 0
        val cool = coolDown(available, lastDone)
        val withACoolDown = longest * SECONDS_PER_MINUTE - warmUp - cool.sumOf { it.seconds } -
            rests(1 + cool.size + wanted)
        val without = minOf(longest, COOL_DOWN_OVER_MINUTES) * SECONDS_PER_MINUTE -
            warmUp - rests(1 + wanted)
        return maxOf(withACoolDown, without)
    }

    /** The rests between a run of that many pieces. */
    private fun rests(pieces: Int): Int = REST_BETWEEN * (pieces - 1).coerceAtLeast(0)

    /**
     * How many main movements today has room for.
     *
     * Three ordinarily. Fewer only in pacing mode, where the envelope can be smaller
     * than three of anything: asking for three inside a two minute limit meant the
     * room was smaller than the shortest movement in the library and the session came
     * back with one thing in it, which is worse than two.
     */
    private fun howMany(
        available: List<Movement>,
        inputs: SessionInputs,
        lastDone: Map<String, Long>,
        most: Int,
        shortest: Int,
    ): Int = (most downTo 1).firstOrNull { many ->
        shortest * many <= mainBudget(available, inputs, lastDone, many)
    } ?: 1

    /**
     * The longest today's session may come to.
     *
     * Eight minutes ordinarily, and the person's own envelope in pacing mode. A
     * session cannot both be planned to eight minutes and stay at or below the five
     * the person set, and LOGIC.md section 7 is clear about which of those wins.
     */
    private fun longestMinutes(inputs: SessionInputs): Int =
        inputs.pacingMinutes?.coerceAtLeast(1) ?: LONGEST_MINUTES

    /** True in pacing mode, where nothing the app asks for is allowed to grow. */
    private fun pacing(inputs: SessionInputs): Boolean = inputs.pacingMinutes != null

    /**
     * What an easy day is made of: mobility, breathing, or a short walk.
     *
     * The warm ups are the mobility and the main movements counted in minutes or in
     * breaths are the other two. Everything else in the library is the work.
     */
    private fun isGentle(movement: Movement): Boolean = when (movement.piece) {
        Piece.WarmUp -> true
        Piece.Main -> movement.counted == Counted.Minutes || movement.counted == Counted.Taps
        Piece.CoolDown -> false
    }

    private fun lastDoneByMovement(inputs: SessionInputs): Map<String, Long> =
        inputs.history.groupBy { it.movementId }
            .mapValues { (_, done) -> done.maxOf { it.epochDay } }

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
            pacing(inputs) -> asked
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
        !pacing(inputs) && inputs.lastFelt == Felt.Easy && inputs.feltBefore == Felt.Easy ->
            Adaptation(Adaptation.Kind.OneMore)

        else -> Adaptation(Adaptation.Kind.None)
    }

    private fun daysAway(inputs: SessionInputs): Long =
        inputs.lastSessionDay?.let { inputs.today - it } ?: 0

    /**
     * The warm up and the cool down rotate too.
     *
     * Least recently done first, the same rule the main movements follow. Taking the
     * first one on the list meant every session for a week opened with the same thirty
     * seconds and ended with the same two stretches, which is the part of a session
     * people stop doing first.
     */
    private fun warmUp(available: List<Movement>, lastDone: Map<String, Long>): Movement? =
        available.filter { it.piece == Piece.WarmUp }
            .minByOrNull { lastDone[it.id] ?: Long.MIN_VALUE }

    private fun coolDown(available: List<Movement>, lastDone: Map<String, Long>): List<Movement> =
        available.filter { it.piece == Piece.CoolDown }
            .sortedBy { lastDone[it.id] ?: Long.MIN_VALUE }
            .take(COOL_DOWN_PARTS)

    /** How long a run of steps takes, movements plus the rests between them. */
    private fun seconds(steps: List<Step>): Int = runLength(steps.map { it.movement })

    /** The same, for movements that are not steps yet. */
    private fun runLength(movements: List<Movement>): Int =
        movements.sumOf { it.seconds } + REST_BETWEEN * (movements.size - 1).coerceAtLeast(0)

    /** How many pieces a session ends with. */
    private const val COOL_DOWN_PARTS = 2

    /** How long an easy day runs to. LOGIC.md 15: two to three minutes. */
    private const val EASY_DAY_SECONDS = 120

    private const val REST_BETWEEN = 30
    private const val SECONDS_PER_MINUTE = 60
    private const val NINETY = 90
}
