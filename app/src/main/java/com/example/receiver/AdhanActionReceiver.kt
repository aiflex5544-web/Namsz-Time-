package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.AdhanPlayerService

class AdhanActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_STOP_ADHAN = "com.aistudio.noorpulse.ACTION_STOP_ADHAN"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_STOP_ADHAN) {
            // Stop service
            val serviceIntent = Intent(context, AdhanPlayerService::class.java).apply {
                action = AdhanPlayerService.ACTION_STOP_ADHAN
            }
            context.startService(serviceIntent)

            // Dismiss active foreground notification
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(AdhanPlayerService.NOTIFICATION_ID)
        }
    }
}
