@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.malekroki.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.malekroki.app.domain.PolishText
import com.malekroki.app.domain.ThemeMode
import com.malekroki.app.notifications.ReminderScheduler
import com.malekroki.app.ui.common.ConfirmDialog
import com.malekroki.app.ui.common.SectionHeader
import com.malekroki.app.ui.common.appViewModel
import com.malekroki.app.ui.habits.SwitchRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenLeaves: () -> Unit) {
    val vm = appViewModel { c, _ -> SettingsViewModel(c) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val pendingImport by vm.pendingImport.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var showDeleteAll by rememberSaveable { mutableStateOf(false) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(vm) { vm.messageFlow.collect { snackbar.showSnackbar(it) } }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.export(context.contentResolver, uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.readImport(context.contentResolver, uri)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        vm.setReminders(context, granted)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Ustawienia") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader("Wygląd")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(ThemeMode.SYSTEM to "Systemowy", ThemeMode.LIGHT to "Jasny", ThemeMode.DARK to "Ciemny")
                options.forEachIndexed { i, (mode, label) ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { vm.setTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(label) }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(
                    title = "Kolory z tapety",
                    subtitle = "Dopasuj kolory aplikacji do motywu telefonu (Material You).",
                    checked = settings.dynamicColor,
                    onChange = vm::setDynamicColor,
                )
            }

            SectionHeader("Przypomnienie")
            SwitchRow(
                title = "Codzienne przypomnienie",
                subtitle = "Jedno spokojne powiadomienie dziennie, tylko jeśli coś z planu na dziś jest jeszcze otwarte. Nigdy o „utracie serii”.",
                checked = settings.remindersEnabled,
                onChange = { enable ->
                    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderScheduler.canPostNotifications(context)) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.setReminders(context, enable)
                    }
                },
            )
            if (permissionDenied) {
                Text(
                    "Android nie pozwolił na powiadomienia. Możesz je włączyć w Ustawieniach telefonu → Aplikacje → Małe Kroki → Powiadomienia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (settings.remindersEnabled) {
                ActionRow(Icons.Outlined.Schedule, "Godzina", formatTime(settings.reminderMinuteOfDay)) { showTime = true }
            }
            Text(
                "Przypomnienie wyłączysz tutaj w każdej chwili. Działa bez internetu i bez konta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionHeader("Nieobecności")
            ActionRow(Icons.Outlined.BeachAccess, "Urlopy, choroby i przerwy", "Chroń serie, gdy życie ma inne plany", onClick = onOpenLeaves)

            SectionHeader("Kopia zapasowa")
            ActionRow(Icons.Outlined.FileDownload, "Eksportuj dane", "Zapisz wszystko do pliku JSON") {
                exportLauncher.launch(vm.suggestedFileName())
            }
            ActionRow(Icons.Outlined.FileUpload, "Importuj dane", "Przywróć z pliku JSON — przed zmianą zapytamy o zgodę") {
                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            }
            Text(
                "Twoje dane są tylko na tym telefonie. Aplikacja nie ma konta, reklam ani analityki i nie wysyła niczego do internetu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionHeader("Dane")
            ActionRow(Icons.Outlined.Restore, "Dodaj przykładowe nawyki", "ServiceNow, HTML i CSS, JavaScript, Życie osobiste", onClick = vm::restoreStarters)
            ActionRow(Icons.Outlined.DeleteForever, "Usuń wszystkie dane", "Nawyki, historia, dziennik i nieobecności", destructive = true) {
                showDeleteAll = true
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text("Małe Kroki", style = MaterialTheme.typography.titleSmall)
            Text(
                "Pięć minut się liczy. Piętnaście to dużo. Godzina to bonus, nie wymóg. " +
                    "Odpoczynek, granie i rozrywka są normalną częścią życia, a dzień bez wpisu to po prostu dzień.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showTime) {
        val state = rememberTimePickerState(
            initialHour = settings.reminderMinuteOfDay / 60,
            initialMinute = settings.reminderMinuteOfDay % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("Godzina przypomnienia") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    vm.setReminderTime(context, state.hour * 60 + state.minute)
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Anuluj") } },
        )
    }

    pendingImport?.let { pending ->
        val d = pending.data
        AlertDialog(
            onDismissRequest = vm::cancelImport,
            title = { Text("Zaimportować kopię?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "W pliku: ${PolishText.habits(d.habits.size)}, ${d.completions.size} wykonanych dni, " +
                            "${PolishText.sessions(d.sessions.size)}, nieobecności: ${d.leaves.size}.",
                    )
                    pending.warnings.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Text(
                        "Połącz — doda brakujące wpisy i zaktualizuje istniejące.\nZastąp — usunie obecne dane i wczyta tylko kopię.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { vm.confirmImport(replace = true) }) {
                        Text("Zastąp", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { vm.confirmImport(replace = false) }) { Text("Połącz") }
                }
            },
            dismissButton = { TextButton(onClick = vm::cancelImport) { Text("Anuluj") } },
        )
    }

    if (showDeleteAll) {
        var understood by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            title = { Text("Usunąć wszystkie dane?") },
            text = {
                Column {
                    Text("Nawyki, historia serii, dziennik, refleksje i nieobecności zostaną trwale usunięte. Warto najpierw wyeksportować kopię.")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { understood = !understood }
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = understood, onCheckedChange = { understood = it })
                        Text("Rozumiem, że tego nie da się cofnąć")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = understood,
                    onClick = {
                        vm.deleteEverything()
                        showDeleteAll = false
                    },
                ) { Text("Usuń wszystko", color = if (understood) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAll = false }) { Text("Anuluj") } },
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatTime(minuteOfDay: Int) = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
