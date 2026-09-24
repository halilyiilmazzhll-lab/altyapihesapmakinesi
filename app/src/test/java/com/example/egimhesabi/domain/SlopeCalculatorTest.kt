package com.example.egimhesabi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlopeCalculatorTest {

    @Test
    fun calculate_withTurkishCommaInput_computesCorrectly() {
        val input = CalculationInput(
            baca1KapakKotu = "1164,00",
            baca1AkarKotu = "1162,56",
            baca2KapakKotu = "1164,50",
            baca2AkarKotu = "1162,84",
            mesafe = "34,00",
            minSlopePercent = 0.5,
            maxSlopePercent = 5.0
        )
        val result = SlopeCalculator.calculate(input)

        assertNotNull(result.slopePercent)
        assertNotNull(result.slopeRatio)
        // delta h = 0.28, mesafe = 34
        // slope = (0.28 / 34) * 100 = 0.823529...
        assertEquals(0.8235, result.slopePercent!!, 0.001)
        // ratio = 34 / 0.28 = 121.428...
        assertEquals(121.428, result.slopeRatio!!, 0.01)
        assertEquals(SlopeStatus.OK, result.slopeStatus)
        assertEquals(1, result.slopeDirection) // B2 is higher, flows B2 -> B1
        assertEquals(1.44, result.b1Derinlik!!, 0.01)
        assertEquals(1.66, result.b2Derinlik!!, 0.01)
        assertFalse(result.b1NegativeDepth)
        assertFalse(result.b2NegativeDepth)
        assertFalse(result.invalidDistance)
    }

    @Test
    fun calculate_withDotInput_computesCorrectly() {
        val input = CalculationInput(
            baca1KapakKotu = "100.0",
            baca1AkarKotu = "98.0",
            baca2KapakKotu = "100.0",
            baca2AkarKotu = "97.0",
            mesafe = "100.0",
            minSlopePercent = 0.5,
            maxSlopePercent = 5.0
        )
        val result = SlopeCalculator.calculate(input)

        // delta h = 1.0, mesafe = 100.0 -> slope = 1.0%
        assertEquals(1.0, result.slopePercent!!, 0.001)
        assertEquals(100.0, result.slopeRatio!!, 0.001)
        assertEquals(SlopeStatus.OK, result.slopeStatus)
        assertEquals(-1, result.slopeDirection) // B1 is higher, flows B1 -> B2
    }

    @Test
    fun calculate_withFlatLine_returnsZeroSlopeAndTooLow() {
        val input = CalculationInput(
            baca1AkarKotu = "100.00",
            baca2AkarKotu = "100.00",
            mesafe = "50.00",
            minSlopePercent = 0.5,
            maxSlopePercent = 5.0
        )
        val result = SlopeCalculator.calculate(input)

        assertEquals(0.0, result.slopePercent!!, 0.001)
        assertNull(result.slopeRatio)
        assertEquals(0.0, result.heightDiff!!, 0.001)
        assertEquals(SlopeStatus.TOO_LOW, result.slopeStatus)
        assertEquals(0, result.slopeDirection)
    }

    @Test
    fun calculate_withZeroOrNegativeDistance_flagsInvalidDistance() {
        val zeroDistInput = CalculationInput(
            baca1AkarKotu = "100.00",
            baca2AkarKotu = "99.00",
            mesafe = "0"
        )
        val zeroResult = SlopeCalculator.calculate(zeroDistInput)
        assertTrue(zeroResult.invalidDistance)
        assertNull(zeroResult.slopePercent)

        val negDistInput = CalculationInput(
            baca1AkarKotu = "100.00",
            baca2AkarKotu = "99.00",
            mesafe = "-15.5"
        )
        val negResult = SlopeCalculator.calculate(negDistInput)
        assertTrue(negResult.invalidDistance)
        assertNull(negResult.slopePercent)
    }

    @Test
    fun calculate_withNegativeDepth_flagsWarning() {
        val input = CalculationInput(
            baca1KapakKotu = "98.00",
            baca1AkarKotu = "100.00", // Kapak is below akar
            baca2KapakKotu = "105.00",
            baca2AkarKotu = "100.00",
            mesafe = "50.0"
        )
        val result = SlopeCalculator.calculate(input)

        assertTrue(result.b1NegativeDepth)
        assertFalse(result.b2NegativeDepth)
        assertEquals(-2.0, result.b1Derinlik!!, 0.001)
    }

    @Test
    fun calculate_slopeLimits_boundaries() {
        // Exact min limit: 0.5%
        val minInput = CalculationInput(
            baca1AkarKotu = "100.50",
            baca2AkarKotu = "100.00",
            mesafe = "100.0",
            minSlopePercent = 0.5,
            maxSlopePercent = 5.0
        )
        val minResult = SlopeCalculator.calculate(minInput)
        assertEquals(0.5, minResult.slopePercent!!, 0.001)
        assertEquals(SlopeStatus.NEAR_LIMIT, minResult.slopeStatus)

        // Below min limit: 0.2%
        val lowInput = CalculationInput(
            baca1AkarKotu = "100.20",
            baca2AkarKotu = "100.00",
            mesafe = "100.0",
            minSlopePercent = 0.5,
            maxSlopePercent = 5.0
        )
        val lowResult = SlopeCalculator.calculate(lowInput)
        assertEquals(SlopeStatus.TOO_LOW, lowResult.slopeStatus)

        // Above max limit: 6.0%
        val highInput = CalculationInput(
            baca1AkarKotu = "106.00",
            baca2AkarKotu = "100.00",
            mesafe = "100.0",
            minSlopePercent = 0.5,
            maxSlopePercent = 5.0
        )
        val highResult = SlopeCalculator.calculate(highInput)
        assertEquals(SlopeStatus.TOO_HIGH, highResult.slopeStatus)
    }

    @Test
    fun calculate_withInvertedLimits_handlesSafely() {
        // min = 5.0, max = 0.5 (reversed)
        val input = CalculationInput(
            baca1AkarKotu = "101.00",
            baca2AkarKotu = "100.00",
            mesafe = "100.0",
            minSlopePercent = 5.0,
            maxSlopePercent = 0.5
        )
        val result = SlopeCalculator.calculate(input)
        assertEquals(1.0, result.slopePercent!!, 0.001)
        assertEquals(SlopeStatus.OK, result.slopeStatus)
    }

    @Test
    fun calculate_withMalformedStrings_doesNotCrash() {
        val input = CalculationInput(
            baca1KapakKotu = "abc",
            baca1AkarKotu = "12..34",
            baca2KapakKotu = "NaN",
            baca2AkarKotu = "Infinity",
            mesafe = "---"
        )
        val result = SlopeCalculator.calculate(input)
        assertNull(result.slopePercent)
        assertEquals(SlopeStatus.UNKNOWN, result.slopeStatus)
    }

    @Test
    fun conversions_percentAndRatio() {
        assertEquals(200.0, SlopeCalculator.percentToRatio(0.5), 0.001)
        assertEquals(20.0, SlopeCalculator.percentToRatio(5.0), 0.001)
        assertEquals(0.5, SlopeCalculator.ratioToPercent(200.0), 0.001)
        assertEquals(5.0, SlopeCalculator.ratioToPercent(20.0), 0.001)
    }
}
