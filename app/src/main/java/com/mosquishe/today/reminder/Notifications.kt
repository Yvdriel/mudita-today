package com.mosquishe.today.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

/** Notification channel id + the intent extras shared across the reminder pipeline. */
const val CHANNEL_REMINDERS = "reminders"
const val EXTRA_TASK_ID = "task_id"
const val EXTRA_TASK_TITLE = "task_title"
const val EXTRA_TASK_TIME = "task_time"

/**
 * Create the high-importance reminder channel. Idempotent (re-creating an existing channel is a
 * no-op aside from name/description updates), so it's safe to call on every app start.
 * minSdk is 28, so [NotificationChannel] is always available.
 */
fun createReminderChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_REMINDERS,
        "Reminders",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Time-of-day reminders for your to-dos"
        // Show on the lock screen if the system ever allows it (hygiene; the Kompakt's launcher
        // gates third-party lock-screen notifications regardless — the full-screen intent is what
        // actually surfaces the reminder there).
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    }
    context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
}
