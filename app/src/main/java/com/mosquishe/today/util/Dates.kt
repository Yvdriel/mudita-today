package com.mosquishe.today.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Date helpers: conversions for the MMD date picker (UTC millis) + human labels. */
object Dates {

    private const val MS_PER_DAY = 86_400_000L
    private val dayMonth = DateTimeFormatter.ofPattern("EEE d MMM")
    private val dayMonthYear = DateTimeFormatter.ofPattern("EEE d MMM yyyy")
    private val logDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val hourMinute = DateTimeFormatter.ofPattern("HH:mm")

    /** MMD/Compose date pickers work in UTC-midnight millis. */
    fun localDateToUtcMillis(d: LocalDate): Long = d.toEpochDay() * MS_PER_DAY
    fun utcMillisToLocalDate(ms: Long): LocalDate = LocalDate.ofEpochDay(Math.floorDiv(ms, MS_PER_DAY))

    fun instantToLocalDate(i: Instant): LocalDate = i.atZone(ZoneId.systemDefault()).toLocalDate()

    /** "Today" / "Tomorrow" / "Yesterday" / "Wed 12 Aug" (with year only when not the current year). */
    fun relativeLabel(d: LocalDate, today: LocalDate): String = when (d) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> if (d.year == today.year) d.format(dayMonth) else d.format(dayMonthYear)
    }

    fun fullLabel(d: LocalDate): String = d.format(logDate)

    /** 24-hour "HH:mm" label for a reminder time-of-day. */
    fun timeLabel(t: LocalTime): String = t.format(hourMinute)
}
