package com.mosquishe.today.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

/** Notification channel id + the intent extras shared across the reminder pipeline. */
const val CHANNEL_REMINDERS = "reminders"
const val EXTRA_TASK_ID = "task_id"
const val EXTRA_TASK_TITLE = "task_title"

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
    }
    context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
}
