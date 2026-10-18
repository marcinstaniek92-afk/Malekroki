package com.malekroki.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY archived ASC, sortOrder ASC, createdDate ASC, name ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun get(id: String): HabitEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM habits")
    suspend fun nextSortOrder(): Int

    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Insert
    suspend fun insertAll(habits: List<HabitEntity>)

    @Query("UPDATE habits SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM habits")
    suspend fun deleteAll()
}

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions")
    fun observeAll(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions")
    suspend fun getAll(): List<CompletionEntity>

    @Query("SELECT * FROM completions WHERE date = :date")
    suspend fun getForDate(date: LocalDate): List<CompletionEntity>

    @Query("SELECT * FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun get(habitId: String, date: LocalDate): CompletionEntity?

    /** Returns -1 when a completion for that habit and day already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(completion: CompletionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(completions: List<CompletionEntity>)

    @Query("DELETE FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun delete(habitId: String, date: LocalDate)

    @Query("DELETE FROM completions")
    suspend fun deleteAll()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions")
    suspend fun getAll(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE habitId = :habitId AND date = :date")
    suspend fun getFor(habitId: String, date: LocalDate): List<SessionEntity>

    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Insert
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}

@Dao
interface ReflectionDao {
    @Query("SELECT * FROM reflections WHERE date = :date")
    fun observe(date: LocalDate): Flow<ReflectionEntity?>

    @Query("SELECT * FROM reflections")
    suspend fun getAll(): List<ReflectionEntity>

    @Upsert
    suspend fun upsert(reflection: ReflectionEntity)

    @Insert
    suspend fun insertAll(reflections: List<ReflectionEntity>)

    @Query("DELETE FROM reflections WHERE date = :date")
    suspend fun delete(date: LocalDate)

    @Query("DELETE FROM reflections")
    suspend fun deleteAll()
}

@Dao
interface LeaveDao {
    @Transaction
    @Query("SELECT * FROM leaves ORDER BY startDate ASC")
    fun observeAll(): Flow<List<LeaveWithHabits>>

    @Transaction
    @Query("SELECT * FROM leaves")
    suspend fun getAll(): List<LeaveWithHabits>

    @Upsert
    suspend fun upsert(leave: LeaveEntity)

    @Insert
    suspend fun insertAll(leaves: List<LeaveEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHabitLinks(links: List<LeaveHabitEntity>)

    @Query("DELETE FROM leave_habits WHERE leaveId = :leaveId")
    suspend fun deleteHabitLinks(leaveId: String)

    @Query("DELETE FROM leaves WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM leaves")
    suspend fun deleteAll()
}
