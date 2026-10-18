package com.malekroki.app.data

import androidx.room.withTransaction
import com.malekroki.app.data.db.AppDatabase
import com.malekroki.app.data.db.CompletionEntity
import com.malekroki.app.data.db.ReflectionEntity
import com.malekroki.app.domain.AppData
import com.malekroki.app.domain.Completion
import com.malekroki.app.domain.CompletionRules
import com.malekroki.app.domain.CompletionSource
import com.malekroki.app.domain.DailyReflection
import com.malekroki.app.domain.Habit
import com.malekroki.app.domain.LearningSession
import com.malekroki.app.domain.LeaveCalendar
import com.malekroki.app.domain.LeavePeriod
import com.malekroki.app.domain.LeaveRules
import com.malekroki.app.domain.StarterHabits
import com.malekroki.app.domain.backup.BackupMerger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

/**
 * The only entry point to persisted data. All multi-step writes run in a single
 * Room transaction, so a crash can never leave half-updated streak data behind.
 */
class AppRepository(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val dates: DateProvider,
) {
    private val habitDao = db.habitDao()
    private val completionDao = db.completionDao()
    private val sessionDao = db.sessionDao()
    private val reflectionDao = db.reflectionDao()
    private val leaveDao = db.leaveDao()

    val habits: Flow<List<Habit>> = habitDao.observeAll().map { list -> list.map { it.toDomain() } }
    val completions: Flow<List<Completion>> = completionDao.observeAll().map { list -> list.map { it.toDomain() } }
    val sessions: Flow<List<LearningSession>> = sessionDao.observeAll().map { list -> list.map { it.toDomain() } }
    val leaves: Flow<List<LeavePeriod>> = leaveDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun reflection(date: LocalDate): Flow<DailyReflection?> = reflectionDao.observe(date).map { it?.toDomain() }

    fun newId(): String = UUID.randomUUID().toString()

    // region Starter habits

    suspend fun createStarterHabitsIfFirstLaunch() {
        if (settings.current().starterHabitsCreated) return
        db.withTransaction {
            if (habitDao.getAll().isEmpty()) {
                habitDao.insertAll(StarterHabits.create(dates.today(), ::newId).map { it.toEntity() })
            }
        }
        settings.markStarterHabitsCreated()
    }

    suspend fun restoreStarterHabits() {
        val offset = habitDao.nextSortOrder()
        val starters = StarterHabits.create(dates.today(), ::newId).map { it.copy(sortOrder = it.sortOrder + offset) }
        habitDao.insertAll(starters.map { it.toEntity() })
    }

    // endregion

    // region Habits

    suspend fun getHabit(id: String): Habit? = habitDao.get(id)?.toDomain()

    suspend fun saveHabit(habit: Habit) {
        val existing = habitDao.get(habit.id)
        val toSave = if (existing == null) habit.copy(sortOrder = habitDao.nextSortOrder()) else habit
        habitDao.upsert(toSave.toEntity())
    }

    suspend fun setArchived(id: String, archived: Boolean) = habitDao.setArchived(id, archived)

    /** Completions and leave links are removed by cascade; journal entries keep their topic. */
    suspend fun deleteHabit(id: String) = habitDao.delete(id)

    // endregion

    // region Completions

    /** @return false when the habit was already completed that day (duplicate prevented). */
    suspend fun markDone(habitId: String, date: LocalDate = dates.today()): Boolean =
        completionDao.insert(
            CompletionEntity(habitId, date, CompletionSource.MANUAL.name, dates.nowMillis()),
        ) != -1L

    suspend fun removeCompletion(habitId: String, date: LocalDate) = completionDao.delete(habitId, date)

    suspend fun completionsOn(date: LocalDate): List<Completion> = completionDao.getForDate(date).map { it.toDomain() }

    // endregion

    // region Sessions

    suspend fun getSession(id: String): LearningSession? = sessionDao.get(id)?.toDomain()

    /**
     * Saves a session and keeps the habit's completion in sync for both the new
     * and – after an edit – the previous habit/day.
     * @return true when the session made the habit count for its day.
     */
    suspend fun saveSession(session: LearningSession): Boolean = db.withTransaction<Boolean> {
        val previous = sessionDao.get(session.id)?.toDomain()
        sessionDao.upsert(session.toEntity())
        if (previous != null && (previous.habitId != session.habitId || previous.date != session.date)) {
            syncCompletion(previous.habitId, previous.date)
        }
        syncCompletion(session.habitId, session.date)
    }

    suspend fun deleteSession(id: String) {
        db.withTransaction {
            val previous = sessionDao.get(id)?.toDomain()
            if (previous != null) {
                sessionDao.delete(id)
                syncCompletion(previous.habitId, previous.date)
            }
        }
    }

    /** @return true when the habit counts as done on [date] after syncing. */
    private suspend fun syncCompletion(habitId: String?, date: LocalDate): Boolean {
        if (habitId == null) return false
        val habit = habitDao.get(habitId)?.toDomain() ?: return false
        val sessionsOfDay = sessionDao.getFor(habitId, date).map { it.toDomain() }
        val existing = completionDao.get(habitId, date)?.toDomain()
        val meets = CompletionRules.sessionsMeetMinimum(habit, sessionsOfDay)
        when (CompletionRules.syncAction(meets, existing)) {
            CompletionRules.SyncAction.ADD -> completionDao.insert(
                CompletionEntity(habitId, date, CompletionSource.SESSION.name, dates.nowMillis()),
            )
            CompletionRules.SyncAction.REMOVE -> completionDao.delete(habitId, date)
            CompletionRules.SyncAction.NONE -> Unit
        }
        return completionDao.get(habitId, date) != null
    }

    // endregion

    // region Reflections

    suspend fun saveReflection(date: LocalDate, text: String) {
        if (text.isBlank()) reflectionDao.delete(date)
        else reflectionDao.upsert(ReflectionEntity(date, text.trim(), dates.nowMillis()))
    }

    // endregion

    // region Leaves

    sealed interface LeaveSaveResult {
        data object Saved : LeaveSaveResult
        data class Rejected(val errors: List<String>) : LeaveSaveResult
    }

    suspend fun saveLeave(leave: LeavePeriod): LeaveSaveResult = db.withTransaction<LeaveSaveResult> {
        val existing = leaveDao.getAll().map { it.toDomain() }
        val errors = LeaveRules.validate(leave, existing)
        if (errors.isNotEmpty()) return@withTransaction LeaveSaveResult.Rejected(errors)
        leaveDao.upsert(leave.toEntity())
        leaveDao.deleteHabitLinks(leave.id)
        leaveDao.insertHabitLinks(leave.links())
        LeaveSaveResult.Saved
    }

    suspend fun deleteLeave(id: String) = leaveDao.delete(id)

    suspend fun currentLeaveCalendar(): LeaveCalendar = LeaveCalendar(leaveDao.getAll().map { it.toDomain() })

    // endregion

    // region Backup

    suspend fun snapshot(): AppData = db.withTransaction {
        AppData(
            habits = habitDao.getAll().map { it.toDomain() },
            completions = completionDao.getAll().map { it.toDomain() },
            sessions = sessionDao.getAll().map { it.toDomain() },
            reflections = reflectionDao.getAll().map { it.toDomain() },
            leaves = leaveDao.getAll().map { it.toDomain() },
        )
    }

    suspend fun replaceAll(data: AppData) = db.withTransaction {
        clearTables()
        writeAll(data)
    }

    suspend fun mergeIn(incoming: AppData) = db.withTransaction {
        val merged = BackupMerger.merge(snapshot(), incoming)
        clearTables()
        writeAll(merged)
    }

    /** Deletes every record. Starter habits are not recreated automatically afterwards. */
    suspend fun deleteEverything() = db.withTransaction { clearTables() }

    private suspend fun clearTables() {
        leaveDao.deleteAll() // cascades to leave_habits
        reflectionDao.deleteAll()
        sessionDao.deleteAll()
        completionDao.deleteAll()
        habitDao.deleteAll()
    }

    private suspend fun writeAll(data: AppData) {
        habitDao.insertAll(data.habits.map { it.toEntity() })
        completionDao.insertAll(data.completions.map { it.toEntity() })
        sessionDao.insertAll(data.sessions.map { it.toEntity() })
        reflectionDao.insertAll(data.reflections.map { it.toEntity() })
        leaveDao.insertAll(data.leaves.map { it.toEntity() })
        leaveDao.insertHabitLinks(data.leaves.flatMap { it.links() })
    }

    // endregion
}
