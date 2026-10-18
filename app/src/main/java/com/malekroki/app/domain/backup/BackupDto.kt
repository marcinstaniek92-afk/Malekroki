package com.malekroki.app.domain.backup

import kotlinx.serialization.Serializable

/**
 * JSON backup format, version 1.
 *
 * Dates are ISO-8601 local dates ("2026-10-18"), enums are their names.
 * Every field has a default so older or hand-edited files still decode;
 * correctness is then checked by [BackupValidator].
 */
@Serializable
data class BackupDto(
    val format: String = FORMAT,
    val version: Int = 0,
    val exportedAt: String = "",
    val habits: List<HabitDto> = emptyList(),
    val completions: List<CompletionDto> = emptyList(),
    val sessions: List<SessionDto> = emptyList(),
    val reflections: List<ReflectionDto> = emptyList(),
    val leaves: List<LeaveDto> = emptyList(),
) {
    companion object {
        const val FORMAT = "male-kroki-backup"
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class HabitDto(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val emoji: String = "",
    val completionType: String = "",
    val minimumMinutes: Int = 5,
    val allowConceptCompletion: Boolean = false,
    val minimumDescription: String = "",
    val target: String = "",
    val countsTowardStreak: Boolean = true,
    val isLearning: Boolean = false,
    /** ISO day numbers, 1 = Monday … 7 = Sunday. */
    val scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val createdDate: String = "",
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

@Serializable
data class CompletionDto(
    val habitId: String = "",
    val date: String = "",
    val source: String = "MANUAL",
    val createdAt: Long = 0L,
)

@Serializable
data class SessionDto(
    val id: String = "",
    val habitId: String? = null,
    val topic: String = "",
    val date: String = "",
    val minutes: Int = 0,
    val learned: String = "",
    val questions: String = "",
    val notes: String = "",
    val practicalExercise: Boolean = false,
    val conceptUnderstood: Boolean = false,
    val createdAt: Long = 0L,
)

@Serializable
data class ReflectionDto(
    val date: String = "",
    val text: String = "",
    val updatedAt: Long = 0L,
)

@Serializable
data class LeaveDto(
    val id: String = "",
    val type: String = "",
    val customName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val note: String = "",
    val appliesToAllHabits: Boolean = true,
    val habitIds: List<String> = emptyList(),
    val createdAt: Long = 0L,
)
