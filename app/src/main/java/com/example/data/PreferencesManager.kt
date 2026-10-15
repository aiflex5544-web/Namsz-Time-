package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("noorpulse_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // Preset theme templates for the 4 digital styles
        val STYLE_TEMPLATES = mapOf(
            ClockStyle.STYLE_RED_MOSQUE to CustomDesignConfig(
                backgroundColorHex = 0xFF050505,
                clockNumberColorHex = 0xFFFF1E27,
                prayerNameColorHex = 0xFFFFD700,
                prayerTimeColorHex = 0xFFFFFFFF,
                countdownColorHex = 0xFFFF3B30,
                highlightColorHex = 0xFF2B0909,
                fontSizeScale = 1.0f,
                clockSizeScale = 1.0f,
                spacing = PrayerRowSpacing.COMFORTABLE,
                use24Hour = false,
                showSeconds = true,
                showHijri = true,
                showCountdown = true,
                compactTimetable = false
            ),
            ClockStyle.STYLE_GREEN_ISLAMIC to CustomDesignConfig(
                backgroundColorHex = 0xFF03140D,
                clockNumberColorHex = 0xFF00E676,
                prayerNameColorHex = 0xFF69F0AE,
                prayerTimeColorHex = 0xFFF0FDF4,
                countdownColorHex = 0xFF00E676,
                highlightColorHex = 0xFF0B2E1E,
                fontSizeScale = 1.0f,
                clockSizeScale = 1.0f,
                spacing = PrayerRowSpacing.COMFORTABLE,
                use24Hour = false,
                showSeconds = true,
                showHijri = true,
                showCountdown = true,
                compactTimetable = false
            ),
            ClockStyle.STYLE_PREMIUM_WHITE to CustomDesignConfig(
                backgroundColorHex = 0xFF0F141C,
                clockNumberColorHex = 0xFFFFFFFF,
                prayerNameColorHex = 0xFF94A3B8,
                prayerTimeColorHex = 0xFFF8FAFC,
                countdownColorHex = 0xFF38BDF8,
                highlightColorHex = 0xFF1E293B,
                fontSizeScale = 1.0f,
                clockSizeScale = 1.0f,
                spacing = PrayerRowSpacing.COMFORTABLE,
                use24Hour = false,
                showSeconds = true,
                showHijri = true,
                showCountdown = true,
                compactTimetable = false
            ),
            ClockStyle.STYLE_NEON_DIGITAL to CustomDesignConfig(
                backgroundColorHex = 0xFF090817,
                clockNumberColorHex = 0xFF00F0FF,
                prayerNameColorHex = 0xFFFF007A,
                prayerTimeColorHex = 0xFFE0E7FF,
                countdownColorHex = 0xFFFFB800,
                highlightColorHex = 0xFF1A1238,
                fontSizeScale = 1.0f,
                clockSizeScale = 1.0f,
                spacing = PrayerRowSpacing.COMFORTABLE,
                use24Hour = false,
                showSeconds = true,
                showHijri = true,
                showCountdown = true,
                compactTimetable = false
            )
        )
    }

    // StateFlows for UI observing
    private val _clockStyle = MutableStateFlow(loadClockStyle())
    val clockStyle: StateFlow<ClockStyle> = _clockStyle.asStateFlow()

    private val _customDesign = MutableStateFlow(loadCustomDesign())
    val customDesign: StateFlow<CustomDesignConfig> = _customDesign.asStateFlow()

    private val _location = MutableStateFlow(loadLocation())
    val location: StateFlow<LocationData> = _location.asStateFlow()

    private val _calculationSettings = MutableStateFlow(loadCalculationSettings())
    val calculationSettings: StateFlow<CalculationSettings> = _calculationSettings.asStateFlow()

    private val _customTimes = MutableStateFlow(loadCustomTimes())
    val customTimes: StateFlow<CustomTimesConfig> = _customTimes.asStateFlow()

    private val _adhanSettings = MutableStateFlow(loadAdhanSettings())
    val adhanSettings: StateFlow<AdhanSettings> = _adhanSettings.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString("language", "en") ?: "en")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _persistentNotificationEnabled = MutableStateFlow(prefs.getBoolean("persistent_notification", true))
    val persistentNotificationEnabled: StateFlow<Boolean> = _persistentNotificationEnabled.asStateFlow()

    // Loaders
    private fun loadClockStyle(): ClockStyle {
        val name = prefs.getString("clock_style", ClockStyle.STYLE_RED_MOSQUE.name)
        return try {
            ClockStyle.valueOf(name ?: ClockStyle.STYLE_RED_MOSQUE.name)
        } catch (e: Exception) {
            ClockStyle.STYLE_RED_MOSQUE
        }
    }

    private fun loadCustomDesign(): CustomDesignConfig {
        return CustomDesignConfig(
            backgroundColorHex = prefs.getLong("cd_bg", 0xFF050505),
            clockNumberColorHex = prefs.getLong("cd_num", 0xFFFF1E27),
            prayerNameColorHex = prefs.getLong("cd_pname", 0xFFFFD700),
            prayerTimeColorHex = prefs.getLong("cd_ptime", 0xFFFFFFFF),
            countdownColorHex = prefs.getLong("cd_cd", 0xFFFF3B30),
            highlightColorHex = prefs.getLong("cd_hl", 0xFF2B0909),
            fontSizeScale = prefs.getFloat("cd_fscale", 1.0f),
            clockSizeScale = prefs.getFloat("cd_cscale", 1.0f),
            spacing = PrayerRowSpacing.entries.getOrElse(prefs.getInt("cd_spacing", 1)) { PrayerRowSpacing.COMFORTABLE },
            brightness = prefs.getFloat("cd_brightness", -1.0f),
            use24Hour = prefs.getBoolean("cd_use24", false),
            showSeconds = prefs.getBoolean("cd_seconds", true),
            showHijri = prefs.getBoolean("cd_hijri", true),
            showCountdown = prefs.getBoolean("cd_countdown", true),
            compactTimetable = prefs.getBoolean("cd_compact", false)
        )
    }

    private fun loadLocation(): LocationData {
        return LocationData(
            cityName = prefs.getString("loc_city", "Karachi") ?: "Karachi",
            countryName = prefs.getString("loc_country", "Pakistan") ?: "Pakistan",
            latitude = java.lang.Double.longBitsToDouble(prefs.getLong("loc_lat", java.lang.Double.doubleToLongBits(24.8607))),
            longitude = java.lang.Double.longBitsToDouble(prefs.getLong("loc_lon", java.lang.Double.doubleToLongBits(67.0011))),
            isAutoDetected = prefs.getBoolean("loc_auto", false)
        )
    }

    private fun loadCalculationSettings(): CalculationSettings {
        val methodName = prefs.getString("calc_method", CalculationMethod.KARACHI.name)
        val method = try { CalculationMethod.valueOf(methodName ?: CalculationMethod.KARACHI.name) } catch (e: Exception) { CalculationMethod.KARACHI }
        val asrName = prefs.getString("calc_asr", AsrJuristic.HANAFI.name)
        val asr = try { AsrJuristic.valueOf(asrName ?: AsrJuristic.HANAFI.name) } catch (e: Exception) { AsrJuristic.HANAFI }

        return CalculationSettings(
            method = method,
            asrJuristic = asr,
            fajrOffset = prefs.getInt("calc_fajr_off", 0),
            dhuhrOffset = prefs.getInt("calc_dhuhr_off", 0),
            asrOffset = prefs.getInt("calc_asr_off", 0),
            maghribOffset = prefs.getInt("calc_maghrib_off", 0),
            ishaOffset = prefs.getInt("calc_isha_off", 0),
            hijriOffsetDays = prefs.getInt("calc_hijri_off", 0)
        )
    }

    private fun loadCustomTimes(): CustomTimesConfig {
        return CustomTimesConfig(
            isCustomModeEnabled = prefs.getBoolean("ct_enabled", false),
            fajrHour = prefs.getInt("ct_f_h", 5),
            fajrMinute = prefs.getInt("ct_f_m", 15),
            dhuhrHour = prefs.getInt("ct_d_h", 12),
            dhuhrMinute = prefs.getInt("ct_d_m", 30),
            asrHour = prefs.getInt("ct_a_h", 16),
            asrMinute = prefs.getInt("ct_a_m", 30),
            maghribHour = prefs.getInt("ct_m_h", 18),
            maghribMinute = prefs.getInt("ct_m_m", 15),
            ishaHour = prefs.getInt("ct_i_h", 19),
            ishaMinute = prefs.getInt("ct_i_m", 30)
        )
    }

    private fun loadAdhanSettings(): AdhanSettings {
        val soundTypeName = prefs.getString("adhan_sound", AdhanSoundType.FULL_ADHAN.name)
        val soundType = try { AdhanSoundType.valueOf(soundTypeName ?: AdhanSoundType.FULL_ADHAN.name) } catch (e: Exception) { AdhanSoundType.FULL_ADHAN }

        return AdhanSettings(
            globalEnabled = prefs.getBoolean("adhan_global", true),
            fajrEnabled = prefs.getBoolean("adhan_fajr", true),
            dhuhrEnabled = prefs.getBoolean("adhan_dhuhr", true),
            asrEnabled = prefs.getBoolean("adhan_asr", true),
            maghribEnabled = prefs.getBoolean("adhan_maghrib", true),
            ishaEnabled = prefs.getBoolean("adhan_isha", true),
            soundType = soundType,
            customAudioUri = prefs.getString("adhan_uri", null),
            vibrateOnly = prefs.getBoolean("adhan_vibrate_only", false)
        )
    }

    // Savers
    fun saveClockStyle(style: ClockStyle) {
        prefs.edit().putString("clock_style", style.name).apply()
        _clockStyle.value = style
    }

    fun saveCustomDesign(config: CustomDesignConfig) {
        prefs.edit()
            .putLong("cd_bg", config.backgroundColorHex)
            .putLong("cd_num", config.clockNumberColorHex)
            .putLong("cd_pname", config.prayerNameColorHex)
            .putLong("cd_ptime", config.prayerTimeColorHex)
            .putLong("cd_cd", config.countdownColorHex)
            .putLong("cd_hl", config.highlightColorHex)
            .putFloat("cd_fscale", config.fontSizeScale)
            .putFloat("cd_cscale", config.clockSizeScale)
            .putInt("cd_spacing", config.spacing.ordinal)
            .putFloat("cd_brightness", config.brightness)
            .putBoolean("cd_use24", config.use24Hour)
            .putBoolean("cd_seconds", config.showSeconds)
            .putBoolean("cd_hijri", config.showHijri)
            .putBoolean("cd_countdown", config.showCountdown)
            .putBoolean("cd_compact", config.compactTimetable)
            .apply()
        _customDesign.value = config
    }

    fun saveLocation(location: LocationData) {
        prefs.edit()
            .putString("loc_city", location.cityName)
            .putString("loc_country", location.countryName)
            .putLong("loc_lat", java.lang.Double.doubleToRawLongBits(location.latitude))
            .putLong("loc_lon", java.lang.Double.doubleToRawLongBits(location.longitude))
            .putBoolean("loc_auto", location.isAutoDetected)
            .apply()
        _location.value = location
    }

    fun saveCalculationSettings(settings: CalculationSettings) {
        prefs.edit()
            .putString("calc_method", settings.method.name)
            .putString("calc_asr", settings.asrJuristic.name)
            .putInt("calc_fajr_off", settings.fajrOffset)
            .putInt("calc_dhuhr_off", settings.dhuhrOffset)
            .putInt("calc_asr_off", settings.asrOffset)
            .putInt("calc_maghrib_off", settings.maghribOffset)
            .putInt("calc_isha_off", settings.ishaOffset)
            .putInt("calc_hijri_off", settings.hijriOffsetDays)
            .apply()
        _calculationSettings.value = settings
    }

    fun saveCustomTimes(config: CustomTimesConfig) {
        prefs.edit()
            .putBoolean("ct_enabled", config.isCustomModeEnabled)
            .putInt("ct_f_h", config.fajrHour)
            .putInt("ct_f_m", config.fajrMinute)
            .putInt("ct_d_h", config.dhuhrHour)
            .putInt("ct_d_m", config.dhuhrMinute)
            .putInt("ct_a_h", config.asrHour)
            .putInt("ct_a_m", config.asrMinute)
            .putInt("ct_m_h", config.maghribHour)
            .putInt("ct_m_m", config.maghribMinute)
            .putInt("ct_i_h", config.ishaHour)
            .putInt("ct_i_m", config.ishaMinute)
            .apply()
        _customTimes.value = config
    }

    fun saveAdhanSettings(settings: AdhanSettings) {
        prefs.edit()
            .putBoolean("adhan_global", settings.globalEnabled)
            .putBoolean("adhan_fajr", settings.fajrEnabled)
            .putBoolean("adhan_dhuhr", settings.dhuhrEnabled)
            .putBoolean("adhan_asr", settings.asrEnabled)
            .putBoolean("adhan_maghrib", settings.maghribEnabled)
            .putBoolean("adhan_isha", settings.ishaEnabled)
            .putString("adhan_sound", settings.soundType.name)
            .putString("adhan_uri", settings.customAudioUri)
            .putBoolean("adhan_vibrate_only", settings.vibrateOnly)
            .apply()
        _adhanSettings.value = settings
    }

    fun saveLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _language.value = lang
    }

    fun savePersistentNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("persistent_notification", enabled).apply()
        _persistentNotificationEnabled.value = enabled
    }

    // Active design resolving: returns template for predefined style, or stored customDesign for STYLE_CUSTOM
    fun getEffectiveDesign(): CustomDesignConfig {
        val style = clockStyle.value
        return if (style == ClockStyle.STYLE_CUSTOM) {
            customDesign.value
        } else {
            STYLE_TEMPLATES[style] ?: STYLE_TEMPLATES.getValue(ClockStyle.STYLE_RED_MOSQUE)
        }
    }
}
