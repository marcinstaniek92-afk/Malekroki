package com.malekroki.app

import android.app.Application
import android.content.Context
import com.malekroki.app.data.AppRepository
import com.malekroki.app.data.DateProvider
import com.malekroki.app.data.SettingsRepository
import com.malekroki.app.data.db.AppDatabase
import com.malekroki.app.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual dependency container: small app, no DI framework needed. */
class AppContainer(context: Context) {
    val dates = DateProvider()
    val database: AppDatabase = AppDatabase.create(context)
    val settings = SettingsRepository(context)
    val repository = AppRepository(database, settings, dates)
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

class MaleKrokiApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderScheduler.createChannel(this)
        container.appScope.launch {
            container.repository.createStarterHabitsIfFirstLaunch()
        }
    }
}
