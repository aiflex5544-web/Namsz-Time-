package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.alarm.PrayerAlarmScheduler
import com.example.data.PreferencesManager
import com.example.model.Prayer
import com.example.service.AdhanPlayerService
import com.example.widget.PrayerWidgetProvider

class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val REMINDER_CHANNEL_ID = "prayer_reminder_channel"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action == "com.aistudio.noorpulse.ACTION_MIDNIGHT_REFRESH") {
            // Recalculate and schedule today's prayer times
            PrayerAlarmScheduler.scheduleAllPrayers(context)
            PrayerWidgetProvider.updateAllWidgets(context)
            return
        }

        if (action == PrayerAlarmScheduler.ACTION_PRAYER_ALARM) {
            val prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME) ?: return
            val prayer = try { Prayer.valueOf(prayerName) } catch (e: Exception) { return }

            val prefs = PreferencesManager.getInstance(context)
            val adhanSettings = prefs.adhanSettings.value
            val isUrdu = prefs.language.value == "ur"

            if (!adhanSettings.isPrayerEnabled(prayer)) {
                return
            }

            // Create notification channel if needed
            createNotificationChannel(context)

            val displayPrayerName = if (isUrdu) prayer.urduName else prayer.englishName
            val title = if (isUrdu) "وقتِ $displayPrayerName" else "Time for $displayPrayerName"
            val message = if (isUrdu) "حی علی الصلاۃ، حی علی الفلاح" else "Hurry to prayer, hurry to success"

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pLaunchIntent = PendingIntent.getActivity(
                context,
                prayer.ordinal,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopIntent = Intent(context, AdhanActionReceiver::class.java).apply {
                this.action = AdhanActionReceiver.ACTION_STOP_ADHAN
            }
            val pStopIntent = PendingIntent.getBroadcast(
                context,
                prayer.ordinal + 500,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationBuilder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_noorpulse_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pLaunchIntent)
                .addAction(android.R.drawable.ic_media_pause, if (isUrdu) "بند کریں" else "Dismiss", pStopIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 250, 500))

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(1000 + prayer.ordinal, notificationBuilder.build())

            // Start Adhan audio if enabled and not vibrate-only
            if (!adhanSettings.vibrateOnly) {
                val serviceIntent = Intent(context, AdhanPlayerService::class.java).apply {
                    this.action = AdhanPlayerService.ACTION_PLAY_ADHAN
                    putExtra(AdhanPlayerService.EXTRA_PRAYER, prayer.name)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }

            // Reschedule upcoming prayers
            PrayerAlarmScheduler.scheduleAllPrayers(context)

            // Update Home Screen Widget
            PrayerWidgetProvider.updateAllWidgets(context)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Prayer Time Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications for prayer times and Adhan"
                enableVibration(true)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
