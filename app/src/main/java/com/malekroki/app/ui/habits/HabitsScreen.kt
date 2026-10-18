@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.malekroki.app.ui.habits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.malekroki.app.domain.PolishText
import com.malekroki.app.ui.common.EmojiBadge
import com.malekroki.app.ui.common.EmptyState
import com.malekroki.app.ui.common.Pill
import com.malekroki.app.ui.common.SectionHeader
import com.malekroki.app.ui.common.appViewModel
import com.malekroki.app.ui.common.minimumLabel
import com.malekroki.app.ui.common.scheduleLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(onEdit: (String?) -> Unit) {
    val vm = appViewModel { c, _ -> HabitsViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Nawyki") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEdit(null) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nowy nawyk") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!state.loading && state.active.isEmpty() && state.archived.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🌱",
                        title = "Jeszcze nie masz nawyków",
                        message = "Zacznij od czegoś bardzo małego. Pięć minut naprawdę wystarczy.",
                        actionLabel = "Przywróć przykładowe nawyki",
                        onAction = vm::restoreStarters,
                    )
                }
            }
            items(state.active, key = { it.habit.id }) { row ->
                HabitRow(row, onClick = { onEdit(row.habit.id) })
            }
            if (state.archived.isNotEmpty()) {
                item(key = "archived-header") {
                    SectionHeader("Archiwum")
                    Text(
                        "Zarchiwizowane nawyki nie pojawiają się na ekranie Dzisiaj. Ich historia zostaje.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.archived, key = { it.habit.id }) { row ->
                    HabitRow(row, onClick = { onEdit(row.habit.id) }, onRestore = { vm.setArchived(row.habit.id, false) })
                }
            }
        }
    }
}

@Composable
private fun HabitRow(row: HabitRowUi, onClick: () -> Unit, onRestore: (() -> Unit)? = null) {
    val habit = row.habit
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = "Edytuj nawyk", onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(habit.emoji)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    habit.minimumLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (habit.category.isNotBlank()) Pill(habit.category)
                    Pill(habit.scheduleLabel())
                    if (habit.countsTowardStreak && row.stats.longest > 0) Pill("Rekord ${row.stats.longest}")
                }
                if (row.stats.totalCompletions > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Zrobione łącznie: ${PolishText.times(row.stats.totalCompletions)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onRestore != null) {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Outlined.Unarchive, contentDescription = "Przywróć ${habit.name}")
                }
            }
        }
    }
}
