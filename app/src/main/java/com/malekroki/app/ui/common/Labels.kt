package com.malekroki.app.ui.common

import com.malekroki.app.domain.Habit
import com.malekroki.app.domain.CompletionType
import com.malekroki.app.domain.LeavePeriod
import com.malekroki.app.domain.LeaveType
import com.malekroki.app.domain.PolishText
import java.time.DayOfWeek

fun LeaveType.label(): String = when (this) {
    LeaveType.VACATION -> "Urlop / Wakacje"
    LeaveType.SICK -> "Choroba"
    LeaveType.HOLIDAY -> "Święta"
    LeaveType.PERSONAL -> "Przerwa osobista"
    LeaveType.CUSTOM -> "Własny typ"
}

fun LeaveType.emoji(): String = when (this) {
    LeaveType.VACATION -> "🏖️"
    LeaveType.SICK -> "🤒"
    LeaveType.HOLIDAY -> "🎄"
    LeaveType.PERSONAL -> "🫖"
    LeaveType.CUSTOM -> "📌"
}

fun LeavePeriod.title(): String =
    if (type == LeaveType.CUSTOM && customName.isNotBlank()) customName else type.label()

fun DayOfWeek.shortLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "Pn"
    DayOfWeek.TUESDAY -> "Wt"
    DayOfWeek.WEDNESDAY -> "Śr"
    DayOfWeek.THURSDAY -> "Cz"
    DayOfWeek.FRIDAY -> "Pt"
    DayOfWeek.SATURDAY -> "So"
    DayOfWeek.SUNDAY -> "Nd"
}

/** What counts as "done" for this habit, in one line. */
fun Habit.minimumLabel(): String = minimumDescription.ifBlank {
    when (completionType) {
        CompletionType.MINUTES ->
            if (allowConceptCompletion) "${PolishText.minutes(minimumMinutes)} lub jedno zrozumiane pojęcie"
            else PolishText.minutes(minimumMinutes)
        CompletionType.ACTION -> "Jedno małe działanie"
    }
}

fun Habit.scheduleLabel(): String = when {
    scheduledDays.size == 7 -> "Codziennie"
    scheduledDays == setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY) -> "W dni robocze"
    scheduledDays == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "W weekendy"
    else -> scheduledDays.sorted().joinToString(", ") { it.shortLabel() }
}
