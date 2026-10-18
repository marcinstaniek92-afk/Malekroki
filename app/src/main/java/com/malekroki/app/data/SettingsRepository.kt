package com.malekroki.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.malekroki.app.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    /** Reminders are off until the user turns them on. */
    val remindersEnabled: Boolean = false,
    val reminderMinuteOfDay: Int = 19 * 60,
    val starterHabitsCreated: Boolean = false,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val REMINDERS = booleanPreferencesKey("reminders_enabled")
        val REMINDER_TIME = intPreferencesKey("reminder_minute_of_day")
        val STARTERS = booleanPreferencesKey("starter_habits_created")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == p[Keys.THEME] } ?: ThemeMode.SYSTEM,
            dynamicColor = p[Keys.DYNAMIC] ?: false,
            remindersEnabled = p[Keys.REMINDERS] ?: false,
            reminderMinuteOfDay = p[Keys.REMINDER_TIME] ?: (19 * 60),
            starterHabitsCreated = p[Keys.STARTERS] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC] = enabled }
    suspend fun setRemindersEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.REMINDERS] = enabled }
    suspend fun setReminderTime(minuteOfDay: Int) = context.dataStore.edit { it[Keys.REMINDER_TIME] = minuteOfDay }
    suspend fun markStarterHabitsCreated() = context.dataStore.edit { it[Keys.STARTERS] = true }
}
