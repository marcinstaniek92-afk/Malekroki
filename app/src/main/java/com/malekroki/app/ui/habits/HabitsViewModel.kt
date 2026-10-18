package com.malekroki.app.ui.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malekroki.app.AppContainer
import com.malekroki.app.domain.ALL_DAYS
import com.malekroki.app.domain.CompletionType
import com.malekroki.app.domain.Habit
import com.malekroki.app.domain.HabitHistory
import com.malekroki.app.domain.LeaveCalendar
import com.malekroki.app.domain.StreakCalculator
import com.malekroki.app.domain.StreakStats
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class HabitRowUi(val habit: Habit, val stats: StreakStats)

data class HabitsUiState(
    val loading: Boolean = true,
    val active: List<HabitRowUi> = emptyList(),
    val archived: List<HabitRowUi> = emptyList(),
)

class HabitsViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.repository

    val state: StateFlow<HabitsUiState> = combine(
        container.dates.todayFlow(), repo.habits, repo.completions, repo.leaves,
    ) { today, habits, completions, leaves ->
        val byHabit = completions.groupBy({ it.habitId }, { it.date })
        val calendar = LeaveCalendar(leaves)
        val rows = habits.map { h ->
            HabitRowUi(h, StreakCalculator.stats(HabitHistory(h, byHabit[h.id].orEmpty()), today, calendar))
        }
        HabitsUiState(false, rows.filter { !it.habit.archived }, rows.filter { it.habit.archived })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsUiState())

    fun setArchived(id: String, archived: Boolean) {
        viewModelScope.launch { repo.setArchived(id, archived) }
    }

    fun restoreStarters() {
        viewModelScope.launch { repo.restoreStarterHabits() }
    }
}

data class HabitForm(
    val name: String = "",
    val emoji: String = "✨",
    val description: String = "",
    val category: String = "",
    val completionType: CompletionType = CompletionType.ACTION,
    val minimumMinutes: Int = 5,
    val allowConceptCompletion: Boolean = false,
    val minimumDescription: String = "",
    val target: String = "",
    val countsTowardStreak: Boolean = true,
    val isLearning: Boolean = false,
    val scheduledDays: Set<DayOfWeek> = ALL_DAYS,
    val archived: Boolean = false,
) {
    val nameError: String? get() = if (name.isBlank()) "Podaj nazwę nawyku." else null
    val daysError: String? get() = if (scheduledDays.isEmpty()) "Wybierz przynajmniej jeden dzień." else null
    val isValid: Boolean get() = nameError == null && daysError == null && minimumMinutes in 1..600
}

sealed interface HabitEditEvent {
    data object Saved : HabitEditEvent
    data object Deleted : HabitEditEvent
}

class HabitEditViewModel(container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val repo = container.repository
    private val dates = container.dates
    val habitId: String? = handle.get<String>("id")?.takeIf { it.isNotBlank() }
    private var original: Habit? = null

    private val _form = MutableStateFlow(HabitForm())
    val form: StateFlow<HabitForm> = _form.asStateFlow()

    private val _loaded = MutableStateFlow(habitId == null)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val events = Channel<HabitEditEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    init {
        if (habitId != null) {
            viewModelScope.launch {
                repo.getHabit(habitId)?.let { h ->
                    original = h
                    _form.value = HabitForm(
                        name = h.name, emoji = h.emoji, description = h.description, category = h.category,
                        completionType = h.completionType, minimumMinutes = h.minimumMinutes,
                        allowConceptCompletion = h.allowConceptCompletion, minimumDescription = h.minimumDescription,
                        target = h.target, countsTowardStreak = h.countsTowardStreak, isLearning = h.isLearning,
                        scheduledDays = h.scheduledDays, archived = h.archived,
                    )
                }
                _loaded.value = true
            }
        }
    }

    fun update(transform: (HabitForm) -> HabitForm) = _form.update(transform)

    fun save() {
        val f = _form.value
        if (!f.isValid) return
        viewModelScope.launch {
            val base = original
            val habit = Habit(
                id = base?.id ?: repo.newId(),
                name = f.name.trim(),
                description = f.description.trim(),
                category = f.category.trim(),
                emoji = f.emoji.ifBlank { "✨" },
                completionType = f.completionType,
                minimumMinutes = f.minimumMinutes,
                allowConceptCompletion = f.completionType == CompletionType.MINUTES && f.allowConceptCompletion,
                minimumDescription = f.minimumDescription.trim(),
                target = f.target.trim(),
                countsTowardStreak = f.countsTowardStreak,
                isLearning = f.isLearning,
                scheduledDays = f.scheduledDays,
                // Creation date and order never change on edit, so history stays intact.
                createdDate = base?.createdDate ?: dates.today(),
                archived = f.archived,
                sortOrder = base?.sortOrder ?: 0,
            )
            repo.saveHabit(habit)
            events.send(HabitEditEvent.Saved)
        }
    }

    fun setArchived(archived: Boolean) {
        val id = habitId ?: return
        viewModelScope.launch {
            repo.setArchived(id, archived)
            events.send(HabitEditEvent.Saved)
        }
    }

    fun delete() {
        val id = habitId ?: return
        viewModelScope.launch {
            repo.deleteHabit(id)
            events.send(HabitEditEvent.Deleted)
        }
    }
}
