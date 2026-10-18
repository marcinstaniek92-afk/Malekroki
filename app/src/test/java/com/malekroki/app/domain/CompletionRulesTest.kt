package com.malekroki.app.domain

import com.malekroki.app.domain.CompletionRules.SyncAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class CompletionRulesTest {

    private val learning = habit(type = CompletionType.MINUTES, minimumMinutes = 5)

    @Test
    fun `five minute session meets a five minute minimum`() {
        assertTrue(CompletionRules.sessionsMeetMinimum(learning, listOf(session(minutes = 5))))
    }

    @Test
    fun `four minutes is not yet enough`() {
        assertFalse(CompletionRules.sessionsMeetMinimum(learning, listOf(session(minutes = 4))))
        assertEquals(1, CompletionRules.minutesRemaining(learning, 4))
    }

    @Test
    fun `short sessions on the same day add up`() {
        val sessions = listOf(session(id = "a", minutes = 3), session(id = "b", minutes = 2))
        assertTrue(CompletionRules.sessionsMeetMinimum(learning, sessions))
    }

    @Test
    fun `longer sessions are a bonus, not a requirement`() {
        assertTrue(CompletionRules.sessionsMeetMinimum(learning, listOf(session(minutes = 90))))
        assertEquals(0, CompletionRules.minutesRemaining(learning, 90))
    }

    @Test
    fun `one understood concept counts when the habit allows it`() {
        val conceptHabit = learning.copy(allowConceptCompletion = true)
        assertTrue(CompletionRules.sessionsMeetMinimum(conceptHabit, listOf(session(minutes = 2, concept = true))))
        assertFalse(CompletionRules.sessionsMeetMinimum(learning, listOf(session(minutes = 2, concept = true))))
    }

    @Test
    fun `action based habit is met by any logged session`() {
        assertTrue(CompletionRules.sessionsMeetMinimum(habit(type = CompletionType.ACTION), listOf(session(minutes = 0))))
        assertFalse(CompletionRules.sessionsMeetMinimum(habit(type = CompletionType.ACTION), emptyList()))
    }

    @Test
    fun `sync adds a completion only when none exists - duplicates are prevented`() {
        assertEquals(SyncAction.ADD, CompletionRules.syncAction(true, null))
        val manual = Completion("h1", d("2026-10-18"), CompletionSource.MANUAL)
        val fromSession = manual.copy(source = CompletionSource.SESSION)
        assertEquals(SyncAction.NONE, CompletionRules.syncAction(true, manual))
        assertEquals(SyncAction.NONE, CompletionRules.syncAction(true, fromSession))
    }

    @Test
    fun `deleting the qualifying session removes only a session-derived completion`() {
        val manual = Completion("h1", d("2026-10-18"), CompletionSource.MANUAL)
        assertEquals(SyncAction.REMOVE, CompletionRules.syncAction(false, manual.copy(source = CompletionSource.SESSION)))
        assertEquals(SyncAction.NONE, CompletionRules.syncAction(false, manual))
        assertEquals(SyncAction.NONE, CompletionRules.syncAction(false, null))
    }

    @Test
    fun `late evening in Warsaw stays on the local day although UTC is earlier`() {
        val warsaw = ZoneId.of("Europe/Warsaw")
        // 23:30 local on Oct 18 (CEST, UTC+2) = 21:30 UTC.
        assertEquals(d("2026-10-18"), DateMath.localDate(Instant.parse("2026-10-18T21:30:00Z"), warsaw))
        // 00:30 local on Oct 19 = 22:30 UTC on Oct 18: must be the 19th, not the 18th.
        assertEquals(d("2026-10-19"), DateMath.localDate(Instant.parse("2026-10-18T22:30:00Z"), warsaw))
    }

    @Test
    fun `same instant is a different local day in another time zone`() {
        val instant = Instant.parse("2026-10-18T22:30:00Z")
        assertEquals(d("2026-10-19"), DateMath.today(Clock.fixed(instant, ZoneId.of("Europe/Warsaw"))))
        assertEquals(d("2026-10-18"), DateMath.today(Clock.fixed(instant, ZoneId.of("America/New_York"))))
        assertEquals(d("2026-10-19"), DateMath.today(Clock.fixed(instant, ZoneId.of("Asia/Tokyo"))))
    }

    @Test
    fun `date picker millis convert without shifting the day`() {
        val date = d("2026-03-29") // DST starts in Europe that night
        assertEquals(date, DateMath.fromPickerMillis(DateMath.toPickerMillis(date)))
        assertEquals(d("2028-02-29"), DateMath.fromPickerMillis(DateMath.toPickerMillis(d("2028-02-29"))))
    }

    @Test
    fun `week starts on monday`() {
        assertEquals(d("2026-10-12"), DateMath.weekStart(d("2026-10-18")))
        assertEquals(d("2026-10-12"), DateMath.weekStart(d("2026-10-12")))
        assertEquals(d("2025-12-29"), DateMath.weekStart(d("2026-01-01")))
    }
}
