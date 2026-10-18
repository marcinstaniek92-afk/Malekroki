package com.malekroki.app.domain.backup

import com.malekroki.app.domain.AppData
import com.malekroki.app.domain.Completion
import com.malekroki.app.domain.DailyReflection
import com.malekroki.app.domain.Habit
import com.malekroki.app.domain.LearningSession
import com.malekroki.app.domain.LeavePeriod
import java.time.LocalDate

object BackupMapper {

    fun toDto(data: AppData, exportedAt: String): BackupDto = BackupDto(
        format = BackupDto.FORMAT,
        version = BackupDto.CURRENT_VERSION,
        exportedAt = exportedAt,
        habits = data.habits.map { h ->
            HabitDto(
                id = h.id, name = h.name, description = h.description, category = h.category,
                emoji = h.emoji, completionType = h.completionType.name,
                minimumMinutes = h.minimumMinutes, allowConceptCompletion = h.allowConceptCompletion,
                minimumDescription = h.minimumDescription, target = h.target,
                countsTowardStreak = h.countsTowardStreak, isLearning = h.isLearning,
                scheduledDays = h.scheduledDays.map { it.value }.sorted(),
                createdDate = h.createdDate.toString(), archived = h.archived, sortOrder = h.sortOrder,
            )
        },
        completions = data.completions.sortedWith(compareBy({ it.habitId }, { it.date })).map {
            CompletionDto(it.habitId, it.date.toString(), it.source.name, it.createdAt)
        },
        sessions = data.sessions.sortedBy { it.date }.map { s ->
            SessionDto(
                id = s.id, habitId = s.habitId, topic = s.topic, date = s.date.toString(),
                minutes = s.minutes, learned = s.learned, questions = s.questions, notes = s.notes,
                practicalExercise = s.practicalExercise, conceptUnderstood = s.conceptUnderstood,
                createdAt = s.createdAt,
            )
        },
        reflections = data.reflections.sortedBy { it.date }.map {
            ReflectionDto(it.date.toString(), it.text, it.updatedAt)
        },
        leaves = data.leaves.sortedBy { it.startDate }.map { l ->
            LeaveDto(
                id = l.id, type = l.type.name, customName = l.customName,
                startDate = l.startDate.toString(), endDate = l.endDate.toString(), note = l.note,
                appliesToAllHabits = l.appliesToAllHabits, habitIds = l.habitIds.sorted(),
                createdAt = l.createdAt,
            )
        },
    )
}

/**
 * Merges an imported snapshot into the existing one.
 * Records with the same id are replaced by the imported version; completions are
 * keyed by (habit, day) and an existing completion always wins, so nothing is double-counted.
 */
object BackupMerger {

    fun merge(existing: AppData, incoming: AppData): AppData {
        val habits = LinkedHashMap<String, Habit>()
        existing.habits.forEach { habits[it.id] = it }
        incoming.habits.forEach { habits[it.id] = it }

        val completions = LinkedHashMap<Pair<String, LocalDate>, Completion>()
        incoming.completions.forEach { completions[it.habitId to it.date] = it }
        existing.completions.forEach { completions[it.habitId to it.date] = it }

        val sessions = LinkedHashMap<String, LearningSession>()
        existing.sessions.forEach { sessions[it.id] = it }
        incoming.sessions.forEach { sessions[it.id] = it }

        val reflections = LinkedHashMap<LocalDate, DailyReflection>()
        existing.reflections.forEach { reflections[it.date] = it }
        incoming.reflections.forEach { r ->
            val current = reflections[r.date]
            reflections[r.date] = if (current == null || r.updatedAt >= current.updatedAt) r else current
        }

        val leaves = LinkedHashMap<String, LeavePeriod>()
        existing.leaves.forEach { leaves[it.id] = it }
        incoming.leaves.forEach { leaves[it.id] = it }

        val habitIds = habits.keys
        return AppData(
            habits = habits.values.toList(),
            completions = completions.values.filter { it.habitId in habitIds },
            sessions = sessions.values.map { if (it.habitId != null && it.habitId !in habitIds) it.copy(habitId = null) else it },
            reflections = reflections.values.toList(),
            leaves = leaves.values
                .map { l -> l.copy(habitIds = l.habitIds.filter { it in habitIds }.toSet()) }
                .filter { it.appliesToAllHabits || it.habitIds.isNotEmpty() },
        )
    }
}
