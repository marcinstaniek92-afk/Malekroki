package com.malekroki.app.ui.leave

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malekroki.app.AppContainer
import com.malekroki.app.data.AppRepository
import com.malekroki.app.domain.Habit
import com.malekroki.app.domain.LeavePeriod
import com.malekroki.app.domain.LeaveRules
import com.malekroki.app.domain.LeaveStatus
import com.malekroki.app.domain.LeaveType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LeaveRowUi(val leave: LeavePeriod, val habitNames: List<String>, val status: LeaveStatus)

data class LeaveListUiState(
    val loading: Boolean = true,
    val active: List<LeaveRowUi> = emptyList(),
    val upcoming: List<LeaveRowUi> = emptyList(),
    val past: List<LeaveRowUi> = emptyList(),
)

class LeaveListViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<LeaveListUiState> = combine(
        container.dates.todayFlow(), container.repository.leaves, container.repository.habits,
    ) { today, leaves, habits ->
        val names = habits.associate { it.id to "${it.emoji} ${it.name}" }
        val rows = leaves.map { l ->
            LeaveRowUi(l, if (l.appliesToAllHabits) emptyList() else l.habitIds.mapNotNull(names::get), LeaveRules.status(l, today))
        }
        LeaveListUiState(
            loading = false,
            active = rows.filter { it.status == LeaveStatus.ACTIVE },
            upcoming = rows.filter { it.status == LeaveStatus.UPCOMING },
            past = rows.filter { it.status == LeaveStatus.PAST }.sortedByDescending { it.leave.startDate },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeaveListUiState())
}

data class LeaveForm(
    val type: LeaveType = LeaveType.VACATION,
    val customName: String = "",
    val startDate: LocalDate,
    val endDate: LocalDate,
    val note: String = "",
    val appliesToAllHabits: Boolean = true,
    val habitIds: Set<String> = emptySet(),
) {
    val days: Long get() = endDate.toEpochDay() - startDate.toEpochDay() + 1
}

sealed interface LeaveEditEvent {
    data object Done : LeaveEditEvent
}

class LeaveEditViewModel(container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val repo = container.repository
    private val dates = container.dates
    val leaveId: String? = handle.get<String>("id")?.takeIf { it.isNotBlank() }
    private var original: LeavePeriod? = null

    private val _form = MutableStateFlow(LeaveForm(startDate = dates.today(), endDate = dates.today()))
    val form: StateFlow<LeaveForm> = _form.asStateFlow()

    private val _errors = MutableStateFlow<List<String>>(emptyList())
    val errors: StateFlow<List<String>> = _errors.asStateFlow()

    private val _loaded = MutableStateFlow(leaveId == null)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    val habits: StateFlow<List<Habit>> = combine(repo.habits, _form) { all, f ->
        all.filter { !it.archived || it.id in f.habitIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Other leaves overlapping the current form, for an informative (not blocking) note. */
    val overlaps: StateFlow<Int> = combine(repo.leaves, _form) { leaves, f ->
        LeaveRules.overlapping(toLeave(f, leaveId ?: ""), leaves).size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val events = Channel<LeaveEditEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    init {
        if (leaveId != null) {
            viewModelScope.launch {
                repo.leaves.first().firstOrNull { it.id == leaveId }?.let { l ->
                    original = l
                    _form.value = LeaveForm(
                        type = l.type, customName = l.customName, startDate = l.startDate, endDate = l.endDate,
                        note = l.note, appliesToAllHabits = l.appliesToAllHabits, habitIds = l.habitIds,
                    )
                }
                _loaded.value = true
            }
        }
    }

    fun update(transform: (LeaveForm) -> LeaveForm) {
        _form.update(transform)
        _errors.value = emptyList()
    }

    private fun toLeave(f: LeaveForm, id: String) = LeavePeriod(
        id = id,
        type = f.type,
        customName = if (f.type == LeaveType.CUSTOM) f.customName.trim() else "",
        startDate = f.startDate,
        endDate = f.endDate,
        note = f.note.trim(),
        appliesToAllHabits = f.appliesToAllHabits,
        habitIds = if (f.appliesToAllHabits) emptySet() else f.habitIds,
        createdAt = original?.createdAt ?: dates.nowMillis(),
    )

    fun save() {
        viewModelScope.launch {
            when (val result = repo.saveLeave(toLeave(_form.value, original?.id ?: repo.newId()))) {
                is AppRepository.LeaveSaveResult.Rejected -> _errors.value = result.errors
                AppRepository.LeaveSaveResult.Saved -> events.send(LeaveEditEvent.Done)
            }
        }
    }

    fun delete() {
        val id = leaveId ?: return
        viewModelScope.launch {
            repo.deleteLeave(id)
            events.send(LeaveEditEvent.Done)
        }
    }
}
