package com.malekroki.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.malekroki.app.MainActivity
import com.malekroki.app.MaleKrokiApp
import com.malekroki.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * One gentle, local, daily reminder. Off by default.
 *
 * Uses an inexact alarm window (no exact-alarm permission, battery friendly).
 * Nothing runs in the background besides this single alarm.
 */
object ReminderScheduler {

    const val CHANNEL_ID = "daily_reminder"
    private const val REQUEST_CODE = 1001
    private const val NOTIFICATION_ID = 2001
    private const val WINDOW_MILLIS = 15 * 60 * 1000L

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Codzienne przypomnienie",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Jedno spokojne przypomnienie dziennie, jeśli je włączysz."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, minuteOfDay: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val zone = ZoneId.systemDefault()
        val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        var next = LocalDate.now(zone).atTime(time).atZone(zone)
        if (!next.toInstant().isAfter(java.time.Instant.now())) next = next.plusDays(1)
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            WINDOW_MILLIS,
            pendingIntent(context),
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    internal fun show(context: Context, text: String) {
        if (!canPostNotifications(context)) return
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Małe Kroki")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call: stay silent.
        }
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Fires once a day. Shows a calm note only if something planned is still open today. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = (context.applicationContext as MaleKrokiApp).container
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = container.settings.current()
                if (!settings.remindersEnabled) return@launch
                // Re-arm first so the next day is scheduled even if the check below fails.
                ReminderScheduler.schedule(context, settings.reminderMinuteOfDay)

                val today = container.dates.today()
                val habits = container.repository.habits.first()
                    .filter { !it.archived && today.dayOfWeek in it.scheduledDays }
                val done = container.repository.completionsOn(today).map { it.habitId }.toSet()
                val leaves = container.repository.currentLeaveCalendar()
                val open = habits.filter { it.id !in done && !leaves.isProtected(it.id, today) }
                if (open.isNotEmpty()) {
                    ReminderScheduler.show(context, "Masz chwilę na coś małego? Pięć minut też się liczy.")
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms do not survive reboots, app updates or clock changes; re-arm when enabled. */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val container = (context.applicationContext as MaleKrokiApp).container
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = container.settings.current()
                if (settings.remindersEnabled) ReminderScheduler.schedule(context, settings.reminderMinuteOfDay)
            } finally {
                pending.finish()
            }
        }
    }
}
