package com.malekroki.app.domain

import java.time.LocalDate

/** Descriptive activity numbers. Never a score, never a judgement. */
data class ActivityRate(val completedDays: Int, val plannedDays: Int) {
    /** Null when there were no planned days (e.g. a full leave). */
    val percent: Int? get() = if (plannedDays == 0) null else Math.round(completedDays * 100f / plannedDays)
}

data class WeekMinutes(val weekStart: LocalDate, val minutes: Int)

data class LearningSummary(
    val totalMinutes: Int,
    val sessionCount: Int,
    val recentTopics: List<String>,
    val weekly: List<WeekMinutes>,
)

object Statistics {

    /**
     * Activity over the last [days] days including today. Only days that were
     * genuinely planned count in the denominator: leave, rest days, days before
     * the habit existed and a not-yet-done today are left out.
     */
    fun activityRate(
        history: HabitHistory,
        today: LocalDate,
        leaves: LeaveCalendar,
        days: Int,
    ): ActivityRate {
        var completed = 0
        var planned = 0
        for (date in DateMath.range(today.minusDays((days - 1).toLong()), today)) {
            when (StreakCalculator.dayStatus(history, date, today, leaves)) {
                DayStatus.COMPLETED -> { completed++; planned++ }
                DayStatus.MISSED -> planned++
                else -> Unit
            }
        }
        return ActivityRate(completed, planned)
    }

    /** Combined rate across several habits – summed days, not an average of percentages. */
    fun combinedActivityRate(
        histories: List<HabitHistory>,
        today: LocalDate,
        leaves: LeaveCalendar,
        days: Int,
    ): ActivityRate {
        val rates = histories.map { activityRate(it, today, leaves, days) }
        return ActivityRate(rates.sumOf { it.completedDays }, rates.sumOf { it.plannedDays })
    }

    /**
     * @param topicOf resolves the display topic of a session (current habit name or free topic).
     */
    fun learningSummary(
        sessions: List<LearningSession>,
        today: LocalDate,
        weeks: Int = 8,
        topicOf: (LearningSession) -> String = { it.topic },
    ): LearningSummary {
        val past = sessions.filter { !it.date.isAfter(today) }
        val recentTopics = past
            .sortedWith(compareByDescending<LearningSession> { it.date }.thenByDescending { it.createdAt })
            .map { topicOf(it).trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(6)

        val firstWeek = DateMath.weekStart(today).minusWeeks((weeks - 1).toLong())
        val buckets = (0 until weeks).map { firstWeek.plusWeeks(it.toLong()) }
        val byWeek = past.filter { !it.date.isBefore(firstWeek) }
            .groupBy { DateMath.weekStart(it.date) }
            .mapValues { (_, list) -> list.sumOf { it.minutes } }

        return LearningSummary(
            totalMinutes = past.sumOf { it.minutes },
            sessionCount = past.size,
            recentTopics = recentTopics,
            weekly = buckets.map { WeekMinutes(it, byWeek[it] ?: 0) },
        )
    }
}
