@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.malekroki.app.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.malekroki.app.domain.LearningSummary
import com.malekroki.app.domain.PolishText
import com.malekroki.app.ui.common.EmojiBadge
import com.malekroki.app.ui.common.EmptyState
import com.malekroki.app.ui.common.Pill
import com.malekroki.app.ui.common.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(onNewSession: () -> Unit, onOpenSession: (String) -> Unit) {
    val vm = appViewModel { c, _ -> JournalViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Dziennik") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewSession,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nowa sesja") },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.totalEntries > 0) {
                item(key = "summary") { SummaryCard(state.summary) }
                item(key = "search") {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = vm::setQuery,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Szukaj w notatkach") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Filled.Close, contentDescription = "Wyczyść") }
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                }
                item(key = "filters") {
                    FilterChip(
                        selected = state.onlyQuestions,
                        onClick = vm::toggleQuestions,
                        label = { Text("Pytania do powrotu") },
                    )
                }
            }

            if (!state.loading && state.totalEntries == 0) {
                item {
                    EmptyState(
                        emoji = "📘",
                        title = "Dziennik czeka na pierwszy wpis",
                        message = "Po kilku minutach nauki zapisz jedno zdanie o tym, co nowego wiesz. To wystarczy.",
                        actionLabel = "Zapisz sesję",
                        onAction = onNewSession,
                    )
                }
            } else if (!state.loading && state.entries.isEmpty()) {
                item {
                    EmptyState(emoji = "🔎", title = "Nic nie znaleziono", message = "Spróbuj innego słowa albo wyłącz filtr.")
                }
            }

            items(state.entries, key = { it.session.id }) { entry ->
                EntryCard(entry, onClick = { onOpenSession(entry.session.id) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(summary: LearningSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Łączny czas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(PolishText.duration(summary.totalMinutes), style = MaterialTheme.typography.titleLarge)
                }
                Column(Modifier.weight(1f)) {
                    Text("Sesje", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(summary.sessionCount.toString(), style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Minuty w ostatnich 8 tygodniach", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            WeeklyBars(summary)
            if (summary.recentTopics.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Ostatnio odkrywane", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    summary.recentTopics.forEach { Pill(it) }
                }
            }
        }
    }
}

@Composable
private fun WeeklyBars(summary: LearningSummary) {
    val max = (summary.weekly.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
    val description = summary.weekly.joinToString("; ") { "tydzień od ${PolishText.shortDate(it.weekStart)}: ${it.minutes} min" }
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        summary.weekly.forEachIndexed { index, week ->
            val fraction = week.minutes.toFloat() / max
            val isCurrent = index == summary.weekly.lastIndex
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction.coerceAtLeast(0.04f))
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                        if (week.minutes == 0) MaterialTheme.colorScheme.surfaceContainerHighest
                        else if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    ),
            )
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            summary.weekly.firstOrNull()?.let { PolishText.shortDate(it.weekStart) } ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text("ten tydzień", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EntryCard(entry: JournalEntryUi, onClick: () -> Unit) {
    val s = entry.session
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = "Otwórz wpis", onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(entry.emoji, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.topic, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        PolishText.longDate(s.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Pill(PolishText.duration(s.minutes))
            }
            if (s.learned.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(s.learned, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            if (s.questions.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "❓ ${s.questions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (s.practicalExercise || s.conceptUnderstood) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (s.practicalExercise) Pill("Ćwiczenie praktyczne")
                    if (s.conceptUnderstood) Pill("Zrozumiane pojęcie")
                }
            }
        }
    }
}
