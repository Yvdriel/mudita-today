package com.mosquishe.today.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RemindersTest {

    private val today = LocalDate.of(2026, 6, 6)
    private val at9 = LocalTime.of(9, 0)
    private val now8am = LocalDateTime.of(2026, 6, 6, 8, 0)

    // ---- reminderAt ----

    @Test fun `reminderAt combines date and time`() {
        assertEquals(LocalDateTime.of(2026, 6, 6, 9, 0), Reminders.reminderAt(today, at9))
    }

    @Test fun `reminderAt is null without a date`() {
        assertNull(Reminders.reminderAt(null, at9))
    }

    @Test fun `reminderAt is null without a time`() {
        assertNull(Reminders.reminderAt(today, null))
    }

    // ---- shouldSchedule ----

    @Test fun `schedules an active future reminder`() {
        assertTrue(Reminders.shouldSchedule(today, at9, completed = false, now = now8am))
    }

    @Test fun `does not schedule a past reminder`() {
        val now10am = LocalDateTime.of(2026, 6, 6, 10, 0)
        assertFalse(Reminders.shouldSchedule(today, at9, completed = false, now = now10am))
    }

    @Test fun `does not schedule when completed`() {
        assertFalse(Reminders.shouldSchedule(today, at9, completed = true, now = now8am))
    }

    @Test fun `does not schedule without a date or time`() {
        assertFalse(Reminders.shouldSchedule(null, at9, completed = false, now = now8am))
        assertFalse(Reminders.shouldSchedule(today, null, completed = false, now = now8am))
    }

    // ---- resolveScheduledDate (Things3 auto-set-today) ----

    @Test fun `adding a reminder to an undated to-do anchors it to today`() {
        assertEquals(today, Reminders.resolveScheduledDate(at9, current = null, today = today))
    }

    @Test fun `adding a reminder keeps an existing date`() {
        val tomorrow = today.plusDays(1)
        assertEquals(tomorrow, Reminders.resolveScheduledDate(at9, current = tomorrow, today = today))
    }

    @Test fun `clearing a reminder leaves the date untouched`() {
        assertEquals(today, Reminders.resolveScheduledDate(null, current = today, today = today))
        assertNull(Reminders.resolveScheduledDate(null, current = null, today = today))
    }
}
