package com.malekroki.app.domain

import java.time.LocalDate

/** Editable examples created on first launch. Starters, not obligations. */
object StarterHabits {

    fun create(today: LocalDate, newId: () -> String): List<Habit> = listOf(
        Habit(
            id = newId(),
            name = "ServiceNow",
            description = "Nauka platformy ServiceNow.",
            category = "Nauka",
            emoji = "🧭",
            completionType = CompletionType.MINUTES,
            minimumMinutes = 5,
            allowConceptCompletion = true,
            minimumDescription = "5 minut nauki lub jedno naprawdę zrozumiane pojęcie",
            isLearning = true,
            createdDate = today,
            sortOrder = 0,
        ),
        Habit(
            id = newId(),
            name = "HTML i CSS",
            description = "Nauka i praktyka HTML oraz CSS.",
            category = "Nauka",
            emoji = "🎨",
            completionType = CompletionType.MINUTES,
            minimumMinutes = 5,
            minimumDescription = "5 minut nauki lub pracy praktycznej",
            isLearning = true,
            createdDate = today,
            sortOrder = 1,
        ),
        Habit(
            id = newId(),
            name = "JavaScript",
            description = "Nauka i praktyka JavaScriptu.",
            category = "Nauka",
            emoji = "⚡",
            completionType = CompletionType.MINUTES,
            minimumMinutes = 5,
            minimumDescription = "5 minut nauki lub pracy praktycznej",
            isLearning = true,
            createdDate = today,
            sortOrder = 2,
        ),
        Habit(
            id = newId(),
            name = "Życie osobiste",
            description = "Coś małego i ważnego dla siebie, domu albo bliskich.",
            category = "Życie",
            emoji = "🌿",
            completionType = CompletionType.ACTION,
            minimumDescription = "Jedno małe, ważne działanie dla siebie, domu lub relacji",
            createdDate = today,
            sortOrder = 3,
        ),
    )
}
