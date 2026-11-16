package ca.nexacraft.homekeep.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HelpersTest {

    @Test
    fun parsesIsoDatesAndIgnoresTimestamps() {
        assertEquals(LocalDate.of(2026, 11, 16), Dates.parse("2026-11-16"))
        assertEquals(LocalDate.of(2026, 11, 16), Dates.parse("2026-11-16T10:00:00Z"))
        assertNull(Dates.parse("not a date"))
        assertNull(Dates.parse(null))
    }

    @Test
    fun countsDaysUntil() {
        assertEquals(0L, Dates.daysUntil(Dates.today()))
        assertEquals(10L, Dates.daysUntil(LocalDate.now().plusDays(10).toString()))
        assertNull(Dates.daysUntil(""))
    }

    @Test
    fun addsMonthsAcrossYearBoundary() {
        assertEquals("2027-01-15", Dates.addMonths("2026-11-15", 2))
        assertNull(Dates.addMonths("nope", 2))
    }

    @Test
    fun roundTripsUtcMillis() {
        val iso = "2026-03-01"
        assertEquals(iso, Dates.fromUtcMillis(Dates.toUtcMillis(iso)!!))
        assertNull(Dates.toUtcMillis(null))
    }

    @Test
    fun formatsMoneyAndEditableNumbers() {
        assertNull(formatMoney(null))
        val formatted = formatMoney(1234.5, "CAD")
        assertTrue(formatted!!.contains("1,234.50") || formatted.contains("1 234,50"))
        assertEquals("12", 12.0.asEditable())
        assertEquals("12.5", 12.5.asEditable())
        assertEquals("", (null as Double?).asEditable())
    }
}
