package com.kamsiob.steadyhealth.remind

import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kamsiob.steadyhealth.data.ProfileRepository
import com.kamsiob.steadyhealth.data.ReminderRepository
import com.kamsiob.steadyhealth.data.SteadyDatabase
import com.kamsiob.steadyhealth.engine.ReminderKind
import com.kamsiob.steadyhealth.engine.Reminders
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

        if (!Reminders.maySend(reminders.sentSince(now - A_WEEK), now)) return Result.success()

        val today = LocalDate.now(ZoneId.systemDefault())
        val sunday = today.dayOfWeek == DayOfWeek.SUNDAY
        val due = buildSet {
            if (profile.reminderOn(ReminderKind.Walk)) add(ReminderKind.Walk)
            if (sunday && profile.reminderOn(ReminderKind.WeekNote)) add(ReminderKind.WeekNote)
            if (sunday && profile.reminderOn(ReminderKind.Photo)) add(ReminderKind.Photo)
        }

        val kind = Reminders.pick(due) ?: return Result.success()
        val sent = Reminding.send(context, context.getString(Reminding.words(kind)))
        // Only a reminder that actually went out counts against the ceiling. One
        // the system swallowed should not cost somebody their week's allowance.
        if (sent) reminders.record(kind, now)
        return Result.success()
    }

    companion object {
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
