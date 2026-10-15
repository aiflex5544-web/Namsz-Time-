package com.example

import com.example.calculation.HijriCalendarHelper
import com.example.calculation.PrayerCalculator
import com.example.calculation.QiblaCalculator
import com.example.model.CalculationMethod
import com.example.model.CalculationSettings
import com.example.model.CustomTimesConfig
import com.example.model.Prayer
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NoorPulseLogicTest {

    @Test
    fun testQiblaBearingKarachi() {
        // Karachi coordinates: 24.8607 N, 67.0011 E
        val bearing = QiblaCalculator.calculateBearing(24.8607, 67.0011)
        // Makkah is West-Northwest of Karachi, bearing around 268-270 degrees
        assertTrue("Karachi Qibla bearing should be between 260 and 275 degrees", bearing in 260.0..275.0)
    }

    @Test
    fun testQiblaDistanceToMakkah() {
        val distanceKm = QiblaCalculator.calculateDistanceKm(24.8607, 67.0011)
        // Karachi to Makkah distance is approx 2800-3500 km
        assertTrue("Distance should be around 2800-3500 km", distanceKm in 2800.0..3500.0)
    }

    @Test
    fun testPrayerCalculationKarachi() {
        val cal = Calendar.getInstance().apply {
            timeZone = TimeZone.getTimeZone("Asia/Karachi")
            set(2026, Calendar.OCTOBER, 15, 12, 0, 0)
        }
        val settings = CalculationSettings(method = CalculationMethod.KARACHI)
        val schedule = PrayerCalculator.calculateSchedule(cal, 24.8607, 67.0011, settings)

        assertNotNull(schedule)
        assertEquals(6, schedule.prayers.size) // Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha

        val fajr = schedule.prayers.first { it.prayer == Prayer.FAJR }
        val dhuhr = schedule.prayers.first { it.prayer == Prayer.DHUHR }
        val maghrib = schedule.prayers.first { it.prayer == Prayer.MAGHRIB }

        // Fajr is before Dhuhr, Dhuhr is before Maghrib
        assertTrue("Fajr must precede Dhuhr", fajr.hour < dhuhr.hour)
        assertTrue("Dhuhr must precede Maghrib", dhuhr.hour < maghrib.hour)
    }

    @Test
    fun testCustomTimesMode() {
        val cal = Calendar.getInstance()
        val custom = CustomTimesConfig(
            isCustomModeEnabled = true,
            fajrHour = 5,
            fajrMinute = 20,
            dhuhrHour = 12,
            dhuhrMinute = 45,
            asrHour = 16,
            asrMinute = 40,
            maghribHour = 18,
            maghribMinute = 25,
            ishaHour = 20,
            ishaMinute = 0
        )
        val schedule = PrayerCalculator.calculateSchedule(
            cal, 24.8607, 67.0011, CalculationSettings(), custom
        )

        val fajr = schedule.prayers.first { it.prayer == Prayer.FAJR }
        assertEquals(5, fajr.hour)
        assertEquals(20, fajr.minute)

        val isha = schedule.prayers.first { it.prayer == Prayer.ISHA }
        assertEquals(20, isha.hour)
        assertEquals(0, isha.minute)
    }

    @Test
    fun testHijriCalendarConversion() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15)
        }
        val hijri = HijriCalendarHelper.getHijriDate(cal)
        assertTrue("Hijri year should be around 1448 AH", hijri.year in 1447..1449)
        assertTrue("Hijri month must be between 1 and 12", hijri.month in 1..12)
        assertTrue("Hijri day must be between 1 and 30", hijri.day in 1..30)
        assertFalse("English month name should not be empty", hijri.englishMonthName.isEmpty())
        assertFalse("Urdu month name should not be empty", hijri.urduMonthName.isEmpty())
    }
}
