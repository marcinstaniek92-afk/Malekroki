package com.malekroki.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class PolishTextTest {

    @Test
    fun `polish plural forms`() {
        assertEquals("1 dzień", PolishText.days(1))
        assertEquals("2 dni", PolishText.days(2))
        assertEquals("1 minuta", PolishText.minutes(1))
        assertEquals("3 minuty", PolishText.minutes(3))
        assertEquals("5 minut", PolishText.minutes(5))
        assertEquals("12 minut", PolishText.minutes(12))
        assertEquals("22 minuty", PolishText.minutes(22))
        assertEquals("0 minut", PolishText.minutes(0))
        assertEquals("114 sesji", PolishText.sessions(114))
        assertEquals("24 nawyki", PolishText.habits(24))
    }

    @Test
    fun `durations`() {
        assertEquals("5 min", PolishText.duration(5))
        assertEquals("1 h", PolishText.duration(60))
        assertEquals("1 h 20 min", PolishText.duration(80))
    }

    @Test
    fun `dates are formatted in polish with genitive month`() {
        assertEquals("Niedziela, 18 października", PolishText.longDate(d("2026-10-18")))
        assertEquals("Październik 2026", PolishText.monthYear(d("2026-10-18")))
        // Year differs, so both full dates are shown.
        assertEquals("24 grudnia 2026 – 1 stycznia 2027", PolishText.dateRange(d("2026-12-24"), d("2027-01-01")))
        assertEquals("18 października 2026", PolishText.dateRange(d("2026-10-18"), d("2026-10-18")))
    }

    @Test
    fun `greeting depends on time of day`() {
        assertEquals("Dzień dobry", PolishText.greeting(LocalTime.of(8, 0)))
        assertEquals("Miłego popołudnia", PolishText.greeting(LocalTime.of(14, 0)))
        assertEquals("Dobry wieczór", PolishText.greeting(LocalTime.of(21, 0)))
        assertEquals("Dobry wieczór", PolishText.greeting(LocalTime.of(2, 0)))
    }

    @Test
    fun `no shaming words anywhere in the supportive copy`() {
        val all = PolishText.celebrations + listOf(
            PolishText.FRESH_START, PolishText.COMEBACK, PolishText.LEAVE_SAFE,
            PolishText.LEAVE_REST, PolishText.LEAVE_RETURN,
        ) + (0..6).map { PolishText.lineOfTheDay(d("2026-10-18").plusDays(it.toLong())) }
        val forbidden = listOf("porażk", "straci", "zawiod", "niepowodz", "kara", "wstyd")
        all.forEach { line -> forbidden.forEach { assertFalse("'$line' contains '$it'", line.lowercase().contains(it)) } }
        assertTrue(all.isNotEmpty())
    }
}
