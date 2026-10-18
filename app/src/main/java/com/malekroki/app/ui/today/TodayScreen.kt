@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.malekroki.app.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.malekroki.app.domain.CompletionRules
import com.malekroki.app.domain.CompletionType
import com.malekroki.app.domain.PolishText
import com.malekroki.app.ui.common.ConfirmDialog
import com.malekroki.app.ui.common.EmojiBadge
import com.malekroki.app.ui.common.EmptyState
import com.malekroki.app.ui.common.Pill
import com.malekroki.app.ui.common.SectionHeader
import com.malekroki.app.ui.common.appViewModel
import com.malekroki.app.ui.common.emoji
import com.malekroki.app.ui.common.minimumLabel
import com.malekroki.app.ui.common.shortLabel
import com.malekroki.app.ui.common.title
import com.malekroki.app.ui.theme.LocalStatusColors
import kotlinx.coroutines.launch

@Composable
fun TodayScreen(
    onStartSession: (habitId: String?) -> Unit,
    onOpenSession: (sessionId: String) -> Unit,
    onOpenJournal: () -> Unit,
    onOpenLeaves: () -> Unit,
    onAddHabit: () -> Unit,
) {
    val vm = appViewModel { c, _ -> TodayViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val reflection by vm.reflection.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(vm) {
        vm.eventFlow.collect { event ->
            when (event) {
                is TodayEvent.Completed -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        val result = snackbar.showSnackbar(event.message, actionLabel = "Cofnij", duration = SnackbarDuration.Short)
                        if (result == SnackbarResult.ActionPerformed) vm.undo(event.habitId, event.date)
                    }
                }
            }
        }
    }

    var undoTarget by remember { mutableStateOf<HabitCardUi?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (state.hasLearningHabits) {
                ExtendedFloatingActionButton(
                    onClick = { onStartSession(null) },
                    icon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null) },
                    text = { Text("Sesja nauki") },
                )
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        ) {
            item(key = "header") { Header(state) }

            if (state.activeLeaves.isNotEmpty()) {
                item(key = "leave") { ActiveLeaveCard(state, onOpenLeaves) }
            }

            item(key = "habits-header") {
                if (state.habits.isNotEmpty()) {
                    SectionHeader("Na dziś", action = { Pill("${state.doneToday} z ${state.habits.size}") })
                } else {
                    SectionHeader("Na dziś")
                }
            }
            if (state.habits.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        emoji = "🌱",
                        title = "Nic zaplanowanego na dziś",
                        message = "To też jest w porządku. Możesz dodać nawyk, kiedy przyjdzie ochota.",
                        actionLabel = "Dodaj nawyk",
                        onAction = onAddHabit,
                    )
                }
            }
            items(state.habits, key = { it.habit.id }) { card ->
                HabitCard(
                    card = card,
                    onDone = { vm.markDone(card.habit) },
                    onUndoRequest = { undoTarget = card },
                    onOpenSession = if (card.habit.isLearning) ({ onStartSession(card.habit.id) }) else null,
                )
                Spacer(Modifier.height(10.dp))
            }

            item(key = "week") { WeekCard(state.week) }

            if (state.recentSessions.isNotEmpty()) {
                item(key = "recent-header") {
                    SectionHeader("Ostatnia nauka", action = { TextButton(onClick = onOpenJournal) { Text("Dziennik") } })
                }
                items(state.recentSessions, key = { "s-" + it.session.id }) { row ->
                    RecentSessionRow(row, onClick = { onOpenSession(row.session.id) })
                }
            }

            item(key = "reflection") {
                ReflectionCard(saved = reflection, today = state.today.toString(), onSave = vm::saveReflection)
            }

            if (state.activeLeaves.isEmpty()) {
                item(key = "leave-entry") { LeaveEntryCard(state, onOpenLeaves) }
            }
        }
    }

    undoTarget?.let { card ->
        ConfirmDialog(
            title = "Cofnąć dzisiejsze wykonanie?",
            message = "„${card.habit.name}” wróci do listy na dziś, a seria zostanie przeliczona. Wpisy w dzienniku zostaną.",
            confirmLabel = "Cofnij",
            onConfirm = { vm.undo(card.habit.id, state.today) },
            onDismiss = { undoTarget = null },
        )
    }
}

@Composable
private fun Header(state: TodayUiState) {
    Column(Modifier.padding(top = 24.dp, bottom = 4.dp)) {
        Text(
            PolishText.longDate(state.today),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(state.greeting, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(state.line, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HabitCard(
    card: HabitCardUi,
    onDone: () -> Unit,
    onUndoRequest: () -> Unit,
    onOpenSession: (() -> Unit)?,
) {
    val habit = card.habit
    val stats = card.stats
    val done = stats.completedToday
    val status = LocalStatusColors.current
    val container by animateColorAsState(
        if (done) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "card",
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .then(if (onOpenSession != null) Modifier.clickable(onClickLabel = "Zapisz sesję nauki", onClick = onOpenSession) else Modifier),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (habit.countsTowardStreak) {
                        StreakPill(stats.current)
                        if (stats.longest > 0) Pill("Rekord ${stats.longest}")
                    } else {
                        Pill("Bez serii")
                    }
                }
                val hint = when {
                    !done && card.leaveTitle != null -> "Chronione: ${card.leaveTitle}. ${PolishText.LEAVE_SAFE}"
                    !done && habit.completionType == CompletionType.MINUTES && card.minutesToday > 0 ->
                        "Dziś ${card.minutesToday} min — brakuje ${CompletionRules.minutesRemaining(habit, card.minutesToday)} min"
                    !done && stats.isFreshStart -> PolishText.FRESH_START
                    else -> null
                }
                if (hint != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (card.leaveTitle != null && !done) status.onProtectedDay else MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            DoneButton(done = done, habitName = habit.name, onClick = { if (done) onUndoRequest() else onDone() })
        }
    }
}

@Composable
private fun StreakPill(current: Int) {
    val status = LocalStatusColors.current
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(start = 6.dp, end = 10.dp, top = 3.dp, bottom = 3.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Seria: ${PolishText.days(current)}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = status.streak, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(2.dp))
        AnimatedContent(
            targetState = current,
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.7f)) togetherWith fadeOut() },
            label = "streak",
        ) { value ->
            Text(PolishText.days(value), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun DoneButton(done: Boolean, habitName: String, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(done) {
        if (done) {
            scale.snapTo(0.8f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    val background by animateColorAsState(
        if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "done-bg",
    )
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        modifier = Modifier
            .size(56.dp)
            .scale(scale.value)
            .then(
                if (!done) Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) else Modifier,
            )
            .semantics {
                role = Role.Checkbox
                contentDescription = "Oznacz jako zrobione: $habitName"
                stateDescription = if (done) "Zrobione dziś" else "Jeszcze nie"
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun WeekCard(week: List<WeekDayUi>) {
    val status = LocalStatusColors.current
    SectionHeader("Ten tydzień")
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            week.forEach { day ->
                val fraction = if (day.planned == 0) 0f else day.done.toFloat() / day.planned
                val description = buildString {
                    append(PolishText.longDate(day.date))
                    append(": ")
                    when {
                        day.isFuture -> append("jeszcze przed Tobą")
                        day.planned == 0 && day.protectedDay -> append("nieobecność")
                        else -> append("zrobione ${day.done} z ${day.planned}")
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
                ) {
                    Text(
                        day.date.dayOfWeek.shortLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    day.done > 0 && fraction >= 1f -> status.completed
                                    day.done > 0 -> status.completed.copy(alpha = 0.25f + 0.5f * fraction)
                                    day.protectedDay && day.planned == 0 -> status.protectedDay
                                    day.isFuture -> MaterialTheme.colorScheme.surfaceContainerLow
                                    else -> status.missed
                                },
                            )
                            .then(if (day.isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            when {
                                day.protectedDay && day.done == 0 && day.planned == 0 -> "🛡"
                                else -> day.date.dayOfMonth.toString()
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (day.done > 0 && fraction >= 0.5f) status.onCompleted else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSessionRow(row: SessionRowUi, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiBadge(row.emoji, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.topic, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val learned = row.session.learned.ifBlank { "Bez notatki" }
            Text(
                learned,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(PolishText.duration(row.session.minutes), style = MaterialTheme.typography.labelLarge)
            Text(PolishText.shortDate(row.session.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReflectionCard(saved: String?, today: String, onSave: (String) -> Unit) {
    SectionHeader("Jak minął dzień?")
    if (saved == null) return
    var text by rememberSaveable(today, saved) { mutableStateOf(saved) }
    val changed = text.trim() != saved.trim()
    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Kilka słów tylko dla Ciebie (opcjonalnie)") },
            minLines = 2,
            maxLines = 6,
            shape = MaterialTheme.shapes.medium,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (changed) {
                TextButton(onClick = { onSave(text) }) { Text("Zapisz") }
            } else if (saved.isNotBlank()) {
                Text(
                    "Zapisane",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun ActiveLeaveCard(state: TodayUiState, onOpenLeaves: () -> Unit) {
    val status = LocalStatusColors.current
    val leave = state.activeLeaves.first()
    Spacer(Modifier.height(12.dp))
    Card(
        onClick = onOpenLeaves,
        colors = CardDefaults.cardColors(containerColor = status.protectedDay, contentColor = status.onProtectedDay),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(leave.type.emoji(), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${leave.title()} do ${PolishText.shortDate(leave.endDate)}", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (state.activeLeaves.size > 1) "${PolishText.LEAVE_SAFE} (aktywne: ${state.activeLeaves.size})" else PolishText.LEAVE_SAFE,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun LeaveEntryCard(state: TodayUiState, onOpenLeaves: () -> Unit) {
    Spacer(Modifier.height(20.dp))
    OutlinedCard(
        onClick = onOpenLeaves,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.BeachAccess, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Nieobecności", style = MaterialTheme.typography.titleSmall)
                val upcoming = state.upcomingLeaves.firstOrNull()
                Text(
                    if (upcoming != null) "Zaplanowane: ${upcoming.title()} od ${PolishText.shortDate(upcoming.startDate)}"
                    else "Urlop, choroba albo przerwa? Twoja seria może poczekać.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
