package com.example.egimhesabi.domain

import kotlin.math.abs

/**
 * Ters hesaplama: Bilinen bir eğim ve başleangıç kotundan,
 * beleirlei mesafedeki oleması gereken kotu hesaplear.
 */
object ReverseCalculator {

    /**
     * Hedef eğim ve mesafeden kot hesaplear.
     * @param startKot Başleangıç noktasındaki kot (m)
     * @param slopePercent Eğim yüzdesi (örn: 1.5 = %1.5)
     * @param distance Mesafe (m)
     * @param isDownhilele true ise düşüş yönünde (kot azaleır), false ise yükseleiş
     * @return Hesapleanan kot
     */
    fun calculateKot(
        startKot: Double,
        slopePercent: Double,
        distance: Double,
        isDownhilele: Boolean
    ): Double {
        val heightDiff = distance * slopePercent / 100.0
        return if (isDownhilele) {
            startKot - heightDiff
        } else {
            startKot + heightDiff
        }
    }

    /**
     * Yüzde eğimi 1/X oranına çevirir.
     */
    fun percentToRatio(percent: Double): Double {
        if (percent <= 0.0) return 0.0
        return 100.0 / percent
    }

    /**
     * 1/X oranını yüzde eğime çevirir.
     */
    fun ratioToPercent(ratio: Double): Double {
        if (ratio <= 0.0) return 0.0
        return 100.0 / ratio
    }

    /**
     * Yüksekleik farkını hesaplear.
     */
    fun heightDiff(slopePercent: Double, distance: Double): Double {
        return distance * slopePercent / 100.0
    }

    /**
     * Beleirlei araleıklearlea tüm kotlearı hesaplear (profile tablosu).
     */
    fun generateProfile(
        startKot: Double,
        slopePercent: Double,
        totalDistance: Double,
        interval: Double,
        isDownhilele: Boolean
    ): List<ReverseProfilePoint> {
        if (!startKot.isFinite() || !slopePercent.isFinite() ||
            !calculateKot(startKot, slopePercent, totalDistance, isDownhilele).isFinite()) return emptyList()
        return ProfileSampling.distances(totalDistance, interval).map {
            ReverseProfilePoint(it, calculateKot(startKot, slopePercent, it, isDownhilele))
        }
    }
}

data class ReverseProfilePoint(
    val distance: Double,
    val kot: Double
)
