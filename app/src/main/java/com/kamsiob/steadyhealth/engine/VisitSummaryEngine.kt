package com.kamsiob.steadyhealth.engine

import com.kamsiob.steadyhealth.ai.Fact
import com.kamsiob.steadyhealth.ai.QuestionCandidate
import com.kamsiob.steadyhealth.ai.SummaryValidator
import com.kamsiob.steadyhealth.ai.VisitBrief
import com.kamsiob.steadyhealth.ai.VisitWindow
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.AbilityState
import java.time.LocalDate

/** One thing the person said they wanted, then and now. */
data class ItemHistory(
    val text: String,
    val firstRating: Int,
    val firstDay: Long,
    val lastRating: Int,
    val lastDay: Long,
)

/** One tag, how many days it appeared on, and the last few sentences carrying it. */
data class Mention(val tag: String, val days: Int, val recent: List<String>)

/** Days moved and minutes, for one month of the window. */
data class MonthOfSessions(val month: String, val daysMoved: Int, val minutes: Int)

/** A stretch with no sessions in it. */
data class Gap(val fromDay: Long, val toDay: Long) {
    val days: Int get() = (toDay - fromDay).toInt()
}

/** Everything the repository gathered, before any of it has been chosen. */
data class VisitInputs(
    val window: VisitWindow,
    val weeks: Int,
    val checkCount: Int,
    val results: List<MeasureResult>,
    val items: List<ItemHistory>,
    val mentions: List<Mention>,
    val months: List<MonthOfSessions>,
    val gaps: List<Gap>,
    val currentStep: String,
    val weightFirstKg: Double?,
    val weightLastKg: Double?,
    val weighInCount: Int,
    val fastestWeeklyLossKg: Double?,
    val abilityNames: Map<AbilityDomain, String>,
    val measureNames: Map<String, String>,
    val stateNames: Map<AbilityState, String>,
    val pacing: Boolean,
    val anyExclusions: Boolean,
    val wayChanged: Boolean,
)

/**
 * The visit summary brief, assembled by the engine and nothing else.
 *
 * ADDENDUM-01, folded into LOGIC.md 13b: the engine does all selection, all
 * arithmetic, and all thresholding, and hands the model a structured brief where
 * every fact carries an id. The model never computes and never states a number it
 * was not given.
 *
 * So every sentence below is written here, with the numbers already worked out,
 * and the model's whole job is to say the same things in fewer words. If the
 * model is not installed, these sentences are the summary, which is why they are
 * written to be read rather than to be parsed.
 */
object VisitSummaryEngine {

    /** A tag has to appear on this many days to be mentioned at all. LOGIC.md 13b. */
    const val TAG_DAYS = 5

    /** A tracked item moving this far is a change rather than noise. */
    const val ITEM_POINTS = 2

    /** A gap this long is worth stating. */
    const val GAP_DAYS = 14

    /** A gap this long, with a body tag in it, is worth asking about. */
    const val LONG_GAP_DAYS = 30

    /** Losing faster than this, averaged over four weeks, is worth asking about. */
    const val FAST_LOSS_KG_PER_WEEK = 1.5

    /** The body tags, which are always passed if they appeared at all. */
    val bodyTags = setOf("sore", "pain", "unwell")

    /** True when there is enough to say anything at all. LOGIC.md 13b. */
    fun enough(inputs: VisitInputs): Boolean =
        inputs.weeks >= VisitBrief.LEAST_WEEKS && inputs.checkCount >= VisitBrief.LEAST_CHECKS

    fun brief(inputs: VisitInputs): VisitBrief {
        val facts = buildList {
            addAll(abilityFacts(inputs))
            addAll(measureFacts(inputs))
            addAll(itemFacts(inputs))
            addAll(mentionFacts(inputs))
            addAll(sessionFacts(inputs))
            addAll(leverFacts(inputs))
        }
        return VisitBrief(
            window = inputs.window,
            facts = facts,
            candidates = candidates(inputs, facts),
            pacing = inputs.pacing,
            anyExclusions = inputs.anyExclusions,
            wayChanged = inputs.wayChanged,
        )
    }

    /** All four, always, including Same. LOGIC.md 13b. */
    private fun abilityFacts(inputs: VisitInputs): List<Fact> =
        AbilityDomain.entries.map { domain ->
            val state = AbilityEngine.stateOf(domain, inputs.results)
            val name = inputs.abilityNames[domain] ?: domain.id
            Fact(
                id = "ability_${domain.id}",
                kind = "ability",
                text = "$name is ${inputs.stateNames[state] ?: state.id}.",
            )
        }

    /** Every measure taken at least twice. Never a percentage, never a projection. */
    private fun measureFacts(inputs: VisitInputs): List<Fact> = inputs.results
        .groupBy { it.measureId }
        .mapNotNull { (id, taken) ->
            val measure = Measures.byId(id) ?: return@mapNotNull null
            if (taken.size < 2) return@mapNotNull null
            val sorted = taken.sortedBy { it.epochDay }
            val first = sorted.first()
            val last = sorted.last()
            val name = inputs.measureNames[id] ?: id
            val change = MeasureChange(measure, first, last)
            Fact(
                id = "measure_$id",
                kind = "measure",
                text = "$name went from ${whole(first.value)} in ${monthOf(first.epochDay)} " +
                    "to ${whole(last.value)} in ${monthOf(last.epochDay)}, " +
                    "taken ${taken.size} times." +
                    if (change.beyondNoise) "" else " That is inside the range this measure moves in anyway.",
                numbers = listOf(first.value, last.value, taken.size.toDouble()),
                months = listOf(monthOf(first.epochDay), monthOf(last.epochDay)),
            )
        }

    /** The person's own list, with their own words in quotation marks. */
    private fun itemFacts(inputs: VisitInputs): List<Fact> = inputs.items.mapIndexed { index, item ->
        val moved = kotlin.math.abs(item.lastRating - item.firstRating) >= ITEM_POINTS
        Fact(
            id = "item_$index",
            kind = "item",
            text = "On their own list, \"${item.text}\" went from ${item.firstRating} in " +
                "${monthOf(item.firstDay)} to ${item.lastRating} in ${monthOf(item.lastDay)}." +
                if (moved) "" else " That is within the range these ratings move in anyway.",
            numbers = listOf(item.firstRating.toDouble(), item.lastRating.toDouble()),
            months = listOf(monthOf(item.firstDay), monthOf(item.lastDay)),
            quotes = listOf(item.text),
        )
    }

    /**
     * What the person mentioned, with up to three of their own sentences.
     *
     * A tag has to have appeared on five days to be here, except the body tags,
     * which are passed whenever they appeared at all. That asymmetry is
     * deliberate: somebody writing "my knee" twice in six months is worth a
     * doctor knowing, and somebody writing "cooked" twice is not.
     */
    private fun mentionFacts(inputs: VisitInputs): List<Fact> = inputs.mentions
        .filter { it.days >= TAG_DAYS || it.tag in bodyTags }
        .map { mention ->
            Fact(
                id = "mention_${mention.tag}",
                kind = "mention",
                text = "${label(mention.tag)} came up on ${mention.days} days.",
                numbers = listOf(mention.days.toDouble()),
                quotes = mention.recent.take(MOST_QUOTES),
            )
        }

    private fun sessionFacts(inputs: VisitInputs): List<Fact> = buildList {
        inputs.months.forEach { month ->
            add(
                Fact(
                    id = "sessions_${month.month.lowercase()}",
                    kind = "sessions",
                    text = "In ${month.month} they moved on ${month.daysMoved} days, " +
                        "${month.minutes} minutes in all.",
                    numbers = listOf(month.daysMoved.toDouble(), month.minutes.toDouble()),
                    months = listOf(month.month),
                ),
            )
        }
        if (inputs.currentStep.isNotBlank()) {
            add(
                Fact(
                    id = "current_step",
                    kind = "sessions",
                    text = "What they are doing now: ${inputs.currentStep}.",
                    // The step's own name carries a number ("A 14 minute walk"),
                    // and a fact has to declare every number in it or the
                    // validator is right to reject a summary that repeats one.
                    numbers = SummaryValidator.digitsIn(inputs.currentStep),
                ),
            )
        }
        inputs.gaps.filter { it.days >= GAP_DAYS }.forEachIndexed { index, gap ->
            add(
                Fact(
                    id = "gap_$index",
                    kind = "sessions",
                    text = "There was a gap of ${gap.days} days, from ${monthOf(gap.fromDay)} " +
                        "to ${monthOf(gap.toDay)}.",
                    numbers = listOf(gap.days.toDouble()),
                    months = listOf(monthOf(gap.fromDay), monthOf(gap.toDay)),
                ),
            )
        }
    }

    /**
     * The levers, with weight as a direction word.
     *
     * The smoothed values are in the fact because LOGIC.md 13b puts them in the
     * measures table, and the table is part of the same export. The direction word
     * is what the sentences use.
     */
    private fun leverFacts(inputs: VisitInputs): List<Fact> = buildList {
        val first = inputs.weightFirstKg
        val last = inputs.weightLastKg
        if (first != null && last != null) {
            val direction = when {
                last < first - WeightEngine.SAME_BAND_KG -> "a little lower than at the start"
                last > first + WeightEngine.SAME_BAND_KG -> "a little higher than at the start"
                else -> "about the same as at the start"
            }
            add(
                Fact(
                    id = "weight",
                    kind = "lever",
                    text = "Their weight is $direction, from ${inputs.weighInCount} weigh-ins.",
                    numbers = listOf(inputs.weighInCount.toDouble()),
                ),
            )
        }
    }

    /**
     * What is worth asking about, by the seven rules in LOGIC.md 13b and nothing
     * else.
     *
     * The model may reword one of these. It may not add one, and the validator
     * drops anything in its output that does not carry one of these ids. That is
     * the whole guard between a summary and a language model inventing a concern
     * for somebody to take to a doctor.
     */
    private fun candidates(inputs: VisitInputs, facts: List<Fact>): List<QuestionCandidate> =
        buildList {
            // 1. A body tag on five or more days.
            inputs.mentions.filter { it.tag in bodyTags && it.days >= TAG_DAYS }.forEach {
                add(
                    QuestionCandidate(
                        id = "q_body_${it.tag}",
                        rule = 1,
                        text = "You mentioned ${label(it.tag).lowercase()} on ${it.days} days. " +
                            "Is that worth looking at?",
                        evidence = listOf("mention_${it.tag}"),
                    ),
                )
            }

            // 2. A measure quieter across three consecutive checks.
            AbilityDomain.entries.filter { AbilityEngine.quieterRun(it, inputs.results) }.forEach { domain ->
                val name = inputs.abilityNames[domain] ?: domain.id
                add(
                    QuestionCandidate(
                        id = "q_quieter_${domain.id}",
                        rule = 2,
                        text = "$name has gone one way for three checks. Is that worth a look?",
                        evidence = listOf("ability_${domain.id}"),
                    ),
                )
            }

            // 3. A tracked item that fell by two points or more.
            inputs.items.forEachIndexed { index, item ->
                if (item.firstRating - item.lastRating >= ITEM_POINTS) {
                    add(
                        QuestionCandidate(
                            id = "q_item_$index",
                            rule = 3,
                            text = "\"${item.text}\" is harder than it was. Worth mentioning?",
                            evidence = listOf("item_$index"),
                        ),
                    )
                }
            }

            // 5. Weight falling faster than one and a half kilos a week.
            inputs.fastestWeeklyLossKg?.takeIf { it > FAST_LOSS_KG_PER_WEEK }?.let {
                add(
                    QuestionCandidate(
                        id = "q_fast_loss",
                        rule = 5,
                        text = "Your weight came down quickly for a stretch. Worth checking?",
                        evidence = listOfNotNull(facts.firstOrNull { f -> f.id == "weight" }?.id),
                    ),
                )
            }

            // 6. A month or more away, with a body tag logged in the same period.
            val longGap = inputs.gaps.any { it.days >= LONG_GAP_DAYS }
            val bodyMention = inputs.mentions.any { it.tag in bodyTags && it.days > 0 }
            if (longGap && bodyMention) {
                add(
                    QuestionCandidate(
                        id = "q_gap_body",
                        rule = 6,
                        text = "There was a long gap, and something was bothering you around then. " +
                            "Worth raising?",
                        evidence = facts.filter { it.id.startsWith("gap_") }.map { it.id },
                    ),
                )
            }

            // 7. A measure improved while the person's own rating did not.
            val improved = AbilityDomain.entries.any {
                AbilityEngine.stateOf(it, inputs.results) == AbilityState.Better
            }
            val itemHeldOrFell = inputs.items.any { it.lastRating <= it.firstRating }
            if (improved && itemHeldOrFell) {
                add(
                    QuestionCandidate(
                        id = "q_disagree",
                        rule = 7,
                        text = "A number went up while it did not feel any easier. Worth saying so?",
                        evidence = facts.filter { it.kind == "item" }.map { it.id },
                    ),
                )
            }
        }

    private fun whole(value: Double): String = value.toInt().toString()

    private fun monthOf(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).month.name.lowercase().replaceFirstChar { it.uppercase() }

    /** A tag's own word, for a sentence. Not the person's; the app's. */
    private fun label(tag: String): String =
        tag.split("_").joinToString(" ").replaceFirstChar { it.uppercase() }

    /** Three sentences per tag, and no more. LOGIC.md 13b. */
    private const val MOST_QUOTES = 3
}
