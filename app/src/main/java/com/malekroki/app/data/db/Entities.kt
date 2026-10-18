package com.malekroki.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val emoji: String,
    val completionType: String,
    val minimumMinutes: Int,
    val allowConceptCompletion: Boolean,
    val minimumDescription: String,
    val target: String,
    val countsTowardStreak: Boolean,
    val isLearning: Boolean,
    /** Bit mask, bit 0 = Monday … bit 6 = Sunday. */
    val scheduledDaysMask: Int,
    val createdDate: LocalDate,
    val archived: Boolean,
    val sortOrder: Int,
)

/** Composite primary key (habitId, date): the database itself rejects a second completion per day. */
@Entity(
    tableName = "completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("date")],
)
data class CompletionEntity(
    val habitId: String,
    val date: LocalDate,
    val source: String,
    val createdAt: Long,
)

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("habitId"), Index("date")],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val habitId: String?,
    val topic: String,
    val date: LocalDate,
    val minutes: Int,
    val learned: String,
    val questions: String,
    val notes: String,
    val practicalExercise: Boolean,
    val conceptUnderstood: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "reflections")
data class ReflectionEntity(
    @PrimaryKey val date: LocalDate,
    val text: String,
    val updatedAt: Long,
)

@Entity(tableName = "leaves", indices = [Index("startDate"), Index("endDate")])
data class LeaveEntity(
    @PrimaryKey val id: String,
    val type: String,
    val customName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val note: String,
    val appliesToAllHabits: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "leave_habits",
    primaryKeys = ["leaveId", "habitId"],
    foreignKeys = [
        ForeignKey(
            entity = LeaveEntity::class,
            parentColumns = ["id"],
            childColumns = ["leaveId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId")],
)
data class LeaveHabitEntity(
    val leaveId: String,
    val habitId: String,
)

data class LeaveWithHabits(
    @Embedded val leave: LeaveEntity,
    @Relation(parentColumn = "id", entityColumn = "leaveId")
    val habits: List<LeaveHabitEntity>,
)
