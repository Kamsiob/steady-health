package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.RunRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.domain.Exclusion
import com.kamsiob.steadyhealth.domain.GettingAround
import com.kamsiob.steadyhealth.session.Adaptation
import com.kamsiob.steadyhealth.session.Movements
import com.kamsiob.steadyhealth.session.Noticed
import com.kamsiob.steadyhealth.session.SessionEngine
import com.kamsiob.steadyhealth.session.SessionInputs
import com.kamsiob.steadyhealth.ui.components.SessionCardState

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
        val plan = SessionEngine.plan(
            SessionInputs(
                way = way,
                exclusions = exclusions,
                kit = profile.kit(),
                sore = runs.soreAreas(today),
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
    suspend fun noticedLine(today: Long): String? =
        when (val noticed = Noticed.of(runs.history(), today)) {
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

            null -> null
        }

    private fun string(@StringRes id: Int, vararg args: Any): String =
        application.getString(id, *args)

    private fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        application.resources.getQuantityString(id, count, *args)
}
