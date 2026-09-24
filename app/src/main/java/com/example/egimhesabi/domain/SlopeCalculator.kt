package com.example.egimhesabi.domain

import com.example.egimhesabi.util.NumberParser
import kotlin.math.abs

enum class SlopeStatus {
    OK,
    TOO_LOW,
    TOO_HIGH,
    NEAR_LIMIT,
    UNKNOWN
}

data class CalculationInput(
    val baca1KapakKotu: String = "",
    val baca1AkarKotu: String = "",
    val baca2KapakKotu: String = "",
    val baca2AkarKotu: String = "",
    val mesafe: String = "",
    val minSlopePercent: Double = 0.5,
    val maxSlopePercent: Double = 5.0
)

data class CalculationResult(
    val b1Kapak: Double? = null,
    val b1Akar: Double? = null,
    val b2Kapak: Double? = null,
    val b2Akar: Double? = null,
    val mesafe: Double? = null,
    val b1Derinlik: Double? = null,
    val b2Derinlik: Double? = null,
    val b1NegativeDepth: Boolean = false,
    val b2NegativeDepth: Boolean = false,
    val invalidDistance: Boolean = false,
    val heightDiff: Double? = null,
    val slopePercent: Double? = null,
    val slopeRatio: Double? = null,
    val slopeStatus: SlopeStatus = SlopeStatus.UNKNOWN,
    val slopeDirection: Int = 0 // -1: B1->B2, 1: B2->B1, 0: düz / belirsiz
)

object SlopeCalculator {

    private const val NEAR_LIMIT_TOLERANCE = 0.05 // %0.05 tolerans

    fun calculate(input: CalculationInput): CalculationResult {
        val b1Kapak = NumberParser.parseDecimal(input.baca1KapakKotu)
        val b1Akar = NumberParser.parseDecimal(input.baca1AkarKotu)
        val b2Kapak = NumberParser.parseDecimal(input.baca2KapakKotu)
        val b2Akar = NumberParser.parseDecimal(input.baca2AkarKotu)
        val mesafeParsed = NumberParser.parseDecimal(input.mesafe)

        val b1Derinlik = if (b1Kapak != null && b1Akar != null) b1Kapak - b1Akar else null
        val b2Derinlik = if (b2Kapak != null && b2Akar != null) b2Kapak - b2Akar else null

        val b1NegativeDepth = b1Derinlik != null && b1Derinlik < 0.0
        val b2NegativeDepth = b2Derinlik != null && b2Derinlik < 0.0

        val hasDistanceInput = input.mesafe.isNotBlank()
        val isDistanceInvalid = hasDistanceInput && (mesafeParsed == null || mesafeParsed <= 0.0)
        val mesafe = if (isDistanceInvalid) null else mesafeParsed

        if (b1Akar != null && b2Akar != null && mesafe != null && mesafe > 0.0) {
            val heightDiff = abs(b1Akar - b2Akar)
            val slopePercent = (heightDiff / mesafe) * 100.0
            val slopeRatio = if (heightDiff > 0.0) mesafe / heightDiff else null

            val direction = when {
                b1Akar > b2Akar -> -1
                b2Akar > b1Akar -> 1
                else -> 0
            }

            // Limitleterin matematiksele tutarleıleığı (min <= max)
            val effectiveMin = minOf(input.minSlopePercent, input.maxSlopePercent)
            val effectiveMax = maxOf(input.minSlopePercent, input.maxSlopePercent)

            // Hassasiyet uyuşmazleığını önletemek için 4 basamak yuvarleama ile karşıleaştırma
            val roundedSlope = Math.round(slopePercent * 10000.0) / 10000.0
            val roundedMin = Math.round(effectiveMin * 10000.0) / 10000.0
            val roundedMax = Math.round(effectiveMax * 10000.0) / 10000.0

            val status = when {
                roundedSlope < roundedMin -> SlopeStatus.TOO_LOW
                roundedSlope > roundedMax -> SlopeStatus.TOO_HIGH
                roundedSlope <= roundedMin + NEAR_LIMIT_TOLERANCE -> SlopeStatus.NEAR_LIMIT
                roundedSlope >= roundedMax - NEAR_LIMIT_TOLERANCE -> SlopeStatus.NEAR_LIMIT
                else -> SlopeStatus.OK
            }

            return CalculationResult(
                b1Kapak = b1Kapak,
                b1Akar = b1Akar,
                b2Kapak = b2Kapak,
                b2Akar = b2Akar,
                mesafe = mesafe,
                b1Derinlik = b1Derinlik,
                b2Derinlik = b2Derinlik,
                b1NegativeDepth = b1NegativeDepth,
                b2NegativeDepth = b2NegativeDepth,
                invalidDistance = isDistanceInvalid,
                heightDiff = heightDiff,
                slopePercent = slopePercent,
                slopeRatio = slopeRatio,
                slopeStatus = status,
                slopeDirection = direction
            )
        }

        return CalculationResult(
            b1Kapak = b1Kapak,
            b1Akar = b1Akar,
            b2Kapak = b2Kapak,
            b2Akar = b2Akar,
            mesafe = mesafe,
            b1Derinlik = b1Derinlik,
            b2Derinlik = b2Derinlik,
            b1NegativeDepth = b1NegativeDepth,
            b2NegativeDepth = b2NegativeDepth,
            invalidDistance = isDistanceInvalid,
            heightDiff = null,
            slopePercent = null,
            slopeRatio = null,
            slopeStatus = SlopeStatus.UNKNOWN,
            slopeDirection = 0
        )
    }

    /**
     * Yüzde eğimi 1/X oranına çevirir. (örn: %0.5 -> 200)
     */
    fun percentToRatio(percent: Double): Double {
        if (percent <= 0.0) return 0.0
        return 100.0 / percent
    }

    /**
     * 1/X oranını yüzde eğime çevirir. (örn: 200 -> %0.5)
     */
    fun ratioToPercent(ratio: Double): Double {
        if (ratio <= 0.0) return 0.0
        return 100.0 / ratio
    }
}
