package com.mosquishe.today.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The four top-level views, mirroring Things3's structure (no Someday/Inbox by decision). */
enum class TaskView { TODAY, ANYTIME, UPCOMING, LOGBOOK }

/**
 * Pure logic deciding which calendar day counts as "today" and which view a to-do belongs to.
 * No Android/Room deps → fully unit-testable.
 */
object DateLogic {

    /**
     * The logical "today", honoring the user's day-start setting. A new day's to-dos only appear
     * once the clock passes [dayStart]; before that the previous calendar day is still "today".
     */
    fun logicalToday(now: LocalDateTime, dayStart: LocalTime): LocalDate =
        if (now.toLocalTime() < dayStart) now.toLocalDate().minusDays(1) else now.toLocalDate()

    /**
     * Today = active to-dos that are due. A scheduled date on/before today (overdue carries forward,
     * like Things3) OR a deadline on/before today.
     */
    fun isInToday(scheduledDate: LocalDate?, deadline: LocalDate?, completed: Boolean, today: LocalDate): Boolean {
        if (completed) return false
        val scheduledDue = scheduledDate != null && !scheduledDate.isAfter(today)
        val deadlineDue = deadline != null && !deadline.isAfter(today)
        return scheduledDue || deadlineDue
    }

    /** Anytime = active, undated to-dos whose deadline (if any) is not yet due. */
    fun isInAnytime(scheduledDate: LocalDate?, deadline: LocalDate?, completed: Boolean, today: LocalDate): Boolean {
        if (completed) return false
        if (scheduledDate != null) return false
        return deadline == null || deadline.isAfter(today)
    }

    /** Upcoming = active to-dos scheduled for a future date. */
    fun isInUpcoming(scheduledDate: LocalDate?, completed: Boolean, today: LocalDate): Boolean {
        if (completed) return false
        return scheduledDate != null && scheduledDate.isAfter(today)
    }

    /** Logbook = completed to-dos. */
    fun isInLogbook(completed: Boolean): Boolean = completed
}
