package com.malekroki.app.domain

import java.time.LocalDate

/**
 * Answers "is this habit protected by leave on this day?".
 *
 * Protection is a pure set question, so overlapping leave periods can never
 * double-count a day: a date is either protected or it is not.
 */
class LeaveCalendar(leaves: List<LeavePeriod>) {

    private val leaves: List<LeavePeriod> = leaves.sortedBy { it.startDate }

    fun isProtected(habitId: String, date: LocalDate): Boolean =
        leaves.any { it.covers(habitId, date) }

    fun leavesOn(date: LocalDate): List<LeavePeriod> = leaves.filter { it.contains(date) }

    fun leavesFor(habitId: String, date: LocalDate): List<LeavePeriod> =
        leaves.filter { it.covers(habitId, date) }

    /** Distinct protected dates for a habit within [from]..[to] (inclusive). */
    fun protectedDates(habitId: String, from: LocalDate, to: LocalDate): Set<LocalDate> {
        if (to.isBefore(from)) return emptySet()
        val result = sortedSetOf<LocalDate>()
        for (leave in leaves) {
            if (!leave.appliesTo(habitId)) continue
            var d = maxOf(leave.startDate, from)
            val end = minOf(leave.endDate, to)
            while (!d.isAfter(end)) {
                result += d
                d = d.plusDays(1)
            }
        }
        return result
    }

    companion object {
        val EMPTY = LeaveCalendar(emptyList())
    }
}

enum class LeaveStatus { UPCOMING, ACTIVE, PAST }

object LeaveRules {

    fun status(leave: LeavePeriod, today: LocalDate): LeaveStatus = when {
        today.isBefore(leave.startDate) -> LeaveStatus.UPCOMING
        today.isAfter(leave.endDate) -> LeaveStatus.PAST
        else -> LeaveStatus.ACTIVE
    }

    /**
     * Validates a leave before saving. Overlaps are allowed (they are resolved
     * deterministically by [LeaveCalendar]); exact duplicates are not.
     * Returns human-readable Polish messages; empty list means valid.
     */
    fun validate(leave: LeavePeriod, existing: List<LeavePeriod>): List<String> {
        val errors = mutableListOf<String>()
        if (leave.endDate.isBefore(leave.startDate)) {
            errors += "Data końcowa nie może być wcześniejsza niż początkowa."
        }
        if (!leave.appliesToAllHabits && leave.habitIds.isEmpty()) {
            errors += "Wybierz przynajmniej jeden nawyk albo zaznacz wszystkie."
        }
        if (leave.type == LeaveType.CUSTOM && leave.customName.isBlank()) {
            errors += "Nadaj nazwę własnemu typowi nieobecności."
        }
        val duplicate = existing.any { other ->
            other.id != leave.id &&
                other.type == leave.type &&
                other.customName.trim() == leave.customName.trim() &&
                other.startDate == leave.startDate &&
                other.endDate == leave.endDate &&
                other.appliesToAllHabits == leave.appliesToAllHabits &&
                (leave.appliesToAllHabits || other.habitIds == leave.habitIds)
        }
        if (duplicate) errors += "Taka nieobecność już istnieje."
        return errors
    }

    /** Other leaves that overlap in time and share at least one protected habit. */
    fun overlapping(leave: LeavePeriod, existing: List<LeavePeriod>): List<LeavePeriod> =
        existing.filter { other ->
            other.id != leave.id &&
                !other.endDate.isBefore(leave.startDate) &&
                !other.startDate.isAfter(leave.endDate) &&
                (other.appliesToAllHabits || leave.appliesToAllHabits ||
                    other.habitIds.any { it in leave.habitIds })
        }
}
