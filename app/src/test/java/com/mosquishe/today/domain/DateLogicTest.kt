package com.mosquishe.today.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DateLogicTest {

    private val dayStart3am = LocalTime.of(3, 0)

    @Test fun `before dayStart logical today is previous calendar day`() {
        val now = LocalDateTime.of(2026, 6, 6, 2, 30) // 02:30, before 03:00
        assertEquals(LocalDate.of(2026, 6, 5), DateLogic.logicalToday(now, dayStart3am))
    }

    @Test fun `at dayStart logical today is current calendar day`() {
        val now = LocalDateTime.of(2026, 6, 6, 3, 0)
        assertEquals(LocalDate.of(2026, 6, 6), DateLogic.logicalToday(now, dayStart3am))
    }

    @Test fun `after dayStart logical today is current calendar day`() {
        val now = LocalDateTime.of(2026, 6, 6, 9, 15)
        assertEquals(LocalDate.of(2026, 6, 6), DateLogic.logicalToday(now, dayStart3am))
    }

    @Test fun `midnight dayStart behaves like calendar date`() {
        val now = LocalDateTime.of(2026, 6, 6, 0, 1)
        assertEquals(LocalDate.of(2026, 6, 6), DateLogic.logicalToday(now, LocalTime.MIDNIGHT))
    }

    private val today = LocalDate.of(2026, 6, 6)
    private val yesterday = today.minusDays(1)
    private val tomorrow = today.plusDays(1)

    @Test fun `today includes scheduled today`() {
        assertTrue(DateLogic.isInToday(today, null, false, today))
    }

    @Test fun `today carries overdue scheduled forward`() {
        assertTrue(DateLogic.isInToday(yesterday, null, false, today))
    }

    @Test fun `today excludes future scheduled`() {
        assertFalse(DateLogic.isInToday(tomorrow, null, false, today))
    }

    @Test fun `today includes due-or-overdue deadline even when undated`() {
        assertTrue(DateLogic.isInToday(null, today, false, today))
        assertTrue(DateLogic.isInToday(null, yesterday, false, today))
    }

    @Test fun `today excludes future deadline only`() {
        assertFalse(DateLogic.isInToday(null, tomorrow, false, today))
    }

    @Test fun `today excludes completed`() {
        assertFalse(DateLogic.isInToday(today, null, true, today))
    }

    @Test fun `anytime is active undated with no due deadline`() {
        assertTrue(DateLogic.isInAnytime(null, null, false, today))
        assertTrue(DateLogic.isInAnytime(null, tomorrow, false, today)) // future deadline ok
    }

    @Test fun `anytime excludes dated, completed, and due-deadline items`() {
        assertFalse(DateLogic.isInAnytime(today, null, false, today))   // has a date
        assertFalse(DateLogic.isInAnytime(null, null, true, today))     // completed
        assertFalse(DateLogic.isInAnytime(null, today, false, today))   // deadline due -> Today instead
    }

    @Test fun `upcoming is future-scheduled and active`() {
        assertTrue(DateLogic.isInUpcoming(tomorrow, false, today))
        assertFalse(DateLogic.isInUpcoming(today, false, today))
        assertFalse(DateLogic.isInUpcoming(tomorrow, true, today))
        assertFalse(DateLogic.isInUpcoming(null, false, today))
    }

    @Test fun `logbook is completed`() {
        assertTrue(DateLogic.isInLogbook(true))
        assertFalse(DateLogic.isInLogbook(false))
    }
}
