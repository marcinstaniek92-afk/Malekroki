package com.malekroki.app.data

import com.malekroki.app.domain.AppData
import com.malekroki.app.domain.Completion
import com.malekroki.app.domain.CompletionType
import com.malekroki.app.domain.Habit
import com.malekroki.app.domain.backup.BackupValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Full JSON text round trip (runs with ./gradlew test; uses the real kotlinx.serialization). */
class BackupJsonTest {

    private val data = AppData(
        habits = listOf(
            Habit(id = "h", name = "Życie osobiste", emoji = "🌿", completionType = CompletionType.ACTION, createdDate = LocalDate.of(2026, 10, 1)),
        ),
        completions = listOf(Completion("h", LocalDate.of(2026, 10, 18))),
    )

    @Test
    fun encodeThenDecodeGivesTheSameData() {
        val json = BackupJson.encode(data, "2026-10-18T10:00:00")
        assertTrue(json.contains("\"format\": \"male-kroki-backup\""))
        val result = BackupJson.decodeAndValidate(json)
        assertTrue(result is BackupValidation.Valid)
        assertEquals(data, (result as BackupValidation.Valid).data)
    }

    @Test
    fun brokenJsonIsRejected() {
        assertTrue(BackupJson.decodeAndValidate("{ nope") is BackupValidation.Invalid)
        assertTrue(BackupJson.decodeAndValidate("") is BackupValidation.Invalid)
        assertTrue(BackupJson.decodeAndValidate("[1,2,3]") is BackupValidation.Invalid)
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val json = """{"format":"male-kroki-backup","version":1,"somethingNew":true,"habits":[]}"""
        assertTrue(BackupJson.decodeAndValidate(json) is BackupValidation.Valid)
    }
}
