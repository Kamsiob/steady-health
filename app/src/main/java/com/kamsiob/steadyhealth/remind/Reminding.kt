package com.kamsiob.steadyhealth.remind

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kamsiob.steadyhealth.MainActivity
import com.kamsiob.steadyhealth.R
import com.kamsiob.steadyhealth.engine.ReminderKind

/**
 * Sending one reminder, and only ever one.
 *
 * The ceiling is decided in [com.kamsiob.steadyhealth.engine.Reminders] and asked
 * about here, so that whatever wakes up at seven in the morning cannot be the
 * thing that gets it wrong.
 *
 * Every string is under ten words and none of them refers to a day somebody
 * missed. LOGIC.md section 12 makes both of those rules, and they are the reason
 * this app is allowed to send anything at all.
 */
object Reminding {

    /**
     * The literal rather than `Manifest.permission.POST_NOTIFICATIONS`, which is
     * an API 33 constant and would be inlined into a build whose minimum is 29.
     * The manifest declares the same string.
     */
    const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

    private const val CHANNEL = "steady"
    private const val ONE_AT_A_TIME = 1

    fun channel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.remind_channel),
                // Not high. Nothing this app has to say is worth interrupting
                // somebody's afternoon with a sound.
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.remind_channel_why) },
        )
    }

    /**
     * Whether a notification would actually appear.
     *
     * Asked as one question rather than two. The runtime permission only exists
     * from Android 13, and below it somebody can still switch the app's
     * notifications off in system settings, so "is the permission granted" is the
     * wrong question on every version. This is the right one on all of them.
     */
    fun allowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * Send one. Returns false when the system will not let it through, so the
     * caller does not record a reminder that nobody saw against the ceiling.
     */
    @RequiresPermission(POST_NOTIFICATIONS)
    fun send(context: Context, text: String): Boolean {
        if (!allowed(context)) return false
        channel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(ONE_AT_A_TIME, notification)
        }.isSuccess
    }

    /** The words for one kind. Under ten, and never about a missed day. */
    fun words(kind: ReminderKind) = when (kind) {
        ReminderKind.Walk -> R.string.remind_walk
        ReminderKind.Photo -> R.string.remind_photo
        ReminderKind.WeekNote -> R.string.remind_week
        ReminderKind.StepReady -> R.string.remind_step
    }
}
