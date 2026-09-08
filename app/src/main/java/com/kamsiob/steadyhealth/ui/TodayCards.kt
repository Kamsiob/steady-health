package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.AbilityRepository
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.RunRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.domain.AbilityDomain
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.session.Adaptation
import com.kamsiob.steadyhealth.session.Kit
import com.kamsiob.steadyhealth.session.LookBacks
import com.kamsiob.steadyhealth.session.Movements
import com.kamsiob.steadyhealth.session.Noticed
import com.kamsiob.steadyhealth.session.Piece
import com.kamsiob.steadyhealth.session.SessionEngine
import com.kamsiob.steadyhealth.session.SessionInputs
import com.kamsiob.steadyhealth.session.Week
import com.kamsiob.steadyhealth.ui.components.SessionCardState
import com.kamsiob.steadyhealth.ui.screens.LibraryRow
import com.kamsiob.steadyhealth.ui.screens.SundayUiState
import com.kamsiob.steadyhealth.ui.screens.WeekBar
import java.time.LocalDate
import java.time.ZoneId

/**
 * The two things on Today that come from the session engine.
 *
 * Their own class rather than more of SteadyViewModel, because everything here turns
 * an engine answer into a sentence and none of it holds state. It is also the only
 * place that decides what the card says, so the card and the session that runs cannot
 * drift apart.
 */
class TodayCards(private val application: Application, private val db: SteadyDatabase) {

    private val runs get() = RunRepository(db)
    private val profile get() = ProfileRepository(db)

    /**
     * Today's session, as the card says it.
     *
     * Planned by the same call the Start button makes, so the card cannot promise a
     * session different from the one that runs.
     */
    suspend fun sessionCard(
        today: Long,
        way: GettingAround,
        exclusions: Set<Exclusion>,
    ): SessionCardState {
        val history = runs.history()
        val sore = runs.soreAreas(today)
        val plan = SessionEngine.plan(
            SessionInputs(
                way = way,
                exclusions = exclusions,
                kit = profile.kit(),
                sore = sore,
                history = history,
                lastFelt = runs.lastFelt(),
                feltBefore = runs.feltBefore(),
                today = today,
                lastSessionDay = runs.lastSessionDay(),
                strengthRunLength = runs.strengthRunLength(),
            ),
        )
        if (plan.steps.isEmpty()) {
            return SessionCardState(length = string(R.string.card_nothing))
        }
        return SessionCardState(
            length = plural(R.plurals.card_minutes, plan.minutes, plan.minutes),
            // The main movements only. A warm up and a cool down are named in one
            // line under them, because six lines on a card is a list rather than a
            // card and nobody reads the sixth.
            movements = plan.main.map { it.movement.name },
            bookends = bookends(plan.hasWarmUp, plan.hasCoolDown),
            feeds = plan.feeds
                .takeIf { it.isNotEmpty() }
                ?.joinToString(", ") { string(Labels.forAbility(it)) }
                ?.let { string(R.string.card_feeds, it) },
            adaptation = adaptationLine(plan.adaptation),
            doneToday = history.any { it.epochDay == today },
            // ADDENDUM-03 Part 2: the next offer says which area was left out, so
            // nobody has to wonder why a movement they know went missing.
            leftOut = sore.firstOrNull()
                ?.let { string(R.string.hurt_left_out, string(Labels.forArea(it)).lowercase()) },
        )
    }

    private fun bookends(warmUp: Boolean, coolDown: Boolean): String? = when {
        warmUp && coolDown -> string(R.string.card_with_both)
        warmUp -> string(R.string.card_with_warm_up)
        coolDown -> string(R.string.card_with_cool_down)
        else -> null
    }

    /** One sentence per adaptation, chosen by the engine and never by a model. */
    private fun adaptationLine(adaptation: Adaptation): String? = when (adaptation.kind) {
        Adaptation.Kind.Lighter -> string(R.string.adapt_lighter)
        Adaptation.Kind.Swapped ->
            adaptation.movementName?.let { string(R.string.adapt_swapped, it.lowercase()) }

        Adaptation.Kind.Shorter -> string(R.string.adapt_shorter)
        Adaptation.Kind.OneMore -> string(R.string.adapt_one_more)
        Adaptation.Kind.EasyDay -> string(R.string.adapt_easy_day)
        Adaptation.Kind.AfterUnwell -> string(R.string.adapt_after_unwell)
        Adaptation.Kind.None -> null
    }

    /**
     * The one line the app noticed, or nothing.
     *
     * The rule that chose it is in [Noticed] and is tested without a device. This only
     * turns the answer into a sentence.
     */
    /**
     * Where the week is. ADDENDUM-03 Part 10.
     *
     * Days with a session rather than sessions, because somebody who does three on a
     * Sunday has not had a week of three.
     */
    suspend fun weekLine(today: Long): String {
        val week = Week.of(runs.history(), today, profile.weekTarget())
        return when {
            week.met -> string(R.string.week_met, week.wanted)
            week.done == 0 -> string(R.string.week_none, week.wanted)
            week.toGo == 1 -> plural(R.plurals.week_one_more, week.done, week.done, 1, week.wanted)
            else -> plural(R.plurals.week_so_far, week.done, week.done, week.toGo, week.wanted)
        }
    }

    /**
     * What they said they want, in their words, and one number of their own.
     *
     * ADDENDUM-03 Part 9's "next thing", built from what is actually known. The app
     * does not say how far there is to go: the thresholds for that are research
     * MASTER_SPEC 11 marks open, and a made up one would be the app inventing a
     * finish line for somebody else's life.
     */
    suspend fun theNextThing(): Pair<String, String?>? {
        val item = AbilityRepository(db).items().firstOrNull() ?: return null
        val yours = string(R.string.want_yours, item.text)
        val domain = AbilityDomain.entries.firstOrNull { it.id == item.domain }
            ?: return yours to null
        val last = runs.history()
            .filter { Movements.byId(it.movementId)?.domain == domain }
            .maxByOrNull { it.epochDay }
            ?: return yours to null
        val name = Movements.byId(last.movementId)?.name ?: return yours to null
        return yours to string(R.string.want_where, name, last.result)
    }

    suspend fun noticedLines(today: Long): List<String> =
        Noticed.all(runs.history(), today, profile.anchorDay()).map { say(it) }

    private fun say(noticed: Noticed): String =
        when (noticed) {
            is Noticed.Climbed -> string(
                R.string.noticed_climbed,
                Movements.byId(noticed.movementId)?.name.orEmpty(),
                noticed.to,
                noticed.from,
            )

            is Noticed.MoreDaysThanLastWeek -> plural(
                R.plurals.noticed_more_days,
                noticed.thisWeek,
                noticed.thisWeek,
                noticed.lastWeek,
            )

            is Noticed.HowMany -> plural(
                R.plurals.noticed_how_many,
                noticed.sessions,
                noticed.sessions,
            )

            is Noticed.DayNumber -> string(R.string.noticed_day, noticed.day)
        }

    /**
     * The library: everything this person could be offered, and what each one needs.
     *
     * Suppressed areas are shown rather than hidden, marked as left out for now, so
     * nobody has to wonder where a movement they know went. Movements for another way
     * of getting around are not here at all, because they are not theirs.
     */
    suspend fun library(way: GettingAround, exclusions: Set<Exclusion>, today: Long): List<LibraryRow> {
        val sore = runs.soreAreas(today)
        val kit = profile.kit()
        val mine = Movements.all.filter { way in it.ways && it.piece == Piece.Main }
        return mine.map { movement ->
            LibraryRow(
                id = movement.id,
                name = movement.name,
                feeds = string(R.string.library_for, string(Labels.forAbility(movement.domain))),
                needs = string(needs(movement.kit)),
                leftOut = movement.area in sore ||
                    movement.excludedBy.any { it in exclusions } ||
                    !movement.kit.all { it == Kit.None || it in kit },
            )
        }
    }

    private fun needs(kit: Set<Kit>): Int = when {
        Kit.Chair in kit -> R.string.library_needs_chair
        Kit.Step in kit -> R.string.library_needs_step
        Kit.Wall in kit -> R.string.library_needs_wall
        Kit.Floor in kit -> R.string.library_needs_floor
        Kit.Band in kit -> R.string.library_needs_band
        Kit.Weights in kit -> R.string.library_needs_weight
        else -> R.string.library_needs_nothing
    }

    /**
     * The last four weeks, each against the number the person chose. Part 10.
     *
     * Four separate answers. Nothing joins them and nothing totals them, which is
     * what makes this a picture of a month rather than a run of days.
     */
    suspend fun weekBars(): List<WeekBar> = fourWeeks().mapIndexed { at, week ->
        WeekBar(
            done = week.done,
            wanted = week.wanted,
            spoken = string(R.string.weeks_bar, at + 1, week.done, week.wanted),
        )
    }

    /**
     * One sentence about those four weeks, and never about a run of them.
     *
     * "Three good weeks and one quiet one. That's what most months look like." The
     * second half is the whole point: it says a quiet week is ordinary rather than a
     * thing that went wrong.
     */
    suspend fun weeksSaid(): String {
        val weeks = fourWeeks()
        val met = weeks.count { it.met }
        val quiet = weeks.size - met
        return when {
            quiet == 0 -> string(R.string.weeks_all)
            met == 0 -> string(R.string.weeks_none)
            quiet == 1 -> plural(R.plurals.weeks_most, met, met, quiet)
            else -> plural(R.plurals.weeks_some, met, met, quiet)
        }
    }

    /** The same thing then and now, in their own history. Part 9. */
    suspend fun lookBackLine(): String? {
        val back = LookBacks.of(runs.history(), todayEpochDay()) ?: return null
        val name = Movements.byId(back.movementId)?.name ?: return null
        return string(R.string.look_back_line, name, back.then, back.now)
    }

    /**
     * The Sunday review. ADDENDUM-03 Part 10.
     *
     * Everything in it already happened except the last line, which names one day
     * that would meet the week. One concrete thing rather than a plan, because a plan
     * for somebody else's week is the app deciding what their Thursday is for.
     */
    suspend fun sunday(today: Long, dayNames: List<String>): SundayUiState {
        val history = runs.history()
        val week = Week.of(history, today, profile.weekTarget())
        val since = today - Week.DAYS + 1
        val movements = history.filter { it.epochDay in since..today }
            .mapNotNull { Movements.byId(it.movementId)?.name }
            .distinct()

        return SundayUiState(
            did = listOfNotNull(
                plural(R.plurals.sunday_days, week.done, week.done),
                movements.takeIf { it.isNotEmpty() }?.joinToString(", "),
            ),
            moved = lookBackLine(),
            noticed = Noticed.of(history, today)?.let { say(it) },
            ahead = ahead(week, dayNames),
        )
    }

    /**
     * One day that would meet the week, named.
     *
     * The day after tomorrow if there is room for it, because naming today is not
     * looking forward and naming a day already gone is worse than saying nothing.
     */
    private fun ahead(week: Week, dayNames: List<String>): String {
        if (week.met) return string(R.string.sunday_ahead_met)
        val ahead = dayNames.getOrNull(1) ?: return string(R.string.sunday_ahead_met)
        return string(R.string.sunday_concrete, ahead, week.done + 1)
    }

    private suspend fun fourWeeks(): List<Week> =
        Week.fourWeeks(runs.history(), todayEpochDay(), profile.weekTarget())

    private fun todayEpochDay(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    private fun string(@StringRes id: Int, vararg args: Any): String =
        application.getString(id, *args)

    private fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        application.resources.getQuantityString(id, count, *args)
}
