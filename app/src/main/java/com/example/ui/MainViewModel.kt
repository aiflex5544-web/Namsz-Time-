package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.alarm.PrayerAlarmScheduler
import com.example.calculation.HijriCalendarHelper
import com.example.calculation.HijriDate
import com.example.calculation.PrayerCalculator
import com.example.calculation.QiblaCalculator
import com.example.data.PreferencesManager
import com.example.model.*
import com.example.sensor.CompassManager
import com.example.sensor.CompassState
import com.example.widget.PrayerWidgetProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class ActiveScreen {
    MOSQUE_CLOCK,
    CLOCK_STYLES,
    CUSTOM_TIMES,
    QIBLA_COMPASS,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager.getInstance(application)
    private val compassManager = CompassManager(application)
    private var previewMediaPlayer: MediaPlayer? = null

    // Preferences Flows
    val clockStyle = prefs.clockStyle
    val customDesign = prefs.customDesign
    val location = prefs.location
    val calculationSettings = prefs.calculationSettings
    val customTimes = prefs.customTimes
    val adhanSettings = prefs.adhanSettings
    val language = prefs.language
    val persistentNotificationEnabled = prefs.persistentNotificationEnabled

    // Active Screen
    private val _currentScreen = MutableStateFlow(ActiveScreen.MOSQUE_CLOCK)
    val currentScreen: StateFlow<ActiveScreen> = _currentScreen.asStateFlow()

    // Screen stack for back handling
    private val screenStack = ArrayDeque<ActiveScreen>()

    // Current Time Flow (ticks every second using true system timestamps)
    private val _currentTimestamp = MutableStateFlow(System.currentTimeMillis())
    val currentTimestamp: StateFlow<Long> = _currentTimestamp.asStateFlow()

    // Compass
    val compassState: StateFlow<CompassState> = compassManager.compassState

    // Preview audio playing state
    private val _isPreviewPlaying = MutableStateFlow(false)
    val isPreviewPlaying: StateFlow<Boolean> = _isPreviewPlaying.asStateFlow()

    // Location loading state
    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private var tickerJob: Job? = null

    init {
        startClockTicker()
    }

    private fun startClockTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                _currentTimestamp.value = System.currentTimeMillis()
                delay(1000L - (System.currentTimeMillis() % 1000L))
            }
        }
    }

    fun startCompass() {
        compassManager.startListening()
    }

    fun stopCompass() {
        compassManager.stopListening()
    }

    fun navigateTo(screen: ActiveScreen) {
        if (_currentScreen.value != screen) {
            screenStack.push(_currentScreen.value)
            _currentScreen.value = screen
            if (screen == ActiveScreen.QIBLA_COMPASS) {
                startCompass()
            } else {
                stopCompass()
            }
        }
    }

    fun navigateBack(): Boolean {
        return if (screenStack.isNotEmpty()) {
            val prev = screenStack.pop()
            _currentScreen.value = prev
            if (prev == ActiveScreen.QIBLA_COMPASS) {
                startCompass()
            } else {
                stopCompass()
            }
            true
        } else if (_currentScreen.value != ActiveScreen.MOSQUE_CLOCK) {
            _currentScreen.value = ActiveScreen.MOSQUE_CLOCK
            stopCompass()
            true
        } else {
            false
        }
    }

    // Schedule Computation
    val schedule: StateFlow<PrayerDaySchedule> = combine(
        currentTimestamp,
        location,
        calculationSettings,
        customTimes
    ) { now, loc, calc, custom ->
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        PrayerCalculator.calculateSchedule(cal, loc.latitude, loc.longitude, calc, custom)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PrayerCalculator.calculateSchedule(
            Calendar.getInstance(),
            location.value.latitude,
            location.value.longitude,
            calculationSettings.value,
            customTimes.value
        )
    )

    // Hijri Date
    val hijriDate: StateFlow<HijriDate> = combine(
        currentTimestamp,
        calculationSettings
    ) { now, calc ->
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        HijriCalendarHelper.getHijriDate(cal, calc.hijriOffsetDays)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HijriCalendarHelper.getHijriDate(Calendar.getInstance(), calculationSettings.value.hijriOffsetDays)
    )

    // Qibla Calculations
    val qiblaBearing: StateFlow<Double> = location.map { loc ->
        QiblaCalculator.calculateBearing(loc.latitude, loc.longitude)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        QiblaCalculator.calculateBearing(location.value.latitude, location.value.longitude)
    )

    val qiblaDistanceKm: StateFlow<Double> = location.map { loc ->
        QiblaCalculator.calculateDistanceKm(loc.latitude, loc.longitude)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        QiblaCalculator.calculateDistanceKm(location.value.latitude, location.value.longitude)
    )

    // Active Effective Design
    val effectiveDesign: StateFlow<CustomDesignConfig> = combine(
        clockStyle,
        customDesign
    ) { style, custom ->
        if (style == ClockStyle.STYLE_CUSTOM) {
            custom
        } else {
            PreferencesManager.STYLE_TEMPLATES[style] ?: PreferencesManager.STYLE_TEMPLATES.getValue(ClockStyle.STYLE_RED_MOSQUE)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        prefs.getEffectiveDesign()
    )

    // Actions
    fun setClockStyle(style: ClockStyle) {
        prefs.saveClockStyle(style)
    }

    fun updateCustomDesign(config: CustomDesignConfig) {
        prefs.saveCustomDesign(config)
    }

    fun restoreDefaultDesign() {
        prefs.saveCustomDesign(PreferencesManager.STYLE_TEMPLATES.getValue(ClockStyle.STYLE_RED_MOSQUE))
        prefs.saveClockStyle(ClockStyle.STYLE_RED_MOSQUE)
    }

    fun resetToActiveStyle() {
        val current = clockStyle.value
        val template = PreferencesManager.STYLE_TEMPLATES[current] ?: PreferencesManager.STYLE_TEMPLATES.getValue(ClockStyle.STYLE_RED_MOSQUE)
        prefs.saveCustomDesign(template)
    }

    fun setLocation(loc: LocationData) {
        prefs.saveLocation(loc)
        rescheduleAlarmsAndWidget()
    }

    fun setCalculationSettings(settings: CalculationSettings) {
        prefs.saveCalculationSettings(settings)
        rescheduleAlarmsAndWidget()
    }

    fun setCustomTimes(config: CustomTimesConfig) {
        prefs.saveCustomTimes(config)
        rescheduleAlarmsAndWidget()
    }

    fun setAdhanSettings(settings: AdhanSettings) {
        prefs.saveAdhanSettings(settings)
        rescheduleAlarmsAndWidget()
    }

    fun setLanguage(lang: String) {
        prefs.saveLanguage(lang)
    }

    fun setPersistentNotification(enabled: Boolean) {
        prefs.savePersistentNotificationEnabled(enabled)
    }

    private fun rescheduleAlarmsAndWidget() {
        val context = getApplication<Application>()
        PrayerAlarmScheduler.scheduleAllPrayers(context)
        PrayerWidgetProvider.updateAllWidgets(context)
    }

    // Auto-detect GPS location
    fun refreshCurrentLocation(onSuccess: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        val context = getApplication<Application>()
        _isLocating.value = true
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    _isLocating.value = false
                    if (loc != null) {
                        viewModelScope.launch {
                            val cityName = reverseGeocode(context, loc.latitude, loc.longitude)
                            val locData = LocationData(
                                cityName = cityName ?: "Current Location",
                                countryName = "",
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                isAutoDetected = true
                            )
                            setLocation(locData)
                            onSuccess?.invoke()
                        }
                    } else {
                        onError?.invoke("Could not acquire accurate GPS coordinates. Please ensure Location services are active.")
                    }
                }
                .addOnFailureListener { e ->
                    _isLocating.value = false
                    onError?.invoke(e.message ?: "Failed to get location")
                }
        } catch (e: SecurityException) {
            _isLocating.value = false
            onError?.invoke("Location permission not granted")
        }
    }

    private fun reverseGeocode(context: Context, lat: Double, lon: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.countryName
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // Audio Preview
    fun toggleAudioPreview() {
        if (_isPreviewPlaying.value) {
            stopAudioPreview()
        } else {
            playAudioPreview()
        }
    }

    private fun playAudioPreview() {
        stopAudioPreview()
        val context = getApplication<Application>()
        val adhan = adhanSettings.value

        try {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            var initialized = false
            if (!adhan.customAudioUri.isNullOrEmpty()) {
                try {
                    player.setDataSource(context, Uri.parse(adhan.customAudioUri))
                    initialized = true
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Failed to load custom uri for preview", e)
                }
            }

            if (!initialized) {
                val resId = when (adhan.soundType) {
                    AdhanSoundType.SHORT_TAKBEER -> R.raw.adhan_short
                    AdhanSoundType.BEEP_ALERT -> R.raw.beep_alert
                    else -> R.raw.adhan_default
                }
                val afd = context.resources.openRawResourceFd(resId)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            }

            player.prepare()
            player.setOnCompletionListener {
                stopAudioPreview()
            }
            player.start()
            previewMediaPlayer = player
            _isPreviewPlaying.value = true
        } catch (e: Exception) {
            Log.e("MainViewModel", "Preview audio failed: ${e.message}")
            stopAudioPreview()
        }
    }

    fun stopAudioPreview() {
        try {
            previewMediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            previewMediaPlayer = null
        } catch (e: Exception) {
            Log.w("MainViewModel", "Error stopping preview: ${e.message}")
        } finally {
            _isPreviewPlaying.value = false
        }
    }

    override fun onCleared() {
        stopAudioPreview()
        stopCompass()
        tickerJob?.cancel()
        super.onCleared()
    }
}
