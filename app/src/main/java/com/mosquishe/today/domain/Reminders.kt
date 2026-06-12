package com.mosquishe.today.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Pure Things3-style reminder rules. A reminder is a time-of-day on a to-do's "When"
 * (`scheduledDate`); it fires at that date+time. No Android/Room deps → fully unit-testable.
 */
object Reminders {

    /** The wall-clock moment a reminder fires, or null if the to-do can't carry one (no date/time). */
    fun reminderAt(scheduledDate: LocalDate?, reminderTime: LocalTime?): LocalDateTime? =
        if (scheduledDate != null && reminderTime != null) scheduledDate.atTime(reminderTime) else null

    /** Whether an alarm should be armed right now: an active to-do whose reminder is still in the future. */
    fun shouldSchedule(
        scheduledDate: LocalDate?,
        reminderTime: LocalTime?,
        completed: Boolean,
        now: LocalDateTime,
    ): Boolean {
        val at = reminderAt(scheduledDate, reminderTime) ?: return false
        return !completed && at.isAfter(now)
    }

    /**
     * Things3: adding a reminder to an undated to-do anchors it to Today. Returns the date the
     * to-do should carry once [reminderTime] is set — Today when it had none, otherwise unchanged.
     */
    fun resolveScheduledDate(reminderTime: LocalTime?, current: LocalDate?, today: LocalDate): LocalDate? =
        if (reminderTime != null && current == null) today else current
}
