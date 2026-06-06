package com.mosquishe.today.domain

import java.time.LocalDate

/** Recurrence period unit. */
enum class RecurrenceUnit { DAY, WEEK, MONTH }

/**
 * A repeat rule: every [interval] [unit]s (e.g. interval=2, unit=WEEK = every 2 weeks).
 * Pure value type — no Android/Room deps so it is unit-testable and reusable in a Room converter.
 */
data class Recurrence(val interval: Int, val unit: RecurrenceUnit) {
    init {
        require(interval >= 1) { "interval must be >= 1, was $interval" }
    }

    /** Compact storage form: "<interval>:<UNIT>", e.g. "2:WEEK". */
    fun toStorageString(): String = "$interval:${unit.name}"

    /** Date of the next occurrence after [from]. */
    fun nextDate(from: LocalDate): LocalDate = when (unit) {
        RecurrenceUnit.DAY -> from.plusDays(interval.toLong())
        RecurrenceUnit.WEEK -> from.plusWeeks(interval.toLong())
        RecurrenceUnit.MONTH -> from.plusMonths(interval.toLong())
    }

    companion object {
        val DAILY = Recurrence(1, RecurrenceUnit.DAY)
        val WEEKLY = Recurrence(1, RecurrenceUnit.WEEK)
        val MONTHLY = Recurrence(1, RecurrenceUnit.MONTH)

        /** Parse [toStorageString]; null/blank/malformed → null (treated as "no repeat"). */
        fun parse(stored: String?): Recurrence? {
            if (stored.isNullOrBlank()) return null
            val parts = stored.split(":")
            if (parts.size != 2) return null
            val interval = parts[0].toIntOrNull() ?: return null
            if (interval < 1) return null
            val unit = RecurrenceUnit.entries.firstOrNull { it.name == parts[1] } ?: return null
            return Recurrence(interval, unit)
        }
    }
}
