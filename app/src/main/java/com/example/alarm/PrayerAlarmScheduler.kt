package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.calculation.PrayerCalculator
import com.example.data.PreferencesManager
import com.example.model.Prayer
import com.example.receiver.PrayerAlarmReceiver
import java.util.Calendar

object PrayerAlarmScheduler {
    private const val TAG = "PrayerAlarmScheduler"
    const val ACTION_PRAYER_ALARM = "com.aistudio.noorpulse.ACTION_PRAYER_ALARM"
    const val EXTRA_PRAYER_NAME = "extra_prayer_name"

    private fun getRequestCode(prayer: Prayer): Int = when (prayer) {
        Prayer.FAJR -> 1001
        Prayer.SUNRISE -> 1002
        Prayer.DHUHR -> 1003
        Prayer.ASR -> 1004
        Prayer.MAGHRIB -> 1005
        Prayer.ISHA -> 1006
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleAllPrayers(context: Context) {
        val prefs = PreferencesManager.getInstance(context)
        val location = prefs.location.value
        val calcSettings = prefs.calculationSettings.value
        val customTimes = prefs.customTimes.value
        val adhanSettings = prefs.adhanSettings.value

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        // Schedule today's or tomorrow's times for all 5 main prayers
        val mainPrayers = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

        for (prayer in mainPrayers) {
            // Check if user enabled notification/alarm for this prayer
            if (!adhanSettings.isPrayerEnabled(prayer)) {
                cancelAlarm(context, prayer)
                continue
            }

            // Calculate for today
            val todayCal = Calendar.getInstance()
            val todaySchedule = PrayerCalculator.calculateSchedule(
                todayCal,
                location.latitude,
                location.longitude,
                calcSettings,
                customTimes
            )
            val todayPrayerTime = todaySchedule.prayers.firstOrNull { it.prayer == prayer }

            val triggerTime: Long = if (todayPrayerTime != null && todayPrayerTime.timestampMillis > now + 1000L) {
                todayPrayerTime.timestampMillis
            } else {
                // Time for today has already passed, schedule for tomorrow
                val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
                val tomorrowSchedule = PrayerCalculator.calculateSchedule(
                    tomorrowCal,
                    location.latitude,
                    location.longitude,
                    calcSettings,
                    customTimes
                )
                val tomorrowPrayerTime = tomorrowSchedule.prayers.firstOrNull { it.prayer == prayer }
                tomorrowPrayerTime?.timestampMillis ?: (now + 24 * 3600 * 1000L)
            }

            scheduleExactOrInexact(context, alarmManager, prayer, triggerTime)
        }

        // Also schedule daily midnight refresher alarm at 00:01 AM
        scheduleMidnightRefresh(context, alarmManager)
    }

    private fun scheduleExactOrInexact(
        context: Context,
        alarmManager: AlarmManager,
        prayer: Prayer,
        triggerTimeMillis: Long
    ) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM
            putExtra(EXTRA_PRAYER_NAME, prayer.name)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(prayer),
            intent,
            flags
        )

        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for ${prayer.name} at $triggerTimeMillis")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission denied, falling back to setAndAllowWhileIdle: ${e.message}")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, prayer: Prayer) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM
        }
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, getRequestCode(prayer), intent, flags)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduleMidnightRefresh(context: Context, alarmManager: AlarmManager) {
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 2)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = "com.aistudio.noorpulse.ACTION_MIDNIGHT_REFRESH"
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, 9999, intent, flags)

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                midnight.timeInMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error scheduling midnight refresh: ${e.message}")
        }
    }
}
