package com.mosquishe.today.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import androidx.core.content.getSystemService
import com.mosquishe.today.data.settings.SettingsStore

/** The intent extras shared across the reminder pipeline. */
const val EXTRA_TASK_ID = "task_id"
const val EXTRA_TASK_TITLE = "task_title"
const val EXTRA_TASK_TIME = "task_time"

private const val CHANNEL_BASE = "reminders"

/**
 * The reminder channel id for a given sound. A channel's sound is immutable once created, so we
 * encode the chosen sound into the id and create a fresh channel when it changes — see
 * [applyReminderChannel]. id↔sound is 1:1, so re-selecting an old sound reuses its id safely.
 */
fun reminderChannelId(soundUri: String?): String {
    val token = when (soundUri) {
        null -> "default"
        SettingsStore.REMINDER_SOUND_SILENT -> "silent"
        else -> Integer.toHexString(soundUri.hashCode())
    }
    return "${CHANNEL_BASE}_$token"
}

/**
 * Ensure the reminder channel for [soundUri] exists (high importance, public on the lock screen,
 * gentle notification-stream sound) and delete any stale reminder channels so the user sees one
 * "Reminders" channel. Idempotent — safe to call on every app start and whenever the sound changes.
 */
fun applyReminderChannel(context: Context, soundUri: String?) {
    val manager = context.getSystemService<NotificationManager>() ?: return
    val targetId = reminderChannelId(soundUri)

    // Remove the original "reminders" channel and any other-sound channels we created earlier.
    manager.notificationChannels
        .filter { (it.id == CHANNEL_BASE || it.id.startsWith("${CHANNEL_BASE}_")) && it.id != targetId }
        .forEach { manager.deleteNotificationChannel(it.id) }

    if (manager.getNotificationChannel(targetId) != null) return

    val channel = NotificationChannel(targetId, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
        description = "Time-of-day reminders for your to-dos"
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION) // gentle: notification stream, respects DND/volume
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        when (soundUri) {
            SettingsStore.REMINDER_SOUND_SILENT -> setSound(null, null)
            null -> setSound(Settings.System.DEFAULT_NOTIFICATION_URI, attrs)
            else -> setSound(Uri.parse(soundUri), attrs)
        }
    }
    manager.createNotificationChannel(channel)
}
