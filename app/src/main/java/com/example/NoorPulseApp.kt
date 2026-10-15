package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.alarm.PrayerAlarmScheduler
import com.example.receiver.PrayerAlarmReceiver
import com.example.service.AdhanPlayerService
import com.example.widget.PrayerWidgetProvider

class NoorPulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        setupNotificationChannels()
        PrayerAlarmScheduler.scheduleAllPrayers(this)
        PrayerWidgetProvider.updateAllWidgets(this)
    }

    private fun setupNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // High Priority Adhan & Prayer Alerts
            val reminderChannel = NotificationChannel(
                PrayerAlarmReceiver.REMINDER_CHANNEL_ID,
                "Prayer Time Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when prayer times arrive with Adhan alerts"
                enableVibration(true)
            }

            // Foreground Audio Playback Controls
            val audioChannel = NotificationChannel(
                AdhanPlayerService.CHANNEL_ID,
                "Adhan Audio Playback",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active Adhan playback and controls"
                setSound(null, null)
            }

            // Persistent Next-Prayer Lockscreen / Status Bar Notification
            val persistentChannel = NotificationChannel(
                "persistent_prayer_channel",
                "Next Prayer Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live next prayer countdown on status bar and lockscreen"
                setShowBadge(false)
            }

            nm.createNotificationChannels(listOf(reminderChannel, audioChannel, persistentChannel))
        }
    }
}
