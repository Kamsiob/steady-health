package com.kamsiob.steadyhealth.remind

import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.data.DailyPromptRepository
import com.kamsiob.steadyhealth.data.PlanRepository
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.ReminderRepository
import com.kamsiob.steadyhealth.data.RunRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.engine.DailyPrompt
import com.kamsiob.steadyhealth.engine.Prompt
import com.kamsiob.steadyhealth.engine.ReminderKind
import com.kamsiob.steadyhealth.engine.Reminders
import com.kamsiob.steadyhealth.plan.ReviewDate
import com.kamsiob.steadyhealth.plan.ReviewPrompt
import com.kamsiob.steadyhealth.session.SessionEngine
import com.kamsiob.steadyhealth.session.SessionInputs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * The one thing that ever sends a reminder.
 *
 * It wakes once a day, works out what is due, asks the ceiling whether anything
 * may go out at all, sends at most one, and records it. Every decision it makes
 * that could be wrong lives in a pure function it calls, because a scheduler is a
 * thing that runs at seven in the morning with nobody watching.
 *
 * Nothing here reads a missed day, because there is no rule in this app that
 * fires on one and no string that mentions one.
 */
class ReminderWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    @RequiresPermission(Reminding.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!Reminding.allowed(context)) return Result.success()

        val db = SteadyDatabase.get(context)
        val profile = ProfileRepository(db)
        val reminders = ReminderRepository(db)
        val now = System.currentTimeMillis()

        // The one daily prompt goes first and outside the ceiling. The ceiling is
        // what keeps everything else from adding up to noise; this is the one the
        // app is for, and it stops itself when it is being ignored.
        if (daily(context, db, profile)) return Result.success()

        if (!Reminders.maySend(reminders.sentSince(now - A_WEEK), now)) return Result.success()

        val plans = PlanRepository(db)
        val appointment = appointment(profile, plans, LocalDate.now(ZoneId.systemDefault()))
        val kind = Reminders.pick(due(profile, appointment)) ?: return Result.success()
        val sent = Reminding.send(context, context.getString(Reminding.words(kind)))
        // Only a reminder that actually went out counts against the ceiling. One
        // the system swallowed should not cost somebody their week's allowance.
        if (sent) reminders.record(kind, now)
        // And the appointment is only written down as said once it has been said,
        // for the same reason. Its window is two days wide so that a day lost to
        // the ceiling is not the prompt lost with it.
        if (sent && kind == ReminderKind.Review && appointment != null) {
            plans.reviewPromptSent(appointment)
        }
        return Result.success()
    }

    /**
     * Everything the switches and the calendar say could go out today.
     *
     * Which one of them actually does is the ceiling's decision and not this one's.
     * Nothing here reads a day somebody missed, which is why the only dates it looks
     * at are today's day of the week and an appointment somebody typed in.
     */
    private suspend fun due(profile: ProfileRepository, appointment: Long?): Set<ReminderKind> {
        val sunday = LocalDate.now(ZoneId.systemDefault()).dayOfWeek == DayOfWeek.SUNDAY
        return buildSet {
            if (appointment != null) add(ReminderKind.Review)
            if (profile.reminderOn(ReminderKind.Walk)) add(ReminderKind.Walk)
            if (sunday && profile.reminderOn(ReminderKind.WeekNote)) add(ReminderKind.WeekNote)
            if (sunday && profile.reminderOn(ReminderKind.Photo)) add(ReminderKind.Photo)
        }
    }

    /**
     * The appointment today's one prompt would be about, or null.
     *
     * Every rule about which day and how many times lives in ReviewDate, which is a
     * pure function, and this only supplies it with what is on the phone. The switch
     * is checked first so that a phone with the setting off never even reads the
     * plans.
     */
    private suspend fun appointment(
        profile: ProfileRepository,
        plans: PlanRepository,
        today: LocalDate,
    ): Long? {
        if (!profile.reminderOn(ReminderKind.Review)) return null
        val prompt = ReviewDate.due(plans.reviewDays(), today.toEpochDay(), plans.reviewPrompted())
        return (prompt as? ReviewPrompt.Send)?.onDay
    }

    /**
     * The daily prompt, if it is on and if today is a day for it.
     *
     * Returns true when it sent something, so nothing else goes out on top of it.
     * One notification a day is one notification a day.
     */
    @RequiresPermission(Reminding.POST_NOTIFICATIONS)
    private suspend fun daily(
        context: Context,
        db: SteadyDatabase,
        profile: ProfileRepository,
    ): Boolean {
        if (!profile.reminderOn(ReminderKind.Daily)) return false

        val prompts = DailyPromptRepository(db)
        val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        return when (DailyPrompt.today(prompts.history(), today)) {
            Prompt.Quiet -> false

            Prompt.TurnItOff -> {
                profile.setReminderOn(ReminderKind.Daily, false)
                profile.setDailyGaveUp(true)
                false
            }

            Prompt.StillHere ->
                sendDaily(context, prompts, today, context.getString(R.string.daily_still_here))

            Prompt.Ordinary ->
                sendDaily(context, prompts, today, rotating(context, db, today))
        }
    }

    @RequiresPermission(Reminding.POST_NOTIFICATIONS)
    private suspend fun sendDaily(
        context: Context,
        prompts: DailyPromptRepository,
        today: Long,
        words: String,
    ): Boolean {
        val sent = Reminding.send(context, words, daily = true)
        if (sent) prompts.sent(today)
        return sent
    }

    /**
     * Which of the three daily lines today gets.
     *
     * Chosen by the day itself, so nothing has to be stored and the same day says the
     * same thing however many times this runs. The third names today's session, which
     * is the one that makes somebody open it; when there is no session to name, the
     * rotation falls back to one that is always true.
     */
    private suspend fun rotating(context: Context, db: SteadyDatabase, today: Long): String =
        when ((today % THREE_LINES).toInt()) {
            0 -> context.getString(R.string.daily_ready)
            1 -> context.getString(R.string.daily_short)
            else -> named(context, db, today) ?: context.getString(R.string.daily_ready)
        }

    private suspend fun named(context: Context, db: SteadyDatabase, today: Long): String? {
        val profile = ProfileRepository(db)
        val runs = RunRepository(db)
        val plan = SessionEngine.plan(
            SessionInputs(
                way = profile.gettingAround(),
                exclusions = profile.exclusions(),
                kit = profile.kit(),
                sore = runs.soreAreas(today),
                history = runs.history(),
                lastFelt = runs.lastFelt(),
                feltBefore = runs.feltBefore(),
                today = today,
                lastSessionDay = runs.lastSessionDay(),
                strengthRunLength = runs.strengthRunLength(),
            ),
        )
        val first = plan.main.firstOrNull()?.movement?.name ?: return null
        return context.resources.getQuantityString(
            R.plurals.daily_named,
            plan.minutes,
            first,
            plan.minutes,
        )
    }

    companion object {
        private const val THREE_LINES = 3

        private const val WORK = "steady-reminders"
        private const val A_WEEK = 7L * 24 * 60 * 60 * 1000

        /**
         * Once a day, and only while at least one switch is on.
         *
         * Cancelled outright when they are all off, rather than left running and
         * doing nothing, because a job that wakes a phone to decide not to speak
         * is still a job waking a phone.
         */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build(),
            )
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK)
        }
    }
}
