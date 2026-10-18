@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.malekroki.app.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.malekroki.app.domain.CompletionType
import com.malekroki.app.domain.PolishText
import com.malekroki.app.ui.common.BackButton
import com.malekroki.app.ui.common.ConfirmDialog
import com.malekroki.app.ui.common.appViewModel
import com.malekroki.app.ui.common.shortLabel
import java.time.DayOfWeek

private val EMOJIS = listOf(
    "✨", "📘", "🧭", "🎨", "⚡", "💻", "🧠", "🌿", "🏃", "🏋️", "🧘", "🚶",
    "🎸", "✍️", "🌍", "🗣️", "🍳", "🏠", "💧", "😴", "❤️", "🤝", "📷", "🎮",
)

private val CATEGORIES = listOf("Nauka", "Zdrowie", "Ruch", "Dom", "Relacje", "Twórczość", "Życie")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitEditScreen(onDone: () -> Unit) {
    val vm = appViewModel { c, handle -> HabitEditViewModel(c, handle) }
    val form by vm.form.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    val isNew = vm.habitId == null
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var triedToSave by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(vm) { vm.eventFlow.collect { onDone() } }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Nowy nawyk" else "Edytuj nawyk") },
                navigationIcon = { BackButton(onDone) },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { vm.setArchived(!form.archived) }) {
                            Icon(
                                if (form.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                                contentDescription = if (form.archived) "Przywróć z archiwum" else "Archiwizuj",
                            )
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Usuń nawyk")
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
            OutlinedTextField(
                value = form.name,
                onValueChange = { v -> vm.update { it.copy(name = v) } },
                label = { Text("Nazwa") },
                singleLine = true,
                isError = triedToSave && form.nameError != null,
                supportingText = {
                    val error = form.nameError
                    if (triedToSave && error != null) Text(error)
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Label("Ikona")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EMOJIS.forEach { emoji ->
                    val selected = form.emoji == emoji
                    Surface(
                        onClick = { vm.update { it.copy(emoji = emoji) } },
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = if (selected) "Ikona $emoji, wybrana" else "Ikona $emoji" },
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(emoji, style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }
            OutlinedTextField(
                value = form.emoji,
                onValueChange = { v -> vm.update { it.copy(emoji = v.take(8)) } },
                label = { Text("Albo wpisz własne emoji") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.description,
                onValueChange = { v -> vm.update { it.copy(description = v) } },
                label = { Text("Opis (opcjonalnie)") },
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Label("Kategoria")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORIES.forEach { c ->
                    FilterChip(
                        selected = form.category == c,
                        onClick = { vm.update { it.copy(category = if (it.category == c) "" else c) } },
                        label = { Text(c) },
                    )
                }
            }
            OutlinedTextField(
                value = form.category,
                onValueChange = { v -> vm.update { it.copy(category = v) } },
                label = { Text("Własna kategoria") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()
            Label("Kiedy nawyk jest zaliczony?")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(CompletionType.MINUTES to "Czas", CompletionType.ACTION to "Jedno działanie")
                options.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = form.completionType == type,
                        onClick = { vm.update { it.copy(completionType = type) } },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    ) { Text(label) }
                }
            }

            if (form.completionType == CompletionType.MINUTES) {
                Text("Minimum na dzień", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30).forEach { m ->
                        FilterChip(
                            selected = form.minimumMinutes == m,
                            onClick = { vm.update { it.copy(minimumMinutes = m) } },
                            label = { Text("$m min") },
                        )
                    }
                }
                var minutesText by rememberSaveable(form.minimumMinutes) { mutableStateOf(form.minimumMinutes.toString()) }
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { v ->
                        minutesText = v.filter(Char::isDigit).take(3)
                        minutesText.toIntOrNull()?.let { n -> if (n in 1..600) vm.update { it.copy(minimumMinutes = n) } }
                    },
                    label = { Text("Własne minimum (minuty)") },
                    singleLine = true,
                    isError = minutesText.toIntOrNull()?.let { it !in 1..600 } ?: true,
                    supportingText = { Text("Pięć minut wystarczy. Dłużej to miły bonus.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                SwitchRow(
                    title = "Jedno zrozumiane pojęcie też się liczy",
                    subtitle = "Sesja z zaznaczonym „zrozumiałem pojęcie” zalicza dzień niezależnie od czasu.",
                    checked = form.allowConceptCompletion,
                    onChange = { v -> vm.update { it.copy(allowConceptCompletion = v) } },
                )
            }

            OutlinedTextField(
                value = form.minimumDescription,
                onValueChange = { v -> vm.update { it.copy(minimumDescription = v) } },
                label = { Text("Twoje minimum słowami (opcjonalnie)") },
                placeholder = {
                    Text(
                        if (form.completionType == CompletionType.MINUTES) PolishText.minutes(form.minimumMinutes)
                        else "np. jedna rzecz dla domu",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.target,
                onValueChange = { v -> vm.update { it.copy(target = v) } },
                label = { Text("Cel osobisty (opcjonalnie)") },
                supportingText = { Text("Coś, do czego zmierzasz. Nie jest wymagany do zaliczenia dnia.") },
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()
            Label("Dni")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val selected = day in form.scheduledDays
                    FilterChip(
                        selected = selected,
                        onClick = {
                            vm.update { f -> f.copy(scheduledDays = if (selected) f.scheduledDays - day else f.scheduledDays + day) }
                        },
                        label = { Text(day.shortLabel()) },
                    )
                }
            }
            Text(
                form.daysError ?: "Dni poza planem nie przerywają serii.",
                style = MaterialTheme.typography.bodySmall,
                color = if (form.daysError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SwitchRow(
                title = "Liczy się do serii",
                subtitle = "Wyłącz, jeśli chcesz tylko notować, bez liczenia dni z rzędu.",
                checked = form.countsTowardStreak,
                onChange = { v -> vm.update { it.copy(countsTowardStreak = v) } },
            )
            SwitchRow(
                title = "Nawyk naukowy",
                subtitle = "Pozwala zapisywać sesje nauki i notatki w dzienniku.",
                checked = form.isLearning,
                onChange = { v -> vm.update { it.copy(isLearning = v) } },
            )

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f).height(52.dp)) { Text("Anuluj") }
                Button(
                    onClick = {
                        triedToSave = true
                        vm.save()
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text("Zapisz") }
            }
            if (!isNew) {
                Text(
                    "Zmiany dotyczą kolejnych dni. Historia wykonań zostaje bez zmian.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Usunąć nawyk?",
            message = "„${form.name}” i jego historia wykonań zostaną usunięte. Wpisy w dzienniku zostaną. " +
                "Jeśli chcesz tylko zrobić przerwę, wybierz archiwizację.",
            confirmLabel = "Usuń",
            destructive = true,
            onConfirm = vm::delete,
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
internal fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        onClick = { onChange(!checked) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.role = Role.Switch },
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}
