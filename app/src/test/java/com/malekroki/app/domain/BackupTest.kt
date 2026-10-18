package com.malekroki.app.domain

import com.malekroki.app.domain.backup.BackupDto
import com.malekroki.app.domain.backup.BackupMapper
import com.malekroki.app.domain.backup.BackupMerger
import com.malekroki.app.domain.backup.BackupValidation
import com.malekroki.app.domain.backup.BackupValidator
import com.malekroki.app.domain.backup.CompletionDto
import com.malekroki.app.domain.backup.HabitDto
import com.malekroki.app.domain.backup.LeaveDto
import com.malekroki.app.domain.backup.SessionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class BackupTest {

    private fun sample(): AppData {
        val js = habit(id = "js", type = CompletionType.MINUTES).copy(
            name = "JavaScript", emoji = "⚡", isLearning = true,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        )
        val life = habit(id = "life").copy(name = "Życie osobiste", archived = true)
        return AppData(
            habits = listOf(js, life),
            completions = listOf(
                Completion("js", d("2026-10-17"), CompletionSource.SESSION, 10),
                Completion("life", d("2026-10-18"), CompletionSource.MANUAL, 11),
            ),
            sessions = listOf(session(id = "s1", habitId = "js", date = "2026-10-17", minutes = 12, topic = "JavaScript")),
            reflections = listOf(DailyReflection(d("2026-10-18"), "Spokojny dzień.", 5)),
            leaves = listOf(leave(from = "2026-12-24", to = "2027-01-01", all = false, habits = setOf("js"), type = LeaveType.HOLIDAY)),
        )
    }

    private fun valid(dto: BackupDto): BackupValidation.Valid {
        val result = BackupValidator.validate(dto)
        assertTrue("Expected valid backup but got $result", result is BackupValidation.Valid)
        return result as BackupValidation.Valid
    }

    private fun invalid(dto: BackupDto): List<String> {
        val result = BackupValidator.validate(dto)
        assertTrue("Expected invalid backup", result is BackupValidation.Invalid)
        return (result as BackupValidation.Invalid).errors
    }

    @Test
    fun `exported data imports back unchanged`() {
        val data = sample()
        val restored = valid(BackupMapper.toDto(data, "2026-10-18T10:00:00")).data
        assertEquals(data.habits.toSet(), restored.habits.toSet())
        assertEquals(data.completions.toSet(), restored.completions.toSet())
        assertEquals(data.sessions, restored.sessions)
        assertEquals(data.reflections, restored.reflections)
        assertEquals(data.leaves, restored.leaves)
    }

    @Test
    fun `wrong format and newer version are rejected`() {
        val dto = BackupMapper.toDto(sample(), "")
        assertEquals(1, invalid(dto.copy(format = "something-else")).size)
        assertTrue(invalid(dto.copy(version = 99)).first().contains("nowszej"))
        assertEquals(1, invalid(dto.copy(version = 0)).size)
    }

    @Test
    fun `completion pointing to a missing habit is rejected`() {
        val dto = BackupMapper.toDto(sample(), "")
        val broken = dto.copy(completions = dto.completions + CompletionDto("ghost", "2026-10-18"))
        assertTrue(invalid(broken).single().contains("nieistniejącego"))
    }

    @Test
    fun `invalid dates, enums and minutes are reported`() {
        val dto = BackupDto(
            version = 1,
            habits = listOf(
                HabitDto(id = "a", name = "A", completionType = "MINUTES", createdDate = "2026-02-30"),
                HabitDto(id = "b", name = "B", completionType = "SOMETIMES", createdDate = "2026-01-01"),
                HabitDto(id = "c", name = "C", completionType = "ACTION", createdDate = "2026-01-01", minimumMinutes = 0),
            ),
        )
        assertEquals(3, invalid(dto).size)
    }

    @Test
    fun `duplicate habit ids are rejected`() {
        val h = HabitDto(id = "a", name = "A", completionType = "ACTION", createdDate = "2026-01-01")
        assertTrue(invalid(BackupDto(version = 1, habits = listOf(h, h))).single().contains("powtórzony"))
    }

    @Test
    fun `duplicate completions are skipped with a warning`() {
        val h = HabitDto(id = "a", name = "A", completionType = "ACTION", createdDate = "2026-01-01")
        val c = CompletionDto("a", "2026-10-18")
        val result = valid(BackupDto(version = 1, habits = listOf(h), completions = listOf(c, c)))
        assertEquals(1, result.data.completions.size)
        assertEquals(1, result.warnings.size)
    }

    @Test
    fun `leave and session validation`() {
        val h = HabitDto(id = "a", name = "A", completionType = "ACTION", createdDate = "2026-01-01")
        val badLeave = LeaveDto(id = "l", type = "SICK", startDate = "2026-10-10", endDate = "2026-10-01")
        val badSession = SessionDto(id = "s", habitId = "a", date = "2026-10-10", minutes = -5)
        assertEquals(2, invalid(BackupDto(version = 1, habits = listOf(h), leaves = listOf(badLeave), sessions = listOf(badSession))).size)
    }

    @Test
    fun `empty backup is valid`() {
        assertEquals(AppData(), valid(BackupDto(version = 1)).data)
    }

    @Test
    fun `merge keeps existing completions and does not double count`() {
        val existing = sample()
        val incoming = sample().copy(
            habits = sample().habits.map { if (it.id == "js") it.copy(name = "JS (nowa nazwa)") else it },
            completions = listOf(
                Completion("js", d("2026-10-17"), CompletionSource.MANUAL, 99),
                Completion("js", d("2026-10-18"), CompletionSource.MANUAL, 100),
            ),
        )
        val merged = BackupMerger.merge(existing, incoming)
        assertEquals(2, merged.habits.size)
        assertEquals("JS (nowa nazwa)", merged.habits.first { it.id == "js" }.name)
        assertEquals(3, merged.completions.size)
        assertEquals(CompletionSource.SESSION, merged.completions.first { it.habitId == "js" && it.date == d("2026-10-17") }.source)
    }

    @Test
    fun `merge adds new records from both sides`() {
        val other = AppData(
            habits = listOf(habit(id = "new")),
            sessions = listOf(session(id = "s2", habitId = "new")),
        )
        val merged = BackupMerger.merge(sample(), other)
        assertEquals(3, merged.habits.size)
        assertEquals(2, merged.sessions.size)
        assertEquals(1, merged.leaves.size)
    }
}
