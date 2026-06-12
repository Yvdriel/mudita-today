package com.mosquishe.today.reminder

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mosquishe.today.MainActivity
import com.mosquishe.today.TodayApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * Fired by [AlarmReminderScheduler] when a reminder is due. Re-reads the to-do to guard against a
 * stale alarm (deleted / completed / reminder cleared since arming), then posts a notification whose
 * tap opens the to-do in [MainActivity].
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return

        val pending = goAsync()
        val app = context.applicationContext as TodayApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = app.container.repository.getTask(taskId)?.task
                if (task != null && !task.completed && task.reminderTime != null) {
                    val title = task.title.ifBlank { "To-do" }
                    val time = task.reminderTime.format(TIME_FORMAT)
                    // Resolve the user's chosen sound → its channel, ensuring it exists before posting.
                    val soundUri = app.container.settings.reminderSoundValue()
                    applyReminderChannel(context, soundUri)
                    postNotification(context, reminderChannelId(soundUri), task.id, title, time)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun postNotification(context: Context, channelId: String, taskId: Long, title: String, time: String) {
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Full-screen intent: when the device is locked/asleep this launches an activity over the
        // keyguard (the only way past the Kompakt's lock-screen notification allow-list); when the
        // screen is on it falls back to a normal heads-up notification.
        val alert = Intent(context, ReminderAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_TIME, time)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            alert,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText("Reminder · $time")
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPi)
            .setFullScreenIntent(fullScreenPi, true)
            .build()

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        try {
            manager.notify(taskId.toInt(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted (Android 13+) — drop silently.
        }
    }

    private companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
