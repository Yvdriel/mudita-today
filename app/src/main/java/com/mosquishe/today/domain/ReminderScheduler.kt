package com.mosquishe.today.domain

import java.time.LocalDateTime

/**
 * Schedules and cancels the OS-level alarm that fires a to-do's reminder notification.
 * Kept as an interface so [com.mosquishe.today.data.repo.TaskRepository] stays free of Android
 * deps (and unit-testable); the real implementation lives in the app's reminder package.
 */
interface ReminderScheduler {
    /** (Re)arm the reminder for [taskId] to fire at [at] (device local time). */
    fun schedule(taskId: Long, title: String, at: LocalDateTime)

    /** Cancel any pending reminder for [taskId]. Safe to call when none is armed. */
    fun cancel(taskId: Long)
}

/** No-op default so the repository can be built (and tested) without an Android scheduler. */
object NoopReminderScheduler : ReminderScheduler {
    override fun schedule(taskId: Long, title: String, at: LocalDateTime) = Unit
    override fun cancel(taskId: Long) = Unit
}
