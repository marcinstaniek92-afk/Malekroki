package com.malekroki.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class StreakCalculatorTest {

    private val noLeave = LeaveCalendar.EMPTY

    private fun stats(h: HabitHistory, today: String, leaves: LeaveCalendar = noLeave) =
        StreakCalculator.stats(h, d(today), leaves)

    @Test
    fun `no completions gives zero streaks`() {
        val s = stats(history(habit()), "2026-10-18")
        assertEquals(0, s.current)
        assertEquals(0, s.longest)
        assertFalse(s.isFreshStart)
    }

    @Test
    fun `consecutive days including today form the current streak`() {
        val s = stats(history(habit(), *days("2026-10-14", "2026-10-18")), "2026-10-18")
        assertEquals(5, s.current)
        assertEquals(5, s.longest)
        assertTrue(s.completedToday)
    }

    @Test
    fun `today not done yet does not break the streak`() {
        val s = stats(history(habit(), *days("2026-10-14", "2026-10-17")), "2026-10-18")
        assertEquals(4, s.current)
        assertFalse(s.completedToday)
    }

    @Test
    fun `a missed day ends the current streak but keeps the personal best`() {
        val h = history(habit(), *days("2026-10-01", "2026-10-10"), *days("2026-10-15", "2026-10-17"))
        val s = stats(h, "2026-10-18")
        assertEquals(3, s.current)
        assertEquals(10, s.longest)
    }

    @Test
    fun `missing yesterday and today means a fresh start with history preserved`() {
        val h = history(habit(), *days("2026-10-01", "2026-10-16"))
        val s = stats(h, "2026-10-18")
        assertEquals(0, s.current)
        assertEquals(16, s.longest)
        assertTrue(s.isFreshStart)
    }

    @Test
    fun `streak crosses month boundary`() {
        val s = stats(history(habit(), *days("2026-09-28", "2026-10-03")), "2026-10-03")
        assertEquals(6, s.current)
    }

    @Test
    fun `streak crosses year boundary`() {
        val s = stats(history(habit(created = "2025-12-01"), *days("2025-12-29", "2026-01-02")), "2026-01-02")
        assertEquals(5, s.current)
    }

    @Test
    fun `leap day is a normal day in a streak`() {
        val h = history(habit(created = "2028-02-01"), "2028-02-27", "2028-02-28", "2028-02-29", "2028-03-01")
        assertEquals(4, stats(h, "2028-03-01").current)
    }

    @Test
    fun `skipping the leap day breaks the streak`() {
        val h = history(habit(created = "2028-02-01"), "2028-02-27", "2028-02-28", "2028-03-01")
        val s = stats(h, "2028-03-01")
        assertEquals(1, s.current)
        assertEquals(2, s.longest)
    }

    @Test
    fun `non leap year goes from february 28 straight to march 1`() {
        val h = history(habit(created = "2027-02-01"), "2027-02-27", "2027-02-28", "2027-03-01")
        assertEquals(3, stats(h, "2027-03-01").current)
    }

    @Test
    fun `streak survives the DST change because only calendar days are compared`() {
        // Europe/Warsaw switches back on 2026-10-25; the day has 25 hours.
        val h = history(habit(), *days("2026-10-23", "2026-10-27"))
        assertEquals(5, stats(h, "2026-10-27").current)
    }

    @Test
    fun `duplicate dates count once`() {
        val h = HabitHistory(habit(), listOf(d("2026-10-18"), d("2026-10-18"), d("2026-10-17")))
        val s = stats(h, "2026-10-18")
        assertEquals(2, s.current)
        assertEquals(2, s.totalCompletions)
    }

    @Test
    fun `removing a completion in the middle recalculates both streaks`() {
        val all = days("2026-10-10", "2026-10-18").toList()
        val withGap = all - "2026-10-14"
        val s = stats(history(habit(), *withGap.toTypedArray()), "2026-10-18")
        assertEquals(4, s.current)
        assertEquals(4, s.longest)
    }

    @Test
    fun `future completions are ignored`() {
        val h = history(habit(), "2026-10-17", "2026-10-18", "2026-10-19")
        val s = stats(h, "2026-10-18")
        assertEquals(2, s.current)
        assertEquals(2, s.totalCompletions)
    }

    @Test
    fun `completions recorded before creation date extend history`() {
        val h = history(habit(created = "2026-10-18"), *days("2026-10-15", "2026-10-18"))
        assertEquals(4, stats(h, "2026-10-18").current)
    }

    @Test
    fun `days before the habit existed are not missed days`() {
        val h = history(habit(created = "2026-10-17"))
        assertEquals(DayStatus.BEFORE_START, StreakCalculator.dayStatus(h, d("2026-10-10"), d("2026-10-18"), noLeave))
        assertEquals(DayStatus.MISSED, StreakCalculator.dayStatus(h, d("2026-10-17"), d("2026-10-18"), noLeave))
        assertEquals(DayStatus.PENDING, StreakCalculator.dayStatus(h, d("2026-10-18"), d("2026-10-18"), noLeave))
    }

    @Test
    fun `rest days of a weekday-only habit neither break nor extend the streak`() {
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        // Thu 15, Fri 16 done; Sat 17 and Sun 18 are rest days; Mon 19 done.
        val h = history(habit(created = "2026-10-01", scheduledDays = weekdays), "2026-10-15", "2026-10-16", "2026-10-19")
        val s = stats(h, "2026-10-19")
        assertEquals(3, s.current)
        assertEquals(DayStatus.REST, StreakCalculator.dayStatus(h, d("2026-10-18"), d("2026-10-19"), noLeave))
    }

    @Test
    fun `habit not counted toward streak reports zero streaks but keeps totals`() {
        val h = history(habit(countsTowardStreak = false), *days("2026-10-14", "2026-10-18"))
        val s = stats(h, "2026-10-18")
        assertEquals(0, s.current)
        assertEquals(5, s.totalCompletions)
    }

    @Test
    fun `longest streak is never smaller than current streak`() {
        val h = history(habit(), "2026-10-01", "2026-10-02", *days("2026-10-10", "2026-10-18"))
        val s = stats(h, "2026-10-18")
        assertTrue(s.longest >= s.current)
        assertEquals(9, s.current)
    }
}
