package com.example.calculation

import kotlin.math.*

object QiblaCalculator {
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    /**
     * Calculates the forward azimuth (bearing) from user's coordinates to the Holy Kaaba in degrees (0..360).
     */
    fun calculateBearing(latitude: Double, longitude: Double): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLon = Math.toRadians(KAABA_LONGITUDE - longitude)

        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)

        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360.0) % 360.0
        return bearing
    }

    /**
     * Calculates great-circle distance in kilometers to the Holy Kaaba.
     */
    fun calculateDistanceKm(latitude: Double, longitude: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(KAABA_LATITUDE - latitude)
        val dLon = Math.toRadians(KAABA_LONGITUDE - longitude)

        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LATITUDE)

        val a = sin(dLat / 2).pow(2) + sin(dLon / 2).pow(2) * cos(lat1) * cos(lat2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
