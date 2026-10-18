package com.malekroki.app.domain

import java.time.LocalDate

internal fun d(text: String): LocalDate = LocalDate.parse(text)

internal fun habit(
    id: String = "h1",
    created: String = "2026-01-01",
    type: CompletionType = CompletionType.ACTION,
    minimumMinutes: Int = 5,
    allowConcept: Boolean = false,
    countsTowardStreak: Boolean = true,
    scheduledDays: Set<java.time.DayOfWeek> = ALL_DAYS,
) = Habit(
    id = id,
    name = "Nawyk $id",
    completionType = type,
    minimumMinutes = minimumMinutes,
    allowConceptCompletion = allowConcept,
    countsTowardStreak = countsTowardStreak,
    scheduledDays = scheduledDays,
    createdDate = d(created),
)

internal fun history(habit: Habit, vararg dates: String) = HabitHistory(habit, dates.map(::d))

internal fun days(from: String, to: String): Array<String> =
    DateMath.range(d(from), d(to)).map { it.toString() }.toTypedArray()

internal fun leave(
    id: String = "l1",
    from: String,
    to: String,
    all: Boolean = true,
    habits: Set<String> = emptySet(),
    type: LeaveType = LeaveType.SICK,
) = LeavePeriod(
    id = id, type = type, startDate = d(from), endDate = d(to),
    appliesToAllHabits = all, habitIds = habits,
)

internal fun session(
    id: String = "s1",
    habitId: String? = "h1",
    date: String = "2026-10-18",
    minutes: Int = 5,
    concept: Boolean = false,
    topic: String = "Temat",
    createdAt: Long = 0L,
) = LearningSession(
    id = id, habitId = habitId, topic = topic, date = d(date), minutes = minutes,
    conceptUnderstood = concept, createdAt = createdAt,
)
