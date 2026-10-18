package com.malekroki.app.data

import com.malekroki.app.domain.DateMath
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

/**
 * Single source of "today". The system default zone is read on every call,
 * so travelling or changing the time zone is picked up immediately.
 */
class DateProvider(private val clock: () -> Clock = { Clock.systemDefaultZone() }) {

    fun today(): LocalDate = DateMath.today(clock())

    fun timeNow(): LocalTime = LocalTime.now(clock())

    fun nowMillis(): Long = clock().millis()

    /**
     * Emits today's date and re-emits when the day changes. It only runs while the
     * UI collects it (flows are shared with WhileSubscribed), so it costs nothing in the background.
     */
    fun todayFlow(): Flow<LocalDate> = flow {
        while (true) {
            emit(today())
            delay(30_000)
        }
    }.distinctUntilChanged()
}
