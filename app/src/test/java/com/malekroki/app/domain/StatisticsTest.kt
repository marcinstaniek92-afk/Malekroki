package com.malekroki.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatisticsTest {

    @Test
    fun `seven day rate counts only planned days`() {
        // Oct 12..18; done 12,13,15,16; 14 and 17 missed; today pending.
        val h = history(habit(), "2026-10-12", "2026-10-13", "2026-10-15", "2026-10-16")
        val rate = Statistics.activityRate(h, d("2026-10-18"), LeaveCalendar.EMPTY, 7)
        assertEquals(4, rate.completedDays)
        assertEquals(6, rate.plannedDays)
        assertEquals(67, rate.percent)
    }

    @Test
    fun `leave days are excluded from the rate`() {
        val h = history(habit(), "2026-10-12", "2026-10-13")
        val rate = Statistics.activityRate(h, d("2026-10-18"), LeaveCalendar(listOf(leave(from = "2026-10-14", to = "2026-10-18"))), 7)
        assertEquals(2, rate.completedDays)
        assertEquals(2, rate.plannedDays)
        assertEquals(100, rate.percent)
    }

    @Test
    fun `days before creation are excluded and an empty window has no percent`() {
        val h = history(habit(created = "2026-10-18"))
        val rate = Statistics.activityRate(h, d("2026-10-18"), LeaveCalendar.EMPTY, 30)
        assertEquals(0, rate.plannedDays)
        assertNull(rate.percent)
    }

    @Test
    fun `thirty day window spans the month boundary`() {
        val h = history(habit(created = "2026-09-01"), *days("2026-09-19", "2026-10-18"))
        val rate = Statistics.activityRate(h, d("2026-10-18"), LeaveCalendar.EMPTY, 30)
        assertEquals(30, rate.completedDays)
        assertEquals(100, rate.percent)
    }

    @Test
    fun `combined rate sums days instead of averaging percentages`() {
        val a = history(habit(id = "a", created = "2026-10-18"), "2026-10-18")
        val b = history(habit(id = "b", created = "2026-10-12"), "2026-10-12")
        val rate = Statistics.combinedActivityRate(listOf(a, b), d("2026-10-18"), LeaveCalendar.EMPTY, 7)
        assertEquals(2, rate.completedDays)
        assertEquals(7, rate.plannedDays)
    }

    @Test
    fun `learning summary totals, recent topics and weekly buckets`() {
        val sessions = listOf(
            session(id = "1", date = "2026-10-18", minutes = 15, topic = "JavaScript", createdAt = 3),
            session(id = "2", date = "2026-10-17", minutes = 5, topic = "CSS Grid", createdAt = 2),
            session(id = "3", date = "2026-10-05", minutes = 60, topic = "JavaScript", createdAt = 1),
            session(id = "4", date = "2026-10-25", minutes = 30, topic = "Przyszłość"),
        )
        val summary = Statistics.learningSummary(sessions, d("2026-10-18"), weeks = 3)
        assertEquals(80, summary.totalMinutes)
        assertEquals(3, summary.sessionCount)
        assertEquals(listOf("JavaScript", "CSS Grid"), summary.recentTopics)
        assertEquals(listOf(d("2026-09-28"), d("2026-10-05"), d("2026-10-12")), summary.weekly.map { it.weekStart })
        assertEquals(listOf(0, 60, 20), summary.weekly.map { it.minutes })
    }

    @Test
    fun `learning summary uses the resolved topic`() {
        val sessions = listOf(session(habitId = "h1", topic = "Stara nazwa"))
        val summary = Statistics.learningSummary(sessions, d("2026-10-18")) { "Nowa nazwa" }
        assertEquals(listOf("Nowa nazwa"), summary.recentTopics)
    }
}
