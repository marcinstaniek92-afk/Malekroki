package com.malekroki.app.ui.settings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malekroki.app.AppContainer
import com.malekroki.app.data.AppSettings
import com.malekroki.app.data.BackupJson
import com.malekroki.app.domain.AppData
import com.malekroki.app.domain.ThemeMode
import com.malekroki.app.domain.backup.BackupValidation
import com.malekroki.app.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** A validated import waiting for the user to choose merge or replace. */
data class PendingImport(val data: AppData, val warnings: List<String>)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val settingsRepo = container.settings
    private val repo = container.repository

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    val pendingImport: StateFlow<PendingImport?> = _pendingImport.asStateFlow()

    private val messages = Channel<String>(Channel.BUFFERED)
    val messageFlow = messages.receiveAsFlow()

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDynamicColor(enabled) }
    }

    fun setReminders(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setRemindersEnabled(enabled)
            if (enabled) ReminderScheduler.schedule(context.applicationContext, settings.value.reminderMinuteOfDay)
            else ReminderScheduler.cancel(context.applicationContext)
        }
    }

    fun setReminderTime(context: Context, minuteOfDay: Int) {
        viewModelScope.launch {
            settingsRepo.setReminderTime(minuteOfDay)
            if (settingsRepo.current().remindersEnabled) ReminderScheduler.schedule(context.applicationContext, minuteOfDay)
        }
    }

    fun suggestedFileName(): String = "male-kroki-kopia-${container.dates.today()}.json"

    fun export(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val json = BackupJson.encode(repo.snapshot(), LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                        ?: throw IOException("Brak dostępu do pliku")
                }
                messages.send("Kopia zapasowa zapisana.")
            } catch (e: Exception) {
                messages.send("Nie udało się zapisać kopii: ${e.message ?: "nieznany błąd"}")
            }
        }
    }

    fun readImport(resolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val text = try {
                withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { stream ->
                        // Generous limit for a personal backup; protects against picking a huge wrong file.
                        val bytes = stream.readBytes()
                        if (bytes.size > 20 * 1024 * 1024) throw IOException("Plik jest za duży.")
                        String(bytes, Charsets.UTF_8)
                    } ?: throw IOException("Nie można otworzyć pliku.")
                }
            } catch (e: Exception) {
                messages.send(e.message ?: "Nie można odczytać pliku.")
                return@launch
            }
            when (val result = withContext(Dispatchers.Default) { BackupJson.decodeAndValidate(text) }) {
                is BackupValidation.Invalid -> messages.send("Nie zaimportowano danych:\n" + result.errors.joinToString("\n"))
                is BackupValidation.Valid -> _pendingImport.value = PendingImport(result.data, result.warnings)
            }
        }
    }

    fun cancelImport() { _pendingImport.value = null }

    fun confirmImport(replace: Boolean) {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        viewModelScope.launch {
            try {
                if (replace) repo.replaceAll(pending.data) else repo.mergeIn(pending.data)
                messages.send(if (replace) "Dane zostały zastąpione kopią." else "Kopia została połączona z Twoimi danymi.")
            } catch (e: Exception) {
                messages.send("Import nie powiódł się. Twoje dane nie zostały zmienione.")
            }
        }
    }

    fun deleteEverything() {
        viewModelScope.launch {
            repo.deleteEverything()
            messages.send("Wszystkie dane zostały usunięte.")
        }
    }

    fun restoreStarters() {
        viewModelScope.launch {
            repo.restoreStarterHabits()
            messages.send("Dodano przykładowe nawyki.")
        }
    }
}
