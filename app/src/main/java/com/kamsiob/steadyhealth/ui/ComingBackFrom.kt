package com.kamsiob.steadyhealth.ui

import android.app.Application
import androidx.annotation.StringRes
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.RunRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.session.Answered
import com.kamsiob.steadyhealth.session.Aside
import com.kamsiob.steadyhealth.session.ComingBack
import com.kamsiob.steadyhealth.session.ComingBackEngine
import com.kamsiob.steadyhealth.session.Gap
import com.kamsiob.steadyhealth.session.WhyAway
import java.time.LocalDate
import java.time.ZoneId

/**
 * Coming back after a while away. ADDENDUM-03 Part 15.
 *
 * Its own class rather than more of SteadyViewModel, which reached detekt's size rule
 * for the fifth time. Everything here is one subject: whether to ask, what the answer
 * changes, and the two sentences that follow it.
 *
 * The rule itself is in [ComingBackEngine] and is tested without a device. This reads
 * what it needs and writes back the one answer.
 */
class ComingBackFrom(private val application: Application, private val db: SteadyDatabase) {

    private val profile get() = ProfileRepository(db)
    private val runs get() = RunRepository(db)

    /**
     * The one answer to "what has been happening?". ADDENDUM-03 Part 15.
     *
     * The answer is stored against the gap it answers, so the question is asked once
     * per gap and never again for that one, whichever of the four was tapped.
     */
    suspend fun answer(why: WhyAway) {
        val last = runs.lastSessionDay() ?: return
        val easing = ComingBackEngine.answer(why, gapNow(last))
        profile.setWhyAway(why.id, last, today())
        if (Aside.WorthAWord in easing.asides) profile.setSaidWorthAWord(true)
    }

    private suspend fun gapNow(lastSessionDay: Long): Gap = Gap(
        today = today(),
        lastSessionDay = lastSessionDay,
        answered = profile.whyAway()?.let { (id, after, on) ->
            WhyAway.entries.firstOrNull { it.id == id }?.let { why ->
                Answered(why = why, afterLastSessionDay = after, onDay = on)
            }
        },
        saidWorthAWordBefore = profile.saidWorthAWord(),
    )

    /** Whether to ask, and what has already been settled. */
    suspend fun now(): ComingBack {
        val last = runs.lastSessionDay() ?: return ComingBack.Ordinary
        return ComingBackEngine.of(gapNow(last))
    }

    /**
     * What is said after the answer, and then not again.
     *
     * Two sentences at most, both of them Part 15's own words, and neither of them
     * about a day nobody did anything.
     */
    fun said(back: ComingBack): List<String> {
        val settled = back as? ComingBack.Settled ?: return emptyList()
        return listOfNotNull(
            string(R.string.back_unwell_said)
                .takeIf { Aside.TakingItSlowly in settled.easing.asides },
            string(R.string.back_worth_a_word)
                .takeIf { Aside.WorthAWord in settled.easing.asides },
        )
    }

    private fun string(@StringRes id: Int, vararg args: Any): String =
        application.getString(id, *args)

    private fun today(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
}
