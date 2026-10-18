package com.malekroki.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.malekroki.app.data.db.AppDatabase
import com.malekroki.app.domain.CompletionSource
import com.malekroki.app.domain.CompletionType
import com.malekroki.app.domain.Habit
import com.malekroki.app.domain.LearningSession
import com.malekroki.app.domain.LeavePeriod
import com.malekroki.app.domain.LeaveType
import com.malekroki.app.domain.backup.BackupValidation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Persistence and data-relationship tests against a real (in-memory) Room database.
 * Run with: ./gradlew connectedDebugAndroidTest (device or emulator required).
 */
@RunWith(AndroidJUnit4::class)
class RepositoryInstrumentedTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: AppRepository
    private val today = LocalDate.of(2026, 10, 18)
    private val clock = Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneId.of("Europe/Warsaw"))

    private val js = Habit(
        id = "js", name = "JavaScript", completionType = CompletionType.MINUTES, minimumMinutes = 5,
        isLearning = true, createdDate = LocalDate.of(2026, 10, 1),
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repo = AppRepository(db, SettingsRepository(context), DateProvider { clock })
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun duplicateCompletionIsRejectedByTheDatabase() = runBlocking {
        repo.saveHabit(js)
        assertTrue(repo.markDone("js", today))
        assertFalse(repo.markDone("js", today))
        assertEquals(1, repo.completions.first().size)
    }

    @Test
    fun fiveMinuteSessionCompletesHabitAndDeletingItRemovesCompletion() = runBlocking {
        repo.saveHabit(js)
        val session = LearningSession("s1", "js", "JavaScript", today, minutes = 5)
        assertTrue(repo.saveSession(session))
        assertEquals(CompletionSource.SESSION, repo.completionsOn(today).single().source)

        repo.deleteSession("s1")
        assertTrue(repo.completionsOn(today).isEmpty())
    }

    @Test
    fun manualCompletionSurvivesSessionDeletion() = runBlocking {
        repo.saveHabit(js)
        repo.markDone("js", today)
        repo.saveSession(LearningSession("s1", "js", "JavaScript", today, minutes = 2))
        repo.deleteSession("s1")
        assertEquals(1, repo.completionsOn(today).size)
    }

    @Test
    fun movingSessionToAnotherDayMovesTheCompletion() = runBlocking {
        repo.saveHabit(js)
        repo.saveSession(LearningSession("s1", "js", "JavaScript", today, minutes = 10))
        repo.saveSession(LearningSession("s1", "js", "JavaScript", today.minusDays(1), minutes = 10))
        assertTrue(repo.completionsOn(today).isEmpty())
        assertEquals(1, repo.completionsOn(today.minusDays(1)).size)
    }

    @Test
    fun deletingHabitCascadesCompletionsButKeepsJournal() = runBlocking {
        repo.saveHabit(js)
        repo.saveSession(LearningSession("s1", "js", "JavaScript", today, minutes = 10))
        repo.deleteHabit("js")
        assertTrue(repo.completions.first().isEmpty())
        val session = repo.getSession("s1")!!
        assertNull(session.habitId)
        assertEquals("JavaScript", session.topic)
    }

    @Test
    fun editingHabitKeepsHistory() = runBlocking {
        repo.saveHabit(js)
        repo.markDone("js", today.minusDays(1))
        repo.saveHabit(js.copy(name = "JS", minimumMinutes = 15))
        assertEquals("JS", repo.getHabit("js")!!.name)
        assertEquals(1, repo.completions.first().size)
    }

    @Test
    fun leaveWithSelectedHabitsRoundTripsAndDuplicatesAreRejected() = runBlocking {
        repo.saveHabit(js)
        val leave = LeavePeriod("l1", LeaveType.SICK, startDate = today, endDate = today.plusDays(3),
            appliesToAllHabits = false, habitIds = setOf("js"))
        assertEquals(AppRepository.LeaveSaveResult.Saved, repo.saveLeave(leave))
        assertEquals(setOf("js"), repo.leaves.first().single().habitIds)
        assertTrue(repo.saveLeave(leave.copy(id = "l2")) is AppRepository.LeaveSaveResult.Rejected)
    }

    @Test
    fun exportedJsonImportsBackIntoAnEmptyDatabase() = runBlocking {
        repo.saveHabit(js)
        repo.markDone("js", today)
        repo.saveSession(LearningSession("s1", "js", "JavaScript", today, minutes = 7, learned = "Closures"))
        repo.saveReflection(today, "Dobry dzień")
        val before = repo.snapshot()

        val json = BackupJson.encode(before, "2026-10-18T10:00:00")
        repo.deleteEverything()
        assertTrue(repo.habits.first().isEmpty())

        val result = BackupJson.decodeAndValidate(json)
        assertTrue(result is BackupValidation.Valid)
        repo.replaceAll((result as BackupValidation.Valid).data)
        val after = repo.snapshot()
        assertEquals(before.habits, after.habits)
        assertEquals(before.completions.toSet(), after.completions.toSet())
        assertEquals(before.sessions, after.sessions)
        assertEquals(before.reflections, after.reflections)
    }

    @Test
    fun invalidJsonIsRejectedWithoutTouchingData() = runBlocking {
        repo.saveHabit(js)
        val result = BackupJson.decodeAndValidate("{ to nie jest json")
        assertTrue(result is BackupValidation.Invalid)
        assertEquals(1, repo.habits.first().size)
    }
}
