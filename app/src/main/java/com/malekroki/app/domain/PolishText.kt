package com.malekroki.app.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Polish wording helpers. Kept pure so they can be unit-tested. */
object PolishText {

    val LOCALE: Locale = Locale.forLanguageTag("pl-PL")

    private val longDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", LOCALE)
    private val longDateWithYear = DateTimeFormatter.ofPattern("d MMMM yyyy", LOCALE)
    private val shortDate = DateTimeFormatter.ofPattern("d MMM", LOCALE)
    private val monthYear = DateTimeFormatter.ofPattern("LLLL yyyy", LOCALE)

    /** Picks the right Polish plural form: 1 dzień, 2 dni, 5 dni; 1 minuta, 2 minuty, 5 minut. */
    fun plural(n: Int, one: String, few: String, many: String): String {
        val abs = kotlin.math.abs(n)
        return when {
            abs == 1 -> one
            abs % 10 in 2..4 && abs % 100 !in 12..14 -> few
            else -> many
        }
    }

    fun days(n: Int) = "$n ${plural(n, "dzień", "dni", "dni")}"
    fun minutes(n: Int) = "$n ${plural(n, "minuta", "minuty", "minut")}"
    fun sessions(n: Int) = "$n ${plural(n, "sesja", "sesje", "sesji")}"
    fun habits(n: Int) = "$n ${plural(n, "nawyk", "nawyki", "nawyków")}"
    fun times(n: Int) = "$n ${plural(n, "raz", "razy", "razy")}"

    /** 45 → "45 min", 80 → "1 h 20 min", 120 → "2 h". */
    fun duration(totalMinutes: Int): String {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h == 0 -> "$m min"
            m == 0 -> "$h h"
            else -> "$h h $m min"
        }
    }

    fun capitalize(text: String) = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(LOCALE) else it.toString() }

    fun longDate(date: LocalDate) = capitalize(longDate.format(date))
    fun longDateWithYear(date: LocalDate): String = longDateWithYear.format(date)
    fun shortDate(date: LocalDate): String = shortDate.format(date).trimEnd('.')
    fun monthYear(date: LocalDate) = capitalize(monthYear.format(date))

    fun dateRange(start: LocalDate, end: LocalDate): String = when {
        start == end -> longDateWithYear(start)
        start.year == end.year -> "${shortDate(start)} – ${longDateWithYear(end)}"
        else -> "${longDateWithYear(start)} – ${longDateWithYear(end)}"
    }

    fun greeting(time: LocalTime): String = when (time.hour) {
        in 5..11 -> "Dzień dobry"
        in 12..17 -> "Miłego popołudnia"
        else -> "Dobry wieczór"
    }

    private val dailyLines = listOf(
        "Pięć minut też się liczy.",
        "Małe kroki też prowadzą do celu.",
        "Liczy się regularność, nie intensywność.",
        "Każdy powrót się liczy.",
        "Odpoczynek też jest częścią procesu.",
        "Ciekawość to dobry powód, żeby zacząć.",
        "Nowy dzień, nowa szansa.",
    )

    /** One calm line per day, stable for the whole day. */
    fun lineOfTheDay(date: LocalDate): String =
        dailyLines[Math.floorMod(date.toEpochDay(), dailyLines.size.toLong()).toInt()]

    val celebrations = listOf(
        "Zrobione. Małe kroki też prowadzą do celu.",
        "Jest! Dobrze, że była na to chwila.",
        "Zaliczone. Regularność robi swoje.",
        "Gotowe. Reszta dnia jest Twoja.",
    )

    fun celebration(seed: Int): String = celebrations[Math.floorMod(seed, celebrations.size)]

    const val FRESH_START = "Nowy dzień, nowa szansa."
    const val COMEBACK = "Każdy powrót się liczy."
    const val LEAVE_SAFE = "Twoja seria jest bezpieczna podczas nieobecności."
    const val LEAVE_REST = "Odpoczynek też jest częścią procesu."
    const val LEAVE_RETURN = "Wracaj do swoich nawyków we własnym tempie."
}
