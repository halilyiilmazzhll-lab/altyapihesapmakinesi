package com.example.egimhesabi.domain

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

const val DEFAULT_PIPE_LENGTH_METERS = 1.5
const val PIPE_TOP_OFFSET_METERS = 0.40
const val CORRECTION_DISPLAY_EPSILON_METERS = 0.0005

data class PipeLevelPoint(
    val pipeNumber: Int,
    val segmentLength: Double,
    val distance: Double,
    val designElevation: Double,
    val expectedStaffReading: Double?
)

data class MeterLevelPoint(
    val distance: Double,
    val designElevation: Double,
    val expectedStaffReading: Double?
)

enum class CorrectionDirection {
    UP,
    DOWN,
    ON_GRADE
}

data class LevelCorrection(
    val direction: CorrectionDirection,
    val amountMeters: Double,
    val signedDifferenceMeters: Double
)

/**
 * RS baglantisi, proje hatti ve sahadaki boru kot kontrolu hesaplari.
 */
object LevelingCalculator {
    private const val DISTANCE_EPSILON = 1e-9
    private const val MAX_PIPE_COUNT = 10_000
    private const val MAX_METER_INTERVAL_COUNT = 15_000

    /** RS kotu + RS geri okumasi = gozleme duzlemi (alet) kotu. */
    fun instrumentElevation(referenceElevation: Double, backsightReading: Double): Double? {
        if (!referenceElevation.isFinite() ||
            !backsightReading.isFinite() ||
            backsightReading < 0.0
        ) {
            return null
        }

        return (referenceElevation + backsightReading).takeIf { it.isFinite() }
    }

    /** Alet kotu - okunan mira = sahada gorulen noktanin kotu. */
    fun pointElevation(instrumentElevation: Double, staffReading: Double): Double? {
        if (!instrumentElevation.isFinite() ||
            !staffReading.isFinite() ||
            staffReading < 0.0
        ) {
            return null
        }

        return (instrumentElevation - staffReading).takeIf { it.isFinite() }
    }

    /** Geri okuma - hedef okuma = referansa gore kot farki. */
    fun heightDifference(backsightReading: Double, staffReading: Double): Double? {
        if (!backsightReading.isFinite() ||
            !staffReading.isFinite() ||
            backsightReading < 0.0 ||
            staffReading < 0.0
        ) {
            return null
        }

        return (backsightReading - staffReading).takeIf { it.isFinite() }
    }

    /** Baca 1'den itibaren verilen mesafedeki dogrusal proje akar kotu. */
    fun designElevation(
        startElevation: Double,
        endElevation: Double,
        totalDistance: Double,
        distanceFromStart: Double
    ): Double? {
        if (!startElevation.isFinite() ||
            !endElevation.isFinite() ||
            !totalDistance.isFinite() ||
            !distanceFromStart.isFinite() ||
            totalDistance <= 0.0 ||
            distanceFromStart < -DISTANCE_EPSILON ||
            distanceFromStart > totalDistance + DISTANCE_EPSILON
        ) {
            return null
        }

        val position = distanceFromStart.coerceIn(0.0, totalDistance)
        val result = startElevation + (endElevation - startElevation) * (position / totalDistance)
        return result.takeIf { it.isFinite() }
    }

    /** Normal dik mirayla okunmasi gereken hedef mira degeri. */
    fun expectedStaffReading(instrumentElevation: Double, designElevation: Double): Double? {
        if (!instrumentElevation.isFinite() || !designElevation.isFinite()) return null
        return (instrumentElevation - designElevation).takeIf { it.isFinite() }
    }

    /** Mira temas noktasinin, proje akar kotuna gore kotu. */
    fun staffContactElevation(invertElevation: Double, contactOffset: Double): Double? {
        if (!invertElevation.isFinite() || !contactOffset.isFinite()) return null
        return (invertElevation + contactOffset).takeIf { it.isFinite() }
    }

    /** Hat uzunlugunu 1,50 m'lik borulara ayirir; son kisa parcayi da sayar. */
    fun pipeCount(
        totalDistance: Double,
        pipeLength: Double = DEFAULT_PIPE_LENGTH_METERS
    ): Int? {
        if (!totalDistance.isFinite() ||
            !pipeLength.isFinite() ||
            totalDistance <= 0.0 ||
            pipeLength <= 0.0
        ) {
            return null
        }

        val rawCount = totalDistance / pipeLength
        val nearestWhole = round(rawCount)
        val count = if (abs(rawCount - nearestWhole) <= DISTANCE_EPSILON) {
            nearestWhole.toLong()
        } else {
            ceil(rawCount).toLong()
        }

        if (count !in 1..MAX_PIPE_COUNT.toLong()) return null
        return count.toInt()
    }

    fun generatePipePoints(
        startElevation: Double,
        endElevation: Double,
        totalDistance: Double,
        instrumentElevation: Double?,
        pipeLength: Double = DEFAULT_PIPE_LENGTH_METERS,
        staffContactOffset: Double = 0.0
    ): List<PipeLevelPoint> {
        val count = pipeCount(totalDistance, pipeLength) ?: return emptyList()

        return (1..count).mapNotNull { pipeNumber ->
            val startDistance = (pipeNumber - 1) * pipeLength
            val endDistance = if (pipeNumber == count) {
                totalDistance
            } else {
                (pipeNumber * pipeLength).coerceAtMost(totalDistance)
            }
            val designElevation = designElevation(
                startElevation = startElevation,
                endElevation = endElevation,
                totalDistance = totalDistance,
                distanceFromStart = endDistance
            ) ?: return@mapNotNull null

            PipeLevelPoint(
                pipeNumber = pipeNumber,
                segmentLength = endDistance - startDistance,
                distance = endDistance,
                designElevation = designElevation,
                expectedStaffReading = instrumentElevation?.let { instrument ->
                    staffContactElevation(designElevation, staffContactOffset)?.let { contactElevation ->
                        expectedStaffReading(instrument, contactElevation)
                    }
                }
            )
        }
    }

    fun generateMeterPoints(
        startElevation: Double,
        endElevation: Double,
        totalDistance: Double,
        instrumentElevation: Double?,
        interval: Double = 1.0,
        staffContactOffset: Double = 0.0
    ): List<MeterLevelPoint> {
        if (!interval.isFinite() || interval <= 0.0 || !totalDistance.isFinite() || totalDistance <= 0.0) {
            return emptyList()
        }

        val wholeIntervalCount = floor(totalDistance / interval).toLong()
        if (wholeIntervalCount !in 0..MAX_METER_INTERVAL_COUNT.toLong()) return emptyList()

        val points = mutableListOf<MeterLevelPoint>()
        for (index in 0..wholeIntervalCount) {
            val distance = index * interval
            val elevation = designElevation(startElevation, endElevation, totalDistance, distance)
                ?: return emptyList()
            points += MeterLevelPoint(
                distance = distance,
                designElevation = elevation,
                expectedStaffReading = instrumentElevation?.let { instrument ->
                    staffContactElevation(elevation, staffContactOffset)?.let { contactElevation ->
                        expectedStaffReading(instrument, contactElevation)
                    }
                }
            )
        }

        val lastDistance = points.lastOrNull()?.distance
        if (lastDistance == null || abs(lastDistance - totalDistance) > DISTANCE_EPSILON) {
            val elevation = designElevation(startElevation, endElevation, totalDistance, totalDistance)
                ?: return emptyList()
            points += MeterLevelPoint(
                distance = totalDistance,
                designElevation = elevation,
                expectedStaffReading = instrumentElevation?.let { instrument ->
                    staffContactElevation(elevation, staffContactOffset)?.let { contactElevation ->
                        expectedStaffReading(instrument, contactElevation)
                    }
                }
            )
        }

        return points
    }

    /**
     * Pozitif fark yukari, negatif fark asagi hareket ettirmek gerektigini gosterir.
     */
    fun correction(
        designElevation: Double,
        actualElevation: Double,
        displayEpsilonMeters: Double = CORRECTION_DISPLAY_EPSILON_METERS
    ): LevelCorrection? {
        if (!designElevation.isFinite() ||
            !actualElevation.isFinite() ||
            !displayEpsilonMeters.isFinite() ||
            displayEpsilonMeters < 0.0
        ) {
            return null
        }

        val difference = designElevation - actualElevation
        if (!difference.isFinite()) return null

        val direction = when {
            difference > displayEpsilonMeters -> CorrectionDirection.UP
            difference < -displayEpsilonMeters -> CorrectionDirection.DOWN
            else -> CorrectionDirection.ON_GRADE
        }
        return LevelCorrection(
            direction = direction,
            amountMeters = abs(difference),
            signedDifferenceMeters = difference
        )
    }
}
