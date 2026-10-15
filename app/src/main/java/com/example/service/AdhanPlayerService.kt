package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesManager
import com.example.model.AdhanSoundType
import com.example.model.Prayer
import com.example.receiver.AdhanActionReceiver

class AdhanPlayerService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    companion object {
        private const val TAG = "AdhanPlayerService"
        const val CHANNEL_ID = "adhan_playback_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_PLAY_ADHAN = "com.aistudio.noorpulse.ACTION_PLAY_ADHAN"
        const val ACTION_STOP_ADHAN = "com.aistudio.noorpulse.ACTION_STOP_ADHAN"
        const val EXTRA_PRAYER = "extra_prayer"
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_ADHAN) {
            stopAdhanPlayback()
            stopSelf()
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra(EXTRA_PRAYER) ?: "Prayer"
        val prayer = try { Prayer.valueOf(prayerName) } catch (e: Exception) { null }

        startForeground(NOTIFICATION_ID, buildPlaybackNotification(prayerName))
        playAdhan(prayer)

        return START_NOT_STICKY
    }

    private fun playAdhan(prayer: Prayer?) {
        stopAdhanPlayback()

        val prefs = PreferencesManager.getInstance(this)
        val adhanSettings = prefs.adhanSettings.value

        if (adhanSettings.vibrateOnly) {
            stopSelf()
            return
        }

        try {
            // Request Audio Focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            stopAdhanPlayback()
                            stopSelf()
                        }
                    }
                    .build()
                audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
            }

            // Determine audio source
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            var initialized = false
            if (!adhanSettings.customAudioUri.isNullOrEmpty()) {
                try {
                    player.setDataSource(this, Uri.parse(adhanSettings.customAudioUri))
                    initialized = true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load custom audio uri, falling back to default", e)
                }
            }

            if (!initialized) {
                val rawResId = when (adhanSettings.soundType) {
                    AdhanSoundType.SHORT_TAKBEER -> R.raw.adhan_short
                    AdhanSoundType.BEEP_ALERT -> R.raw.beep_alert
                    else -> R.raw.adhan_default
                }
                val afd = resources.openRawResourceFd(rawResId)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            }

            player.prepare()
            player.setOnCompletionListener {
                stopAdhanPlayback()
                stopSelf()
            }
            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                stopAdhanPlayback()
                stopSelf()
                true
            }

            player.start()
            mediaPlayer = player

        } catch (e: Exception) {
            Log.e(TAG, "Error playing adhan", e)
            stopAdhanPlayback()
            stopSelf()
        }
    }

    private fun stopAdhanPlayback() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error during adhan cleanup: ${e.message}")
        }
    }

    private fun buildPlaybackNotification(prayerName: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pLaunchIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, AdhanActionReceiver::class.java).apply {
            action = com.example.receiver.AdhanActionReceiver.ACTION_STOP_ADHAN
        }
        val pStopIntent = PendingIntent.getBroadcast(this, 201, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_noorpulse_logo)
            .setContentTitle("Adhan: $prayerName")
            .setContentText("Adhan call to prayer is currently playing")
            .setContentIntent(pLaunchIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop Adhan", pStopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Adhan Audio Playback",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows active Adhan audio player controls"
                setSound(null, null)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopAdhanPlayback()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
