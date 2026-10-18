package com.malekroki.app.domain.backup

import com.malekroki.app.domain.AppData
import com.malekroki.app.domain.Completion
import com.malekroki.app.domain.CompletionRules
import com.malekroki.app.domain.CompletionSource
import com.malekroki.app.domain.CompletionType
import com.malekroki.app.domain.DailyReflection
import com.malekroki.app.domain.Habit
import com.malekroki.app.domain.LearningSession
import com.malekroki.app.domain.LeavePeriod
import com.malekroki.app.domain.LeaveType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException

sealed interface BackupValidation {
    data class Valid(val data: AppData, val warnings: List<String>) : BackupValidation
    data class Invalid(val errors: List<String>) : BackupValidation
}

/**
 * Turns a decoded [BackupDto] into [AppData], or explains in Polish what is wrong.
 * Nothing is written to the database unless validation succeeds.
 */
object BackupValidator {

    private const val MAX_REPORTED_ERRORS = 8

    fun validate(dto: BackupDto): BackupValidation {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (dto.format != BackupDto.FORMAT) {
            return BackupValidation.Invalid(listOf("To nie jest kopia zapasowa Małych Kroków."))
        }
        if (dto.version < 1) {
            return BackupValidation.Invalid(listOf("Brak numeru wersji kopii zapasowej."))
        }
        if (dto.version > BackupDto.CURRENT_VERSION) {
            return BackupValidation.Invalid(
                listOf("Ta kopia pochodzi z nowszej wersji aplikacji (wersja ${dto.version}). Zaktualizuj aplikację."),
            )
        }

        // Habits
        val habits = mutableListOf<Habit>()
        val habitIds = mutableSetOf<String>()
        dto.habits.forEachIndexed { i, h ->
            val label = "Nawyk #${i + 1}"
            if (h.id.isBlank()) { errors += "$label: brak identyfikatora."; return@forEachIndexed }
            if (!habitIds.add(h.id)) { errors += "$label: powtórzony identyfikator."; return@forEachIndexed }
            if (h.name.isBlank()) errors += "$label: brak nazwy."
            val type = enumOrNull<CompletionType>(h.completionType)
            if (type == null) errors += "$label: nieznany sposób zaliczania „${h.completionType}”."
            if (h.minimumMinutes !in CompletionRules.MIN_MINUTES..CompletionRules.MAX_MINUTES_PER_SESSION) {
                errors += "$label: minimum musi wynosić od 1 do 1440 minut."
            }
            val days = h.scheduledDays.mapNotNull { n -> if (n in 1..7) DayOfWeek.of(n) else null }.toSet()
            if (days.size != h.scheduledDays.toSet().size || days.isEmpty()) {
                errors += "$label: nieprawidłowe dni tygodnia."
            }
            val created = parseDate(h.createdDate)
            if (created == null) errors += "$label: nieprawidłowa data utworzenia."
            if (type != null && created != null && h.name.isNotBlank() && days.isNotEmpty()) {
                habits += Habit(
                    id = h.id,
                    name = h.name.trim(),
                    description = h.description,
                    category = h.category,
                    emoji = h.emoji.ifBlank { "✨" },
                    completionType = type,
                    minimumMinutes = h.minimumMinutes,
                    allowConceptCompletion = h.allowConceptCompletion,
                    minimumDescription = h.minimumDescription,
                    target = h.target,
                    countsTowardStreak = h.countsTowardStreak,
                    isLearning = h.isLearning,
                    scheduledDays = days,
                    createdDate = created,
                    archived = h.archived,
                    sortOrder = h.sortOrder,
                )
            }
        }

        // Completions
        val completions = mutableListOf<Completion>()
        val completionKeys = mutableSetOf<Pair<String, LocalDate>>()
        var duplicateCompletions = 0
        dto.completions.forEachIndexed { i, c ->
            val label = "Wykonanie #${i + 1}"
            if (c.habitId !in habitIds) { errors += "$label: odwołuje się do nieistniejącego nawyku."; return@forEachIndexed }
            val date = parseDate(c.date)
            if (date == null) { errors += "$label: nieprawidłowa data."; return@forEachIndexed }
            val source = enumOrNull<CompletionSource>(c.source) ?: CompletionSource.MANUAL
            if (completionKeys.add(c.habitId to date)) {
                completions += Completion(c.habitId, date, source, c.createdAt)
            } else {
                duplicateCompletions++
            }
        }
        if (duplicateCompletions > 0) {
            warnings += "Pominięto powtórzone wykonania: $duplicateCompletions."
        }

        // Sessions
        val sessions = mutableListOf<LearningSession>()
        val sessionIds = mutableSetOf<String>()
        dto.sessions.forEachIndexed { i, s ->
            val label = "Sesja #${i + 1}"
            if (s.id.isBlank() || !sessionIds.add(s.id)) { errors += "$label: brak lub powtórzony identyfikator."; return@forEachIndexed }
            if (s.habitId != null && s.habitId !in habitIds) { errors += "$label: odwołuje się do nieistniejącego nawyku."; return@forEachIndexed }
            val date = parseDate(s.date)
            if (date == null) { errors += "$label: nieprawidłowa data."; return@forEachIndexed }
            if (s.minutes !in 0..CompletionRules.MAX_MINUTES_PER_SESSION) { errors += "$label: nieprawidłowy czas trwania."; return@forEachIndexed }
            sessions += LearningSession(
                id = s.id, habitId = s.habitId, topic = s.topic, date = date, minutes = s.minutes,
                learned = s.learned, questions = s.questions, notes = s.notes,
                practicalExercise = s.practicalExercise, conceptUnderstood = s.conceptUnderstood,
                createdAt = s.createdAt,
            )
        }

        // Reflections
        val reflections = mutableListOf<DailyReflection>()
        val reflectionDates = mutableSetOf<LocalDate>()
        dto.reflections.forEachIndexed { i, r ->
            val date = parseDate(r.date)
            if (date == null) { errors += "Refleksja #${i + 1}: nieprawidłowa data."; return@forEachIndexed }
            if (reflectionDates.add(date) && r.text.isNotBlank()) {
                reflections += DailyReflection(date, r.text, r.updatedAt)
            }
        }

        // Leaves
        val leaves = mutableListOf<LeavePeriod>()
        val leaveIds = mutableSetOf<String>()
        dto.leaves.forEachIndexed { i, l ->
            val label = "Nieobecność #${i + 1}"
            if (l.id.isBlank() || !leaveIds.add(l.id)) { errors += "$label: brak lub powtórzony identyfikator."; return@forEachIndexed }
            val type = enumOrNull<LeaveType>(l.type)
            val start = parseDate(l.startDate)
            val end = parseDate(l.endDate)
            when {
                type == null -> errors += "$label: nieznany typ „${l.type}”."
                start == null || end == null -> errors += "$label: nieprawidłowe daty."
                end.isBefore(start) -> errors += "$label: koniec przed początkiem."
                l.habitIds.any { it !in habitIds } -> errors += "$label: odwołuje się do nieistniejącego nawyku."
                !l.appliesToAllHabits && l.habitIds.isEmpty() -> errors += "$label: nie wskazano nawyków."
                else -> leaves += LeavePeriod(
                    id = l.id, type = type, customName = l.customName, startDate = start, endDate = end,
                    note = l.note, appliesToAllHabits = l.appliesToAllHabits,
                    habitIds = if (l.appliesToAllHabits) emptySet() else l.habitIds.toSet(),
                    createdAt = l.createdAt,
                )
            }
        }

        if (errors.isNotEmpty()) {
            val shown = errors.take(MAX_REPORTED_ERRORS)
            val more = errors.size - shown.size
            return BackupValidation.Invalid(if (more > 0) shown + "…oraz $more innych problemów." else shown)
        }
        return BackupValidation.Valid(AppData(habits, completions, sessions, reflections, leaves), warnings)
    }

    private fun parseDate(text: String): LocalDate? = try {
        LocalDate.parse(text.trim())
    } catch (e: DateTimeParseException) {
        null
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String): T? =
        enumValues<T>().firstOrNull { it.name == name.trim() }
}
