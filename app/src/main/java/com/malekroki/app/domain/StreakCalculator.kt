package com.malekroki.app.domain

import java.time.LocalDate

/** The meaning of one calendar day for one habit. */
enum class DayStatus {
    /** The habit was done on this day. */
    COMPLETED,

    /** A planned day in the past without a completion. Breaks a streak – gently. */
    MISSED,

    /** Covered by leave and not done: neither breaks nor extends the streak. */
    PROTECTED,

    /** Not a planned day for this habit: neither breaks nor extends the streak. */
    REST,

    /** Today, not done yet. Never counts as missed. */
    PENDING,

    /** In the future. */
    FUTURE,

    /** In the future and covered by scheduled leave. */
    FUTURE_PROTECTED,

    /** Before the habit existed (and before its first recorded completion). */
    BEFORE_START,
}

data class StreakStats(
    val current: Int,
    val longest: Int,
    val totalCompletions: Int,
    val completedToday: Boolean,
    val protectedToday: Boolean,
) {
    /** True when there is history but the current streak is zero – time for a kind message. */
    val isFreshStart: Boolean get() = current == 0 && totalCompletions > 0

    companion object {
        val EMPTY = StreakStats(0, 0, 0, completedToday = false, protectedToday = false)
    }
}

/**
 * Per-habit history in local calendar dates.
 *
 * Date strategy: every completion is stored as a [LocalDate] computed in the
 * device's time zone at the moment of completion. Streaks compare calendar days
 * only, so DST changes, UTC offsets and later time-zone changes cannot move a
 * completion to another day.
 */
class HabitHistory(
    val habit: Habit,
    completionDates: Collection<LocalDate>,
) {
    val completions: Set<LocalDate> = completionDates.toHashSet()

    /** History starts at creation, or earlier when older completions were recorded. */
    val effectiveStart: LocalDate = completions.minOrNull()
        ?.let { minOf(it, habit.createdDate) } ?: habit.createdDate
}

object StreakCalculator {

    fun dayStatus(
        history: HabitHistory,
        date: LocalDate,
        today: LocalDate,
        leaves: LeaveCalendar,
    ): DayStatus {
        val habitId = history.habit.id
        if (date in history.completions && !date.isAfter(today)) return DayStatus.COMPLETED
        if (date.isAfter(today)) {
            return if (leaves.isProtected(habitId, date)) DayStatus.FUTURE_PROTECTED else DayStatus.FUTURE
        }
        if (leaves.isProtected(habitId, date)) return DayStatus.PROTECTED
        if (date.isBefore(history.effectiveStart)) return DayStatus.BEFORE_START
        if (date.dayOfWeek !in history.habit.scheduledDays) return DayStatus.REST
        if (date == today) return DayStatus.PENDING
        return DayStatus.MISSED
    }

    /**
     * Current streak: walk back from today. Completed days count, protected,
     * rest and pending days are skipped, and the first missed day ends the walk.
     */
    fun currentStreak(history: HabitHistory, today: LocalDate, leaves: LeaveCalendar): Int {
        var count = 0
        var date = today
        while (!date.isBefore(history.effectiveStart)) {
            when (dayStatus(history, date, today, leaves)) {
                DayStatus.COMPLETED -> count++
                DayStatus.MISSED, DayStatus.BEFORE_START -> return count
                else -> Unit
            }
            date = date.minusDays(1)
        }
        return count
    }

    /** Longest streak ever, using exactly the same day rules as [currentStreak]. */
    fun longestStreak(history: HabitHistory, today: LocalDate, leaves: LeaveCalendar): Int {
        var best = 0
        var run = 0
        var date = history.effectiveStart
        while (!date.isAfter(today)) {
            when (dayStatus(history, date, today, leaves)) {
                DayStatus.COMPLETED -> {
                    run++
                    if (run > best) best = run
                }
                DayStatus.MISSED -> run = 0
                else -> Unit
            }
            date = date.plusDays(1)
        }
        return best
    }

    fun stats(history: HabitHistory, today: LocalDate, leaves: LeaveCalendar): StreakStats {
        val completedToday = today in history.completions
        val protectedToday = leaves.isProtected(history.habit.id, today)
        val total = history.completions.count { !it.isAfter(today) }
        if (!history.habit.countsTowardStreak) {
            return StreakStats(0, 0, total, completedToday, protectedToday)
        }
        return StreakStats(
            current = currentStreak(history, today, leaves),
            longest = longestStreak(history, today, leaves),
            totalCompletions = total,
            completedToday = completedToday,
            protectedToday = protectedToday,
        )
    }
}
