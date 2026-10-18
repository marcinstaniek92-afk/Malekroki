@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.malekroki.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.malekroki.app.domain.DayStatus
import com.malekroki.app.domain.PolishText
import com.malekroki.app.ui.common.EmojiBadge
import com.malekroki.app.ui.common.Pill
import com.malekroki.app.ui.common.SectionHeader
import com.malekroki.app.ui.common.appViewModel
import com.malekroki.app.ui.common.emoji
import com.malekroki.app.ui.common.shortLabel
import com.malekroki.app.ui.common.title
import com.malekroki.app.ui.theme.LocalStatusColors
import java.time.DayOfWeek
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onOpenLeaves: () -> Unit) {
    val vm = appViewModel { c, _ -> CalendarViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Kalendarz") },
                actions = {
                    if (state.month != YearMonth.from(state.today)) {
                        TextButton(onClick = vm::goToToday) { Text("Dziś") }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            HabitFilter(state, onSelect = vm::selectHabit)
            Column(Modifier.padding(horizontal = 16.dp)) {
                MonthHeader(state.month, onPrev = { vm.changeMonth(-1) }, onNext = { vm.changeMonth(1) })
                MonthGrid(state, onDayClick = vm::openDay)
                Legend(singleHabit = state.selectedHabit != null)
                StatsSection(state)
                TextButton(onClick = onOpenLeaves, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Zarządzaj nieobecnościami")
                }
            }
        }
    }

    state.detail?.let { detail ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = vm::closeDay, sheetState = sheetState) {
            DaySheet(detail, onToggle = { habitId, done -> vm.setCompleted(habitId, detail.date, done) })
        }
    }
}

@Composable
private fun HabitFilter(state: CalendarUiState, onSelect: (String?) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = state.selectedHabit == null, onClick = { onSelect(null) }, label = { Text("Wszystkie") })
        state.habits.forEach { h ->
            FilterChip(
                selected = state.selectedHabit?.id == h.id,
                onClick = { onSelect(h.id) },
                label = { Text("${h.emoji} ${h.name}") },
            )
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Poprzedni miesiąc") }
        Text(
            PolishText.monthYear(month.atDay(1)),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Następny miesiąc") }
    }
}

@Composable
private fun MonthGrid(state: CalendarUiState, onDayClick: (java.time.LocalDate) -> Unit) {
    val status = LocalStatusColors.current
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        DayOfWeek.entries.forEach {
            Text(
                it.shortLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
    state.days.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { day ->
                val single = day.status
                val fraction = if (day.planned == 0) 0f else day.done.toFloat() / day.planned
                val (bg, fg) = when {
                    !day.inMonth -> Color.Transparent to scheme.outline
                    single == DayStatus.COMPLETED -> status.completed to status.onCompleted
                    single == DayStatus.PROTECTED -> status.protectedDay to status.onProtectedDay
                    single == DayStatus.FUTURE_PROTECTED -> Color.Transparent to status.onProtectedDay
                    single == DayStatus.MISSED -> status.missed to scheme.onSurfaceVariant
                    single != null -> Color.Transparent to scheme.onSurface
                    day.done > 0 -> status.completed.copy(alpha = 0.25f + 0.75f * fraction) to
                        (if (fraction >= 0.5f) status.onCompleted else scheme.onSurface)
                    day.protectedDay -> status.protectedDay to status.onProtectedDay
                    day.planned > 0 && !day.date.isAfter(state.today) && !day.isToday -> status.missed to scheme.onSurfaceVariant
                    else -> Color.Transparent to scheme.onSurface
                }
                val description = buildString {
                    append(PolishText.longDateWithYear(day.date))
                    append(": ")
                    append(
                        when {
                            single == DayStatus.COMPLETED -> "zrobione"
                            single == DayStatus.PROTECTED -> "chronione nieobecnością"
                            single == DayStatus.FUTURE_PROTECTED -> "zaplanowana nieobecność"
                            single == DayStatus.MISSED -> "bez wpisu"
                            single == DayStatus.REST -> "poza planem"
                            single != null -> "brak danych"
                            else -> "zrobione ${day.done} z ${day.planned}" + if (day.protectedDay) ", nieobecność" else ""
                        },
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .then(if (day.isToday) Modifier.border(2.dp, scheme.primary, CircleShape) else Modifier)
                        .then(
                            if (day.futureProtected || single == DayStatus.FUTURE_PROTECTED) {
                                Modifier.border(1.5.dp, status.onProtectedDay.copy(alpha = 0.6f), CircleShape)
                            } else Modifier,
                        )
                        .then(if (day.inMonth) Modifier.clickable(onClickLabel = "Pokaż dzień") { onDayClick(day.date) } else Modifier)
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        day.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (day.inMonth) fg else fg.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(singleHabit: Boolean) {
    val status = LocalStatusColors.current
    val scheme = MaterialTheme.colorScheme
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LegendItem(if (singleHabit) "Zrobione" else "Zrobione (im ciemniej, tym więcej)", fill = status.completed)
        LegendItem("Bez wpisu", fill = status.missed)
        LegendItem("Nieobecność", fill = status.protectedDay)
        LegendItem("Zaplanowana nieobecność", fill = Color.Transparent, border = status.onProtectedDay)
        LegendItem("Dziś", fill = Color.Transparent, border = scheme.primary)
    }
}

@Composable
private fun LegendItem(label: String, fill: Color, border: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(fill)
                .then(if (border != null) Modifier.border(1.5.dp, border, CircleShape) else Modifier),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatsSection(state: CalendarUiState) {
    SectionHeader(state.selectedHabit?.let { "${it.emoji} ${it.name}" } ?: "Wszystkie nawyki")
    val streak = state.streak
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (streak != null && state.selectedHabit?.countsTowardStreak == true) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Obecna seria", PolishText.days(streak.current), Modifier.weight(1f))
                StatTile("Najdłuższa seria", PolishText.days(streak.longest), Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Ostatnie 7 dni", state.rate7.percent?.let { "$it%" } ?: "—", Modifier.weight(1f), "${state.rate7.completedDays} z ${state.rate7.plannedDays} dni w planie")
            StatTile("Ostatnie 30 dni", state.rate30.percent?.let { "$it%" } ?: "—", Modifier.weight(1f), "${state.rate30.completedDays} z ${state.rate30.plannedDays} dni w planie")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Czas nauki", PolishText.duration(state.learningMinutes), Modifier.weight(1f))
            StatTile("Sesje nauki", state.learningSessions.toString(), Modifier.weight(1f))
        }
        Text(
            "Dni nieobecności i dni poza planem nie są wliczane. To opis aktywności, nie ocena.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier, caption: String? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DaySheet(detail: DayDetail, onToggle: (String, Boolean) -> Unit) {
    val status = LocalStatusColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {
        Text(PolishText.longDate(detail.date), style = MaterialTheme.typography.titleLarge)
        Text(detail.date.year.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        detail.leaves.forEach { leave ->
            Spacer(Modifier.height(10.dp))
            Pill(
                "${leave.type.emoji()} ${leave.title()} — ${PolishText.LEAVE_SAFE}",
                container = status.protectedDay,
                content = status.onProtectedDay,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (!detail.editable) {
            Text(
                "To dzień w przyszłości. Wykonania można zaznaczać dziś lub wstecz.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (detail.habits.isEmpty()) {
            Text("Brak nawyków.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                "Coś umknęło? Możesz to poprawić, a seria przeliczy się sama.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        detail.habits.forEach { item ->
            val done = item.status == DayStatus.COMPLETED
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .then(if (detail.editable) Modifier.clickable { onToggle(item.habit.id, !done) } else Modifier)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmojiBadge(item.habit.emoji, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.habit.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        when (item.status) {
                            DayStatus.COMPLETED -> "Zrobione"
                            DayStatus.PROTECTED -> "Chronione nieobecnością"
                            DayStatus.FUTURE_PROTECTED -> "Zaplanowana nieobecność"
                            DayStatus.REST -> "Poza planem"
                            DayStatus.PENDING -> "Jeszcze przed Tobą"
                            DayStatus.BEFORE_START -> "Przed utworzeniem nawyku"
                            DayStatus.FUTURE -> "W przyszłości"
                            DayStatus.MISSED -> "Bez wpisu"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (detail.editable) {
                    Checkbox(checked = done, onCheckedChange = { onToggle(item.habit.id, it) })
                }
            }
        }
    }
}
