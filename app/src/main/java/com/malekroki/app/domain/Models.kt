package com.malekroki.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * How a habit is considered done for a given day.
 *
 * - [MINUTES]: the summed duration of the day's sessions reaches [Habit.minimumMinutes]
 *   (or one understood concept, when [Habit.allowConceptCompletion] is set).
 *   A one-tap "done" from the dashboard always counts as well: the user is trusted.
 * - [ACTION]: a single meaningful action, confirmed with one tap.
 */
enum class CompletionType { MINUTES, ACTION }

/** Where a completion record came from. Session-derived records are kept in sync with sessions. */
enum class CompletionSource { MANUAL, SESSION }

enum class LeaveType { VACATION, SICK, HOLIDAY, PERSONAL, CUSTOM }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

val ALL_DAYS: Set<DayOfWeek> = DayOfWeek.entries.toSet()

data class Habit(
    val id: String,
    val name: String,
    val description: String = "",
    val category: String = "",
    val emoji: String = "✨",
    val completionType: CompletionType = CompletionType.ACTION,
    /** Minimum minutes per day; only meaningful for [CompletionType.MINUTES]. */
    val minimumMinutes: Int = 5,
    /** "One genuinely understood concept" also satisfies the minimum. */
    val allowConceptCompletion: Boolean = false,
    /** Human-readable minimum, shown on the dashboard. */
    val minimumDescription: String = "",
    /** Optional personal target – a hope, never a requirement. */
    val target: String = "",
    val countsTowardStreak: Boolean = true,
    /** Learning habits get learning sessions and journal entries. */
    val isLearning: Boolean = false,
    /** Days on which the habit is planned. Other days neither break nor extend a streak. */
    val scheduledDays: Set<DayOfWeek> = ALL_DAYS,
    val createdDate: LocalDate,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

/** At most one completion per habit per local calendar day (enforced by the composite key). */
data class Completion(
    val habitId: String,
    val date: LocalDate,
    val source: CompletionSource = CompletionSource.MANUAL,
    val createdAt: Long = 0L,
)

data class LearningSession(
    val id: String,
    /** Null when the habit was deleted or the session is about a free topic. */
    val habitId: String?,
    /** Free topic, or a snapshot of the habit name at the time of saving. */
    val topic: String,
    val date: LocalDate,
    val minutes: Int,
    val learned: String = "",
    val questions: String = "",
    val notes: String = "",
    val practicalExercise: Boolean = false,
    val conceptUnderstood: Boolean = false,
    val createdAt: Long = 0L,
)

data class DailyReflection(
    val date: LocalDate,
    val text: String,
    val updatedAt: Long = 0L,
)

data class LeavePeriod(
    val id: String,
    val type: LeaveType,
    /** Used for [LeaveType.CUSTOM]. */
    val customName: String = "",
    val startDate: LocalDate,
    /** Inclusive. */
    val endDate: LocalDate,
    val note: String = "",
    /** When true the leave protects every habit, including ones created later. */
    val appliesToAllHabits: Boolean = true,
    val habitIds: Set<String> = emptySet(),
    val createdAt: Long = 0L,
) {
    fun contains(date: LocalDate): Boolean = !date.isBefore(startDate) && !date.isAfter(endDate)

    fun appliesTo(habitId: String): Boolean = appliesToAllHabits || habitId in habitIds

    fun covers(habitId: String, date: LocalDate): Boolean = appliesTo(habitId) && contains(date)

    val lengthInDays: Long get() = endDate.toEpochDay() - startDate.toEpochDay() + 1
}

/** Everything the app stores, as one snapshot. Used for export, import and merging. */
data class AppData(
    val habits: List<Habit> = emptyList(),
    val completions: List<Completion> = emptyList(),
    val sessions: List<LearningSession> = emptyList(),
    val reflections: List<DailyReflection> = emptyList(),
    val leaves: List<LeavePeriod> = emptyList(),
)
