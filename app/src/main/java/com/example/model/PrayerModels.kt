package com.example.model

enum class Prayer(
    val englishName: String,
    val urduName: String,
    val isMainPrayer: Boolean = true
) {
    FAJR("Fajr", "فجر"),
    SUNRISE("Sunrise", "طلوعِ آفتاب", isMainPrayer = false),
    DHUHR("Dhuhr", "ظہر"),
    ASR("Asr", "عصر"),
    MAGHRIB("Maghrib", "مغرب"),
    ISHA("Isha", "عشاء")
}

data class PrayerTime(
    val prayer: Prayer,
    val hour: Int,
    val minute: Int,
    val timestampMillis: Long
) {
    fun format12Hour(): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val m = String.format("%02d", minute)
        val amPm = if (hour < 12) "AM" else "PM"
        return "$h:$m $amPm"
    }

    fun format24Hour(): String {
        return String.format("%02d:%02d", hour, minute)
    }

    fun format(use24Hour: Boolean): String = if (use24Hour) format24Hour() else format12Hour()
}

data class PrayerDaySchedule(
    val dateKey: String, // YYYY-MM-DD
    val prayers: List<PrayerTime>,
    val nextPrayer: PrayerTime?,
    val millisUntilNext: Long
)

enum class ClockStyle(val displayName: String, val urduName: String) {
    STYLE_RED_MOSQUE("Red Mosque LED", "سرخ مسجد ایل ای ڈی"),
    STYLE_GREEN_ISLAMIC("Green Islamic Clock", "سبز اسلامی گھڑی"),
    STYLE_PREMIUM_WHITE("Premium White", "پریمیم وائٹ"),
    STYLE_NEON_DIGITAL("Neon Digital", "نیون ڈیجیٹل"),
    STYLE_CUSTOM("Custom Design", "اپنی مرضی کا ڈیزائن")
}

enum class PrayerRowSpacing(val displayName: String, val dpValue: Int) {
    COMPACT("Compact", 4),
    COMFORTABLE("Comfortable", 8),
    SPACIOUS("Spacious", 14)
}

data class CustomDesignConfig(
    val backgroundColorHex: Long = 0xFF000000,
    val clockNumberColorHex: Long = 0xFFFF2222,
    val prayerNameColorHex: Long = 0xFFFFD700,
    val prayerTimeColorHex: Long = 0xFFFFFFFF,
    val countdownColorHex: Long = 0xFFFF5555,
    val highlightColorHex: Long = 0xFF330000,
    val fontSizeScale: Float = 1.0f,
    val clockSizeScale: Float = 1.0f,
    val spacing: PrayerRowSpacing = PrayerRowSpacing.COMFORTABLE,
    val brightness: Float = -1.0f, // -1 means system default
    val use24Hour: Boolean = false,
    val showSeconds: Boolean = true,
    val showHijri: Boolean = true,
    val showCountdown: Boolean = true,
    val compactTimetable: Boolean = false
)

data class LocationData(
    val cityName: String = "Karachi",
    val countryName: String = "Pakistan",
    val latitude: Double = 24.8607,
    val longitude: Double = 67.0011,
    val isAutoDetected: Boolean = false
)

enum class CalculationMethod(val displayName: String, val fajrAngle: Double, val ishaAngle: Double) {
    KARACHI("University of Islamic Sciences, Karachi", 18.0, 18.0),
    MWL("Muslim World League (MWL)", 18.0, 17.0),
    ISNA("Islamic Society of North America (ISNA)", 15.0, 15.0),
    UMM_AL_QURA("Umm Al-Qura University, Makkah", 18.5, 0.0), // 90 min after maghrib
    EGYPT("Egyptian General Authority of Survey", 19.5, 17.5)
}

enum class AsrJuristic(val displayName: String, val shadowRatio: Int) {
    HANAFI("Hanafi (Standard in Pakistan/South Asia)", 2),
    SHAFI("Shafi'i / Maliki / Hanbali", 1)
}

data class CalculationSettings(
    val method: CalculationMethod = CalculationMethod.KARACHI,
    val asrJuristic: AsrJuristic = AsrJuristic.HANAFI,
    val fajrOffset: Int = 0,
    val dhuhrOffset: Int = 0,
    val asrOffset: Int = 0,
    val maghribOffset: Int = 0,
    val ishaOffset: Int = 0,
    val hijriOffsetDays: Int = 0
)

enum class AdhanSoundType(val displayName: String, val resourceId: Int?) {
    FULL_ADHAN("Harmonic Adhan Call", 1),
    SHORT_TAKBEER("Short Takbeer Chime", 2),
    BEEP_ALERT("Gentle Beep Alert", 3),
    SYSTEM_RINGTONE("System Alarm Sound", null)
}

data class AdhanSettings(
    val globalEnabled: Boolean = true,
    val fajrEnabled: Boolean = true,
    val dhuhrEnabled: Boolean = true,
    val asrEnabled: Boolean = true,
    val maghribEnabled: Boolean = true,
    val ishaEnabled: Boolean = true,
    val soundType: AdhanSoundType = AdhanSoundType.FULL_ADHAN,
    val customAudioUri: String? = null,
    val vibrateOnly: Boolean = false
) {
    fun isPrayerEnabled(prayer: Prayer): Boolean = when (prayer) {
        Prayer.FAJR -> fajrEnabled && globalEnabled
        Prayer.DHUHR -> dhuhrEnabled && globalEnabled
        Prayer.ASR -> asrEnabled && globalEnabled
        Prayer.MAGHRIB -> maghribEnabled && globalEnabled
        Prayer.ISHA -> ishaEnabled && globalEnabled
        Prayer.SUNRISE -> false
    }
}

data class CustomTimesConfig(
    val isCustomModeEnabled: Boolean = false,
    val fajrHour: Int = 5,
    val fajrMinute: Int = 15,
    val dhuhrHour: Int = 12,
    val dhuhrMinute: Int = 30,
    val asrHour: Int = 16,
    val asrMinute: Int = 30,
    val maghribHour: Int = 18,
    val maghribMinute: Int = 15,
    val ishaHour: Int = 19,
    val ishaMinute: Int = 30
)
