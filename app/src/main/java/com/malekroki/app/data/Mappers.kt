package com.malekroki.app.data

import com.malekroki.app.data.db.CompletionEntity
import com.malekroki.app.data.db.HabitEntity
import com.malekroki.app.data.db.LeaveEntity
import com.malekroki.app.data.db.LeaveHabitEntity
import com.malekroki.app.data.db.LeaveWithHabits
import com.malekroki.app.data.db.ReflectionEntity
import com.malekroki.app.data.db.SessionEntity
import com.malekroki.app.domain.Completion
import com.malekroki.app.domain.CompletionSource
import com.malekroki.app.domain.CompletionType
import com.malekroki.app.domain.DailyReflection
import com.malekroki.app.domain.Habit
import com.malekroki.app.domain.LearningSession
import com.malekroki.app.domain.LeavePeriod
import com.malekroki.app.domain.LeaveType
import java.time.DayOfWeek

internal fun Set<DayOfWeek>.toMask(): Int = fold(0) { acc, day -> acc or (1 shl (day.value - 1)) }

internal fun Int.toDays(): Set<DayOfWeek> =
    DayOfWeek.entries.filter { this and (1 shl (it.value - 1)) != 0 }.toSet()

private inline fun <reified T : Enum<T>> enumOr(name: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: fallback

internal fun HabitEntity.toDomain() = Habit(
    id = id, name = name, description = description, category = category, emoji = emoji,
    completionType = enumOr(completionType, CompletionType.ACTION),
    minimumMinutes = minimumMinutes, allowConceptCompletion = allowConceptCompletion,
    minimumDescription = minimumDescription, target = target,
    countsTowardStreak = countsTowardStreak, isLearning = isLearning,
    scheduledDays = scheduledDaysMask.toDays().ifEmpty { DayOfWeek.entries.toSet() },
    createdDate = createdDate, archived = archived, sortOrder = sortOrder,
)

internal fun Habit.toEntity() = HabitEntity(
    id = id, name = name, description = description, category = category, emoji = emoji,
    completionType = completionType.name, minimumMinutes = minimumMinutes,
    allowConceptCompletion = allowConceptCompletion, minimumDescription = minimumDescription,
    target = target, countsTowardStreak = countsTowardStreak, isLearning = isLearning,
    scheduledDaysMask = scheduledDays.toMask(), createdDate = createdDate,
    archived = archived, sortOrder = sortOrder,
)

internal fun CompletionEntity.toDomain() =
    Completion(habitId, date, enumOr(source, CompletionSource.MANUAL), createdAt)

internal fun Completion.toEntity() = CompletionEntity(habitId, date, source.name, createdAt)

internal fun SessionEntity.toDomain() = LearningSession(
    id = id, habitId = habitId, topic = topic, date = date, minutes = minutes,
    learned = learned, questions = questions, notes = notes,
    practicalExercise = practicalExercise, conceptUnderstood = conceptUnderstood, createdAt = createdAt,
)

internal fun LearningSession.toEntity() = SessionEntity(
    id = id, habitId = habitId, topic = topic, date = date, minutes = minutes,
    learned = learned, questions = questions, notes = notes,
    practicalExercise = practicalExercise, conceptUnderstood = conceptUnderstood, createdAt = createdAt,
)

internal fun ReflectionEntity.toDomain() = DailyReflection(date, text, updatedAt)
internal fun DailyReflection.toEntity() = ReflectionEntity(date, text, updatedAt)

internal fun LeaveWithHabits.toDomain() = LeavePeriod(
    id = leave.id,
    type = enumOr(leave.type, LeaveType.PERSONAL),
    customName = leave.customName,
    startDate = leave.startDate,
    endDate = leave.endDate,
    note = leave.note,
    appliesToAllHabits = leave.appliesToAllHabits,
    habitIds = habits.map { it.habitId }.toSet(),
    createdAt = leave.createdAt,
)

internal fun LeavePeriod.toEntity() = LeaveEntity(
    id = id, type = type.name, customName = customName, startDate = startDate, endDate = endDate,
    note = note, appliesToAllHabits = appliesToAllHabits, createdAt = createdAt,
)

internal fun LeavePeriod.links(): List<LeaveHabitEntity> =
    if (appliesToAllHabits) emptyList() else habitIds.map { LeaveHabitEntity(id, it) }
