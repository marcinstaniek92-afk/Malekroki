package com.malekroki.app.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object CompletionRules {

    const val MIN_MINUTES = 1
    const val MAX_MINUTES_PER_SESSION = 24 * 60

    /**
     * Do the sessions recorded for one habit on one day satisfy its minimum?
     * Minutes are summed across the day, so two short sessions count together.
     */
    fun sessionsMeetMinimum(habit: Habit, sessionsOfDay: List<LearningSession>): Boolean {
        if (sessionsOfDay.isEmpty()) return false
        return when (habit.completionType) {
            CompletionType.MINUTES -> {
                val minutes = sessionsOfDay.sumOf { it.minutes.coerceAtLeast(0) }
                val required = habit.minimumMinutes.coerceAtLeast(MIN_MINUTES)
                minutes >= required ||
                    (habit.allowConceptCompletion && sessionsOfDay.any { it.conceptUnderstood })
            }
            // Logging a session for an action-based habit means the action happened.
            CompletionType.ACTION -> true
        }
    }

    enum class SyncAction { ADD, REMOVE, NONE }

    /**
     * Keeps session-derived completions consistent after a session is saved, edited or deleted.
     * Manual completions are never removed automatically.
     */
    fun syncAction(meetsMinimum: Boolean, existing: Completion?): SyncAction = when {
        meetsMinimum && existing == null -> SyncAction.ADD
        !meetsMinimum && existing?.source == CompletionSource.SESSION -> SyncAction.REMOVE
        else -> SyncAction.NONE
    }

    /** How many more minutes are needed today; 0 when the minimum is met. */
    fun minutesRemaining(habit: Habit, minutesToday: Int): Int =
        if (habit.completionType != CompletionType.MINUTES) 0
        else (habit.minimumMinutes - minutesToday).coerceAtLeast(0)
}

object DateMath {

    /** The local calendar day of an instant in a zone. Never compare raw UTC timestamps. */
    fun localDate(instant: Instant, zone: ZoneId): LocalDate = instant.atZone(zone).toLocalDate()

    fun today(clock: Clock): LocalDate = LocalDate.now(clock)

    /** Inclusive list of dates. */
    fun range(from: LocalDate, to: LocalDate): List<LocalDate> {
        if (to.isBefore(from)) return emptyList()
        val out = ArrayList<LocalDate>((to.toEpochDay() - from.toEpochDay() + 1).toInt())
        var d = from
        while (!d.isAfter(to)) {
            out += d
            d = d.plusDays(1)
        }
        return out
    }

    /** Monday of the week containing [date]. */
    fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

    /**
     * Calendar pickers report midnight UTC. Converting with UTC (not the device zone)
     * is what keeps a picked date from shifting by one day.
     */
    fun fromPickerMillis(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()

    fun toPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
}
