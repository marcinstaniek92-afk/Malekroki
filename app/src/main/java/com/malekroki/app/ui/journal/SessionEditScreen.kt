@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.malekroki.app.ui.journal

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.malekroki.app.domain.CompletionType
import com.malekroki.app.domain.PolishText
import com.malekroki.app.ui.common.BackButton
import com.malekroki.app.ui.common.ConfirmDialog
import com.malekroki.app.ui.common.DatePickerButton
import com.malekroki.app.ui.common.appViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionEditScreen(onDone: () -> Unit) {
    val vm = appViewModel { c, handle -> SessionEditViewModel(c, handle) }
    val form by vm.form.collectAsStateWithLifecycle()
    val timer by vm.timer.collectAsStateWithLifecycle()
    val habits by vm.habits.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val isNew = vm.sessionId == null
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var triedToSave by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(vm) { vm.eventFlow.collect { onDone() } }

    val selectedHabit = habits.firstOrNull { it.id == form.habitId }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Sesja nauki" else "Wpis w dzienniku") },
                navigationIcon = { BackButton(onDone) },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Usuń wpis") }
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
            Text("Czego dotyczyła nauka?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                habits.forEach { h ->
                    FilterChip(
                        selected = form.habitId == h.id,
                        onClick = { vm.update { it.copy(habitId = if (it.habitId == h.id) null else h.id) } },
                        label = { Text("${h.emoji} ${h.name}") },
                    )
                }
                FilterChip(
                    selected = form.habitId == null,
                    onClick = { vm.update { it.copy(habitId = null) } },
                    label = { Text("Inny temat") },
                )
            }
            if (form.habitId == null) {
                OutlinedTextField(
                    value = form.topic,
                    onValueChange = { v -> vm.update { it.copy(topic = v) } },
                    label = { Text("Temat") },
                    singleLine = true,
                    isError = triedToSave && form.topicError != null,
                    supportingText = { Text(form.topicError?.takeIf { triedToSave } ?: "Wpis bez nawyku trafi tylko do dziennika.") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            DatePickerButton(
                label = "Dzień",
                date = form.date,
                onDateChange = { d -> vm.update { it.copy(date = d) } },
                maxDate = vm.today,
                showDialog = showDate,
                onShowDialogChange = { showDate = it },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Ile czasu?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 15, 30, 60).forEach { m ->
                    FilterChip(
                        selected = form.minutesText == m.toString(),
                        onClick = { vm.update { it.copy(minutesText = m.toString()) } },
                        label = { Text(if (m == 60) "1 h" else "$m min") },
                    )
                }
            }
            OutlinedTextField(
                value = form.minutesText,
                onValueChange = { v -> vm.update { it.copy(minutesText = v.filter(Char::isDigit).take(4)) } },
                label = { Text("Minuty") },
                singleLine = true,
                isError = form.minutesError != null,
                supportingText = { form.minutesError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            TimerCard(
                running = timer.running,
                elapsedMs = { timer.elapsedMs(SystemClock.elapsedRealtime()) },
                hasTime = timer.accumulatedMs > 0 || timer.running,
                onToggle = vm::toggleTimer,
                onUse = vm::useTimer,
                onReset = vm::resetTimer,
            )

            if (selectedHabit != null) {
                if (selectedHabit.completionType == CompletionType.MINUTES && selectedHabit.allowConceptCompletion) {
                    CheckRow("Jedno nowe pojęcie stało się jasne", form.conceptUnderstood) { v -> vm.update { it.copy(conceptUnderstood = v) } }
                }
                val meets = vm.meetsMinimumAlone(selectedHabit, form)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (meets) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        if (meets) "To spełnia Twoje minimum — „${selectedHabit.name}” będzie zaliczone za ten dzień."
                        else "Minimum to ${PolishText.minutes(selectedHabit.minimumMinutes)}. Krótkie sesje z jednego dnia sumują się. Każdy wpis i tak trafia do dziennika.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            OutlinedTextField(
                value = form.learned,
                onValueChange = { v -> vm.update { it.copy(learned = v) } },
                label = { Text("Co nowego wiem?") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.questions,
                onValueChange = { v -> vm.update { it.copy(questions = v) } },
                label = { Text("Pytania do powrotu (opcjonalnie)") },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.notes,
                onValueChange = { v -> vm.update { it.copy(notes = v) } },
                label = { Text("Notatki (opcjonalnie)") },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            CheckRow("Było ćwiczenie praktyczne", form.practicalExercise) { v -> vm.update { it.copy(practicalExercise = v) } }

            Button(
                onClick = {
                    triedToSave = true
                    vm.save()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text("Zapisz") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Usunąć wpis?",
            message = "Wpis zniknie z dziennika. Jeśli to on zaliczał nawyk w tym dniu, seria zostanie przeliczona.",
            confirmLabel = "Usuń",
            destructive = true,
            onConfirm = vm::delete,
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun TimerCard(
    running: Boolean,
    elapsedMs: () -> Long,
    hasTime: Boolean,
    onToggle: () -> Unit,
    onUse: () -> Unit,
    onReset: () -> Unit,
) {
    var display by remember { mutableLongStateOf(elapsedMs()) }
    LaunchedEffect(running) {
        display = elapsedMs()
        while (running) {
            delay(500)
            display = elapsedMs()
        }
    }
    val totalSeconds = display / 1000
    val text = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Timer (opcjonalnie)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text, style = MaterialTheme.typography.headlineSmall)
            }
            if (hasTime && !running) {
                TextButton(onClick = onReset) { Text("Wyzeruj") }
                TextButton(onClick = onUse) { Text("Użyj") }
            }
            FilledTonalButton(onClick = onToggle) {
                Icon(if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (running) "Pauza" else if (hasTime) "Wznów" else "Start")
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
