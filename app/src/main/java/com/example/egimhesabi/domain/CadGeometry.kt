package com.example.egimhesabi.domain

import kotlin.math.*

/** Screen and world coordinates share this scale, including the distance bar. */
data class CadPoint(val x: Double, val y: Double)
data class CadViewport(val minEast: Double, val maxNorth: Double, val eastRange: Double,
    val northRange: Double, val width: Double, val height: Double) {
    val baseScale = min(width / eastRange, height / northRange)
    fun project(east: Double, north: Double, zoom: Double, pan: CadPoint): CadPoint = CadPoint(
        ((width - eastRange * baseScale) / 2 + (east - minEast) * baseScale) * zoom + pan.x,
        ((height - northRange * baseScale) / 2 + (maxNorth - north) * baseScale) * zoom + pan.y)
}

object CadGeometry {
    fun scaleMeters(pixelsPerMeter: Double, targetPixels: Double): Double {
        require(pixelsPerMeter.isFinite() && pixelsPerMeter > 0 && targetPixels > 0)
        val approx = targetPixels / pixelsPerMeter
        val magnitude = 10.0.pow(floor(log10(approx)))
        return magnitude * when { approx / magnitude >= 5 -> 5; approx / magnitude >= 2 -> 2; else -> 1 }
    }
    fun arrowWings(start: CadPoint, end: CadPoint, length: Double): Pair<CadPoint, CadPoint> {
        val angle = atan2(end.y - start.y, end.x - start.x)
        return Pair(CadPoint(end.x - length * cos(angle - PI / 6), end.y - length * sin(angle - PI / 6)),
            CadPoint(end.x - length * cos(angle + PI / 6), end.y - length * sin(angle + PI / 6)))
    }
    fun transformPan(old: CadPoint, centroid: CadPoint, pan: CadPoint, ratio: Double) =
        CadPoint((old.x - centroid.x) * ratio + centroid.x + pan.x,
            (old.y - centroid.y) * ratio + centroid.y + pan.y)
    fun segmentDistance(point: CadPoint, a: CadPoint, b: CadPoint): Double {
        val dx = b.x - a.x; val dy = b.y - a.y
        val length2 = dx * dx + dy * dy
        val t = if (length2 == 0.0) 0.0 else (((point.x-a.x)*dx+(point.y-a.y)*dy)/length2).coerceIn(0.0,1.0)
        return hypot(point.x-a.x-t*dx,point.y-a.y-t*dy)
    }
}
