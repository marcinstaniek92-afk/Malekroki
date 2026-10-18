package com.malekroki.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaveProtectionTest {

    private fun stats(h: HabitHistory, today: String, vararg leaves: LeavePeriod) =
        StreakCalculator.stats(h, d(today), LeaveCalendar(leaves.toList()))

    @Test
    fun `sick leave preserves a 30 day gym streak without increasing it`() {
        val gym = habit(id = "gym", created = "2026-09-01")
        // 30 days done, then a cold from Oct 1 to Oct 4, back on Oct 5.
        val h = history(gym, *days("2026-09-01", "2026-09-30"), "2026-10-05")
        val sick = leave(from = "2026-10-01", to = "2026-10-04", all = false, habits = setOf("gym"))
        val s = stats(h, "2026-10-05", sick)
        assertEquals(31, s.current)
        assertEquals(31, s.longest)
    }

    @Test
    fun `without leave the same history starts over`() {
        val gym = habit(id = "gym", created = "2026-09-01")
        val h = history(gym, *days("2026-09-01", "2026-09-30"), "2026-10-05")
        val s = stats(h, "2026-10-05")
        assertEquals(1, s.current)
        assertEquals(30, s.longest)
    }

    @Test
    fun `active leave keeps the streak visible during the leave`() {
        val h = history(habit(), *days("2026-10-01", "2026-10-14"))
        val s = stats(h, "2026-10-18", leave(from = "2026-10-15", to = "2026-10-20"))
        assertEquals(14, s.current)
        assertTrue(s.protectedToday)
    }

    @Test
    fun `protected days are not completions`() {
        val h = history(habit(), "2026-10-14")
        val s = stats(h, "2026-10-18", leave(from = "2026-10-15", to = "2026-10-18"))
        assertEquals(1, s.current)
        assertEquals(1, s.totalCompletions)
        assertEquals(
            DayStatus.PROTECTED,
            StreakCalculator.dayStatus(h, d("2026-10-16"), d("2026-10-18"), LeaveCalendar(listOf(leave(from = "2026-10-15", to = "2026-10-18")))),
        )
    }

    @Test
    fun `completing a habit during leave still counts`() {
        val h = history(habit(), "2026-10-14", "2026-10-16")
        val s = stats(h, "2026-10-18", leave(from = "2026-10-15", to = "2026-10-18"))
        assertEquals(2, s.current)
    }

    @Test
    fun `leave only protects selected habits`() {
        val calendar = LeaveCalendar(listOf(leave(from = "2026-10-15", to = "2026-10-17", all = false, habits = setOf("gym"))))
        val gym = history(habit(id = "gym"), *days("2026-10-10", "2026-10-14"), "2026-10-18")
        val js = history(habit(id = "js"), *days("2026-10-10", "2026-10-14"), "2026-10-18")
        assertEquals(6, StreakCalculator.currentStreak(gym, d("2026-10-18"), calendar))
        assertEquals(1, StreakCalculator.currentStreak(js, d("2026-10-18"), calendar))
    }

    @Test
    fun `leave across the year boundary`() {
        val h = history(habit(created = "2025-12-01"), *days("2025-12-20", "2025-12-23"), "2026-01-03")
        val s = stats(h, "2026-01-03", leave(from = "2025-12-24", to = "2026-01-02", type = LeaveType.HOLIDAY))
        assertEquals(5, s.current)
    }

    @Test
    fun `overlapping leaves do not double count protected days`() {
        val a = leave(id = "a", from = "2026-10-10", to = "2026-10-15")
        val b = leave(id = "b", from = "2026-10-13", to = "2026-10-20", all = false, habits = setOf("h1"))
        val dates = LeaveCalendar(listOf(a, b)).protectedDates("h1", d("2026-10-01"), d("2026-10-31"))
        assertEquals(11, dates.size)
        assertEquals(d("2026-10-10"), dates.first())
        assertEquals(d("2026-10-20"), dates.last())
    }

    @Test
    fun `deleting a leave recalculates the streak from real history`() {
        val h = history(habit(), *days("2026-10-01", "2026-10-09"), "2026-10-18")
        val withLeave = stats(h, "2026-10-18", leave(from = "2026-10-10", to = "2026-10-17"))
        val withoutLeave = stats(h, "2026-10-18")
        assertEquals(10, withLeave.current)
        assertEquals(1, withoutLeave.current)
        assertEquals(9, withoutLeave.longest)
    }

    @Test
    fun `shortening a leave exposes the uncovered missed day`() {
        val h = history(habit(), *days("2026-10-01", "2026-10-09"), "2026-10-18")
        val shortened = stats(h, "2026-10-18", leave(from = "2026-10-10", to = "2026-10-16"))
        assertEquals(1, shortened.current)
    }

    @Test
    fun `leave starting today protects today`() {
        val h = history(habit(), *days("2026-10-10", "2026-10-17"))
        val s = stats(h, "2026-10-18", leave(from = "2026-10-18", to = "2026-10-25"))
        assertTrue(s.protectedToday)
        assertEquals(8, s.current)
    }

    @Test
    fun `leave ending yesterday lets normal tracking resume today`() {
        val h = history(habit(), *days("2026-10-01", "2026-10-09"))
        val s = stats(h, "2026-10-18", leave(from = "2026-10-10", to = "2026-10-17"))
        assertEquals(9, s.current)
        assertFalse(s.protectedToday)
        assertEquals(
            DayStatus.PENDING,
            StreakCalculator.dayStatus(h, d("2026-10-18"), d("2026-10-18"), LeaveCalendar(listOf(leave(from = "2026-10-10", to = "2026-10-17")))),
        )
    }

    @Test
    fun `future leave is shown as scheduled protection`() {
        val cal = LeaveCalendar(listOf(leave(from = "2026-10-20", to = "2026-10-22")))
        val h = history(habit())
        assertEquals(DayStatus.FUTURE_PROTECTED, StreakCalculator.dayStatus(h, d("2026-10-21"), d("2026-10-18"), cal))
        assertEquals(DayStatus.FUTURE, StreakCalculator.dayStatus(h, d("2026-10-23"), d("2026-10-18"), cal))
    }

    @Test
    fun `leave for all habits also covers habits created later`() {
        val l = leave(from = "2026-10-01", to = "2026-10-31")
        assertTrue(l.covers("created-later", d("2026-10-15")))
    }

    @Test
    fun `archived habit keeps its streak rules`() {
        val h = history(habit().copy(archived = true), *days("2026-10-01", "2026-10-05"), "2026-10-08")
        val s = stats(h, "2026-10-08", leave(from = "2026-10-06", to = "2026-10-07"))
        assertEquals(6, s.current)
    }

    @Test
    fun `validation rejects end before start, empty scope and duplicates`() {
        val existing = leave(id = "a", from = "2026-10-10", to = "2026-10-12")
        assertEquals(1, LeaveRules.validate(leave(id = "b", from = "2026-10-12", to = "2026-10-10"), emptyList()).size)
        assertEquals(1, LeaveRules.validate(leave(id = "b", from = "2026-10-10", to = "2026-10-10", all = false), emptyList()).size)
        assertEquals(1, LeaveRules.validate(existing.copy(id = "b"), listOf(existing)).size)
        assertTrue(LeaveRules.validate(existing, listOf(existing)).isEmpty())
        assertEquals(
            1,
            LeaveRules.validate(existing.copy(id = "c", type = LeaveType.CUSTOM, customName = " "), emptyList()).size,
        )
    }

    @Test
    fun `overlap detection respects scopes`() {
        val gymOnly = leave(id = "a", from = "2026-10-10", to = "2026-10-15", all = false, habits = setOf("gym"))
        val jsOnly = leave(id = "b", from = "2026-10-12", to = "2026-10-20", all = false, habits = setOf("js"))
        val everything = leave(id = "c", from = "2026-10-14", to = "2026-10-14")
        assertTrue(LeaveRules.overlapping(jsOnly, listOf(gymOnly)).isEmpty())
        assertEquals(2, LeaveRules.overlapping(everything, listOf(gymOnly, jsOnly)).size)
    }

    @Test
    fun `leave status relative to today`() {
        val l = leave(from = "2026-10-18", to = "2026-10-20")
        assertEquals(LeaveStatus.UPCOMING, LeaveRules.status(l, d("2026-10-17")))
        assertEquals(LeaveStatus.ACTIVE, LeaveRules.status(l, d("2026-10-18")))
        assertEquals(LeaveStatus.ACTIVE, LeaveRules.status(l, d("2026-10-20")))
        assertEquals(LeaveStatus.PAST, LeaveRules.status(l, d("2026-10-21")))
    }
}
