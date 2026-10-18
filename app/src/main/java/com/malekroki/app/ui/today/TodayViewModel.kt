package com.malekroki.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malekroki.app.AppContainer
import com.malekroki.app.domain.DateMath
import com.malekroki.app.domain.DayStatus
import com.malekroki.app.domain.Habit
import com.malekroki.app.domain.HabitHistory
import com.malekroki.app.domain.LearningSession
import com.malekroki.app.domain.LeaveCalendar
import com.malekroki.app.domain.LeavePeriod
import com.malekroki.app.domain.LeaveRules
import com.malekroki.app.domain.LeaveStatus
import com.malekroki.app.domain.PolishText
import com.malekroki.app.domain.StreakCalculator
import com.malekroki.app.domain.StreakStats
import com.malekroki.app.ui.common.title
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitCardUi(
    val habit: Habit,
    val stats: StreakStats,
    val minutesToday: Int,
    /** Title of the leave protecting this habit today, if any. */
    val leaveTitle: String?,
)

data class WeekDayUi(
    val date: LocalDate,
    val done: Int,
    val planned: Int,
    val protectedDay: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
)

data class SessionRowUi(val session: LearningSession, val topic: String, val emoji: String)

data class TodayUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val greeting: String = "",
    val line: String = "",
    val habits: List<HabitCardUi> = emptyList(),
    val week: List<WeekDayUi> = emptyList(),
    val recentSessions: List<SessionRowUi> = emptyList(),
    val activeLeaves: List<LeavePeriod> = emptyList(),
    val upcomingLeaves: List<LeavePeriod> = emptyList(),
    val hasLearningHabits: Boolean = false,
) {
    val doneToday: Int get() = habits.count { it.stats.completedToday }
}

sealed interface TodayEvent {
    data class Completed(val habitId: String, val date: LocalDate, val message: String) : TodayEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.repository
    private val dates = container.dates
    private val todayFlow = dates.todayFlow()

    private val events = Channel<TodayEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val state: StateFlow<TodayUiState> = combine(
        todayFlow, repo.habits, repo.completions, repo.sessions, repo.leaves,
    ) { today, habits, completions, sessions, leaves ->
        buildState(today, habits, completions.groupBy({ it.habitId }, { it.date }), sessions, leaves)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    val reflection: StateFlow<String?> = todayFlow
        .flatMapLatest { day -> repo.reflection(day) }
        .map { it?.text ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun buildState(
        today: LocalDate,
        allHabits: List<Habit>,
        completionsByHabit: Map<String, List<LocalDate>>,
        sessions: List<LearningSession>,
        leaves: List<LeavePeriod>,
    ): TodayUiState {
        val calendar = LeaveCalendar(leaves)
        val active = allHabits.filter { !it.archived }
        val habitsById = allHabits.associateBy { it.id }
        val histories = active.associate { it.id to HabitHistory(it, completionsByHabit[it.id].orEmpty()) }

        val todays = active.filter { h ->
            today.dayOfWeek in h.scheduledDays || today in histories.getValue(h.id).completions
        }
        val cards = todays.map { h ->
            val history = histories.getValue(h.id)
            HabitCardUi(
                habit = h,
                stats = StreakCalculator.stats(history, today, calendar),
                minutesToday = sessions.filter { it.habitId == h.id && it.date == today }.sumOf { it.minutes },
                leaveTitle = calendar.leavesFor(h.id, today).firstOrNull()?.title(),
            )
        }

        val weekStart = DateMath.weekStart(today)
        val week = (0L..6L).map { offset ->
            val date = weekStart.plusDays(offset)
            var done = 0
            var planned = 0
            var protectedAny = false
            for (h in active) {
                when (StreakCalculator.dayStatus(histories.getValue(h.id), date, today, calendar)) {
                    DayStatus.COMPLETED -> { done++; planned++ }
                    DayStatus.MISSED, DayStatus.PENDING, DayStatus.FUTURE -> planned++
                    DayStatus.PROTECTED, DayStatus.FUTURE_PROTECTED -> protectedAny = true
                    else -> Unit
                }
            }
            WeekDayUi(date, done, planned, protectedAny, date == today, date.isAfter(today))
        }

        val recent = sessions.take(3).map { s ->
            val habit = s.habitId?.let(habitsById::get)
            SessionRowUi(s, habit?.name ?: s.topic.ifBlank { "Nauka" }, habit?.emoji ?: "📘")
        }

        return TodayUiState(
            loading = false,
            today = today,
            greeting = PolishText.greeting(dates.timeNow()),
            line = PolishText.lineOfTheDay(today),
            habits = cards,
            week = week,
            recentSessions = recent,
            activeLeaves = leaves.filter { LeaveRules.status(it, today) == LeaveStatus.ACTIVE },
            upcomingLeaves = leaves.filter { LeaveRules.status(it, today) == LeaveStatus.UPCOMING }.take(2),
            hasLearningHabits = active.any { it.isLearning },
        )
    }

    fun markDone(habit: Habit) {
        viewModelScope.launch {
            val today = dates.today()
            val inserted = repo.markDone(habit.id, today)
            if (inserted) {
                events.send(
                    TodayEvent.Completed(habit.id, today, PolishText.celebration((dates.nowMillis() / 1000).toInt())),
                )
            }
        }
    }

    fun undo(habitId: String, date: LocalDate) {
        viewModelScope.launch { repo.removeCompletion(habitId, date) }
    }

    fun saveReflection(text: String) {
        viewModelScope.launch { repo.saveReflection(dates.today(), text) }
    }
}
