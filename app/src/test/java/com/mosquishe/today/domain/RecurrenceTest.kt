package com.mosquishe.today.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecurrenceTest {

    @Test fun `next date advances by unit and interval`() {
        val d = LocalDate.of(2026, 6, 6)
        assertEquals(LocalDate.of(2026, 6, 7), Recurrence.DAILY.nextDate(d))
        assertEquals(LocalDate.of(2026, 6, 13), Recurrence.WEEKLY.nextDate(d))
        assertEquals(LocalDate.of(2026, 7, 6), Recurrence.MONTHLY.nextDate(d))
        assertEquals(LocalDate.of(2026, 6, 20), Recurrence(2, RecurrenceUnit.WEEK).nextDate(d))
        assertEquals(LocalDate.of(2026, 9, 6), Recurrence(3, RecurrenceUnit.MONTH).nextDate(d))
    }

    @Test fun `monthly clamps short months`() {
        // Jan 31 + 1 month -> Feb 28 (java.time clamps to last valid day)
        assertEquals(LocalDate.of(2026, 2, 28), Recurrence.MONTHLY.nextDate(LocalDate.of(2026, 1, 31)))
    }

    @Test fun `round trips through storage string`() {
        val r = Recurrence(2, RecurrenceUnit.WEEK)
        assertEquals("2:WEEK", r.toStorageString())
        assertEquals(r, Recurrence.parse("2:WEEK"))
    }

    @Test fun `parse handles null blank and malformed`() {
        assertNull(Recurrence.parse(null))
        assertNull(Recurrence.parse(""))
        assertNull(Recurrence.parse("   "))
        assertNull(Recurrence.parse("WEEK"))
        assertNull(Recurrence.parse("0:WEEK"))
        assertNull(Recurrence.parse("2:FORTNIGHT"))
        assertNull(Recurrence.parse("x:WEEK"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `interval must be positive`() {
        Recurrence(0, RecurrenceUnit.DAY)
    }
}
