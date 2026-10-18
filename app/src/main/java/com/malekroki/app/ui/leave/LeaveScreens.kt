@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.malekroki.app.ui.leave

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.malekroki.app.domain.DateMath
import com.malekroki.app.domain.LeaveStatus
import com.malekroki.app.domain.LeaveType
import com.malekroki.app.domain.PolishText
import com.malekroki.app.ui.common.BackButton
import com.malekroki.app.ui.common.ConfirmDialog
import com.malekroki.app.ui.common.EmptyState
import com.malekroki.app.ui.common.Pill
import com.malekroki.app.ui.common.SectionHeader
import com.malekroki.app.ui.common.appViewModel
import com.malekroki.app.ui.common.emoji
import com.malekroki.app.ui.common.label
import com.malekroki.app.ui.common.title
import com.malekroki.app.ui.theme.LocalStatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveListScreen(onBack: () -> Unit, onEdit: (String?) -> Unit) {
    val vm = appViewModel { c, _ -> LeaveListViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Nieobecności") }, navigationIcon = { BackButton(onBack) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEdit(null) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Dodaj") },
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
            item {
                Text(
                    "${PolishText.LEAVE_REST} Dni nieobecności nie przerywają serii i nie są liczone jako wykonane.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!state.loading && state.active.isEmpty() && state.upcoming.isEmpty() && state.past.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🏖️",
                        title = "Brak nieobecności",
                        message = "Urlop, choroba, święta albo po prostu przerwa — dodaj je tutaj, a Twoje serie poczekają.",
                        actionLabel = "Dodaj nieobecność",
                        onAction = { onEdit(null) },
                    )
                }
            }
            section("Trwa teraz", state.active, onEdit)
            section("Zaplanowane", state.upcoming, onEdit)
            section("Minione", state.past, onEdit)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    rows: List<LeaveRowUi>,
    onEdit: (String?) -> Unit,
) {
    if (rows.isEmpty()) return
    item(key = "h-$title") { SectionHeader(title) }
    items(rows, key = { it.leave.id }) { row -> LeaveRow(row, onClick = { onEdit(row.leave.id) }) }
}

@Composable
private fun LeaveRow(row: LeaveRowUi, onClick: () -> Unit) {
    val status = LocalStatusColors.current
    val l = row.leave
    val active = row.status == LeaveStatus.ACTIVE
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (active) status.protectedDay else MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = if (active) status.onProtectedDay else MaterialTheme.colorScheme.onSurface,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = "Edytuj nieobecność", onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(l.type.emoji(), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(l.title(), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${PolishText.dateRange(l.startDate, l.endDate)} · ${PolishText.days(l.lengthInDays.toInt())}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    if (l.appliesToAllHabits) "Wszystkie nawyki" else row.habitNames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (l.note.isNotBlank()) {
                    Text(l.note, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LeaveEditScreen(onDone: () -> Unit) {
    val vm = appViewModel { c, handle -> LeaveEditViewModel(c, handle) }
    val form by vm.form.collectAsStateWithLifecycle()
    val habits by vm.habits.collectAsStateWithLifecycle()
    val errors by vm.errors.collectAsStateWithLifecycle()
    val overlaps by vm.overlaps.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val isNew = vm.leaveId == null
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    val status = LocalStatusColors.current

    LaunchedEffect(vm) { vm.eventFlow.collect { onDone() } }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Nowa nieobecność" else "Edytuj nieobecność") },
                navigationIcon = { BackButton(onDone) },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Anuluj nieobecność")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!loaded) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Rodzaj", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LeaveType.entries.forEach { type ->
                    FilterChip(
                        selected = form.type == type,
                        onClick = { vm.update { it.copy(type = type) } },
                        label = { Text("${type.emoji()} ${type.label()}") },
                    )
                }
            }
            if (form.type == LeaveType.CUSTOM) {
                OutlinedTextField(
                    value = form.customName,
                    onValueChange = { v -> vm.update { it.copy(customName = v) } },
                    label = { Text("Nazwa typu") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text("Kiedy", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Outlined.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("${PolishText.dateRange(form.startDate, form.endDate)} · ${PolishText.days(form.days.toInt().coerceAtLeast(0))}")
            }

            Text("Które nawyki chronić?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = form.appliesToAllHabits,
                    onClick = { vm.update { it.copy(appliesToAllHabits = true) } },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Wszystkie") }
                SegmentedButton(
                    selected = !form.appliesToAllHabits,
                    onClick = { vm.update { it.copy(appliesToAllHabits = false) } },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Wybrane") }
            }
            if (form.appliesToAllHabits) {
                Text(
                    "Chroni wszystkie nawyki — także te, które dodasz później.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column {
                    habits.forEach { h ->
                        val checked = h.id in form.habitIds
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    vm.update { f -> f.copy(habitIds = if (checked) f.habitIds - h.id else f.habitIds + h.id) }
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                            Text("${h.emoji} ${h.name}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Text(
                        "Pozostałe nawyki możesz robić normalnie, jeśli masz ochotę.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = form.note,
                onValueChange = { v -> vm.update { it.copy(note = v) } },
                label = { Text("Notatka (opcjonalnie)") },
                supportingText = { Text("Nie musisz niczego wyjaśniać.") },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = status.protectedDay, contentColor = status.onProtectedDay),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(PolishText.LEAVE_SAFE, style = MaterialTheme.typography.bodyMedium)
                    Text(PolishText.LEAVE_RETURN, style = MaterialTheme.typography.bodySmall)
                    if (overlaps > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Ten okres nakłada się na inne nieobecności ($overlaps). Wspólne dni liczą się raz.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            errors.forEach { e ->
                Pill(e, container = MaterialTheme.colorScheme.errorContainer, content = MaterialTheme.colorScheme.onErrorContainer)
            }

            Button(onClick = vm::save, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Zapisz") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = DateMath.toPickerMillis(form.startDate),
            initialSelectedEndDateMillis = DateMath.toPickerMillis(form.endDate),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = pickerState.selectedStartDateMillis?.let(DateMath::fromPickerMillis)
                        val end = pickerState.selectedEndDateMillis?.let(DateMath::fromPickerMillis) ?: start
                        if (start != null && end != null) vm.update { it.copy(startDate = start, endDate = end) }
                        showPicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Anuluj") } },
        ) {
            DateRangePicker(
                state = pickerState,
                title = { Text("Wybierz dni", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Anulować nieobecność?",
            message = "Serie zostaną przeliczone według faktycznej historii, bez ochrony tych dni.",
            confirmLabel = "Anuluj nieobecność",
            destructive = true,
            onConfirm = vm::delete,
            onDismiss = { showDelete = false },
        )
    }
}
