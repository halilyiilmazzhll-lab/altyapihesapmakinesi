package com.example.egimhesabi.domain

import kotlin.math.ceil

/** Bounds both allocation and work before any profile points are created. */
object ProfileSampling {
    const val MAX_POINTS = 15_001

    fun distances(totalDistance: Double, interval: Double): List<Double> {
        if (!totalDistance.isFinite() || !interval.isFinite() || totalDistance <= 0 || interval <= 0) return emptyList()
        val segments = ceil(totalDistance / interval)
        if (!segments.isFinite() || segments < 1 || segments >= MAX_POINTS) return emptyList()
        return (0..segments.toInt()).map { (it * interval).coerceAtMost(totalDistance) }
    }
}
