package com.mosquishe.today.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.mosquishe.today.domain.ReminderScheduler
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Android [ReminderScheduler]: arms an exact [AlarmManager] alarm that broadcasts to
 * [ReminderReceiver] at the reminder's wall-clock time. The PendingIntent request code is the
 * task id, so each to-do owns exactly one pending alarm (re-arming replaces it; cancel clears it).
 */
class AlarmReminderScheduler(private val context: Context) : ReminderScheduler {

    private val alarmManager = context.getSystemService<AlarmManager>()!!

    override fun schedule(taskId: Long, title: String, at: LocalDateTime) {
        val triggerAtMillis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = pendingIntent(taskId, title, create = true) ?: return
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            // Exact alarms not permitted (rare for a reminder app, and unrestricted on the Kompakt):
            // fall back to a doze-friendly inexact alarm so the reminder still fires, just less precisely.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    override fun cancel(taskId: Long) {
        pendingIntent(taskId, title = null, create = false)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * Build the broadcast PendingIntent for [taskId]. With [create] false it returns null when no
     * alarm is pending (FLAG_NO_CREATE) — used by [cancel]. Extras are ignored when matching an
     * existing PendingIntent, so the request code (task id) is what keeps them distinct.
     */
    private fun pendingIntent(taskId: Long, title: String?, create: Boolean): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_TASK_ID, taskId)
            if (title != null) putExtra(EXTRA_TASK_TITLE, title)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, taskId.toInt(), intent, flags)
    }

    private companion object {
        const val ACTION_FIRE = "com.mosquishe.today.action.REMINDER_FIRE"
    }
}
