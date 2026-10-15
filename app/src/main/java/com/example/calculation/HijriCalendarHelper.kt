package com.example.calculation

import java.util.Calendar
import kotlin.math.floor

data class HijriDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val englishMonthName: String,
    val urduMonthName: String
) {
    fun formatEnglish(): String = "$day $englishMonthName $year AH"
    fun formatUrdu(): String = "$day $urduMonthName $year ھ"
    fun format(isUrdu: Boolean): String = if (isUrdu) formatUrdu() else formatEnglish()
}

object HijriCalendarHelper {

    private val englishMonths = listOf(
        "Muharram", "Safar", "Rabi\' al-Awwal", "Rabi\' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha\'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi\'dah", "Dhu al-Hijjah"
    )

    private val urduMonths = listOf(
        "محرم الحرام", "صفر المظفر", "ربیع الاول", "ربیع الثانی",
        "جمادی الاول", "جمادی الثانی", "رجب المرجب", "شعبان المعظم",
        "رمضان المبارک", "شوال المکرم", "ذوالقعدہ", "ذوالحجہ"
    )

    fun getHijriDate(calendar: Calendar, offsetDays: Int = 0): HijriDate {
        val cal = (calendar.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, offsetDays)
        }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)

        val jd = gregorianToJulianDay(y, m, d)
        return julianDayToHijri(jd)
    }

    private fun gregorianToJulianDay(year: Int, month: Int, day: Int): Double {
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

    private fun julianDayToHijri(jd: Double): HijriDate {
        val l = (floor(jd - 1948440 + 10632)).toInt()
        val n = (l - 1) / 10631
        val l2 = l - 10631 * n + 354
        val j = (((10985 - l2) / 5316) * ((50 * l2) / 17719)) + (((l2 / 5670)) * ((43 * l2) / 15238))
        val l3 = l2 - (((30 - j) / 15) * ((17719 * j) / 50)) - ((j / 16) * ((15238 * j) / 43)) + 29
        val month = ((24 * l3) / 709)
        val day = l3 - ((709 * month) / 24)
        val year = 30 * n + j - 30

        val safeMonth = maxOf(1, minOf(12, month))
        val safeDay = maxOf(1, minOf(30, day))

        return HijriDate(
            day = safeDay,
            month = safeMonth,
            year = year,
            englishMonthName = englishMonths[safeMonth - 1],
            urduMonthName = urduMonths[safeMonth - 1]
        )
    }
}
