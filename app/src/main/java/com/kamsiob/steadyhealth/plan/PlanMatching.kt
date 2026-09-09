package com.kamsiob.steadyhealth.plan

import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.session.Counted
import com.kamsiob.steadyhealth.session.Movement
import com.kamsiob.steadyhealth.session.Movements
import com.kamsiob.steadyhealth.session.SessionPlan
import com.kamsiob.steadyhealth.session.Step

/**
 * Reading a therapist's plan. ADDENDUM-03 Part 6, LOGIC.md 17.
 *
 * Four ways in and all four arrive here as lines of text: a photographed sheet after
 * the text is pulled off it, something the person said out loud, a pick from the
 * library, or typing. This turns each line into a proposal and nothing more. Part 6
 * says every item is shown with what the app matched it to and nothing is saved
 * unconfirmed, so an unmatched line is a normal outcome rather than a failure, and it
 * is deliberately visible in the result instead of quietly missing from it.
 *
 * MATCHING IS A WRITTEN TABLE AND NOT A DISTANCE. Edit distance would match "heel
 * slides" to "heel raises" at a good score and be wrong on a page somebody was handed
 * at a hospital. A table can be read, argued with, and corrected, and when it does not
 * know a phrase it says so rather than reaching for the nearest thing. The whole
 * trade is that an unmatched line costs one tap and a confident wrong match costs
 * somebody weeks of the wrong movement.
 *
 * NOTHING HERE CHANGES A PLAN. Part 6: the app runs it exactly as given, and reps and
 * frequency move only when the person moves them. There is no function in this file
 * that returns a plan with different numbers in it than the ones it was handed, and
 * there should never be one.
 */
object PlanMatching {

    /**
     * A block of text cut into the lines a plan is made of.
     *
     * Newlines and semicolons, list markers taken off the front, and "and" only where
     * both halves name a movement on their own. That last rule is the whole reason
     * this is not a one liner: "heel and toe" is a movement in the library and
     * "morning and evening" is a frequency, so splitting on the word itself would
     * invent items nobody wrote. Splitting only when both halves stand up on their own
     * gets the spoken case, which is one long sentence with two movements in it.
     */
    fun lines(text: String): List<String> = text
        .split('\n', ';')
        .map { LIST_MARKER.replace(it.trim(), "").trim() }
        .filter { line -> line.any(Char::isLetter) }
        .flatMap(::splitOnAnd)

    /** A block of text straight to proposals, for the spoken and the scanned way in. */
    fun readAll(text: String): List<PlanItem> = read(lines(text))

    /**
     * One proposal per line, in the order they were given.
     *
     * The order is the therapist's order and the confirmation screen keeps it, because
     * a sheet is often written in the order they want it done.
     */
    fun read(lines: List<String>): List<PlanItem> = lines
        .map(String::trim)
        .filter { line -> line.any(Char::isLetter) }
        .map(::readOne)

    /**
     * What the app makes of one line.
     *
     * The frequency is read and then cut out of the line before the number is looked
     * for, because "3 times a week" begins with a number that is not a count of
     * anything the person does in one go.
     *
     * A rate the app has no case for is cut out too, and this is the part worth
     * arguing with. "Five times an hour" and "2 x daily" both sit a number next to a
     * stretch of time, and left in the line that number gets read as repetitions, so
     * the app would say five repetitions where the sheet said five times. Cutting the
     * whole shape out leaves both boxes empty under the person's own words, which is
     * a question they answer in one tap rather than an answer they have to catch.
     */
    fun readOne(line: String): PlanItem {
        val text = X_RATE.replace(digits(line), " ")
        val often = readOften(text)
        val left = often?.let { text.replace(it.words, " ") } ?: text
        val rest = UNKNOWN_RATE.replace(left, " ")
        val found = match(line)
        return PlanItem(
            line = line.trim(),
            movement = found.movement,
            howMany = howMany(rest, found.movement),
            howOften = often?.often ?: HowOften.Unsaid,
            sureness = found.sureness,
            eachSide = EACH_SIDE.containsMatchIn(text),
            couldBe = found.couldBe,
        )
    }

    /**
     * The movements on both the therapist's plan and the app's own suggestion.
     *
     * Part 6's overlap rule: one of these is done once and counts for both, and the
     * app says so once. Meant for a plan the person has already confirmed, since a
     * guess the app has not had agreed is not something to tell them their therapist
     * asked for.
     */
    fun onBoth(plan: List<PlanItem>, session: SessionPlan): List<Movement> {
        val theirs = plan.mapNotNull { it.movement?.id }.toSet()
        return session.steps.map { it.movement }.filter { it.id in theirs }.distinctBy { it.id }
    }

    /**
     * The app's own suggestions with the plan's movements taken out.
     *
     * This is the "Also, if you want more" list. Part 6 is firm that the two are never
     * merged, so this removes rather than combines: what comes back is only ever the
     * app's own, and the plan it was handed comes back untouched.
     */
    fun alsoIfYouWantMore(plan: List<PlanItem>, session: SessionPlan): List<Step> {
        val theirs = plan.mapNotNull { it.movement?.id }.toSet()
        return session.steps.filterNot { it.movement.id in theirs }
    }

    /**
     * The items to raise rather than to drop.
     *
     * Part 6: a plan movement that clashes with something the person said they avoid
     * is flagged and left in, and the app asks them to raise it at the appointment.
     * So this returns items and never a shorter plan, and the caller has nowhere to
     * put the answer if it decided to remove one.
     *
     * The way they get around counts as a clash for the same reason: a sheet asking
     * somebody who uses a wheelchair for heel raises is worth a sentence at the next
     * appointment, and it is still their therapist's line to change and not the app's.
     */
    fun toAskAbout(
        plan: List<PlanItem>,
        exclusions: Set<Exclusion>,
        way: GettingAround,
    ): List<PlanItem> = plan.filter { item ->
        val movement = item.movement
        movement != null && (movement.excludedBy.any { it in exclusions } || way !in movement.ways)
    }

    /** Every movement the table names. Public so the test can hold it to the library. */
    val tableIds: Set<String> get() = NAMED.keys + LIKELY.keys

    // --- Splitting -----------------------------------------------------------

    private fun splitOnAnd(line: String): List<String> {
        // "Avoid deep squats and stairs" has to stay one line. Cut in half, the second
        // half loses the word that made it a warning and comes back as an item to do.
        if (SAID_NOT_TO.containsMatchIn(normalise(line))) return listOf(line)
        val parts = line.split(AND).map(String::trim).filter { it.isNotBlank() }
        val eachStandsAlone = parts.size > 1 && parts.all { match(it).movement != null }
        return if (eachStandsAlone) parts else listOf(line)
    }

    // --- Matching ------------------------------------------------------------

    /** One phrase in the table, already normalised, and what it is worth. */
    private data class Phrase(val words: String, val movement: Movement, val sureness: Sureness) {

        /** The same words as a set, held once here rather than cut up on every line. */
        val wordSet: Set<String> = words.split(" ").filter(String::isNotBlank).toSet()
    }

    /** What one line came to: a movement, or the reason there is not one. */
    private data class Match(
        val movement: Movement?,
        val sureness: Sureness,
        val couldBe: List<Movement>,
    )

    /**
     * Which movement a line names, or why the app is not going to say.
     *
     * A phrase sitting inside a longer matched phrase is dropped, so "wall push ups"
     * beats "push ups" and "semi tandem stand" beats "tandem stand" without the table
     * having to say which is more specific. What is left after that is the set of
     * separate things the line names, and a line naming two of them gets neither: on
     * "sit to stands and heel raises" the first is not the more likely one, only the
     * earlier one.
     *
     * A line that says not to do something gets nothing at all. "Avoid stairs" reads
     * as the word stairs to anything looking for a movement, and a sheet that says to
     * leave something out is the one line where matching it correctly puts the person
     * in front of the thing they were told to skip.
     */
    private fun match(line: String): Match {
        val hay = normalise(line)
        if (SAID_NOT_TO.containsMatchIn(hay)) return Match(null, Sureness.Unmatched, emptyList())
        val hits = PHRASES.filter { hay.contains(it.words) }
        val kept = hits.filter { hit ->
            hits.none { it.words.length > hit.words.length && it.words.contains(hit.words) }
        }
        val movements = (kept.map { it.movement } + alsoNamed(hay, kept)).distinctBy { it.id }
        val named = kept.any { it.sureness == Sureness.Named }
        return when {
            movements.isEmpty() -> Match(null, Sureness.Unmatched, emptyList())
            movements.size > 1 -> Match(null, Sureness.MoreThanOne, movements)
            named -> Match(movements.first(), Sureness.Named, emptyList())
            else -> Match(movements.first(), Sureness.Likely, emptyList())
        }
    }

    /**
     * Movements the line names in words the table only knows in one order.
     *
     * "Step ups on a higher step" is the phrase for the higher step with two words in
     * the middle of it, so read as a phrase it is the low step up and nothing else,
     * which is a confident wrong answer on a line that said the opposite. A phrase
     * whose every word is on the line, and which says more about the movement than
     * the phrase that did match, is offered beside that one and the line comes back
     * naming neither. Words in the wrong order are enough to raise a question here and
     * deliberately not enough to answer one, which is why this widens what the app is
     * unsure about and never what it is sure of.
     */
    private fun alsoNamed(hay: String, kept: List<Phrase>): List<Movement> {
        val said = hay.split(" ").filter(String::isNotBlank).toSet()
        return PHRASES.filter { phrase ->
            said.containsAll(phrase.wordSet) &&
                kept.any { held ->
                    held.movement.id != phrase.movement.id &&
                        phrase.wordSet.size > held.wordSet.size &&
                        phrase.wordSet.containsAll(held.wordSet)
                }
        }.map { it.movement }
    }

    /**
     * The line as words the table can be compared against.
     *
     * Lowercase, everything that is not a letter or a digit becomes a space, plurals
     * come off, and the whole thing is padded with spaces so a phrase match is always
     * a whole word match. Padding is why "walk" does not match "sidewalk".
     */
    private fun normalise(text: String): String =
        text.lowercase()
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ", prefix = " ", postfix = " ", transform = ::singular)

    /**
     * A word with its plural taken off, roughly, and the same way on both sides.
     *
     * It only has to be consistent, not correct: the table goes through this too, so
     * "presses" and "press" both come out as "press" and it does not matter that
     * "raises" would come out as "raise" either way. Short words are left alone
     * because "sts" and "ups" are not plurals of anything.
     */
    private fun singular(word: String): String = when {
        word in SHORT_PLURALS -> SHORT_PLURALS.getValue(word)
        word.length >= SHORTEST_PLURAL && ES_PLURALS.any(word::endsWith) -> word.dropLast(2)
        word.length >= SHORTEST_PLURAL && word.endsWith("s") && !word.endsWith("ss") -> word.dropLast(1)
        else -> word
    }

    // --- How often -----------------------------------------------------------

    /** A frequency, and the words it was read from, so they can be cut out. */
    private data class ReadOften(val often: HowOften, val words: String)

    private fun readOften(text: String): ReadOften? =
        otherDay(text) ?: aDay(text) ?: aWeek(text)

    private fun otherDay(text: String): ReadOften? =
        EVERY_OTHER_DAY.find(text)?.let { ReadOften(HowOften.EveryOtherDay, it.value) }

    private fun aDay(text: String): ReadOften? {
        val counted = TIMES_A_DAY.find(text) ?: TIMES_DAILY.find(text) ?: X_A_DAY.find(text)
        if (counted != null) {
            return times(counted)?.let { ReadOften(HowOften.ADay(it), counted.value) }
        }
        // Two named parts of the day is the other way a sheet writes twice a day, and
        // it is worth reading because it is the one that never carries a number.
        val parts = MORNING_AND.find(text)
        if (parts != null) return ReadOften(HowOften.ADay(TWICE), parts.value)
        return EVERY_DAY.find(text)?.let { ReadOften(HowOften.ADay(ONCE), it.value) }
    }

    private fun aWeek(text: String): ReadOften? {
        val counted = TIMES_A_WEEK.find(text) ?: X_A_WEEK.find(text)
        if (counted != null) {
            return times(counted)?.let { ReadOften(HowOften.AWeek(it), counted.value) }
        }
        return WEEKLY.find(text)?.let { ReadOften(HowOften.AWeek(ONCE), it.value) }
    }

    private fun times(found: MatchResult): Int? =
        found.groupValues[1].toIntOrNull()?.takeIf { it in 1..MOST_TIMES }

    // --- How many ------------------------------------------------------------

    /**
     * The number the line asks for.
     *
     * Rounds are pulled out first and the line is rewritten without them, so "3 sets of
     * 8" and "8" go down the same path afterwards and the unit words that follow are
     * still there to be read.
     */
    private fun howMany(text: String, movement: Movement?): HowMany {
        val (rounds, rest) = sets(text)
        return minutes(rest) ?: seconds(rest, rounds) ?: reps(rest, rounds) ?: bare(rest, movement, rounds)
    }

    /** Rounds, and the line with the words that said so taken out of it. */
    private data class ReadSets(val rounds: Int, val rest: String)

    /**
     * How many rounds, written either way round.
     *
     * Sheets put the rounds first ("3 sets of 8") and sheets put them last ("8 reps, 3
     * sets"), and reading only the first shape leaves the second saying one set of
     * eight. That is a quiet way to hand somebody a third of what their therapist
     * asked for, and quiet is the problem: nothing on the confirmation screen would
     * look wrong.
     */
    private fun sets(text: String): ReadSets {
        val of = SETS_OF.find(text)
        if (of != null) {
            val rest = text.replaceRange(of.range, " ${of.groupValues[2]} ")
            return ReadSets(number(of.groupValues[1], MOST_SETS) ?: ONCE, rest)
        }
        val after = SETS_AFTER.find(text) ?: return ReadSets(ONCE, text)
        return ReadSets(number(after.groupValues[1], MOST_SETS) ?: ONCE, text.replaceRange(after.range, " "))
    }

    private fun minutes(text: String): HowMany? =
        MINUTES.find(text)?.let { minutesOf(number(it.groupValues[1], MOST_MINUTES)) }

    private fun seconds(text: String, sets: Int): HowMany? =
        (SECONDS.find(text) ?: HOLD_FOR.find(text))
            ?.let { holdOf(number(it.groupValues[1], MOST_SECONDS), sets) }

    private fun reps(text: String, sets: Int): HowMany? =
        (TIMES_BY.find(text) ?: REPS.find(text))
            ?.let { repsOf(number(it.groupValues[1], MOST_REPS), sets) }

    /**
     * A number with no unit beside it, read in the unit the movement is counted in.
     *
     * "Tandem stand 30" means thirty seconds because a tandem stand is a hold, and a
     * sheet that says it that way is not being unclear. This reads the unit and never
     * the number: the thirty is the therapist's and stays the therapist's.
     */
    private fun bare(text: String, movement: Movement?, sets: Int): HowMany {
        val found = ANY_NUMBER.find(text) ?: return HowMany.Unsaid
        return when (movement?.counted) {
            Counted.Minutes -> minutesOf(number(found.value, MOST_MINUTES))
            Counted.Hold -> holdOf(number(found.value, MOST_SECONDS), sets)
            else -> repsOf(number(found.value, MOST_REPS), sets)
        }
    }

    /**
     * A number, or nothing if it is outside what a person could be asked for.
     *
     * A scanned page carries phone numbers, dates and page numbers, and a plan item
     * saying two hundred and eight repetitions is worse than one with an empty box.
     * The line itself is kept either way, so nothing the therapist wrote is lost.
     */
    private fun number(text: String, most: Int): Int? =
        text.toIntOrNull()?.takeIf { it in 1..most }

    private fun repsOf(reps: Int?, sets: Int): HowMany =
        reps?.let { HowMany.Reps(it, sets) } ?: HowMany.Unsaid

    private fun holdOf(seconds: Int?, sets: Int): HowMany =
        seconds?.let { HowMany.Hold(it, sets) } ?: HowMany.Unsaid

    private fun minutesOf(minutes: Int?): HowMany =
        minutes?.let { HowMany.Minutes(it) } ?: HowMany.Unsaid

    /**
     * Number words to digits, before anything else is read.
     *
     * Spoken plans arrive as words: Part 6's own example is "ten sit to stands twice a
     * day". "One" is deliberately not in the table. This library has "one leg", "one
     * foot" and "one hand" in the names of five movements, and no therapist has ever
     * asked anybody for one repetition, so reading it as a number would cost more than
     * it could ever win.
     */
    private fun digits(text: String): String =
        text.lowercase()
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ", prefix = " ", postfix = " ") { NUMBER_WORDS[it] ?: it }

    // --- The table -----------------------------------------------------------

    /**
     * Phrases that mean one movement and nothing else in this library.
     *
     * Written the way people write them on a sheet or say them out loud, not the way
     * the library names them, which is why "STS", "chair stands" and "sit to stands"
     * are all here against the same movement. One spelling of each is enough: the line
     * and the table both go through the same plural rule, so "chair stands" covers
     * "chair stand" as well.
     */
    private val NAMED: Map<String, List<String>> = mapOf(
        "warm_march" to listOf("marching on the spot", "marching in place"),
        "warm_shoulder_rolls" to listOf("shoulder rolls", "shoulder circles"),
        "warm_ankle_circles" to listOf("ankle circles"),
        "warm_side_reach" to listOf("reaching side to side", "side reaches", "side bends"),
        "warm_heel_toe" to listOf("heel and toe", "heel and toe rocking"),
        "warm_seated_march" to listOf("seated marching", "sitting marching"),
        "sit_to_stand" to listOf("sit to stands", "sts", "chair stands", "chair rises"),
        "sit_to_stand_slow" to listOf("slow chair stands", "slow sit to stands"),
        "sit_to_stand_high" to listOf("stands from a high seat", "high seat stands", "high chair stands"),
        "sit_to_stand_one_arm" to listOf("chair stands one hand", "sit to stands one hand"),
        "mini_squat" to listOf("mini squats", "small squats", "half squats"),
        "step_up_low" to listOf("step ups"),
        "step_up_high" to listOf("step ups higher step", "higher step ups", "high step ups"),
        "half_kneel_to_stand" to listOf("half kneel to stand", "half kneeling to stand", "kneel to stand"),
        "floor_to_stand" to listOf("floor to stand", "getting up from the floor", "up from the floor"),
        "glute_bridge" to listOf("bridges", "glute bridges", "hip bridges", "bridging"),
        "seated_forward_lean" to listOf("forward lean and back", "seated forward lean", "forward leans"),
        "pressure_relief_lift" to listOf("lifts from the armrests", "pressure relief lifts", "armrest lifts"),
        "transfer_practice" to listOf("transfers", "transfer practice"),
        "sit_to_edge" to listOf("sitting up to the edge", "sit to the edge", "edge of bed sitting"),
        "walk" to listOf("walk", "walking"),
        "walk_brisk" to listOf("brisk walk", "brisk walking", "fast walking"),
        "step_in_place" to listOf("stepping on the spot", "step on the spot", "stepping in place"),
        "stairs" to listOf("stairs", "stair climbing", "climbing stairs"),
        "wheeling" to listOf("wheeling", "wheel yourself"),
        "slow_breaths" to listOf("slow breaths", "deep breaths", "breathing exercises"),
        "heel_slides" to listOf("heel slides"),
        "wall_push_up" to listOf("wall push ups", "wall press ups", "push ups against the wall"),
        "counter_push_up" to listOf("counter push ups", "kitchen counter push ups"),
        "incline_push_up" to listOf("incline push ups", "low incline push ups"),
        "band_row" to listOf("band rows", "resistance band rows", "rows with a band"),
        "band_press" to listOf("band press", "band chest press", "chest press with a band"),
        "carry_walk" to listOf("carrying something", "farmers carry", "carrying shopping"),
        "towel_squeeze" to listOf("towel squeezes", "grip squeezes", "hand squeezes"),
        "overhead_reach" to listOf("overhead reaches", "reaching overhead", "arms overhead"),
        "feet_together" to listOf("feet together"),
        // "Semi tandem stand" has to be here whole. Half of it names one movement and
        // the other half names another, and neither half is inside the other.
        "semi_tandem" to listOf("semi tandem stand", "semi tandem"),
        "tandem" to listOf("tandem stand", "heel to toe stand"),
        "one_leg" to listOf("one leg stand", "single leg stand", "standing on one leg", "one foot"),
        "one_leg_head_turns" to listOf("one leg with head turns", "head turns", "one foot looking around"),
        "heel_raises" to listOf("heel raises", "calf raises", "heel lifts"),
        "heel_toe_walk" to listOf("heel to toe walking", "heel to toe walk", "tandem walking"),
        "seated_balance" to listOf("sitting unsupported", "unsupported sitting", "seated balance"),
        "ankle_pumps" to listOf("ankle pumps"),
        "seated_rotation" to listOf("turning to look behind", "seated rotation", "trunk rotation"),
        "cool_walk" to listOf("easy walking", "gentle walking", "easy walk", "gentle walk"),
        "cool_calf_stretch" to listOf("calf stretches"),
        "cool_chest_stretch" to listOf("chest stretches", "doorway stretches"),
        "cool_seated_breathing" to listOf("sitting and breathing", "seated breathing"),
        "cool_thigh_stretch" to listOf("thigh stretches", "quad stretches", "quadriceps stretches"),
    )

    /**
     * Phrases that name a family rather than a movement.
     *
     * A therapist writing "push ups" has one of three in mind and the sheet does not
     * say which, so the app proposes the gentlest of them and the screen shows the
     * proposal as a guess. Every one of these is a phrase the library has a more
     * specific spelling of in [NAMED], and the longer spelling always wins.
     */
    private val LIKELY: Map<String, List<String>> = mapOf(
        "wall_push_up" to listOf("push ups", "press ups"),
        "mini_squat" to listOf("squats"),
        "band_row" to listOf("rows"),
        "warm_march" to listOf("marching"),
        "slow_breaths" to listOf("breathing", "deep breathing"),
        "tandem" to listOf("tandem"),
        "carry_walk" to listOf("carrying"),
    )

    /**
     * Words that end in s and are not plurals of anything, plus the short plurals the
     * general rule leaves alone because taking a letter off a three letter word turns
     * a phone number into a movement.
     */
    private val SHORT_PLURALS: Map<String, String> = mapOf(
        "ups" to "up",
        "reps" to "rep",
        "sets" to "set",
        "rows" to "row",
        "legs" to "leg",
        "arms" to "arm",
        "hips" to "hip",
        "toes" to "toe",
        "secs" to "sec",
        "mins" to "min",
    )

    private val ES_PLURALS = listOf("ches", "shes", "sses", "xes")

    private val PHRASES: List<Phrase> =
        NAMED.flatMap { (id, words) -> phrases(id, words, Sureness.Named) } +
            LIKELY.flatMap { (id, words) -> phrases(id, words, Sureness.Likely) }

    private fun phrases(id: String, words: List<String>, sureness: Sureness): List<Phrase> {
        val movement = Movements.byId(id) ?: return emptyList()
        return words.map { Phrase(normalise(it), movement, sureness) }
    }

    /** Spoken numbers. One is deliberately absent, and [digits] says why. */
    private val NUMBER_WORDS: Map<String, String> = mapOf(
        "once" to "1 time",
        "twice" to "2 times",
        "two" to "2",
        "three" to "3",
        "four" to "4",
        "five" to "5",
        "six" to "6",
        "seven" to "7",
        "eight" to "8",
        "nine" to "9",
        "ten" to "10",
        "eleven" to "11",
        "twelve" to "12",
        "fifteen" to "15",
        "twenty" to "20",
        "thirty" to "30",
        "forty" to "40",
        "sixty" to "60",
    )

    private val LIST_MARKER = Regex("""^(?:[-*•]|\d+\s*[.)])\s*""")
    private val AND = Regex("""\s+and\s+""", RegexOption.IGNORE_CASE)
    private val EACH_SIDE = Regex("""(?:each|both|per)\s+(?:side|leg|arm|hand|way)s?""")
    private val EVERY_OTHER_DAY = Regex("""every other day""")
    private val TIMES_A_DAY = Regex("""(\d+)\s*times?\s*(?:a|per|each)\s*day""")
    private val TIMES_DAILY = Regex("""(\d+)\s*times?\s*daily""")
    private val EVERY_DAY = Regex("""\b(?:daily|every day|each day)\b""")
    private val MORNING_AND = Regex("""morning\s+and\s+(?:evening|night|afternoon)""")
    private val TIMES_A_WEEK = Regex("""(\d+)\s*times?\s*(?:a|per|each)\s*week""")
    private val WEEKLY = Regex("""\bweekly\b""")
    private val SETS_OF = Regex("""(\d+)\s*(?:sets?|x)\s*(?:of\s+)?(\d+)""")
    private val SETS_AFTER = Regex("""(\d+)\s*sets?\b""")
    private val MINUTES = Regex("""(\d+)\s*(?:minutes?|mins?)\b""")
    private val SECONDS = Regex("""(\d+)\s*(?:seconds?|secs?)\b""")
    private val HOLD_FOR = Regex("""hold\s+(?:for\s+)?(\d+)""")
    private val TIMES_BY = Regex("""\bx\s*(\d+)""")
    private val REPS = Regex("""(\d+)\s*(?:reps?|repetitions?)\b""")
    private val ANY_NUMBER = Regex("""\d+""")

    /**
     * The words that turn a line into something not to do.
     *
     * Kept short and kept whole words. Every one of them costs the app a match it
     * might have got right, and that is the trade the whole file is built on: a line
     * the person confirms by hand costs a tap, and "no stairs" read as stairs costs
     * them the one thing their therapist wrote the line to prevent.
     */
    private val SAID_NOT_TO = Regex("""\b(?:avoid|avoiding|no|not|never|dont|instead|rather)\b""")

    /**
     * A number sat straight against a stretch of time by an x, which the app leaves.
     *
     * "2 x daily" is twice a day to one person and two repetitions a day to the next,
     * and an x means repetitions elsewhere on the same sheets ("heel raises x15"), so
     * there is nothing here to be sure about. The whole shape comes out and both boxes
     * stay empty. [X_A_DAY] is the same notation with an article in it, which settles
     * it: "2 x a day" is a rate and cannot be read any other way.
     */
    private val X_RATE = Regex("""\d+\s*x\s*(?:day|daily|week|weekly)\b""")

    private val X_A_DAY = Regex("""(\d+)\s*x\s*(?:a|per|each)\s+day\b""")
    private val X_A_WEEK = Regex("""(\d+)\s*x\s*(?:a|per|each)\s+week\b""")

    /** A rate with a period [HowOften] has no case for, such as five times an hour. */
    private val UNKNOWN_RATE = Regex("""\d+\s*times?\s+(?:a|an|per|each)\s+\w+""")

    private const val ONCE = 1
    private const val TWICE = 2

    /** More than this many times a day or a week is a misread rather than an ask. */
    private const val MOST_TIMES = 12

    private const val MOST_SETS = 10
    private const val MOST_REPS = 200
    private const val MOST_SECONDS = 600
    private const val MOST_MINUTES = 300
    private const val SHORTEST_PLURAL = 5
}
