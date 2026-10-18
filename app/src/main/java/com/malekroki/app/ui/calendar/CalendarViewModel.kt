package com.malekroki.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malekroki.app.AppContainer
import com.malekroki.app.domain.ActivityRate
import com.malekroki.app.domain.DateMath
import com.malekroki.app.domain.DayStatus
import com.malekroki.app.domain.Habit
import com.malekroki.app.domain.HabitHistory
import com.malekroki.app.domain.LeaveCalendar
import com.malekroki.app.domain.LeavePeriod
import com.malekroki.app.domain.Statistics
import com.malekroki.app.domain.StreakCalculator
import com.malekroki.app.domain.StreakStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarDayUi(
    val date: LocalDate,
    val inMonth: Boolean,
    val isToday: Boolean,
    /** Status for a single selected habit; null in the "all habits" view. */
    val status: DayStatus?,
    val done: Int,
    val planned: Int,
    val protectedDay: Boolean,
    val futureProtected: Boolean,
)

data class DayDetailHabit(val habit: Habit, val status: DayStatus)

data class DayDetail(
    val date: LocalDate,
    val habits: List<DayDetailHabit>,
    val leaves: List<LeavePeriod>,
    val editable: Boolean,
)

data class CalendarUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val month: YearMonth = YearMonth.now(),
    val habits: List<Habit> = emptyList(),
    val selectedHabit: Habit? = null,
    val days: List<CalendarDayUi> = emptyList(),
    val streak: StreakStats? = null,
    val rate7: ActivityRate = ActivityRate(0, 0),
    val rate30: ActivityRate = ActivityRate(0, 0),
    val learningMinutes: Int = 0,
    val learningSessions: Int = 0,
    val detail: DayDetail? = null,
)

private data class Selection(val habitId: String?, val month: YearMonth?, val detailDate: LocalDate?)

class CalendarViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val selection = MutableStateFlow(Selection(null, null, null))

    private val data = combine(repo.habits, repo.completions, repo.sessions, repo.leaves) { h, c, s, l ->
        Quad(h, c.groupBy({ it.habitId }, { it.date }), s, l)
    }

    val state: StateFlow<CalendarUiState> = combine(container.dates.todayFlow(), data, selection) { today, d, sel ->
        build(today, d, sel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    private data class Quad(
        val habits: List<Habit>,
        val completions: Map<String, List<LocalDate>>,
        val sessions: List<com.malekroki.app.domain.LearningSession>,
        val leaves: List<LeavePeriod>,
    )

    private fun build(today: LocalDate, d: Quad, sel: Selection): CalendarUiState {
        val calendar = LeaveCalendar(d.leaves)
        val month = sel.month ?: YearMonth.from(today)
        val visibleHabits = d.habits.filter { !it.archived || it.id == sel.habitId }
        val selected = d.habits.firstOrNull { it.id == sel.habitId }
        val histories = d.habits.associate { it.id to HabitHistory(it, d.completions[it.id].orEmpty()) }
        val scope = if (selected != null) listOf(selected) else d.habits.filter { !it.archived }

        val first = month.atDay(1)
        val gridStart = DateMath.weekStart(first)
        val gridEnd = DateMath.weekStart(month.atEndOfMonth()).plusDays(6)
        val days = DateMath.range(gridStart, gridEnd).map { date ->
            var done = 0
            var planned = 0
            var protectedAny = false
            var futureProtected = false
            var single: DayStatus? = null
            for (h in scope) {
                val status = StreakCalculator.dayStatus(histories.getValue(h.id), date, today, calendar)
                if (selected != null) single = status
                when (status) {
                    DayStatus.COMPLETED -> { done++; planned++ }
                    DayStatus.MISSED, DayStatus.PENDING, DayStatus.FUTURE -> planned++
                    DayStatus.PROTECTED -> protectedAny = true
                    DayStatus.FUTURE_PROTECTED -> futureProtected = true
                    else -> Unit
                }
            }
            CalendarDayUi(
                date = date,
                inMonth = YearMonth.from(date) == month,
                isToday = date == today,
                status = single,
                done = done,
                planned = planned,
                protectedDay = protectedAny,
                futureProtected = futureProtected,
            )
        }

        val scopeHistories = scope.map { histories.getValue(it.id) }
        val sessions = if (selected != null) d.sessions.filter { it.habitId == selected.id } else d.sessions
        val past = sessions.filter { !it.date.isAfter(today) }

        val detail = sel.detailDate?.let { date ->
            DayDetail(
                date = date,
                habits = d.habits
                    .filter { !it.archived || date in histories.getValue(it.id).completions }
                    .map { DayDetailHabit(it, StreakCalculator.dayStatus(histories.getValue(it.id), date, today, calendar)) },
                leaves = calendar.leavesOn(date),
                editable = !date.isAfter(today),
            )
        }

        return CalendarUiState(
            loading = false,
            today = today,
            month = month,
            habits = visibleHabits,
            selectedHabit = selected,
            days = days,
            streak = selected?.let { StreakCalculator.stats(histories.getValue(it.id), today, calendar) },
            rate7 = Statistics.combinedActivityRate(scopeHistories, today, calendar, 7),
            rate30 = Statistics.combinedActivityRate(scopeHistories, today, calendar, 30),
            learningMinutes = past.sumOf { it.minutes },
            learningSessions = past.size,
            detail = detail,
        )
    }

    fun selectHabit(id: String?) = selection.value.let { selection.value = it.copy(habitId = id) }

    fun changeMonth(delta: Long) {
        val current = state.value.month
        selection.value = selection.value.copy(month = current.plusMonths(delta))
    }

    fun goToToday() {
        selection.value = selection.value.copy(month = null)
    }

    fun openDay(date: LocalDate) {
        selection.value = selection.value.copy(detailDate = date)
    }

    fun closeDay() {
        selection.value = selection.value.copy(detailDate = null)
    }

    fun setCompleted(habitId: String, date: LocalDate, completed: Boolean) {
        viewModelScope.launch {
            if (completed) repo.markDone(habitId, date) else repo.removeCompletion(habitId, date)
        }
    }
}
