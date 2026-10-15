package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.calculation.PrayerCalculator
import com.example.data.PreferencesManager
import com.example.model.Prayer
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.aistudio.noorpulse.ACTION_UPDATE_WIDGET") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, PrayerWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, allWidgetIds)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, PrayerWidgetProvider::class.java).apply {
                action = "com.aistudio.noorpulse.ACTION_UPDATE_WIDGET"
            }
            context.sendBroadcast(intent)
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = PreferencesManager.getInstance(context)
            val location = prefs.location.value
            val calcSettings = prefs.calculationSettings.value
            val customTimes = prefs.customTimes.value
            val use24h = prefs.customDesign.value.use24Hour

            val cal = Calendar.getInstance()
            val schedule = PrayerCalculator.calculateSchedule(
                cal,
                location.latitude,
                location.longitude,
                calcSettings,
                customTimes
            )

            val views = RemoteViews(context.packageName, R.layout.widget_prayer_clock)

            // Header info
            views.setTextViewText(R.id.widget_title, "NoorPulse")
            views.setTextViewText(R.id.widget_location, location.cityName)

            // Next Prayer info
            val next = schedule.nextPrayer
            if (next != null) {
                views.setTextViewText(R.id.widget_next_prayer, "${next.prayer.englishName} • ${next.format(use24h)}")
                val diffMs = schedule.millisUntilNext
                val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMs) % 60
                views.setTextViewText(R.id.widget_countdown, String.format("-%02d:%02d:%02d", hours, minutes, seconds))
            } else {
                views.setTextViewText(R.id.widget_next_prayer, "Prayers Completed")
                views.setTextViewText(R.id.widget_countdown, "--:--:--")
            }

            // Five prayer times
            val prayersMap = schedule.prayers.associateBy { it.prayer }
            views.setTextViewText(R.id.widget_time_fajr, prayersMap[Prayer.FAJR]?.format(use24h) ?: "--:--")
            views.setTextViewText(R.id.widget_time_dhuhr, prayersMap[Prayer.DHUHR]?.format(use24h) ?: "--:--")
            views.setTextViewText(R.id.widget_time_asr, prayersMap[Prayer.ASR]?.format(use24h) ?: "--:--")
            views.setTextViewText(R.id.widget_time_maghrib, prayersMap[Prayer.MAGHRIB]?.format(use24h) ?: "--:--")
            views.setTextViewText(R.id.widget_time_isha, prayersMap[Prayer.ISHA]?.format(use24h) ?: "--:--")

            // On tap: open app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
