package com.example.calculation

import com.example.model.AsrJuristic
import com.example.model.CalculationMethod
import com.example.model.CalculationSettings
import com.example.model.CustomTimesConfig
import com.example.model.Prayer
import com.example.model.PrayerDaySchedule
import com.example.model.PrayerTime
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.*

object PrayerCalculator {

    private fun d2r(d: Double): Double = d * Math.PI / 180.0
    private fun r2d(r: Double): Double = r * 180.0 / Math.PI

    private fun fixHour(a: Double): Double {
        var h = a - 24.0 * floor(a / 24.0)
        if (h < 0) h += 24.0
        return h
    }

    private fun fixAngle(a: Double): Double {
        var ang = a - 360.0 * floor(a / 360.0)
        if (ang < 0) ang += 360.0
        return ang
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private data class SunPosition(val declination: Double, val equationOfTime: Double)

    private fun computeSunPosition(jd: Double): SunPosition {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(d2r(g)) + 0.020 * sin(d2r(2 * g)))
        val e = 23.439 - 0.00000036 * d

        val sinL = sin(d2r(l))
        val cosL = cos(d2r(l))
        val sinE = sin(d2r(e))
        val cosE = cos(d2r(e))

        val raRad = atan2(cosE * sinL, cosL)
        var raHours = fixHour(r2d(raRad) / 15.0)

        val declination = r2d(asin(sinE * sinL))
        var eqTime = q / 15.0 - raHours
        if (eqTime > 12.0) eqTime -= 24.0
        if (eqTime < -12.0) eqTime += 24.0

        return SunPosition(declination, eqTime)
    }

    private fun sunHourAngle(angle: Double, latitude: Double, declination: Double): Double {
        val phi = d2r(latitude)
        val delta = d2r(declination)
        val cosH = (-sin(d2r(angle)) - sin(phi) * sin(delta)) / (cos(phi) * cos(delta))
        if (cosH > 1.0) return 0.0 // sun never rises to this angle
        if (cosH < -1.0) return 12.0 // sun never sets below this angle
        return r2d(acos(cosH)) / 15.0
    }

    private fun asrHourAngle(shadowRatio: Int, latitude: Double, declination: Double): Double {
        val phi = d2r(latitude)
        val delta = d2r(declination)
        val diff = abs(latitude - declination)
        val cotAngle = shadowRatio + tan(d2r(diff))
        val angleRad = atan(1.0 / cotAngle)
        val cosH = (sin(angleRad) - sin(phi) * sin(delta)) / (cos(phi) * cos(delta))
        if (cosH > 1.0) return 0.0
        if (cosH < -1.0) return 12.0
        return r2d(acos(cosH)) / 15.0
    }

    fun calculateSchedule(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        settings: CalculationSettings,
        customConfig: CustomTimesConfig? = null
    ): PrayerDaySchedule {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val tzOffsetHours = calendar.timeZone.getOffset(calendar.timeInMillis) / 3600000.0

        val dateKey = String.format("%04d-%02d-%02d", year, month, day)

        if (customConfig != null && customConfig.isCustomModeEnabled) {
            // Use Custom Times Mode
            val fajrTime = createPrayerTime(calendar, Prayer.FAJR, customConfig.fajrHour, customConfig.fajrMinute)
            // Sunrise can be standard calculated or 1h15m after Fajr
            val dhuhrTime = createPrayerTime(calendar, Prayer.DHUHR, customConfig.dhuhrHour, customConfig.dhuhrMinute)
            val asrTime = createPrayerTime(calendar, Prayer.ASR, customConfig.asrHour, customConfig.asrMinute)
            val maghribTime = createPrayerTime(calendar, Prayer.MAGHRIB, customConfig.maghribHour, customConfig.maghribMinute)
            val ishaTime = createPrayerTime(calendar, Prayer.ISHA, customConfig.ishaHour, customConfig.ishaMinute)

            // Approximate sunrise as 1h15m after Fajr for display
            val sunriseCal = (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, customConfig.fajrHour)
                set(Calendar.MINUTE, customConfig.fajrMinute)
                add(Calendar.MINUTE, 75)
            }
            val sunriseTime = PrayerTime(
                Prayer.SUNRISE,
                sunriseCal.get(Calendar.HOUR_OF_DAY),
                sunriseCal.get(Calendar.MINUTE),
                sunriseCal.timeInMillis
            )

            val list = listOf(fajrTime, sunriseTime, dhuhrTime, asrTime, maghribTime, ishaTime)
            return buildScheduleWithNext(calendar, dateKey, list)
        }

        // Automatic Astronomical Calculations
        val jd = julianDay(year, month, day)
        val sun = computeSunPosition(jd)

        // Solar Noon / Dhuhr transit
        val dhuhrTransit = fixHour(12.0 + tzOffsetHours - (longitude / 15.0) - sun.equationOfTime)

        val fajrAngle = settings.method.fajrAngle
        val fajrHA = sunHourAngle(fajrAngle, latitude, sun.declination)
        val sunriseHA = sunHourAngle(0.8333, latitude, sun.declination)
        val asrHA = asrHourAngle(settings.asrJuristic.shadowRatio, latitude, sun.declination)
        val maghribHA = sunriseHA // Sunset matches sunrise hour angle symmetrically

        val ishaAngle = settings.method.ishaAngle
        val ishaHA = if (ishaAngle > 0.0) {
            sunHourAngle(ishaAngle, latitude, sun.declination)
        } else {
            0.0 // Umm Al Qura
        }

        val rawFajr = fixHour(dhuhrTransit - fajrHA)
        val rawSunrise = fixHour(dhuhrTransit - sunriseHA)
        val rawDhuhr = fixHour(dhuhrTransit)
        val rawAsr = fixHour(dhuhrTransit + asrHA)
        val rawMaghrib = fixHour(dhuhrTransit + maghribHA)
        val rawIsha = if (ishaAngle > 0.0) {
            fixHour(dhuhrTransit + ishaHA)
        } else {
            fixHour(rawMaghrib + 1.5) // Umm Al Qura: 90 minutes after Maghrib
        }

        val fajrTime = hourFractionToPrayerTime(calendar, Prayer.FAJR, rawFajr, settings.fajrOffset)
        val sunriseTime = hourFractionToPrayerTime(calendar, Prayer.SUNRISE, rawSunrise, 0)
        val dhuhrTime = hourFractionToPrayerTime(calendar, Prayer.DHUHR, rawDhuhr, settings.dhuhrOffset)
        val asrTime = hourFractionToPrayerTime(calendar, Prayer.ASR, rawAsr, settings.asrOffset)
        val maghribTime = hourFractionToPrayerTime(calendar, Prayer.MAGHRIB, rawMaghrib, settings.maghribOffset)
        val ishaTime = hourFractionToPrayerTime(calendar, Prayer.ISHA, rawIsha, settings.ishaOffset)

        val list = listOf(fajrTime, sunriseTime, dhuhrTime, asrTime, maghribTime, ishaTime)
        return buildScheduleWithNext(calendar, dateKey, list)
    }

    private fun hourFractionToPrayerTime(
        calendar: Calendar,
        prayer: Prayer,
        hourFraction: Double,
        offsetMinutes: Int
    ): PrayerTime {
        val totalMinutes = (hourFraction * 60.0).roundToInt() + offsetMinutes
        val h = ((totalMinutes / 60) % 24 + 24) % 24
        val m = ((totalMinutes % 60) + 60) % 60
        return createPrayerTime(calendar, prayer, h, m)
    }

    private fun createPrayerTime(calendar: Calendar, prayer: Prayer, hour: Int, minute: Int): PrayerTime {
        val cal = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return PrayerTime(prayer, hour, minute, cal.timeInMillis)
    }

    private fun buildScheduleWithNext(
        currentCalendar: Calendar,
        dateKey: String,
        prayers: List<PrayerTime>
    ): PrayerDaySchedule {
        val nowMillis = currentCalendar.timeInMillis

        // Main prayers eligible for next prayer countdown: Fajr, Dhuhr, Asr, Maghrib, Isha
        val mainPrayers = prayers.filter { it.prayer.isMainPrayer }
        var next = mainPrayers.firstOrNull { it.timestampMillis > nowMillis }

        val millisUntilNext = if (next != null) {
            next.timestampMillis - nowMillis
        } else {
            // Next prayer is tomorrow's Fajr
            val todayFajr = prayers.first { it.prayer == Prayer.FAJR }
            val tomorrowFajrMillis = todayFajr.timestampMillis + 24 * 3600 * 1000L
            next = PrayerTime(
                Prayer.FAJR,
                todayFajr.hour,
                todayFajr.minute,
                tomorrowFajrMillis
            )
            tomorrowFajrMillis - nowMillis
        }

        return PrayerDaySchedule(
            dateKey = dateKey,
            prayers = prayers,
            nextPrayer = next,
            millisUntilNext = max(0L, millisUntilNext)
        )
    }
}
