package com.malekroki.app.ui.journal

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malekroki.app.AppContainer
import com.malekroki.app.domain.CompletionRules
import com.malekroki.app.domain.CompletionType
import com.malekroki.app.domain.Habit
import com.malekroki.app.domain.LearningSession
import com.malekroki.app.domain.LearningSummary
import com.malekroki.app.domain.Statistics
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
import java.time.LocalDate

data class JournalEntryUi(val session: LearningSession, val topic: String, val emoji: String)

data class JournalUiState(
    val loading: Boolean = true,
    val query: String = "",
    val onlyQuestions: Boolean = false,
    val summary: LearningSummary = LearningSummary(0, 0, emptyList(), emptyList()),
    val entries: List<JournalEntryUi> = emptyList(),
    val totalEntries: Int = 0,
    val hasLearningHabits: Boolean = false,
)

class JournalViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.repository
    private val query = MutableStateFlow("")
    private val onlyQuestions = MutableStateFlow(false)

    val state: StateFlow<JournalUiState> = combine(
        container.dates.todayFlow(), repo.sessions, repo.habits, query, onlyQuestions,
    ) { today, sessions, habits, q, questionsOnly ->
        val byId = habits.associateBy { it.id }
        fun topicOf(s: LearningSession) = s.habitId?.let { byId[it]?.name } ?: s.topic.ifBlank { "Nauka" }
        val entries = sessions.map { s -> JournalEntryUi(s, topicOf(s), s.habitId?.let { byId[it]?.emoji } ?: "📘") }
        val needle = q.trim().lowercase()
        val filtered = entries.filter { e ->
            (!questionsOnly || e.session.questions.isNotBlank()) &&
                (needle.isEmpty() || listOf(e.topic, e.session.learned, e.session.questions, e.session.notes)
                    .any { it.lowercase().contains(needle) })
        }
        JournalUiState(
            loading = false,
            query = q,
            onlyQuestions = questionsOnly,
            summary = Statistics.learningSummary(sessions, today, weeks = 8, topicOf = ::topicOf),
            entries = filtered,
            totalEntries = entries.size,
            hasLearningHabits = habits.any { it.isLearning && !it.archived },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())

    fun setQuery(value: String) { query.value = value }
    fun toggleQuestions() = onlyQuestions.update { !it }
}

data class SessionForm(
    val habitId: String? = null,
    val topic: String = "",
    val date: LocalDate = LocalDate.now(),
    val minutesText: String = "",
    val learned: String = "",
    val questions: String = "",
    val notes: String = "",
    val practicalExercise: Boolean = false,
    val conceptUnderstood: Boolean = false,
) {
    val minutes: Int? get() = minutesText.toIntOrNull()
    val minutesError: String?
        get() = when {
            minutesText.isBlank() -> null
            minutes == null || minutes!! > CompletionRules.MAX_MINUTES_PER_SESSION -> "Podaj liczbę minut (0–1440)."
            else -> null
        }
    val topicError: String? get() = if (habitId == null && topic.isBlank()) "Wybierz nawyk albo wpisz temat." else null
    val isValid: Boolean get() = minutesError == null && topicError == null
}

data class TimerState(val running: Boolean = false, val accumulatedMs: Long = 0L, val startedAt: Long = 0L) {
    fun elapsedMs(now: Long): Long = accumulatedMs + if (running) now - startedAt else 0L
}

sealed interface SessionEvent {
    data class Saved(val counted: Boolean) : SessionEvent
    data object Deleted : SessionEvent
}

class SessionEditViewModel(container: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val repo = container.repository
    private val dates = container.dates
    val sessionId: String? = handle.get<String>("id")?.takeIf { it.isNotBlank() }
    private var original: LearningSession? = null

    val today: LocalDate = dates.today()

    private val _form = MutableStateFlow(
        SessionForm(habitId = handle.get<String>("habitId")?.takeIf { it.isNotBlank() }, date = today),
    )
    val form: StateFlow<SessionForm> = _form.asStateFlow()

    private val _timer = MutableStateFlow(TimerState())
    val timer: StateFlow<TimerState> = _timer.asStateFlow()

    private val _loaded = MutableStateFlow(sessionId == null)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** Learning habits (plus the currently linked one, even if archived). */
    val habits: StateFlow<List<Habit>> = combine(repo.habits, _form) { habits, f ->
        habits.filter { (it.isLearning && !it.archived) || it.id == f.habitId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val events = Channel<SessionEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    init {
        if (sessionId != null) {
            viewModelScope.launch {
                repo.getSession(sessionId)?.let { s ->
                    original = s
                    _form.value = SessionForm(
                        habitId = s.habitId, topic = s.topic, date = s.date, minutesText = s.minutes.toString(),
                        learned = s.learned, questions = s.questions, notes = s.notes,
                        practicalExercise = s.practicalExercise, conceptUnderstood = s.conceptUnderstood,
                    )
                }
                _loaded.value = true
            }
        }
    }

    fun update(transform: (SessionForm) -> SessionForm) = _form.update(transform)

    fun toggleTimer() {
        val now = SystemClock.elapsedRealtime()
        _timer.update { t ->
            if (t.running) t.copy(running = false, accumulatedMs = t.elapsedMs(now))
            else t.copy(running = true, startedAt = now)
        }
    }

    /** Copies the timer into the minutes field, rounded to the nearest minute (at least 1). */
    fun useTimer() {
        val ms = _timer.value.elapsedMs(SystemClock.elapsedRealtime())
        val minutes = ((ms + 30_000) / 60_000).toInt().coerceAtLeast(1)
        _timer.update { it.copy(running = false, accumulatedMs = ms) }
        _form.update { it.copy(minutesText = minutes.toString()) }
    }

    fun resetTimer() { _timer.value = TimerState() }

    /** Live hint: does this entry on its own meet the habit's minimum? */
    fun meetsMinimumAlone(habit: Habit?, f: SessionForm): Boolean {
        if (habit == null) return false
        val draft = LearningSession("draft", habit.id, "", f.date, f.minutes ?: 0, conceptUnderstood = f.conceptUnderstood)
        return CompletionRules.sessionsMeetMinimum(habit, listOf(draft))
    }

    fun save() {
        val f = _form.value
        if (!f.isValid) return
        viewModelScope.launch {
            val habitName = f.habitId?.let { id -> repo.getHabit(id)?.name }
            val session = LearningSession(
                id = original?.id ?: repo.newId(),
                habitId = f.habitId,
                topic = habitName ?: f.topic.trim(),
                date = f.date,
                minutes = f.minutes ?: 0,
                learned = f.learned.trim(),
                questions = f.questions.trim(),
                notes = f.notes.trim(),
                practicalExercise = f.practicalExercise,
                conceptUnderstood = f.conceptUnderstood,
                createdAt = original?.createdAt ?: dates.nowMillis(),
            )
            val counted = repo.saveSession(session)
            events.send(SessionEvent.Saved(counted))
        }
    }

    fun delete() {
        val id = sessionId ?: return
        viewModelScope.launch {
            repo.deleteSession(id)
            events.send(SessionEvent.Deleted)
        }
    }

    companion object {
        fun isMinutes(habit: Habit?) = habit?.completionType == CompletionType.MINUTES
    }
}
