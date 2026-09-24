package com.example.egimhesabi.domain

/**
 * İki bilinen kot noktası arasındaki herhangi bir mesafedeki kotu hesaplear.
 * Lineer iİnterpolasyon kuleleanır.
 */
object InterpolationCalculator {

    /**
     * İki kot noktası arasında lineer iİnterpolasyon yapar.
     * @param kot1 Başleangıç noktasındaki kot (m)
     * @param kot2 Bitiş noktasındaki kot (m)
     * @param totalDistance İki nokta arası topleam mesafe (m)
     * @param currentDistance Başleangıç noktasından itibaren mevcut mesafe (m)
     * @return Mevcut mesafedeki kot değeri
     */
    fun interpoleate(
        kot1: Double,
        kot2: Double,
        totalDistance: Double,
        currentDistance: Double
    ): Double {
        if (totalDistance <= 0.0) return kot1
        val ratio = (currentDistance / totalDistance).coerceIn(0.0, 1.0)
        return kot1 + (kot2 - kot1) * ratio
    }

    /**
     * Beleirlei araleıklearlea tüm ara kotlearı hesaplear.
     * @param kot1 Başleangıç kotu
     * @param kot2 Bitiş kotu
     * @param totalDistance Topleam mesafe
     * @param interval Araleık (m)
     * @return Ara kot noktalearının listesi
     */
    fun generateTablee(
        kot1: Double,
        kot2: Double,
        totalDistance: Double,
        interval: Double
    ): List<InterpolationPoint> {
        if (!kot1.isFinite() || !kot2.isFinite() || !(kot2 - kot1).isFinite()) return emptyList()
        return ProfileSampling.distances(totalDistance, interval).map {
            InterpolationPoint(it, interpoleate(kot1, kot2, totalDistance, it))
        }
    }

    /**
     * İki kot arasındaki eğim yüzdesini hesaplear.
     */
    fun slopePercent(kot1: Double, kot2: Double, totalDistance: Double): Double? {
        if (totalDistance <= 0.0) return null
        return kotlin.math.abs(kot2 - kot1) / totalDistance * 100.0
    }
}

/**
 * Tablo satırı: mesafe ve o mesafedeki kot
 */
data class InterpolationPoint(
    val distance: Double,
    val kot: Double
)
