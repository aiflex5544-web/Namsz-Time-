package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.alarm.PrayerAlarmScheduler
import com.example.widget.PrayerWidgetProvider

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "Received system broadcast: $action. Restoring prayer alarms and widget...")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                PrayerAlarmScheduler.scheduleAllPrayers(context)
                PrayerWidgetProvider.updateAllWidgets(context)
            }
        }
    }
}
